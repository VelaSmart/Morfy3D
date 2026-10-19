# DECKRA App

Aplicativo em planejamento para configurar e utilizar o controlador físico DECKRA, parte do universo MORFY.

> **Estado atual:** planejamento. Este repositório ainda não contém um firmware confirmado, protocolo serial definido ou instalador funcional. A interface conceitual do site não deve ser tratada como software já implementado.

## Objetivo

Criar um aplicativo para configurar o hardware DECKRA conectado a um computador e, em uma fase posterior, disponibilizar o firmware para a placa Arduino escolhida.

O site público de apresentação é [DECKRA](https://velasmart.github.io/Morfy3D/deckra.html). O aplicativo será desenvolvido nesta branch de forma independente da vitrine comercial.

## Funcionalidades previstas

- Detectar e selecionar a porta de comunicação do dispositivo.
- Exibir o estado da conexão e orientar a recuperação quando desconectado.
- Descobrir e configurar os controles físicos depois que o hardware for definido.
- Mapear potenciômetros para funções, incluindo volume por aplicativo quando o sistema operacional e a integração escolhida permitirem.
- Atribuir teclas a ações ou atalhos configuráveis.
- Salvar e carregar perfis de configuração.
- Atualizar firmware somente depois de definir a placa, o processo seguro de atualização e os mecanismos de recuperação.
- Fornecer diagnóstico básico e registros úteis para suporte.

Esses itens são metas, não funcionalidades já implementadas.

## Arquitetura proposta

Separar o projeto em três partes:

1. **Aplicativo desktop:** interface de configuração, perfis, estado da conexão e integração com o sistema operacional.
2. **Protocolo do dispositivo:** mensagens versionadas para handshake, identificação, leitura de controles e gravação de configuração.
3. **Firmware:** leitura dos controles e envio de eventos, executado no microcontrolador escolhido.

Começar com limites bem definidos entre a interface e a comunicação USB/serial facilita trocar a placa ou o transporte sem reescrever a interface inteira.

### Stack a validar

Ainda não há stack fixada. Uma opção inicial é **Tauri + TypeScript** para a interface desktop e um módulo de comunicação serial apropriado ao sistema operacional. O firmware poderá ser feito em C/C++ no Arduino IDE ou PlatformIO, conforme a placa selecionada. Validar as dependências e as permissões do sistema operacional antes de tomar a decisão definitiva.

## Protocolo de comunicação: requisitos iniciais

O formato final deve ser documentado e versionado antes da implementação. No mínimo, precisa considerar:

- Identificação do dispositivo e versão do protocolo.
- Mensagem de inicialização/handshake e resposta de capacidades.
- Eventos de controles identificados por ID estável.
- Comandos de configuração e confirmação de sucesso/erro.
- Limites de tamanho, validação das mensagens e tratamento de dados incompletos.
- Reconexão, timeout e comportamento seguro após perda de conexão.

Não assumir ainda IDs, quantidade de botões/potenciômetros, baud rate ou formato definitivo das mensagens.

## Estrutura sugerida

```text
deckra-app/
├── README.md
├── docs/
│   ├── PRODUCT_SCOPE.md
│   ├── HARDWARE_DECISIONS.md
│   └── PROTOCOL.md
├── app/                 # interface desktop (a definir)
├── firmware/            # firmware do microcontrolador (a definir)
└── tests/               # testes de protocolo e lógica
```

## Decisões necessárias antes de implementar

1. **Placa exata:** modelo de Arduino/microcontrolador e tensão lógica.
2. **Controles físicos:** quantos potenciômetros, botões, encoders, LEDs ou outros componentes.
3. **Conexão com o PC:** USB serial, USB HID nativo ou outra opção. Nem toda placa Arduino oferece HID nativo sem bibliotecas ou firmware específicos.
4. **Sistemas operacionais:** Windows primeiro ou suporte desde o início a Windows, Linux e macOS.
5. **Volume por aplicativo:** escolher a integração com o sistema operacional, pois enviar um comando genérico pelo Arduino não garante controle de volume de um aplicativo específico.
6. **Ações de teclas:** definir se serão teclas HID, atalhos, macros ou ações tratadas pelo aplicativo.
7. **Distribuição:** assinatura, instalador, atualização e canal para downloads.

## Segurança e confiabilidade

- Nunca aplicar tensão desconhecida aos pinos da placa; validar datasheet e esquema elétrico do hardware real.
- Validar todas as mensagens recebidas antes de alterar a configuração.
- Não executar comandos arbitrários recebidos pela porta serial.
- Separar a configuração da interface dos dados recebidos do hardware.
- Criar testes de reconexão, mensagens inválidas, desconexão durante gravação e perfis corrompidos.
- Manter firmware e aplicativo versionados e compatíveis de forma explícita.

## Próximas etapas sugeridas

1. Confirmar a placa Arduino e o esquema elétrico.
2. Inventariar os controles e definir quais eventos precisam ser enviados ao PC.
3. Fazer uma prova de conceito mínima de conexão, identificação e leitura.
4. Especificar e testar o protocolo versionado.
5. Criar a primeira tela do aplicativo e a persistência de perfis.
6. Implementar as integrações de atalhos e volume por aplicativo.
7. Empacotar e testar o instalador antes de publicar um link no site.

## Relação com o site MORFY

O site em `main` continua sendo a vitrine comercial. Esta branch é destinada ao futuro aplicativo e à documentação técnica. O botão de download em `deckra.html` deve permanecer indisponível até existir um pacote real, testado e publicado.
