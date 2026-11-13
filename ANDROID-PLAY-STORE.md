# MORFY 3D Android — estado da implementação

## Aplicativo
- WebView em tela cheia apontando para https://morfy3d.com.br/.
- Navegação interna restrita ao domínio MORFY; links HTTPS externos abrem fora do app.
- JavaScript e armazenamento web habilitados para o catálogo; arquivos locais, conteúdo misto e erros SSL bloqueados.
- Sem barra nativa, sem compartilhamento nativo e sem telas paralelas: o conteúdo principal é o site.
- Ícone Android gerado com a identidade visual oficial MORFY.
- Firebase Cloud Messaging é a única integração nativa adicional, para avisos de novos produtos.

## Notificações de novos produtos
- O app solicita permissão de notificações no Android 13+ e inscreve usuários autorizados no tópico `morfy-news`.
- O workflow `.github/workflows/product-notifications.yml` compara os IDs de `assets/js/products.js` antes e depois de cada push na `main`.
- Somente IDs novos disparam notificação. O toque abre o produto correspondente no catálogo.
- É necessário configurar os segredos `MORFY_GOOGLE_SERVICES_JSON_BASE64` e `MORFY_FIREBASE_SERVICE_ACCOUNT_JSON`, ambos do mesmo projeto Firebase.

## Build e Play Store
- O workflow Android gera APK debug para teste.
- AAB de release depende dos quatro segredos de assinatura documentados em `app/PLAY-STORE-SETUP.md`.
- Pacote: `br.com.morfy3d.app`; versionCode: 2; targetSdk: 36.
- A aprovação depende também da ficha da loja, Segurança dos dados, política de privacidade, testes e avaliação do Google.

## Segurança
Nunca armazene credenciais Firebase ou keystores no repositório. Use somente GitHub Actions Secrets.
