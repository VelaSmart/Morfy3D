# DECKRA Mobile

Aplicativo Android nativo, separado do antigo WebView do MORFY. Descobre o DECKRA Bridge na rede local via UDP broadcast, pareia por PIN e envia comandos permitidos por WebSocket.

## Estado do protótipo 0.1.0
- Android nativo Kotlin, sem WebView.
- Descoberta UDP e conexão WebSocket.
- Pareamento por PIN de seis dígitos.
- Comandos Windows para volume e mídia.
- Bridge Windows .NET 8 incluído em ../deckra-bridge.
- Workflow GitHub Actions gera APK debug como artifact.

## Build APK
Abra deckra-mobile no Android Studio ou execute gradle assembleDebug com Android SDK e Gradle 8.9. O APK de debug não é assinado para distribuição pública.

## Bridge Windows
Instale .NET 8 SDK/runtime, compile deckra-bridge/DeckraBridge.csproj e execute o binário no PC. Autorize TCP 37892 e UDP 37891 no Firewall do Windows apenas para rede privada.

## Limitações conhecidas
Este primeiro MVP cobre conexão e comandos multimídia básicos. Os comandos de volume e mídia usam teclas multimídia do Windows. Ainda não existe integração com o backend interno do DECKRA Desktop, gestão de perfis, mapeamento de potenciômetros/teclas, volume por aplicativo, TLS nem pareamento persistente. Use apenas em rede privada confiável. A descoberta por broadcast pode ser bloqueada por isolamento de clientes do roteador.
