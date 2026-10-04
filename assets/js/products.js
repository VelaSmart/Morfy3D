/*
 * Configuração da vitrine MORFY.
 * Edite os produtos, imagens e anúncios neste arquivo.
 * demoMode mantém os botões visíveis mesmo sem anúncios configurados.
 * Quando houver produtos reais, troque demoMode para false e demo para false.
 * Use somente links completos e seguros, começando com https://.
 */
window.MORFY_CONFIG = {
  demoMode: true,
  marketplaces: [
    { id: "shopee", label: "Shopee" },
    { id: "mercadoLivre", label: "Mercado Livre" },
    { id: "tiktokShop", label: "TikTok Shop" }
  ],
  products: [
    {
      id: "organizer",
      name: "Organizador Geométrico",
      category: "utilitarios",
      description: "Um conceito de organização para dar lugar às pequenas coisas do dia a dia. Linhas geométricas que encontram espaço na sua mesa.",
      images: [{ src: "./assets/images/organizer.svg", alt: "Ilustração de um organizador geométrico preto" }],
      features: [],
      demo: true,
      marketplaces: { shopee: "", mercadoLivre: "", tiktokShop: "" }
    },
    {
      id: "origami",
      name: "Vaso Origami",
      category: "decoracao",
      description: "Dobras, luz e sombra em uma peça de inspiração geométrica. Um conceito para compor ambientes com personalidade.",
      images: [{ src: "./assets/images/origami.svg", alt: "Ilustração de um vaso branco com faces geométricas" }],
      features: [],
      demo: true,
      marketplaces: { shopee: "", mercadoLivre: "", tiktokShop: "" }
    },
    {
      id: "arc",
      name: "Suporte Arc",
      category: "utilitarios",
      description: "Uma forma simples para acompanhar seu espaço de trabalho. Este conceito explora curvas suaves e uma presença discreta no setup.",
      images: [{ src: "./assets/images/arc.svg", alt: "Ilustração de um suporte preto com perfil curvo" }],
      features: [],
      demo: true,
      marketplaces: { shopee: "", mercadoLivre: "", tiktokShop: "" }
    },
    {
      id: "dragon",
      name: "Dragão Articulado",
      category: "geek",
      description: "A imaginação ganha forma em um personagem cheio de detalhes. Uma proposta visual para quem gosta de fantasia e objetos fora do comum.",
      images: [{ src: "./assets/images/dragon.svg", alt: "Ilustração de um pequeno dragão branco de fantasia" }],
      features: [],
      demo: true,
      marketplaces: { shopee: "", mercadoLivre: "", tiktokShop: "" }
    },
    {
      id: "dock",
      name: "Dock Modular",
      category: "utilitarios",
      description: "Um conceito de apoio para organizar o que acompanha sua rotina. Design que conversa com a mesa, os acessórios e o seu jeito de trabalhar.",
      images: [{ src: "./assets/images/dock.svg", alt: "Ilustração de um dock azul e preto para uma mesa" }],
      features: [],
      demo: true,
      marketplaces: { shopee: "", mercadoLivre: "", tiktokShop: "" }
    },
    {
      id: "linear",
      name: "Cachepot Linear",
      category: "decoracao",
      description: "Ritmo e textura para trazer um novo detalhe ao ambiente. Uma ideia de cachepot que combina linhas verticais e uma silhueta essencial.",
      images: [{ src: "./assets/images/linear.svg", alt: "Ilustração de um cachepot azul com linhas verticais e uma planta" }],
      features: [],
      demo: true,
      marketplaces: { shopee: "", mercadoLivre: "", tiktokShop: "" }
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
