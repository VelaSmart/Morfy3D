# DECKRA — ramo de distribuição

Este ramo guarda os arquivos do **DECKRA** que não fazem parte do site da MORFY (o site fica no ramo `main`).

| Pasta | Conteúdo |
| --- | --- |
| `instalador/` | Instalador mais recente do app DECKRA para Windows (`DECKRA-Setup.exe`) e o `versao.json` consultado pelo app em **Configurações → Atualizações**. |
| `deckra-app/` | Projeto do app DECKRA em Tauri/React, firmware e documentação de hardware e protocolo. |

**Download da versão mais recente:**
https://raw.githubusercontent.com/VelaSmart/Morfy3D/Deckra/instalador/DECKRA-Setup.exe

Os arquivos de `instalador/` são gerados pelo `GERAR_INSTALADOR.bat` do projeto DECKRA e não devem ser editados à mão: o `versao.json` contém o código de verificação (SHA-256) do instalador.
