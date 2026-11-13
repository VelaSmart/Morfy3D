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