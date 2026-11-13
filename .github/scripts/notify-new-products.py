#!/usr/bin/env python3
"""Send one FCM topic message for each newly added MORFY product ID."""
import json
import os
import re
import sys
from pathlib import Path

import requests
from google.auth.transport.requests import Request
from google.oauth2 import service_account

SCOPE = "https://www.googleapis.com/auth/firebase.messaging"
TOPIC = "morfy-news"
BASE_URL = "https://morfy3d.com.br/"


def read_products(path: str) -> dict[str, str]:
    content = Path(path).read_text(encoding="utf-8") if Path(path).exists() else ""
    found: dict[str, str] = {}
    pattern = re.compile(
        r"""["']?id["']?\s*:\s*["']([^"']+)["'][\s\S]{0,240}?["']?name["']?\s*:\s*["']([^"']+)["']"""
    )
    for match in pattern.finditer(content):
        product_id, name = match.group(1).strip(), match.group(2).strip()
        if product_id and name:
            found[product_id] = name
    return found


def main() -> int:
    before_path, after_path = sys.argv[1:3]
    old = read_products(before_path)
    new = read_products(after_path)
    added = [(product_id, name) for product_id, name in new.items() if product_id not in old]

    if not added:
        print("Nenhum produto novo detectado; nenhuma notificação enviada.")
        return 0

    raw_credentials = os.environ.get("FCM_SERVICE_ACCOUNT", "").strip()
    if not raw_credentials:
        print("Aviso: configure o segredo MORFY_FIREBASE_SERVICE_ACCOUNT_JSON para ativar o envio FCM.")
        print("Produtos novos detectados: " + ", ".join(name for _, name in added))
        return 0

    info = json.loads(raw_credentials)
    credentials = service_account.Credentials.from_service_account_info(info, scopes=[SCOPE])
    credentials.refresh(Request())
    project_id = info["project_id"]
    endpoint = f"https://fcm.googleapis.com/v1/projects/{project_id}/messages:send"
    headers = {"Authorization": f"Bearer {credentials.token}", "Content-Type": "application/json"}

    for product_id, name in added:
        payload = {
            "message": {
                "topic": TOPIC,
                "data": {
                    "title": "Novo produto na MORFY 3D",
                    "body": f"{name} acabou de chegar. Confira agora!",
                    "url": f"{BASE_URL}?produto={product_id}",
                },
                "android": {"priority": "HIGH", "ttl": "86400s"},
            }
        }
        response = requests.post(endpoint, headers=headers, json=payload, timeout=30)
        response.raise_for_status()
        print(f"Notificação enviada: {name} ({product_id})")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
