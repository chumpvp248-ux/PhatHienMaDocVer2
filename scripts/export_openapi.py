"""
Script xuất hợp đồng OpenAPI JSON từ FastAPI app sang contracts/openapi.json.
"""

import json
import os
from backend.app.main import app

def export_openapi():
    openapi_schema = app.openapi()
    output_path = os.path.join("contracts", "openapi.json")
    os.makedirs("contracts", exist_ok=True)
    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(openapi_schema, f, indent=2, ensure_ascii=False)
    print(f"Đã xuất hợp đồng OpenAPI thành công tại {output_path}!")

if __name__ == "__main__":
    export_openapi()
