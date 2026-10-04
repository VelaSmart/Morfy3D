/* Verificação estática dos arquivos necessários ao GitHub Pages. */
"use strict";
const fs = require("node:fs");
const path = require("node:path");
const vm = require("node:vm");
const root = path.resolve(__dirname, "..");
const problems = [];
let checkedReferences = 0;

function fail(message) { problems.push(message); }
function walk(directory) {
  return fs.readdirSync(directory, { withFileTypes: true }).flatMap(entry => {
    const absolute = path.join(directory, entry.name);
    return entry.isDirectory() ? walk(absolute) : [absolute];
  });
}
const files = walk(root);
const pages = files.filter(file => file.endsWith(".html"));
const actualPages = pages.map(file => path.relative(root, file)).sort();
if (JSON.stringify(actualPages) !== JSON.stringify(["deckra.html", "index.html"])) {
  fail("O projeto deve conter apenas index.html e deckra.html como páginas HTML.");
}
const sources = new Map(pages.map(file => [file, fs.readFileSync(file, "utf8")]));
const anchors = new Map([...sources].map(([file, html]) => [file, new Set([...html.matchAll(/\bid=["']([^"']+)["']/g)].map(match => match[1]))]));

function reference(source, value) {
  checkedReferences++;
  if (!value || /^(?:https?:|mailto:|tel:|data:)/i.test(value)) return;
  const relativeSource = path.relative(root, source).replaceAll(path.sep, "/");
  let url;
  try { url = new URL(value, "https://example.invalid/repositorio/" + relativeSource); }
  catch { return fail(relativeSource + ": referência inválida " + value); }
  if (!url.pathname.startsWith("/repositorio/")) {
    return fail(relativeSource + ": caminho não funciona em subdiretório: " + value);
  }
  const localPath = decodeURIComponent(url.pathname.slice("/repositorio/".length));
  const target = path.resolve(root, localPath);
  if (!fs.existsSync(target)) return fail(relativeSource + ": arquivo ausente " + value);
  let parent = root;
  for (const segment of localPath.split("/").filter(Boolean)) {
    if (!fs.readdirSync(parent).includes(segment)) {
      fail(relativeSource + ": maiúsculas/minúsculas não correspondem ao arquivo " + value);
      break;
    }
    parent = path.join(parent, segment);
  }
  if (url.hash && anchors.has(target) && !anchors.get(target).has(decodeURIComponent(url.hash.slice(1)))) {
    fail(relativeSource + ": âncora inexistente " + value);
  }
}
for (const [file, html] of sources) {
  for (const match of html.matchAll(/\b(?:src|href)=["']([^"']+)["']/g)) reference(file, match[1]);
  if (/\b(?:checkout|carrinho|pagamento|compradores)\b/i.test(html)) fail(path.basename(file) + ": fluxo de venda interno detectado.");
  if (/R\$\s*\d|\b\d+[,.]\d{2}\s*(?:BRL|reais)\b/i.test(html)) fail(path.basename(file) + ": preço fixo detectado.");
}
for (const file of files.filter(file => file.endsWith(".css"))) {
  for (const match of fs.readFileSync(file, "utf8").matchAll(/url\(\s*["']?([^"')]+)["']?\s*\)/g)) reference(file, match[1].trim());
}
const configFile = path.join(root, "assets/js/products.js");
const context = { window: {} };
vm.runInNewContext(fs.readFileSync(configFile, "utf8"), context, { timeout: 1000 });
const config = context.window.MORFY_CONFIG;
if (!config || typeof config.demoMode !== "boolean") fail("A configuração requer demoMode booleano.");
const ids = new Set();
for (const product of [...config.products, config.deckra]) {
  if (!product.name || !product.category || !product.description) fail("Produto sem nome, categoria ou descrição.");
  if (!Array.isArray(product.features) || !Array.isArray(product.images)) fail(product.name + ": features/images devem ser listas.");
  if (product.id) {
    if (ids.has(product.id)) fail("Identificador duplicado: " + product.id);
    ids.add(product.id);
  }
  for (const image of product.images) {
    reference(path.join(root, "index.html"), image.src);
    if (!image.alt) fail(product.name + ": imagem sem descrição alternativa.");
  }
  for (const market of config.marketplaces) {
    const value = product.marketplaces[market.id];
    if (value === "") continue;
    try {
      const url = new URL(value);
      if (url.protocol !== "https:" || url.username || url.password) fail(product.name + ": link inválido em " + market.label);
    } catch { fail(product.name + ": link inválido em " + market.label); }
  }
}
for (const font of ["Montserrat", "Sora"]) {
  if (!fs.existsSync(path.join(root, "assets/fonts", font.toLowerCase() + ".ttf"))) fail("Fonte local ausente: " + font);
  const license = path.join(root, "assets/fonts", font + "-OFL.txt");
  if (!fs.existsSync(license) || !fs.readFileSync(license, "utf8").includes("SIL OPEN FONT LICENSE")) fail("Licença OFL ausente: " + font);
}
if (!fs.existsSync(path.join(root, ".nojekyll"))) fail("Arquivo .nojekyll ausente.");
if (problems.length) {
  console.error(problems.join("\n"));
  process.exitCode = 1;
} else {
  console.log("OK: 2 páginas, " + checkedReferences + " referências locais/âncoras, " + config.products.length + " produtos, configuração DECKRA, fontes e licenças.");
}
