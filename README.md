# MORFY / Morfy3D

Vitrine de demonstração da **MORFY — Ideias que ganham forma**, com catálogo de impressão 3D e uma página dedicada ao **DECKRA — Hardware feito para você**.

O site usa HTML, CSS e JavaScript, com imagens e fontes locais. Funciona no navegador e pode ser hospedado no GitHub Pages. Não exige build, instalação de pacotes, banco de dados ou backend. A compra acontece nos anúncios específicos dos produtos na Shopee, no Mercado Livre e no TikTok Shop; o site não tem carrinho, checkout, pagamento ou cadastro de compradores.

Repositório: [VelaSmart/Morfy3D](https://github.com/VelaSmart/Morfy3D).

Site publicado: [Abrir MORFY](https://velasmart.github.io/Morfy3D/) · [Abrir DECKRA](https://velasmart.github.io/Morfy3D/deckra.html).

**Esta é uma versão de teste.** Os produtos, ilustrações e imagens conceituais permitem avaliar o visual e a navegação. Não representam estoque, preço, disponibilidade, dimensões, materiais ou compatibilidade confirmados.

## Páginas e recursos

O projeto tem exatamente duas páginas principais:

| Página | Conteúdo |
| --- | --- |
| `index.html` | Catálogo MORFY, busca, filtros de categoria e modal de detalhes com “Onde comprar”. |
| `deckra.html` | Apresentação DECKRA, imagens conceituais e seção de marketplaces acessível pelo hero e pelo encerramento. |

A interface adapta-se a computador e celular. Inclui navegação por teclado, foco visível, textos alternativos, aviso acessível dos botões de demonstração e suporte à preferência de movimento reduzido. A busca considera nome, descrição e categoria, sem diferenciar acentos ou maiúsculas. Cada card abre os detalhes com **“Ver detalhes”**.

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
    └── hero-image-prompt.txt       Registro do prompt da imagem principal
```

Os arquivos do site devem ficar **na raiz do repositório**, sem uma pasta `morfy-site` envolvendo `index.html`. Todos os caminhos de navegação, estilos, scripts, fontes e imagens são relativos, para funcionar também em `/Morfy3D/`.

## Atualizar o catálogo

Edite `assets/js/products.js`. A configuração fica em `window.MORFY_CONFIG`; os cards são gerados a partir da lista `products`.

Este exemplo completo usa um item demonstrativo já existente. Ao duplicá-lo, escolha outro `id` e substitua o conteúdo:

```javascript
{
  id: "organizer",
  name: "Organizador Geométrico",
  category: "utilitarios",
  description: "Um conceito de organização para dar lugar às pequenas coisas do dia a dia.",
  images: [{
    src: "./assets/images/organizer.svg",
    alt: "Ilustração de um organizador geométrico preto"
  }],
  features: [],
  demo: true,
  marketplaces: {
    shopee: "",
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
| `images` | Lista de objetos com caminho `src` e descrição alternativa `alt`. O catálogo e o modal exibem a primeira imagem. |
| `features` | Lista de textos com características confirmadas. Deixe vazia enquanto não houver informações verificadas. |
| `demo` | `true` identifica um conceito; `false` remove o selo “Conceito” e o aviso de demonstração nos detalhes do item real. |
| `marketplaces` | Links separados por produto e por plataforma. |

Para adicionar um produto, duplique um objeto dentro de `products`, altere o identificador e os dados e preserve as vírgulas entre os objetos. Para remover, exclua o objeto correspondente. **A ordem da lista define a ordem dos cards**; mova os objetos para reorganizar o catálogo.

Não acrescente dimensões, materiais, resistência, quantidade de controles ou compatibilidade por suposição. Preencha `features` com dados do produto real e use descrições coerentes com as fotos e os anúncios.

### Categorias e filtros

As categorias atuais são `utilitarios`, `decoracao` e `geek`. O filtro `todos` reúne todos os itens.

Para adicionar outra categoria:

1. Defina o mesmo identificador em `category` nos produtos correspondentes.
2. Adicione seu rótulo ao objeto `categories` no início de `assets/js/app.js`.
3. Adicione um botão em `.category-filters` no `index.html`, com `data-category` igual ao novo identificador e `aria-pressed="false"`.

### Imagens e textos alternativos

Coloque novas imagens em `assets/images/` e atualize o caminho na configuração. Use nomes simples, preferencialmente sem espaços, e preserve maiúsculas e minúsculas. Por exemplo, `./assets/images/organizer.svg` é um caminho relativo válido; `/assets/images/organizer.svg` começa na raiz do domínio e quebra a hospedagem em subdiretório.

Use `alt` para descrever o objeto e o que a imagem mostra. Para fotos reais, substitua descrições como “ilustração” por uma descrição adequada da foto. WebP, JPG e PNG podem ser usados. Redimensione e comprima as imagens antes de adicioná-las para manter o carregamento leve.

## Atualizar o DECKRA

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

Os slogans, a apresentação institucional, os rótulos gerais de teste e outros textos editoriais ficam no HTML. O logotipo DECKRA usa um trecho da referência visual definido no CSS; a troca das fotos do produto pela configuração não modifica esse logo.

## Modo de demonstração e marketplaces

Na configuração inicial:

```javascript
demoMode: true
```

Os três botões aparecem, mesmo com links vazios, para avaliar o layout. Um botão sem anúncio mostra um aviso de demonstração e não abre uma página fictícia. Um link válido já abre o anúncio em nova aba.

Cada produto, inclusive o DECKRA, possui seus próprios campos:

```javascript
marketplaces: {
  shopee: "",
  mercadoLivre: "",
  tiktokShop: ""
}
```

Cole o endereço completo **HTTPS do anúncio daquele produto** no campo da plataforma correspondente. Confirme que ele abre o item correto na Shopee, no Mercado Livre ou no TikTok Shop. Mantenha `""` quando não houver anúncio. Não reutilize um link de outro produto nem substitua por um endereço genérico de loja.

Para exibir somente as plataformas configuradas:

```javascript
demoMode: false
```

Nesse modo, links vazios ou inválidos não geram botões. O produto continua disponível para consulta e, se não houver nenhum link, os detalhes informam que os anúncios ainda não foram configurados. A seção DECKRA usa a mesma regra.

`demoMode` controla a presença dos botões de demonstração. Ao cadastrar produtos reais, altere também `demo: false` em cada objeto correspondente. Revise os textos gerais de demonstração em `index.html` e `deckra.html` antes de apresentar a vitrine como catálogo comercial.

Os botões identificam a plataforma e abrem os anúncios com `target="_blank"` e `rel="noopener noreferrer"`, preservando o site MORFY. O site usa **“Consultar preço na plataforma”**; preço, disponibilidade, frete e condições são definidos no marketplace.

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

O logo MORFY e o painel DECKRA vieram das referências fornecidas. As seis imagens do catálogo são ilustrações SVG de conceitos. A imagem principal `assets/images/morfy-hero.png` foi criada com a ferramenta integrada ImageGen; o registro do prompt está em `tools/hero-image-prompt.txt`. Para a vitrine comercial, use fotos dos produtos reais e atualize os respectivos textos e avisos.

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
| Botão só mostra aviso | Link vazio ou inválido e `demoMode: true`. Cadastre o anúncio real ou mantenha o comportamento de teste. |
| Botão desapareceu | Em `demoMode: false`, a plataforma precisa de um link HTTPS válido. |
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
