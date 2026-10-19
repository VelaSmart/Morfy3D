# Protocolo DECKRA

**Status: rascunho de requisitos.** Ainda não há estrutura de pacote oficial. Não implementar um protocolo definitivo com base em exemplos presumidos.

## Princípios
- Mensagens versionadas.
- Identificação estável dos controles, sem depender da posição física no layout da interface.
- Capacidade de descobrir firmware, recursos disponíveis e versão do protocolo.
- Respostas explícitas de sucesso ou erro para comandos de configuração.
- Validação de tipo, tamanho, intervalo e valores antes de processar qualquer mensagem.
- Recuperação segura após timeout, reinicialização e desconexão.

## Famílias de mensagem a especificar
- HELLO / HELLO_ACK: descoberta e negociação da versão.
- DEVICE_INFO: identificador do dispositivo, versão de firmware e capacidades.
- CONTROL_EVENT: ID do controle e valor ou estado.
- GET_CONFIG / CONFIG_DATA: consulta de perfil/configuração.
- SET_CONFIG / CONFIG_ACK: gravação validada e confirmação.
- ERROR: código de erro estável e informação não sensível para diagnóstico.

Os nomes acima são conceitos, não comandos já implementados. Codificação, delimitadores ou framing, checksums, limites e política de compatibilidade devem ser definidos e testados antes de uso.

## Testes necessários
- Mensagem incompleta, excedendo o tamanho máximo ou malformada.
- Valor fora do intervalo esperado.
- Versão de protocolo incompatível.
- Desconexão durante operação.
- Repetição de comando e timeout.
- Reinicialização do microcontrolador.
