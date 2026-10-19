# Protocolo DECKRA

**Versão do protótipo:** 0.1  
**Transporte:** USB Serial/CDC.  
**Framing inicial:** uma mensagem JSON por linha (JSON Lines, LF).  
**Status:** proposta inicial a validar com hardware real.

## Regras

- Uma linha contém um objeto JSON UTF-8 completo terminado em LF.
- O dispositivo envia mensagens de evento e resposta.
- O aplicativo só envia comandos explicitamente permitidos.
- IDs de controles devem ser estáveis e independentes do texto da interface.
- Valores e tamanhos devem ser validados; mensagens desconhecidas devem ser ignoradas ou respondidas com erro.
- A velocidade serial será configurada pelo firmware e aplicativo em conjunto; escolher e validar um valor ao testar o primeiro firmware.

## Mensagens iniciais do dispositivo

Identificação:

```json
{"type":"hello","protocol":1,"device":"deckra","firmware":"0.1.0","capabilities":["potentiometers","buttons","rgb"]}
```

Potenciômetro:

```json
{"type":"control","control":"pot_1","kind":"potentiometer","value":512,"min":0,"max":1023}
```

Botão:

```json
{"type":"control","control":"button_1","kind":"button","pressed":true}
```

Resposta genérica:

```json
{"type":"ack","id":"cmd-1","ok":true}
```

Erro:

```json
{"type":"error","code":"invalid_command","message":"Unsupported command"}
```

## Comandos do aplicativo

Definir cor RGB por canal lógico:

```json
{"type":"set_rgb","id":"cmd-1","led":"rgb_1","r":0,"g":120,"b":255}
```

Desligar LED:

```json
{"type":"set_rgb","id":"cmd-2","led":"rgb_1","r":0,"g":0,"b":0}
```

Estes exemplos descrevem o contrato pretendido, não garantem que a pinagem ou os LEDs já estejam implementados. O firmware deve validar IDs, canais entre 0 e 255 e o comprimento da linha antes de alterar saídas.

## Próximas decisões

- Definir a taxa serial real e limites máximos da linha.
- Definir periodicidade de amostragem e política de envio dos potenciômetros (mudança mínima/debounce).
- Definir debounce dos botões.
- Definir polaridade dos LEDs RGB.
- Adicionar timeout, reconexão, compatibilidade de versões e testes com mensagens inválidas.
