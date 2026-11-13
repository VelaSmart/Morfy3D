/* Impede arrastar imagens para fora do site sem alterar cliques ou galerias. */
document.addEventListener("dragstart", function (event) {
  if (event.target instanceof Element && event.target.closest("img")) {
    event.preventDefault();
  }
});
