package vn.saodo.appchongluadao.data.model

import android.content.Context
import org.tensorflow.lite.Interpreter
import vn.saodo.appchongluadao.domain.model.FeatureVector
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.math.floor

class TFLiteModelRunner(private val context: Context) {

    private var interpreter: Interpreter? = null
    private val modelFileName = "model.tflite"

    init {
        initInterpreter()
    }

    @Synchronized
    private fun initInterpreter() {
        if (interpreter != null) return
        try {
            val assetFd = context.assets.openFd(modelFileName)
            val inputStream = FileInputStream(assetFd.fileDescriptor)
            val fileChannel = inputStream.channel
            val modelBuffer = fileChannel.map(
                FileChannel.MapMode.READ_ONLY,
                assetFd.startOffset,
                assetFd.declaredLength
            )

            val options = Interpreter.Options().apply {
                setNumThreads(2)
                setUseXNNPACK(true)
            }
            interpreter = Interpreter(modelBuffer, options)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    data class Prediction(
        val probability: Float,
        val riskScore: Int // 1..10
    )

    fun predict(features: FeatureVector): Prediction {
        val interp = interpreter ?: run {
            initInterpreter()
            interpreter ?: return fallbackPrediction(features)
        }

        // Input shape: [1, 20] float32
        val inputBuffer = ByteBuffer.allocateDirect(1 * 20 * 4).apply {
            order(ByteOrder.nativeOrder())
            for (value in features.toFloatArray()) {
                putFloat(value)
            }
            rewind()
        }

        // Output shape: [1, 1] float32
        val outputBuffer = ByteBuffer.allocateDirect(1 * 1 * 4).apply {
            order(ByteOrder.nativeOrder())
        }

        interp.run(inputBuffer, outputBuffer)
        outputBuffer.rewind()
        val prob = outputBuffer.float

        // Áp dụng công thức quy định: clamp(1 + floor(9p + 0.5), 1, 10)
        val score = (1 + floor(9.0 * prob + 0.5)).toInt().coerceIn(1, 10)

        return Prediction(
            probability = prob,
            riskScore = score
        )
    }

    private fun fallbackPrediction(features: FeatureVector): Prediction {
        // Fallback heuristic nếu môi trường chưa load được TFLite
        var p = 0.1f
        if (features.hostIsIp == 1) p += 0.4f
        if (features.hasUserinfo == 1) p += 0.3f
        if (features.sensitiveKeywordCount > 0) p += 0.2f
        if (features.subdomainCount >= 3) p += 0.2f
        val clampedP = p.coerceIn(0.01f, 0.99f)
        val score = (1 + floor(9.0 * clampedP + 0.5)).toInt().coerceIn(1, 10)
        return Prediction(clampedP, score)
    }

    fun close() {
        try {
            interpreter?.close()
            interpreter = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
