(() => {
  const panel = document.querySelector("[data-changelog-url]");
  if (!panel) return;
  let lastText = null;
  let loading = false;
  const versionPattern = /^\s*(\d+(?:\.\d+){1,3}(?:\s+beta)?)\s*[-–]\s*(.+)$/i;

  function render(text) {
    const entries = [];
    let current = null;
    for (const line of text.split(/\r?\n/)) {
      const match = line.match(versionPattern);
      if (match) {
        if (current) entries.push(current);
        current = { version: match[1], date: match[2].trim(), lines: [] };
      } else if (current && line.trim() && !/^[-=]{3,}$/.test(line.trim())) {
        current.lines.push(line);
      }
    }
    if (current) entries.push(current);
    const opened = new Set([...panel.querySelectorAll("details[open]")].map(item => item.dataset.version));
    panel.replaceChildren();
    if (!entries.length) {
      const message = document.createElement("p");
      message.textContent = "Ainda não há versões publicadas.";
      panel.append(message);
      return;
    }
    entries.forEach((entry, index) => {
      const card = document.createElement("details");
      card.className = "deckra-changelog-entry";
      card.dataset.version = entry.version;
      card.open = opened.size ? opened.has(entry.version) || (index === 0 && !opened.has(entries[0].version)) : index === 0;
      if (index === 0) card.classList.add("is-latest");
      const summary = document.createElement("summary");
      const version = document.createElement("strong");
      version.textContent = "v" + entry.version;
      const date = document.createElement("span");
      date.className = "deckra-version-date";
      date.textContent = entry.date;
      summary.append(version, date);
      if (index === 0) {
        const latest = document.createElement("span");
        latest.className = "deckra-latest-label";
        latest.textContent = "MAIS RECENTE";
        summary.append(latest);
      }
      const chevron = document.createElement("span");
      chevron.className = "deckra-version-chevron";
      chevron.setAttribute("aria-hidden", "true");
      chevron.textContent = "⌄";
      summary.append(chevron);
      const body = document.createElement("div");
      body.className = "deckra-change-body";
      let list = null;
      let lastItem = null;
      entry.lines.forEach(line => {
        const value = line.trim();
        if (value.startsWith("-")) {
          if (!list) { list = document.createElement("ul"); body.append(list); }
          lastItem = document.createElement("li");
          lastItem.textContent = value.slice(1).trim();
          list.append(lastItem);
        } else if (/^\s+/.test(line) && lastItem) {
          lastItem.textContent += " " + value;
        } else {
          list = null;
          lastItem = null;
          const heading = document.createElement("h4");
          heading.textContent = value;
          body.append(heading);
        }
      });
      card.append(summary, body);
      panel.append(card);
    });
  }

  async function refresh() {
    if (loading) return;
    loading = true;
    try {
      const url = new URL(panel.dataset.changelogUrl, document.baseURI);
      url.searchParams.set("t", Date.now());
      const response = await fetch(url, { cache: "no-store" });
      if (!response.ok) throw new Error("Changelog unavailable");
      const text = await response.text();
      if (text !== lastText) { render(text); lastText = text; }
    } catch {
      if (lastText === null) {
        const message = document.createElement("p");
        message.textContent = "Não foi possível carregar o histórico. Abra o changelog completo para consultar as versões.";
        panel.replaceChildren(message);
      }
    } finally {
      loading = false;
    }
  }
  refresh();
  setInterval(() => { if (!document.hidden) refresh(); }, 60000);
  document.addEventListener("visibilitychange", () => { if (!document.hidden) refresh(); });
})();

