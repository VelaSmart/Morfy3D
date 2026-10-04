(function () {
  "use strict";

  var categories = {
    todos: "Todos",
    utilitarios: "Utilitários",
    decoracao: "Decoração",
    geek: "Geek"
  };
  var svgNamespace = "http://www.w3.org/2000/svg";

  function createElement(tag, className, text) {
    var element = document.createElement(tag);
    if (className) element.className = className;
    if (text !== undefined) element.textContent = text;
    return element;
  }

  function normalize(value) {
    return String(value || "").normalize("NFD").replace(/[\u0300-\u036f]/g, "").toLowerCase().trim();
  }

  function safeMarketplaceUrl(value) {
    if (typeof value !== "string" || !value.trim()) return null;
    try {
      var url = new URL(value.trim());
      return url.protocol === "https:" && url.hostname && !url.username && !url.password ? url.href : null;
    } catch (error) {
      return null;
    }
  }

  // Ícones próprios e discretos: sacola, aperto de mãos e nota musical.
  function marketplaceIcon(id) {
    var svg = document.createElementNS(svgNamespace, "svg");
    svg.setAttribute("viewBox", "0 0 24 24");
    svg.setAttribute("width", "20");
    svg.setAttribute("height", "20");
    svg.setAttribute("fill", "none");
    svg.setAttribute("stroke", "currentColor");
    svg.setAttribute("stroke-width", "1.6");
    svg.setAttribute("stroke-linecap", "round");
    svg.setAttribute("stroke-linejoin", "round");
    svg.setAttribute("aria-hidden", "true");
    svg.setAttribute("focusable", "false");
    var paths = {
      shopee: ["M5 7h14l1 14H4L5 7Z", "M8 8V6a4 4 0 0 1 8 0v2", "M14 11h-3a1.5 1.5 0 0 0 0 3h2a1.5 1.5 0 0 1 0 3h-3"],
      mercadoLivre: ["m2 10 4-4 4 2 4-2 4 1 4 4-5 7-3 1-7-3-5-6Z", "m10 8-3 3 2 2 4-3 5 5", "m10 16 2 2", "m13 15 3 3"],
      tiktokShop: ["M14 3v12a4 4 0 1 1-4-4", "M14 3c0 3 2 5 5 5"]
    };
    (paths[id] || paths.shopee).forEach(function (shape) {
      var path = document.createElementNS(svgNamespace, "path");
      path.setAttribute("d", shape);
      svg.appendChild(path);
    });
    return svg;
  }

  function ready() {
    var config = window.MORFY_CONFIG || {};
    var products = Array.isArray(config.products) ? config.products : [];
    var marketplaces = Array.isArray(config.marketplaces) ? config.marketplaces : [];
    var toast = document.getElementById("site-toast");
    var toastParent = toast ? toast.parentNode : null;
    var toastTimer;

    function showToast(message) {
      if (!toast) return;
      // Native modal dialogs make the rest of the document inert.
      // Keep their feedback inside the active dialog for sighted and screen-reader users.
      var host = dialog && dialog.open ? dialog : toastParent;
      if (host && toast.parentNode !== host) host.appendChild(toast);
      window.clearTimeout(toastTimer);
      toast.textContent = message;
      toast.hidden = false;
      toast.classList.add("is-visible");
      toastTimer = window.setTimeout(function () {
        toast.classList.remove("is-visible");
        toast.hidden = true;
        toast.textContent = "";
      }, 5500);
    }

    function marketplaceText(marketplace) {
      return marketplace.id === "shopee" ? "Ver na " + marketplace.label : "Ver no " + marketplace.label;
    }

    function renderMarketplaces(container, product) {
      if (!container) return;
      container.replaceChildren();
      var links = product && product.marketplaces ? product.marketplaces : {};
      var buttonCount = 0;

      marketplaces.forEach(function (marketplace) {
        var url = safeMarketplaceUrl(links[marketplace.id]);
        if (!url && !config.demoMode) return;
        var button = createElement(url ? "a" : "button", "marketplace-button");
        button.dataset.marketplace = marketplace.id;
        button.appendChild(marketplaceIcon(marketplace.id));
        button.appendChild(createElement("span", "marketplace-label", marketplaceText(marketplace)));

        if (url) {
          button.href = url;
          button.target = "_blank";
          button.rel = "noopener noreferrer";
          button.setAttribute("aria-label", marketplaceText(marketplace) + " — abre em uma nova aba");
          var arrow = createElement("span", "marketplace-arrow", "↗");
          arrow.setAttribute("aria-hidden", "true");
          button.appendChild(arrow);
        } else {
          button.type = "button";
          button.classList.add("is-demo");
          button.appendChild(createElement("span", "marketplace-status", "Teste"));
          button.setAttribute("aria-label", marketplaceText(marketplace) + ": anúncio ainda não configurado");
          button.addEventListener("click", function () {
            showToast("Site de demonstração: o anúncio de " + product.name + " na plataforma " + marketplace.label + " ainda não foi configurado.");
          });
        }
        container.appendChild(button);
        buttonCount += 1;
      });

      if (!buttonCount) {
        container.appendChild(createElement("p", "marketplace-empty", "Os links para este produto ainda não foram configurados."));
      }
    }

    var grid = document.getElementById("product-grid");
    var search = document.getElementById("product-search");
    var count = document.getElementById("product-count");
    var emptyState = document.getElementById("empty-state");
    var filterButtons = Array.from(document.querySelectorAll("[data-category]"));
    var activeCategory = "todos";
    var dialog = document.getElementById("product-dialog");
    var lastTrigger = null;

    function closeDialog() {
      if (!dialog) return;
      if (typeof dialog.close === "function") dialog.close();
      else {
        dialog.removeAttribute("open");
        document.body.classList.remove("modal-open");
        if (lastTrigger && document.contains(lastTrigger)) lastTrigger.focus();
      }
    }

    function openProduct(product, trigger) {
      if (!dialog) return;
      lastTrigger = trigger;
      var image = document.getElementById("modal-image");
      var firstImage = product.images && product.images[0];
      if (image && firstImage) {
        image.src = firstImage.src;
        image.alt = firstImage.alt || product.name;
      }
      var textFields = {
        "modal-category": categories[product.category] || product.category,
        "modal-title": product.name,
        "modal-description": product.description
      };
      Object.keys(textFields).forEach(function (id) {
        var field = document.getElementById(id);
        if (field) field.textContent = textFields[id] || "";
      });

      var featureList = document.getElementById("modal-features");
      var features = Array.isArray(product.features) ? product.features : [];
      if (featureList) {
        featureList.replaceChildren();
        features.forEach(function (feature) {
          featureList.appendChild(createElement("li", "", String(feature)));
        });
        featureList.hidden = features.length === 0;
        var featureSection = featureList.closest("[data-features-section]");
        if (featureSection) featureSection.hidden = features.length === 0;
      }

      var demoNote = document.getElementById("modal-demo-note");
      if (demoNote) {
        demoNote.hidden = !product.demo;
        demoNote.textContent = product.demo ? "Modelo visual de demonstração. Imagens, características e anúncios oficiais serão configurados posteriormente." : "";
      }
      renderMarketplaces(document.getElementById("modal-marketplaces"), product);
      if (typeof dialog.showModal === "function") dialog.showModal();
      else dialog.setAttribute("open", "");
      document.body.classList.add("modal-open");
      var closeButton = dialog.querySelector("[data-close-dialog]");
      if (closeButton) closeButton.focus({ preventScroll: true });
    }

    function productCard(product) {
      var article = createElement("article", "product-card");
      var visual = createElement("div", "product-visual");
      var firstImage = product.images && product.images[0];
      if (firstImage) {
        var image = createElement("img");
        image.src = firstImage.src;
        image.alt = firstImage.alt || product.name;
        image.loading = "lazy";
        image.decoding = "async";
        image.width = 600;
        image.height = 480;
        visual.appendChild(image);
      }
      if (product.demo) visual.appendChild(createElement("span", "product-tag", "Conceito"));
      var body = createElement("div", "product-body");
      body.appendChild(createElement("span", "product-category", categories[product.category] || product.category));
      body.appendChild(createElement("h3", "", product.name));
      body.appendChild(createElement("p", "", product.description));
      var button = createElement("button", "detail-button", "Ver detalhes");
      button.type = "button";
      button.dataset.product = product.id;
      button.setAttribute("aria-label", "Ver detalhes de " + product.name);
      button.setAttribute("aria-haspopup", "dialog");
      button.addEventListener("click", function () { openProduct(product, button); });
      var arrow = createElement("span", "button-arrow", "↗");
      arrow.setAttribute("aria-hidden", "true");
      button.appendChild(arrow);
      body.appendChild(button);
      article.appendChild(visual);
      article.appendChild(body);
      return article;
    }

    function renderProducts() {
      if (!grid) return;
      var query = normalize(search ? search.value : "");
      var visible = products.filter(function (product) {
        var categoryMatch = activeCategory === "todos" || activeCategory === "all" || product.category === activeCategory;
        var searchable = normalize([product.name, product.description, categories[product.category] || product.category].join(" "));
        return categoryMatch && (!query || searchable.indexOf(query) !== -1);
      });
      grid.replaceChildren();
      visible.forEach(function (product) { grid.appendChild(productCard(product)); });
      if (count) count.textContent = visible.length + (visible.length === 1 ? " produto" : " produtos");
      if (emptyState) emptyState.hidden = visible.length !== 0;
      filterButtons.forEach(function (button) {
        var pressed = button.dataset.category === activeCategory;
        button.setAttribute("aria-pressed", String(pressed));
        button.classList.toggle("is-active", pressed);
      });
    }

    if (search) search.addEventListener("input", renderProducts);
    filterButtons.forEach(function (button) {
      button.addEventListener("click", function () {
        activeCategory = button.dataset.category;
        renderProducts();
      });
    });
    var resetButton = document.getElementById("reset-filters");
    if (resetButton) resetButton.addEventListener("click", function () {
      activeCategory = "todos";
      if (search) {
        search.value = "";
        search.focus();
      }
      renderProducts();
    });
    renderProducts();

    if (dialog) {
      dialog.querySelectorAll("[data-close-dialog]").forEach(function (button) {
        button.addEventListener("click", closeDialog);
      });
      dialog.addEventListener("click", function (event) {
        if (event.target !== dialog) return;
        var rect = dialog.getBoundingClientRect();
        var outside = event.clientX < rect.left || event.clientX > rect.right || event.clientY < rect.top || event.clientY > rect.bottom;
        if (outside) closeDialog();
      });
      dialog.addEventListener("close", function () {
        document.body.classList.remove("modal-open");
        if (toast && toastParent) {
          toast.hidden = true;
          toastParent.appendChild(toast);
        }
        if (lastTrigger && document.contains(lastTrigger)) lastTrigger.focus({ preventScroll: true });
      });
    }

    var deckra = config.deckra || { name: "DECKRA", marketplaces: {} };
    var deckraImages = Array.isArray(deckra.images) ? deckra.images : [];
    document.querySelectorAll("[data-deckra-image]").forEach(function (element) {
      var selectedImage = element.dataset.deckraImage === "detail" ? deckraImages[1] || deckraImages[0] : deckraImages[0];
      if (!selectedImage || typeof selectedImage.src !== "string" || !selectedImage.src.trim()) {
        element.hidden = true;
        element.style.backgroundImage = "none";
        return;
      }
      element.hidden = false;
      element.style.backgroundImage = "url(" + JSON.stringify(selectedImage.src) + ")";
      element.classList.toggle("is-product-photo", selectedImage.layout !== "reference-board");
      element.setAttribute("role", "img");
      element.setAttribute("aria-label", selectedImage.alt || deckra.name);
    });
    document.querySelectorAll("[data-deckra-description]").forEach(function (element) {
      element.textContent = deckra.description || "";
    });
    var deckraFeatures = Array.isArray(deckra.features) ? deckra.features : [];
    var deckraFeatureList = document.getElementById("deckra-features");
    if (deckraFeatureList) {
      deckraFeatureList.replaceChildren();
      deckraFeatures.forEach(function (feature) {
        deckraFeatureList.appendChild(createElement("li", "", String(feature)));
      });
      deckraFeatureList.hidden = deckraFeatures.length === 0;
      var deckraFeatureSection = deckraFeatureList.closest("[data-features-section]");
      if (deckraFeatureSection) deckraFeatureSection.hidden = deckraFeatures.length === 0;
    }
    document.querySelectorAll("[data-deckra-demo]").forEach(function (element) {
      element.hidden = !config.demoMode || !deckra.demo;
    });
    document.querySelectorAll('[data-marketplaces="deckra"]').forEach(function (container) {
      renderMarketplaces(container, deckra);
    });

    var menuButton = document.getElementById("menu-toggle");
    var mainNav = document.getElementById("main-nav");
    function setMenu(open) {
      if (!menuButton || !mainNav) return;
      menuButton.setAttribute("aria-expanded", String(open));
      menuButton.setAttribute("aria-label", open ? "Fechar menu" : "Abrir menu");
      if (open) mainNav.setAttribute("data-open", "true");
      else mainNav.removeAttribute("data-open");
    }
    if (menuButton && mainNav) {
      menuButton.addEventListener("click", function () {
        setMenu(menuButton.getAttribute("aria-expanded") !== "true");
      });
      mainNav.querySelectorAll("a").forEach(function (link) {
        link.addEventListener("click", function () { setMenu(false); });
      });
    }
    document.addEventListener("keydown", function (event) {
      if (event.key !== "Escape") return;
      if (menuButton && menuButton.getAttribute("aria-expanded") === "true") {
        setMenu(false);
        menuButton.focus();
      }
      if (dialog && dialog.hasAttribute("open") && typeof dialog.close !== "function") closeDialog();
    });

    document.querySelectorAll("[data-year]").forEach(function (element) {
      element.textContent = String(new Date().getFullYear());
    });
  }

  if (document.readyState === "loading") document.addEventListener("DOMContentLoaded", ready);
  else ready();
}());
