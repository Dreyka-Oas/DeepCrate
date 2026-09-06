(function () {
  "use strict";

  window.DC = window.DC || {};

  // Opening screen. Injected as early as it can be, from <head>, before <body>
  // paints: it covers the page while the fonts load, fills a cell the way a
  // crate fills, then the two halves part sideways like a lid pulled open.
  // Pure enhancement, so with JS off it does not exist and the site is reached
  // directly. Full loads only, since the router never re-runs the <head>.
  //
  // Must be loaded AFTER core/dc-theme.js (it reads DC.reduceMotion, and the
  // stored theme has to land first or the screen paints in the wrong palette)
  // and AFTER core/dc-lang.js, whose redirect flag it honours.
  if (window.DC._langRedirecting) return;
  if (!document.documentElement) return;
  if (window.DC.reduceMotion && window.DC.reduceMotion()) return;

  var el = document.createElement("div");
  el.id = "dc-loader";
  el.setAttribute("role", "presentation");
  el.setAttribute("aria-hidden", "true");
  el.innerHTML =
    '<div class="dc-loader__half dc-loader__half--left"></div>' +
    '<div class="dc-loader__half dc-loader__half--right"></div>' +
    '<div class="dc-loader__core">' +
      '<div class="dc-loader__cell"><span class="dc-loader__fill"></span></div>' +
      '<p class="dc-loader__mark">Deep<span>Crate</span></p>' +
    "</div>";
  document.documentElement.appendChild(el);
  document.documentElement.classList.add("dc-booting");

  var done = false, capId;
  function onKey(e) { if (e.key === "Escape" || e.key === "Enter" || e.key === " ") finish(); }
  function finish() {
    if (done) return;
    done = true;
    clearTimeout(capId);
    document.removeEventListener("keydown", onKey);
    el.removeEventListener("click", finish);
    el.classList.add("is-done");                                  // the cell and the wordmark go
    setTimeout(function () { el.classList.add("is-open"); }, 320); // then the lid parts
    setTimeout(function () {
      if (el.parentNode) el.parentNode.removeChild(el);
      document.documentElement.classList.remove("dc-booting");
    }, 320 + 760);
  }
  el.addEventListener("click", finish);
  document.addEventListener("keydown", onKey);

  // Held for a minimum beat AND until the fonts are ready, otherwise the
  // wordmark jumps as the site takes over, the whole thing capped so a slow
  // font server cannot hold the page hostage.
  var MIN = 1500, CAP = 3400, start = Date.now();
  capId = setTimeout(finish, CAP);
  function release() { setTimeout(finish, Math.max(0, MIN - (Date.now() - start))); }
  function whenLoaded() {
    var fonts = (document.fonts && document.fonts.ready) ? document.fonts.ready : Promise.resolve();
    fonts.then(release, release);
  }
  if (document.readyState === "complete") whenLoaded();
  else window.addEventListener("load", whenLoaded);
})();
