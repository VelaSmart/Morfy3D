"""Gera o versao.json do DECKRA Mobile (mesmo esquema do DECKRA APP do PC).

Uso:  python gerar_manifesto_mobile.py <apk> <versao> <versionCode> [notas.txt]
Saída: publicar_mobile/DECKRA-Mobile.apk e publicar_mobile/versao.json. Os dois vão para o ramo
"Deckra" do GitHub, pasta "mobile". O app confere esse versao.json ao abrir.
"""
import hashlib
import json
import os
import shutil
import sys

apk, versao, codigo = sys.argv[1], sys.argv[2], int(sys.argv[3])
notas = ""
if len(sys.argv) > 4 and os.path.exists(sys.argv[4]):
    with open(sys.argv[4], encoding="utf-8-sig") as f:
        notas = " ".join(linha.strip() for linha in f if linha.strip())
pasta = os.path.join(os.path.dirname(os.path.abspath(__file__)), "publicar_mobile")
os.makedirs(pasta, exist_ok=True)
destino = os.path.join(pasta, "DECKRA-Mobile.apk")
shutil.copyfile(apk, destino)
dados = open(destino, "rb").read()
manifesto = {
    "versao": versao,
    "versionCode": codigo,
    "url": "https://raw.githubusercontent.com/VelaSmart/Morfy3D/Deckra/mobile/DECKRA-Mobile.apk",
    "sha256": hashlib.sha256(dados).hexdigest(),
    "tamanho": len(dados),
    "notas": notas,
}
with open(os.path.join(pasta, "versao.json"), "w", encoding="utf-8") as f:
    json.dump(manifesto, f, ensure_ascii=False, indent=2)
print(json.dumps(manifesto, ensure_ascii=False, indent=2))
