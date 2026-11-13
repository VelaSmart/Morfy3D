# Build, notificações e publicação Android MORFY 3D

O aplicativo MORFY 3D é um WebView do site oficial. A única integração nativa adicional é o Firebase Cloud Messaging, necessário para receber notificações mesmo com o app fechado. O ícone Android é gerado com a identidade visual oficial MORFY.

## Segredos do GitHub Actions

Cadastre em Settings > Secrets and variables > Actions:

- `MORFY_GOOGLE_SERVICES_JSON_BASE64`: arquivo `google-services.json` codificado em Base64, do app Firebase Android registrado com o pacote `br.com.morfy3d.app`. Ativa a inscrição dos celulares no tópico de novidades.
- `MORFY_FIREBASE_SERVICE_ACCOUNT_JSON`: JSON integral de uma conta de serviço do mesmo projeto Firebase, com permissão para enviar mensagens FCM. É usado somente pelo workflow que detecta novos produtos. Nunca publique esse JSON no repositório nem o envie pelo chat.

Quando um novo produto for adicionado a `assets/js/products.js` e enviado à branch `main`, o workflow compara os IDs com a versão anterior e envia uma notificação ao tópico `morfy-news`. O toque na notificação abre diretamente o produto no catálogo. Alterações em produtos existentes não geram alerta de lançamento.

## Assinatura para Google Play

Para gerar o AAB assinado, configure os quatro segredos abaixo com uma chave de upload própria e estável:

- `MORFY_UPLOAD_KEYSTORE_BASE64`: keystore codificada em Base64.
- `MORFY_UPLOAD_KEYSTORE_PASSWORD`: senha do keystore.
- `MORFY_UPLOAD_KEY_ALIAS`: alias da chave.
- `MORFY_UPLOAD_KEY_PASSWORD`: senha da chave.

Não comite keystores, senhas ou arquivos Firebase. Sem os segredos de assinatura, o workflow gera apenas o APK debug.

O identificador do pacote deve permanecer `br.com.morfy3d.app`. O `versionCode` atual é 2 e deve aumentar a cada atualização publicada.

## Configuração Firebase

1. Criar ou selecionar o projeto Firebase da MORFY.
2. Registrar o app Android com o pacote exato `br.com.morfy3d.app`.
3. Baixar o `google-services.json` e cadastrar o segredo Base64.
4. Criar uma conta de serviço no mesmo projeto, habilitada para enviar mensagens FCM, e cadastrar o JSON no segredo `MORFY_FIREBASE_SERVICE_ACCOUNT_JSON`.
5. Instalar o APK de um build que contenha o Firebase configurado, abrir o app e permitir notificações.
6. Publicar um produto novo em `assets/js/products.js) na branch `main) para testar o fluxo completo.

A conta de serviço e o arquivo de configuração precisam pertencer ao mesmo projeto Firebase. Sem essas configurações externas, o código compila, mas não há entrega de push real.

## Google Play

A Play Store exige um Android App Bundle de release assinado. Faça upload pelo Play Console, preencha a ficha da loja, classificação etária e Segurança dos dados, publique a política de privacidade e execute testes internos. A implementação técnica não garante aprovação do Google.
