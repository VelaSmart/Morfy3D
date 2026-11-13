/* Bloqueia o arraste e o menu nativo apenas nas imagens do site. */
function isSiteImage(event) {
  return event.target instanceof Element && Boolean(event.target.closest("img"));
}

document.addEventListener("dragstart", function (event) {
  if (isSiteImage(event)) event.preventDefault();
});

document.addEventListener("contextmenu", function (event) {
  if (isSiteImage(event)) event.preventDefault();
});
