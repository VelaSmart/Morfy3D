# Escopo do produto DECKRA App

## Problema a resolver
Oferecer uma interface simples para configurar os controles físicos DECKRA e integrá-los ao PC, sem exigir que a pessoa usuária edite o firmware manualmente.

## Usuários e contexto
Pessoas que utilizam o controlador DECKRA para produtividade, games, streaming, música e tarefas cotidianas no computador. São cenários-alvo, não compatibilidades já garantidas.

## Escopo inicial (MVP)
- Detectar portas e identificar o hardware pelo protocolo.
- Mostrar conectado/desconectado e dados básicos do dispositivo.
- Ler eventos dos controles.
- Criar e editar mapeamentos de botões e potenciômetros.
- Salvar/carregar um perfil local.
- Mostrar erros com instruções de recuperação.

## Fora do MVP até validação
- Atualização automática de firmware.
- Sincronização de perfis em nuvem.
- Loja de perfis.
- Compatibilidade com todos os sistemas operacionais.
- Controle garantido de volume por aplicativo sem validar a integração com o sistema operacional.

## Critérios de aceite iniciais
- O aplicativo identifica o dispositivo correto e rejeita dispositivos/protocolos desconhecidos de modo seguro.
- Desconectar o dispositivo não trava a interface; ao reconectar, o usuário pode restabelecer a sessão.
- A interface exibe os eventos recebidos e não inventa controles inexistentes.
- Perfis são validados ao carregar e podem ser rejeitados se estiverem inválidos.
- O mapeamento salvo volta a aparecer ao reiniciar o aplicativo.
- Erros e versões de firmware/protocolo são visíveis para facilitar suporte.

Os critérios finais dependem da escolha da placa e do inventário real dos controles.
