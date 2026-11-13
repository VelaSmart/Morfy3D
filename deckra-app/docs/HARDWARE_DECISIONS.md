# Decisões de hardware DECKRA

## Decisões atuais

- **Placa alvo:** Arduino Leonardo ou Arduino Micro (ATmega32U4).
- **Comunicação inicial com o PC:** USB Serial/CDC.
- **Sistema operacional prioritário:** Windows 10/11.
- **Controles previstos:** potenciômetros, botões e LEDs RGB.
- **Aplicativo:** Tauri 2 + React + TypeScript; backend Rust para gerenciar a porta serial.

## Dados que ainda precisam ser confirmados

- Leonardo ou Micro como placa definitiva.
- Quantidade e identificação dos potenciômetros e botões.
- Modelo, quantidade, tipo elétrico e corrente dos LEDs RGB.
- LEDs RGB de anodo comum ou catodo comum; uso de resistores ou driver/transistores.
- Pinos e esquema elétrico completos.
- Comportamento esperado dos potenciômetros (valores brutos, curva, deadzone e mapeamento).
- Se os botões precisam funcionar como atalhos globais ou apenas dentro do aplicativo.
- Requisitos de perfis e persistência.

## Regras de ligação segura

- Consultar o pinout e a documentação oficial da placa escolhida.
- Confirmar que todos os sinais estejam dentro dos limites elétricos do microcontrolador.
- Potenciômetros devem ser conectados como divisores de tensão adequados à faixa ADC, com GND e referência corretos.
- Cada LED deve ter limitação de corrente. Não conectar LEDs RGB diretamente aos pinos sem dimensionar resistores e corrente.
- Se a corrente do LED exceder o que um GPIO pode fornecer, usar driver ou transistor apropriado.
- Não especificar a pinagem final até que a contagem de controles e o circuito sejam confirmados.

## Por que USB Serial primeiro?

A porta serial permite que o aplicativo receba eventos customizados e configure LEDs sem precisar que o dispositivo se apresente como teclado USB. O Leonardo/Micro têm USB nativo, então USB HID pode ser avaliado depois para teclas físicas, mas misturar HID e Serial na primeira prova de conceito adicionaria complexidade sem necessidade.
