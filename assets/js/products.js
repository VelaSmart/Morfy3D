/*
 * Configuração da vitrine MORFY.
 * Edite os produtos, imagens e anúncios neste arquivo.
 * demoMode=false desliga os botões de teste sem anúncios.
 * unavailable=true mantém uma plataforma visível como indisponível,
 * sem link nem ação de clique. Cadastre anúncios separados por produto.
 * Use somente links completos e seguros, começando com https://.
 */
window.MORFY_CONFIG = {
  demoMode: false,
  marketplaces: [
    { id: "shopee", label: "Shopee" },
    { id: "mercadoLivre", label: "Mercado Livre", unavailable: true },
    { id: "tiktokShop", label: "TikTok Shop", unavailable: true }
  ],
  products: [
    {
      id: "gatinho-cavaleiro",
      name: "Gatinho Cavaleiro",
      category: "geek",
      description: "Um pequeno herói cheio de personalidade. Escultura em impressão 3D com armadura, espada e capa rosa, nas versões branca e laranja.",
      images: [
        { src: "./assets/images/gatinho-cavaleiro-capa.png", alt: "Gatinho Cavaleiro laranja com espada e capa rosa em cenário de jardim", label: "Apresentação" },
        { src: "./assets/images/gatinho-cavaleiro-laranja.png", alt: "Vista frontal do Gatinho Cavaleiro laranja, com armadura cinza, espada e laço rosa", label: "Laranja" },
        { src: "./assets/images/gatinho-cavaleiro-branco.png", alt: "Vista frontal do Gatinho Cavaleiro branco, com armadura cinza, espada e laço rosa", label: "Branco" },
        { src: "./assets/images/gatinho-cavaleiro-lateral.png", alt: "Vista lateral do Gatinho Cavaleiro laranja mostrando a armadura e a capa rosa", label: "Lateral" },
        { src: "./assets/images/gatinho-cavaleiro-verso.png", alt: "Vista traseira do Gatinho Cavaleiro laranja mostrando a capa rosa e a cauda", label: "Verso" },
        { src: "./assets/images/gatinho-cavaleiro-medidas-laranja.png", alt: "Gatinho Cavaleiro laranja com régua ilustrando a altura de 28 cm até a ponta da espada", label: "Medidas · laranja" },
        { src: "./assets/images/gatinho-cavaleiro-medidas-branco.png", alt: "Gatinho Cavaleiro branco com régua ilustrando a altura de 28 cm até a ponta da espada", label: "Medidas · branco" }
      ],
      features: [
        "Escultura decorativa em impressão 3D.",
        "Versões branca e laranja, apresentadas na galeria.",
        "Altura aproximada de 21 cm até a cabeça e 28 cm até a ponta da espada, conforme as imagens fornecidas.",
        "Armadura cinza, espada, capa e laço rosa."
      ],
      demo: false,
      marketplaces: { shopee: "https://br.shp.ee/iUUqJ7XX", mercadoLivre: "", tiktokShop: "" }
    },
    {
      id: "boneco-de-neve",
      name: "Boneco de Neve",
      category: "decoracao",
      description: "Um toque de Natal para a sua decoração. Boneco de neve em impressão 3D com textura em relevo, gorro e cachecol vermelhos, em versões de 6 e 10 cm.",
      images: [
        { src: "./assets/images/boneco-de-neve-capa.png", alt: "Apresentação natalina do Boneco de Neve com gorro e cachecol vermelhos e detalhes da textura em relevo", label: "Apresentação" },
        { src: "./assets/images/boneco-de-neve-detalhe.png", alt: "Boneco de Neve visto de frente e de lado, com corpo branco, nariz laranja e detalhes pretos", label: "Detalhes" },
        { src: "./assets/images/boneco-de-neve-lateral.png", alt: "Vista lateral direita do Boneco de Neve com textura em relevo e cachecol vermelho", label: "Lateral direita" },
        { src: "./assets/images/boneco-de-neve-outro-angulo.png", alt: "Vista lateral esquerda do Boneco de Neve mostrando o gorro, o nariz e os botões", label: "Lateral esquerda" },
        { src: "./assets/images/boneco-de-neve-6cm.png", alt: "Boneco de Neve ao lado de uma régua indicando a versão de 6 cm de altura", label: "6 cm" },
        { src: "./assets/images/boneco-de-neve-10cm.png", alt: "Boneco de Neve ao lado de uma régua indicando a versão de 10 cm de altura", label: "10 cm" }
      ],
      features: [
        "Peça decorativa em impressão 3D com textura em relevo.",
        "Versões de 6 cm e 10 cm de altura, conforme as imagens fornecidas.",
        "Corpo branco, gorro e cachecol vermelhos, nariz laranja e detalhes pretos.",
        "Estilo natalino para compor a decoração."
      ],
      demo: false,
      marketplaces: { shopee: "https://br.shp.ee/w63hYWts", mercadoLivre: "", tiktokShop: "" }
    },
    {
      id: "chaveiro-tnt",
      name: "Chaveiro TNT",
      category: "geek",
      description: "Um detalhe pixelado para levar com você. Chaveiro em impressão 3D com visual de bloco TNT, cores vermelha, branca e preta e argola metálica.",
      images: [
        {"src":"./assets/images/chaveiro-tnt-shopee-01.jpg","alt":"Arte do anúncio com o Chaveiro TNT, argola metálica, diferentes ângulos e detalhes da peça","label":"Apresentação"},
        {"src":"./assets/images/chaveiro-tnt-shopee-02.jpg","alt":"Chaveiro TNT na palma da mão, com corrente e argola metálica, em foto do anúncio","label":"Foto real · na mão"},
        {"src":"./assets/images/chaveiro-tnt-shopee-03.jpg","alt":"Chaveiro TNT sobre uma mesa branca com corrente e argola metálica","label":"Foto real · com argola"},
        {"src":"./assets/images/chaveiro-tnt-shopee-04.jpg","alt":"Bloco TNT segurado entre os dedos, mostrando os detalhes pixelados em relevo","label":"Foto real · detalhes"},
        {"src":"./assets/images/chaveiro-tnt-shopee-05.jpg","alt":"Chaveiro TNT individual com corrente e argola metálica sobre uma mesa branca","label":"Chaveiro individual"},
        {"src":"./assets/images/chaveiro-tnt-shopee-06.jpg","alt":"Três peças TNT empilhadas sobre fundo branco","label":"Peças empilhadas"},
        {"src":"./assets/images/chaveiro-tnt-shopee-07.jpg","alt":"Cinco peças TNT agrupadas sobre fundo branco","label":"Conjunto de peças"}
      ],
      features: [
        "Chaveiro em impressão 3D com visual de bloco TNT.",
        "Detalhes pixelados em relevo, nas cores vermelha, branca e preta.",
        "Corrente e argola metálica apresentadas nas fotos do chaveiro.",
        "Confira no anúncio da Shopee a quantidade, as opções e as condições de compra."
      ],
      demo: false,
      marketplaces: { shopee: "https://br.shp.ee/H42PTTaN", mercadoLivre: "", tiktokShop: "" }
    }
  ],
  deckra: {
    name: "DECKRA",
    category: "Controladores e periféricos",
    description: "Controladores e periféricos personalizados para PC. Uma proposta que une design, fabricação própria e cultura maker para dar forma ao seu jeito de criar, jogar e trabalhar.",
    images: [{
      src: "./assets/images/deckra-reference.png",
      alt: "Referência conceitual de um controlador DECKRA com teclas e controles giratórios",
      layout: "reference-board"
    }],
    features: [],
    demo: true,
    marketplaces: { shopee: "", mercadoLivre: "", tiktokShop: "" }
  }
};
