# MORFY 3D Android — preparação para Google Play

## Implementado
- Destino oficial https://morfy3d.com.br/.
- WebView limitado ao domínio MORFY; links externos HTTPS abrem em apps externos.
- Bloqueio de conteúdo misto, arquivos locais e erros SSL.
- Navegação de retorno, recarregamento e compartilhamento nativo.
- Canal e serviço de notificações Firebase Cloud Messaging.
- Consentimento explícito para notificações no Android 13+.
- Política pública em https://morfy3d.com.br/privacy.html.
- Target SDK 36, versionName 1.1.0 e versionCode 2.

## Ativar push
O cliente inscreve o usuário no tópico FCM morfy-news após consentimento. Para ativar:
1. Criar/selecionar projeto Firebase da MORFY.
2. Registrar o app Android com applicationId br.com.morfy3d.app.
3. Baixar google-services.json e codificar em Base64.
4. Cadastrar o segredo Actions MORFY_GOOGLE_SERVICES_JSON_BASE64.
5. Enviar mensagens ao tópico morfy-news pelo Firebase Console ou backend confiável.

Sem configuração Firebase, o app compila, mas push real não funciona.

## Publicação Play Store
O workflow atual gera APK debug para teste. A Play Store exige AAB de release assinado. Configure uma chave de upload estável e segura, compile bundleRelease e faça upload pelo Play Console. Não use chave debug.

## Pendências pré-publicação
- Definir e publicar canal de contato do controlador de dados na política.
- Preencher Segurança dos dados no Play Console conforme os SDKs ativados.
- Confirmar controle do domínio da política de privacidade.
- Configurar Firebase e testar notificações em dispositivos reais.
- Testar acessibilidade, desempenho, modo offline e navegação.
- Preparar ícone 512x512, screenshots, descrição, classificação etária e formulários do Play Console.
- Realizar teste interno/fechado e aguardar avaliação do Google.

A implementação técnica não garante aprovação. O Google avalia funcionalidade, conteúdo, experiência e declarações de privacidade.
