# Build e publicação Android MORFY 3D

O workflow `.github/workflows/android-apk.yml` gera um APK de depuração para testes. Quando as credenciais de assinatura estão configuradas, também gera um Android App Bundle (.aab) de release assinado.

## Segredos opcionais do GitHub Actions

Para ativar Firebase Push:
- `MORFY_GOOGLE_SERVICES_JSON_BASE64`: conteúdo de `google-services.json` codificado em Base64, do app Firebase registrado com `br.com.morfy3d.app`.

Para gerar AAB assinado:
- `MORFY_UPLOAD_KEYSTORE_BASE64`: keystore de upload codificado em Base64.
- `MORFY_UPLOAD_KEYSTORE_PASSWORD`: senha do keystore.
- `MORFY_UPLOAD_KEY_ALIAS`: alias da chave.
- `MORFY_UPLOAD_KEY_PASSWORD`: senha da chave.

Mantenha os quatro segredos de assinatura correspondentes à mesma keystore. Não comite keystores, senhas ou o arquivo Firebase no repositório. Se as credenciais de assinatura não estiverem presentes, somente o APK de debug será gerado.

O identificador de pacote deve permanecer `br.com.morfy3d.app` para preservar a continuidade do app existente. O `versionCode` precisa aumentar a cada envio à Play Store.
