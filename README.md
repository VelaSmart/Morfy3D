# MORFY / Morfy3D

Vitrine de produtos da **MORFY — Ideias que ganham forma**, com catálogo de impressão 3D e uma página dedicada ao **DECKRA — Hardware feito para você**.

O site usa HTML, CSS e JavaScript, com imagens e fontes locais. Funciona no navegador e pode ser hospedado no GitHub Pages. Não exige build, instalação de pacotes, banco de dados ou backend. A compra acontece nos anúncios específicos dos produtos na Shopee, no Mercado Livre e no TikTok Shop; o site não tem carrinho, checkout, pagamento ou cadastro de compradores.

Repositório: [VelaSmart/Morfy3D](https://github.com/VelaSmart/Morfy3D).

Site publicado: [Abrir MORFY](https://velasmart.github.io/Morfy3D/) · [Abrir DECKRA](https://velasmart.github.io/Morfy3D/deckra.html).

**Catálogo atual:** Gatinho Cavaleiro (versões branca e laranja), Boneco de Neve (6 cm e 10 cm) e Chaveiro TNT, com imagens fornecidas pela marca e artes do anúncio oficial da Shopee para o TNT. Os seis produtos provisórios foram removidos. As medidas cadastradas vêm das imagens enviadas; preço, estoque, quantidade e condições devem ser consultados nos anúncios. Os três produtos possuem links reais da Shopee. Mercado Livre e TikTok Shop aparecem como **Indisponível**, sem ação de clique. A apresentação DECKRA mantém suas referências conceituais e ainda não possui anúncio da Shopee.

## Páginas e recursos

O projeto tem exatamente duas páginas principais:

| Página | Conteúdo |
| --- | --- |
| `index.html` | Catálogo MORFY, busca, filtros de categoria e modal de detalhes com “Onde comprar”. |
| `deckra.html` | Apresentação DECKRA, imagens conceituais e seção de marketplaces acessível pelo hero e pelo encerramento. |

A interface adapta-se a computador e celular. Inclui navegação por teclado, foco visível, textos alternativos, aviso acessível dos botões de demonstração e suporte à preferência de movimento reduzido. A busca considera nome, descrição e categoria, sem diferenciar acentos ou maiúsculas. Cada card abre os detalhes com **“Ver detalhes”**. O modal inclui uma galeria com miniaturas, indicação da imagem selecionada e navegação pelas setas esquerda/direita quando uma miniatura está em foco.

## Clonar e visualizar no computador

Com Git instalado, execute:

```sh
git clone https://github.com/VelaSmart/Morfy3D.git
cd Morfy3D
```

Também é possível baixar o repositório em **Code → Download ZIP** e extrair todos os arquivos.

### Prévia com Node.js

Dentro da pasta do projeto, com Node.js instalado:

```sh
node tools/serve.cjs
```

Abra [o catálogo local](http://127.0.0.1:4173/) ou [a página DECKRA local](http://127.0.0.1:4173/deckra.html). O servidor também aceita [uma prévia em subdiretório](http://127.0.0.1:4173/morfy/), útil para conferir caminhos semelhantes aos usados no GitHub Pages.

Para encerrar, pressione `Ctrl+C` no terminal. O servidor é apenas uma ferramenta de prévia; o site publicado não depende dele.

### Abrir os arquivos diretamente

Abra `index.html` no navegador por um duplo clique, usando o protocolo `file://`. Preserve a estrutura de pastas. Se o navegador restringir algum recurso local, use a prévia com Node.js.

### Alternativa com Python

Com Python instalado, dentro da mesma pasta:

```sh
python -m http.server 4173 --bind 127.0.0.1
```

Use os mesmos endereços locais do catálogo e do DECKRA. A rota de teste `/morfy/` é um recurso do servidor Node.js, não do servidor Python.

## Estrutura dos arquivos

```text
Morfy3D/
├── index.html                     Catálogo MORFY
├── deckra.html                    Apresentação DECKRA
├── .nojekyll                      Publicação estática no GitHub Pages
├── README.md                      Este guia
├── assets/
│   ├── css/styles.css             Identidade visual e responsividade
│   ├── js/products.js             Configuração dos produtos e anúncios
│   ├── js/app.js                  Busca, filtros, detalhes e marketplaces
│   ├── images/                    Logos, referências e imagens dos produtos
│   └── fonts/                     Montserrat, Sora e licenças OFL
└── tools/
    ├── serve.cjs                  Servidor opcional de prévia local
    ├── verify.cjs                 Verificação de arquivos e configuração
    ├── hero-image-prompt.txt       Registro da antiga imagem conceitual
    └── hero-slicing-prompt.txt     Prompt do fundo técnico atual
```

Os arquivos do site devem ficar **na raiz do repositório**, sem uma pasta `morfy-site` envolvendo `index.html`. Todos os caminhos de navegação, estilos, scripts, fontes e imagens são relativos, para funcionar também em `/Morfy3D/`.

## Atualizar o catálogo

Edite `assets/js/products.js`. A configuração fica em `window.MORFY_CONFIG`; os cards são gerados a partir da lista `products`.

Este exemplo usa um produto do catálogo atual. Ao duplicá-lo, escolha outro `id` e substitua os dados e as imagens:

```javascript
{
  id: "gatinho-cavaleiro",
  name: "Gatinho Cavaleiro",
  category: "geek",
  description: "Escultura em impressão 3D com armadura, espada e capa rosa.",
  images: [{
    src: "./assets/images/gatinho-cavaleiro-capa.png",
    alt: "Gatinho Cavaleiro laranja com espada e capa rosa em cenário de jardim",
    label: "Apresentação"
  }],
  features: ["Versões branca e laranja."],
  demo: false,
  marketplaces: {
    shopee: "https://br.shp.ee/iUUqJ7XX",
    mercadoLivre: "",
    tiktokShop: ""
  }
}
```

| Campo | Como editar |
| --- | --- |
| `id` | Identificador único do produto. Não repita entre os itens. |
| `name` | Nome exibido no card e no modal. |
| `category` | Identificador de uma categoria cadastrada. |
| `description` | Texto de apresentação do produto. |
| `images` | Lista de objetos com `src`, `alt` e rótulo opcional `label`. O card usa a primeira imagem; o modal exibe todas as imagens em uma galeria, na ordem da lista. |
| `features` | Lista de textos com características confirmadas. Deixe vazia enquanto não houver informações verificadas. |
| `demo` | `true` identifica um conceito; `false` remove o selo “Conceito” e o aviso de demonstração nos detalhes do item real. |
| `marketplaces` | Links separados por produto e por plataforma. |

Para adicionar um produto, duplique um objeto dentro de `products`, altere o identificador e os dados e preserve as vírgulas entre os objetos. Para remover, exclua o objeto correspondente. **A ordem da lista define a ordem dos cards**; mova os objetos para reorganizar o catálogo.

Não acrescente dimensões, materiais, resistência, quantidade de controles ou compatibilidade por suposição. Preencha `features` com dados do produto real e use descrições coerentes com as fotos e os anúncios.

### Categorias e filtros

O catálogo atual usa `decoracao` (Boneco de Neve) e `geek` (Gatinho Cavaleiro e Chaveiro TNT). O filtro `todos` reúne os três itens. O rótulo `utilitarios` continua disponível no JavaScript para produtos futuros; seu filtro foi retirado enquanto não houver itens dessa categoria.

Para adicionar outra categoria:

1. Defina o mesmo identificador em `category` nos produtos correspondentes.
2. Adicione seu rótulo ao objeto `categories` no início de `assets/js/app.js`.
3. Adicione um botão em `.category-filters` no `index.html`, com `data-category` igual ao novo identificador e `aria-pressed="false"`.

### Imagens e textos alternativos

Coloque novas imagens em `assets/images/` e atualize o caminho na configuração. Use nomes simples, preferencialmente sem espaços, e preserve maiúsculas e minúsculas. Por exemplo, `./assets/images/gatinho-cavaleiro-capa.png` é um caminho relativo válido; `/assets/images/gatinho-cavaleiro-capa.png` começa na raiz do domínio e quebra a hospedagem em subdiretório.

Use `alt` para descrever o objeto e o que a imagem mostra. Para fotos reais, substitua descrições como “ilustração” por uma descrição adequada da foto. WebP, JPG e PNG podem ser usados. Redimensione e comprima as imagens antes de adicioná-las para manter o carregamento leve.

## Atualizar o DECKRA

As 20 imagens dos produtos estão em `assets/images/`, com os prefixos `gatinho-cavaleiro-`, `boneco-de-neve-` e `chaveiro-tnt-`. O TNT usa sete imagens editadas do [anúncio oficial da Shopee](https://shopee.com.br/product/1934551108/58269578490), incluindo capa, fotos reais e conjuntos. As fotos de conjuntos não definem a quantidade vendida, que deve ser consultada no anúncio. A origem de cada imagem do TNT está registrada em `tools/tnt-shopee-image-sources.json`; os arquivos ficam no próprio repositório, sem depender do carregamento externo da Shopee. A imagem repetida do gatinho não foi duplicada. Para alterar a capa, mova a imagem desejada para a primeira posição de `images`. Para incluir outra cor, medida ou vista, acrescente um objeto com `src`, `alt` e `label`. Não crie outro produto apenas para uma foto adicional.

No mesmo arquivo, edite o objeto `deckra`. Mantenha `name: "DECKRA"`, atualize `description`, inclua apenas características confirmadas em `features` e configure os anúncios em `marketplaces`.

A descrição e a lista de características são exibidas na seção de detalhes. A primeira imagem de `images` aparece no hero e no banner do catálogo. A seção de detalhes usa a segunda imagem, quando existir; caso contrário, repete a primeira.

O painel de identidade atual contém vários elementos em uma única imagem. A configuração usa `layout: "reference-board"` para aplicar os recortes próprios dessa referência:

```javascript
images: [{
  src: "./assets/images/deckra-reference.png",
  alt: "Referência conceitual de um controlador DECKRA com teclas e controles giratórios",
  layout: "reference-board"
}]
```

Ao substituir o painel por uma foto real, atualize `src` e `alt` e use `layout: "photo"`, ou omita `layout`. Isso remove os recortes destinados ao painel de identidade. A foto é ajustada ao espaço disponível. Para mostrar uma segunda vista, adicione outro objeto à lista `images`.

Os slogans, a apresentação institucional, os rótulos gerais de teste e outros textos editoriais ficam no HTML. As logos usam PNGs com fundo transparente, separados das imagens dos produtos:

- `assets/images/morfy-official-logo-transparent.png`: logo principal nos cabeçalhos e rodapés.
- `assets/images/morfy-official-symbol-transparent.png`: símbolo da seção “A essência MORFY” e ícone do site.
- `assets/images/deckra-logo-transparent.png`: logo e slogan no hero DECKRA.

Para atualizar a identidade, substitua o PNG correspondente mantendo transparência, proporção e margens livres nas bordas. O CSS usa `contain` para exibir as logos inteiras e permite que a sombra suave do símbolo MORFY ultrapasse a caixa da imagem. Se mudar o nome do arquivo, atualize a referência no HTML ou no CSS. A troca das fotos em `products.js` não modifica essas logos. Os cinco arquivos oficiais fornecidos pela marca estão preservados em `assets/images/`:

| Arquivo recebido | Arquivo no repositório | Uso |
| --- | --- | --- |
| `Morfy (1).png` | `morfy-official-light-source.png` | Versão clara; origem da logo transparente usada no site escuro. |
| `Morfy (2).png` | `morfy-official-symbol-source.png` | Símbolo azul; origem do PNG transparente da seção MORFY e do ícone do site. |
| `Morfy (3).png` | `morfy-official-digital-real.png` | Arte horizontal oficial, disponível para uso futuro. |
| `Morfy (4).png` | `morfy-official-brand-poster.png` | Pôster de identidade oficial, disponível para uso futuro. |
| `Morfy.png` | `morfy-official-dark-source.png` | Versão escura para aplicação sobre fundos claros. |

As versões transparentes mantêm a arte oficial e removem o fundo branco e as margens vazias por processamento de pixels. Os originais foram preservados sem alteração. Os cinco originais desta entrega ficam disponíveis no repositório; as páginas usam apenas as duas versões transparentes derivadas deles.

## Marketplaces e disponibilidade

A configuração atual usa `demoMode: false`. Os links reais da Shopee foram fornecidos pelo responsável pela loja:

| Produto | Anúncio da Shopee |
| --- | --- |
| Gatinho Cavaleiro | https://br.shp.ee/iUUqJ7XX |
| Boneco de Neve | https://br.shp.ee/w63hYWts |
| Chaveiro TNT | https://br.shp.ee/H42PTTaN |

Cada produto possui campos próprios em `marketplaces`. Cole o endereço completo **HTTPS do anúncio daquele produto**. Não use links genéricos de loja ou de outro item. Deixe `""` quando não houver anúncio.

```javascript
marketplaces: {
  shopee: "https://br.shp.ee/H42PTTaN",
  mercadoLivre: "",
  tiktokShop: ""
}
```

Na lista global `marketplaces`, Mercado Livre e TikTok Shop têm `unavailable: true`:

```javascript
{ id: "mercadoLivre", label: "Mercado Livre", unavailable: true },
{ id: "tiktokShop", label: "TikTok Shop", unavailable: true }
```

Esse estado exibe a plataforma com o selo **Indisponível** e botão desabilitado, mesmo se houver um link antigo. Para liberar uma plataforma futuramente, remova `unavailable` ou altere para `false`, cadastre os anúncios em cada produto e confira os destinos. Com `demoMode: false`, uma plataforma sem anúncio e sem `unavailable: true` fica oculta.

O DECKRA ainda não possui link da Shopee, portanto não exibe botão ativo dessa plataforma. Mercado Livre e TikTok Shop aparecem indisponíveis também nessa página.

Os links ativos abrem em nova aba, com `target="_blank"` e `rel="noopener noreferrer"`. O site prioriza **“Consultar preço na plataforma”**; preço, frete, estoque, variações e quantidade são definidos no anúncio.

Os três produtos atuais usam `demo: false`, sem selo de conceito. `demoMode: true` continua disponível para testar botões sem anúncios; ele não reativa plataformas marcadas com `unavailable: true`.

## GitHub Pages: publicação e configuração

O site está publicado em [velasmart.github.io/Morfy3D](https://velasmart.github.io/Morfy3D/), com a branch `main` e a pasta `/(root)` como origem. As instruções abaixo permitem revisar ou reconfigurar a publicação.

1. Confirme que `index.html`, `deckra.html`, `assets/` e `.nojekyll` estão na raiz da branch `main`.
2. Abra [Settings → Pages do repositório](https://github.com/VelaSmart/Morfy3D/settings/pages), com uma conta que tenha permissão para configurar o Pages.
3. Em **Build and deployment → Source**, escolha **Deploy from a branch**.
4. Selecione **main** e **/(root)** e clique em **Save**.
5. Aguarde a publicação e use o endereço mostrado nessa tela.

Não é necessário criar um workflow próprio nem executar um build. O GitHub publica os arquivos estáticos e repete a publicação quando a branch escolhida recebe alterações. Se houver erro, consulte a execução de publicação em **Actions**. Referência: [configurar a origem de publicação do GitHub Pages](https://docs.github.com/en/pages/getting-started-with-github-pages/configuring-a-publishing-source-for-your-github-pages-site).

## Identidade visual e origem dos arquivos

A MORFY usa base escura, azul e a fonte Montserrat. O DECKRA usa base escura, roxo e a fonte Sora. As cores, espaçamentos e regras responsivas estão em `assets/css/styles.css`.

As fontes estão hospedadas localmente. Preserve suas licenças ao distribuir o projeto: [Montserrat — SIL OFL](assets/fonts/Montserrat-OFL.txt) e [Sora — SIL OFL](assets/fonts/Sora-OFL.txt).

As logos oficiais MORFY, o painel DECKRA e as imagens dos três produtos foram fornecidos pela marca. A abertura apresenta a loja, destaca o botão Explorar produtos e usa o fundo técnico original `assets/images/morfy-slicing-hero.png`, criado com a ferramenta integrada ImageGen. O prompt final e a origem estão registrados em `tools/hero-slicing-prompt.txt`. O desenho ilustra modelo, camadas e plano de fatiamento; não é um produto adicional do catálogo. O texto permanece no HTML para acessibilidade, e a posição da imagem se adapta ao celular. As seis ilustrações SVG dos produtos provisórios foram removidas. A antiga imagem conceitual `assets/images/morfy-hero.png` e o registro `tools/hero-image-prompt.txt` permanecem arquivados e não são exibidos no catálogo.

## Verificar alterações

Com Node.js instalado, execute na raiz do projeto:

```sh
node --check assets/js/products.js
node --check assets/js/app.js
node --check tools/serve.cjs
node --check tools/verify.cjs
node tools/verify.cjs
```

As checagens de sintaxe detectam erros nos scripts. O verificador confere as duas páginas, os arquivos e as âncoras locais, o uso de caminhos em subdiretório, maiúsculas e minúsculas, dados dos produtos, imagens, links, fontes, licenças e `.nojekyll`. Ele não confirma estoque, especificações ou se um anúncio pertence ao produto correto.

Depois, confira as duas páginas no navegador em computador e celular: busca, filtros, abertura e fechamento do modal, navegação por teclado, menu e botões dos marketplaces. Teste também o modo `demoMode: false` com e sem anúncios configurados.

## Problemas comuns

| Problema | O que conferir |
| --- | --- |
| Pages retorna 404 | Pages habilitado, publicação concluída, branch `main`, pasta `/(root)` e `index.html` diretamente na raiz. |
| Imagem, estilo ou fonte não carrega | Caminho relativo, arquivo enviado e maiúsculas/minúsculas iguais. Um caminho iniciado por `/` ignora `/Morfy3D/`. |
| Catálogo fica vazio após editar | Erros no console do navegador e resultado de `node --check assets/js/products.js`; confira vírgulas, aspas e colchetes. |
| Categoria nova não tem filtro | Rótulo em `app.js`, botão `data-category` em `index.html` e identificador igual no produto. |
| Botão só mostra aviso | Link vazio ou inválido e `demoMode: true`. Cadastre o anúncio real e desative o modo de teste. |
| Botão desapareceu | Em `demoMode: false`, a plataforma precisa de um link HTTPS válido ou de `unavailable: true` para aparecer desabilitada. |
| Plataforma continua indisponível | Confira `unavailable` na lista global `marketplaces`; altere para `false` e cadastre o anúncio de cada produto. |
| Foto DECKRA aparece com recorte de painel | Remova `layout: "reference-board"` e use `"photo"`, ou omita o campo. |
| Prévia informa que a porta está ocupada | Encerre a prévia anterior. No servidor Node, a variável `MORFY_PREVIEW_PORT` permite escolher outra porta. |
| Alteração publicada não aparece | Aguarde a execução de publicação, confira **Actions** e recarregue o navegador sem cache. |

## Configurar um domínio próprio futuramente

1. Registre o domínio e verifique sua propriedade em **Pages nas configurações da conta ou organização proprietária**, conforme o [guia de verificação de domínio](https://docs.github.com/en/pages/configuring-a-custom-domain-for-your-github-pages-site/verifying-your-custom-domain-for-github-pages).
2. No repositório, abra **Settings → Pages → Custom domain**, informe o domínio e salve.
3. Configure o DNS no provedor. Para um subdomínio como `www`, o registro `CNAME` aponta para `velasmart.github.io`, **sem `/Morfy3D/`**. Para o domínio raiz, siga os registros atuais da [documentação de domínio próprio do GitHub Pages](https://docs.github.com/en/pages/configuring-a-custom-domain-for-your-github-pages-site/managing-a-custom-domain-for-your-github-pages-site).
4. Aguarde a atualização do DNS e habilite **Enforce HTTPS** quando disponível.

Na publicação por branch, salvar o domínio no Pages cria um arquivo `CNAME` no repositório. Preserve-o nas próximas atualizações.

## Antes de usar como vitrine comercial

- Substitua conceitos e imagens demonstrativas pelos produtos e fotos reais.
- Confirme descrições e características, sem promessas ou especificações presumidas.
- Cadastre os anúncios corretos por produto e teste cada botão.
- Use `demoMode: false`, ajuste `demo` dos produtos reais e revise os textos gerais de teste.
- Rode o verificador e confira o site publicado em celular e por teclado.

## Software DECKRA

A seção `deckra.html#software` explica a associação de aplicativos do PC aos potenciômetros para controlar o volume e a atribuição de funções e atalhos às teclas. A imagem `assets/images/deckra-software-demo.png` é uma prévia ilustrativa, não uma captura do programa final.

Para liberar o download, configure `deckra.softwareDownloadUrl` em `assets/js/products.js` com uma URL HTTPS real do instalador ou da página de distribuição. Enquanto o campo estiver vazio, o botão permanece desabilitado como “Download em breve”. Não há instalador, versão ou compatibilidade presumidos no projeto.

O download aparece no início da seção de software, logo após o hero. O botão destacado continua sem ação enquanto não houver instalador configurado. A compra do DECKRA usa a mesma lista vertical do catálogo 3D; plataformas sem anúncio aparecem como indisponíveis, incluindo a Shopee.

As imagens conceituais do hardware DECKRA agora usam arquivos independentes (`deckra-controller-hero.png` e `deckra-controller-detail.png`), gerados com image_gen a partir da direção visual do painel de marca. O hero mostra o dispositivo inteiro, sem ampliação de recortes do moodboard; os detalhes são uma segunda visualização conceitual. Não representam fotografia de um modelo final.