(() => {
  const nav = document.querySelector(".deckra-category-nav");
  const viewport = document.querySelector(".deckra-panels-viewport");
  const track = document.querySelector(".deckra-panels-track");
  if (!nav || !viewport || !track) return;
  const buttons = [...nav.querySelectorAll('[role="tab"]')];
  const panels = buttons.map((button) => document.getElementById(button.getAttribute("aria-controls")));
  const hashes = ["#modelos", "#software", "#onde-comprar", "#atualizacoes"];
  let activeIndex = 0;
  const resizeObserver = new ResizeObserver(() => {
    const active = panels[activeIndex];
    if (active) viewport.style.height = active.scrollHeight + "px";
  });
  function activate(index, updateHash = true, scrollToInfo = true) {
    activeIndex = (index + buttons.length) % buttons.length;
    buttons.forEach((button, i) => {
      const selected = i === activeIndex;
      button.classList.toggle("is-active", selected);
      button.setAttribute("aria-selected", String(selected));
      button.tabIndex = selected ? 0 : -1;
      panels[i].setAttribute("aria-hidden", String(!selected));
      panels[i].inert = !selected;
    });
    track.style.transform = `translateX(-${activeIndex * 100}%)`;
    viewport.style.height = panels[activeIndex].scrollHeight + "px";
    resizeObserver.disconnect();
    resizeObserver.observe(panels[activeIndex]);
    if (updateHash) history.replaceState(null, "", hashes[activeIndex]);
    if (scrollToInfo) {
      const headerHeight = document.querySelector(".site-header")?.getBoundingClientRect().height || 0;
      const hero = document.querySelector(".deckra-hero");
      const navTop = hero ? hero.getBoundingClientRect().bottom + window.scrollY : nav.getBoundingClientRect().top + window.scrollY;
      window.scrollTo({ top: Math.max(0, navTop - headerHeight), behavior: "instant" });
    }
  }
  buttons.forEach((button, index) => {
    button.addEventListener("click", () => activate(index));
    button.addEventListener("keydown", (event) => {
      let next = index;
      if (event.key === "ArrowRight") next = (index + 1) % buttons.length;
      else if (event.key === "ArrowLeft") next = (index - 1 + buttons.length) % buttons.length;
      else if (event.key === "Home") next = 0;
      else if (event.key === "End") next = buttons.length - 1;
      else return;
      event.preventDefault();
      buttons[next].focus();
      activate(next);
    });
  });
  const initialIndex = hashes.indexOf(window.location.hash);
  activate(initialIndex >= 0 ? initialIndex : 0, false, false);
  window.addEventListener("hashchange", () => {
    const index = hashes.indexOf(window.location.hash);
    if (index >= 0 && index !== activeIndex) activate(index, false);
  });
})();

document.addEventListener("click", (event) => {
  const link = event.target.closest('a[href="#software"], a[href="#onde-comprar"], a[href="#atualizacoes"], a[href="#modelos"]');
  if (!link) return;
  const index = ["#modelos", "#software", "#onde-comprar", "#atualizacoes"].indexOf(link.getAttribute("href"));
  const tab = document.querySelectorAll(".deckra-category-tab")[index];
  if (tab) {
    event.preventDefault();
    tab.click();

  }
});

document.addEventListener("click", (event) => {
  const control = event.target.closest("[data-open-deckra-tab]");
  if (!control) return;
  const target = control.dataset.openDeckraTab;
  const tab = document.getElementById("tab-" + target);
  if (tab) {
    tab.click();

  }
});

(() => {
  const gallery = document.querySelector("[data-deckra-gallery]");
  if (!gallery) return;
  const track = gallery.querySelector(".deckra-gallery-track");
  const slides = [...track.querySelectorAll("img")];
  const dots = [...gallery.querySelectorAll(".deckra-gallery-dot")];
  const counter = document.querySelector("[data-deckra-gallery-count]");
  if (slides.length < 2) return;
  let index = 0;
  let timer;
  const reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)");
  function show(next) {
    index = (next + slides.length) % slides.length;
    track.style.transform = `translateX(-${index * 100}%)`;
    slides.forEach((slide, i) => slide.setAttribute("aria-hidden", String(i !== index)));
    dots.forEach((dot, i) => {
      dot.classList.toggle("is-active", i === index);
      dot.setAttribute("aria-pressed", String(i === index));
    });
    if (counter) counter.textContent = `${index + 1} / ${slides.length}`;
  }
  function restart() {
    clearInterval(timer);
    if (reduceMotion.matches) return;
    timer = setInterval(() => {
      if (document.hidden || gallery.closest(".deckra-panel")?.inert || gallery.matches(":hover") || gallery.matches(":focus-within")) return;
      show(index + 1);
    }, 5000);
  }
  gallery.querySelector(".deckra-gallery-prev")?.addEventListener("click", () => { show(index - 1); restart(); });
  gallery.querySelector(".deckra-gallery-next")?.addEventListener("click", () => { show(index + 1); restart(); });
  dots.forEach((dot, i) => dot.addEventListener("click", () => { show(i); restart(); }));
  reduceMotion.addEventListener("change", restart);
  restart();
})();
