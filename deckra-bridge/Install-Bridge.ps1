#requires -RunAsAdministrator
$ErrorActionPreference = "Stop"
$tcpRule = "DECKRA Bridge TCP 37892"
$udpRule = "DECKRA Discovery UDP 37891"
$user = [System.Security.Principal.WindowsIdentity]::GetCurrent().Name
Write-Host "Configurando permissões locais do DECKRA Bridge..." -ForegroundColor Cyan
$urlAcl = & netsh http show urlacl url=http://+:37892/ 2>&1 | Out-String
if ($urlAcl -notmatch [regex]::Escape($user)) {
  & netsh http add urlacl url=http://+:37892/ "user=$user"
  if ($LASTEXITCODE -ne 0) { throw "Não foi possível registrar a reserva HTTP para $user." }
} else {
  Write-Host "Reserva HTTP já existe para este usuário."
}
if (-not (Get-NetFirewallRule -DisplayName $tcpRule -ErrorAction SilentlyContinue)) {
  New-NetFirewallRule -DisplayName $tcpRule -Direction Inbound -Action Allow -Protocol TCP -LocalPort 37892 -Profile Private -RemoteAddress LocalSubnet | Out-Null
}
if (-not (Get-NetFirewallRule -DisplayName $udpRule -ErrorAction SilentlyContinue)) {
  New-NetFirewallRule -DisplayName $udpRule -Direction Inbound -Action Allow -Protocol UDP -LocalPort 37891 -Profile Private -RemoteAddress LocalSubnet | Out-Null
}
Write-Host "Concluído. Portas liberadas somente em redes privadas e na sub-rede local." -ForegroundColor Green
Write-Host "Abra DeckraBridge.exe para iniciar o serviço e exibir o PIN."
