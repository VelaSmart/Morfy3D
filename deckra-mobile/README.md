# DECKRA Mobile

Aplicativo Android nativo, independente do antigo WebView do MORFY. Descobre o DECKRA Bridge na rede local via UDP broadcast, pareia por PIN e envia comandos permitidos por WebSocket.

## Atualizações pelo próprio aplicativo

Cada push na branch `main` que alterar `deckra-mobile/**`, `deckra-bridge/**` ou o workflow dispara a compilação do APK e da Bridge. Para publicar atualizações Android que substituam a instalação existente, configure os segredos `DECKRA_ANDROID_KEYSTORE_BASE64`, `DECKRA_ANDROID_KEYSTORE_PASSWORD`, `DECKRA_ANDROID_KEY_ALIAS` e `DECKRA_ANDROID_KEY_PASSWORD` no repositório. Sem a chave estável, o workflow gera somente APK de teste e não publica atualização Android.

Ao abrir o app, ele consulta essa release pública. Quando encontra um `versionCode` superior, oferece baixar e instalar a atualização. O Android exige que o usuário autorize a instalação e confirme a substituição do aplicativo. Na primeira atualização, pode ser necessário permitir "Instalar apps desconhecidos" para DECKRA Mobile nas configurações do Android.

## Instalação inicial e downloads

Os arquivos publicados ficam em [Releases do DECKRA Mobile](https://github.com/VelaSmart/Morfy3D/releases/tag/deckra-mobile-latest):
- `deckra-mobile.apk`: aplicativo Android.
- `DeckraBridge-Windows.zip`: interface Windows e serviço Bridge. Extraia os arquivos na mesma pasta e abra `DeckraBridge.exe`.

O APK gerado pelo workflow é de debug e serve para testes; ainda não é uma distribuição assinada para Play Store.

## Conexão

1. Execute `DeckraBridge.exe` no Windows.
2. Anote o PIN temporário de seis dígitos mostrado no console.
3. Conecte celular e PC à mesma rede Wi-Fi.
4. No aplicativo, toque em PROCURAR MEU PC, informe o PIN e conecte.

Na primeira execução da Bridge, abra o terminal como administrador se necessário para permitir o registro do listener HTTP. Autorize TCP 37892 e UDP 37891 no Firewall do Windows somente em redes privadas.

## Estado do protótipo 0.1.x

- Android nativo Kotlin, sem WebView.
- Descoberta UDP, conexão WebSocket e pareamento por PIN.
- Comandos Windows para volume e mídia.
- Verificação e instalação assistida de atualizações dentro do app.
- Bridge Windows .NET 8.

## Limitações conhecidas

Este MVP cobre conexão e comandos multimídia básicos. Ainda não existe integração com o backend interno do DECKRA Desktop, gestão de perfis, mapeamento de potenciômetros/teclas, volume por aplicativo, TLS nem pareamento persistente. Use apenas em rede privada confiável. A descoberta por broadcast pode ser bloqueada por isolamento de clientes do roteador.
