(() => {
  const panel = document.querySelector("[data-changelog-url]");
  if (!panel) return;
  fetch(panel.dataset.changelogUrl, { cache: "no-cache" })
    .then((response) => {
      if (!response.ok) throw new Error("Não foi possível carregar o histórico.");
      return response.text();
    })
    .then((text) => {
      const lines = text.split(/\r?\n/);
      const entries = [];
      let current = null;
      for (const line of lines) {
        const version = line.match(/^\s*(\d+(?:\.\d+){1,3}(?:\s+beta)?)\s*[-–]\s*(.+)$/i);
        if (version) {
          if (current) entries.push(current);
          current = { title: version[0].trim(), lines: [] };
          continue;
        }
        if (current && line.trim() && !/^[-=]{3,}$/.test(line.trim())) current.lines.push(line);
      }
      if (current) entries.push(current);
      panel.replaceChildren();
      if (!entries.length) {
        const pre = document.createElement("pre");
        pre.textContent = text.trim() || "Ainda não há atualizações publicadas.";
        panel.append(pre);
        return;
      }
      for (const entry of entries.slice(0, 8)) {
        const details = document.createElement("details");
        details.className = "deckra-changelog-entry";
        const summary = document.createElement("summary");
        summary.textContent = entry.title;
        const body = document.createElement("pre");
        body.textContent = entry.lines.join("\n").trim();
        details.append(summary, body);
        panel.append(details);
      }
    })
    .catch(() => {
      const p = document.createElement("p");
      p.textContent = "Não foi possível carregar as atualizações agora. Use “Abrir changelog completo” para consultar o arquivo.";
      panel.replaceChildren(p);
    });
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
  function activate(index, updateHash = true) {
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
  activate(initialIndex >= 0 ? initialIndex : 0, false);
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
    document.querySelector(".deckra-category-nav")?.scrollIntoView({ behavior: "smooth", block: "center" });
  }
});

document.addEventListener("click", (event) => {
  const control = event.target.closest("[data-open-deckra-tab]");
  if (!control) return;
  const target = control.dataset.openDeckraTab;
  const tab = document.getElementById("tab-" + target);
  if (tab) {
    tab.click();
    document.querySelector(".deckra-category-nav")?.scrollIntoView({ behavior: "smooth", block: "center" });
  }
});
