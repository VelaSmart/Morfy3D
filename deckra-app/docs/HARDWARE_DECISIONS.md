# Decisões de hardware DECKRA

Este documento deve ser preenchido com o hardware real antes de especificar pinagem, tensões, frequência de comunicação ou firmware.

## Dados a confirmar
- Fabricante e modelo exato da placa/microcontrolador:
- Tensão lógica de GPIO:
- Interface USB e capacidades USB HID:
- Quantidade e tipo de potenciômetros:
- Quantidade e tipo de botões:
- Encoders, LEDs, displays ou outros componentes:
- Alimentação e consumo estimado:
- Esquema elétrico e pinagem:
- Sistema operacional prioritário:
- Método de gravação e recuperação de firmware:

## Verificações de segurança
- Consultar datasheet e documentação oficial da placa.
- Não conectar sinais com níveis de tensão não confirmados.
- Verificar se a placa aceita USB HID nativamente ou exige suporte adicional.
- Não definir pinos apenas por conveniência antes do esquema elétrico.
- Separar alimentação de periféricos e lógica, se o desenho do circuito exigir.

## Decisão de ligação ao computador
Opções a avaliar:
- **USB serial:** simples para diagnóstico e mensagens customizadas; o aplicativo precisará interpretar o protocolo.
- **USB HID:** pode ser reconhecido como teclado/controle sem um agente serial, mas depende da placa, do firmware e dos requisitos para configuração.
- **Abordagem híbrida:** HID para eventos em uso e um canal definido para configuração, caso a plataforma de hardware suporte isso.

Escolher após confirmar placa, experiência de uso desejada e requisitos de configuração.
