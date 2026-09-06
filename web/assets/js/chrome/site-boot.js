(function () {
  "use strict";

  window.SITE = window.SITE || {};

  // SLOT opening-screen. Shows the boot overlay that covers the page with
  // the mod's wordmark while the fonts load, then splits into two panels to
  // reveal the site. A new mod rewrites the two <span> tags inside
  // el.innerHTML with its own name and can retune the MIN and CAP timings.
  // The site-loader id and its panel/core classes must survive whatever
  // replaces them, since assets/css/components/fx/loader.css draws the
  // animation off those names, and so must the SITE.reduceMotion() and
  // _langRedirecting guards, or a visitor gets an animation they asked to
  // skip.
  //
  // ============================================================
  //  Opening screen. Injected as early as possible (a <head> script, before
  //  <body> renders): it covers the page with the wordmark while the fonts
  //  load, then parts into two panels to give way to the site. Pure
  //  enhancement: without JS it does not exist and the site stays fully
  //  reachable. Plays on full loads only (the router never re-runs the <head>).
  //
  //  Must be loaded AFTER core/site-motion.js: it reads SITE.reduceMotion(), and
  //  honours the _langRedirecting flag the language preference may set.
  // ============================================================
  if (window.SITE._langRedirecting) return;   // we are already heading to another URL
  if (window.SITE.reduceMotion() || !document.documentElement) return;   // no entry animation

  var el = document.createElement("div");
  el.id = "site-loader";
  el.setAttribute("role", "presentation");
  el.setAttribute("aria-hidden", "true");
  el.innerHTML =
    '<div class="site-loader__panel site-loader__panel--top"></div>' +
    '<div class="site-loader__panel site-loader__panel--bottom"></div>' +
    '<div class="site-loader__core">' +
      '<p class="site-loader__mark"><span>DEEP</span><span class="site-loader__mark-b">CRATE</span></p>' +
    "</div>";
  document.documentElement.appendChild(el);
  document.documentElement.classList.add("site-booting");

  var done = false, capId;
  function onKey(e) { if (e.key === "Escape" || e.key === "Enter" || e.key === " ") finish(); }
  function finish() {
    if (done) return;
    done = true;
    clearTimeout(capId);
    document.removeEventListener("keydown", onKey);
    el.removeEventListener("click", finish);
    el.classList.add("is-done");                                  // the centre block fades
    setTimeout(function () { el.classList.add("is-open"); }, 360); // then the panels part
    setTimeout(function () {
      if (el.parentNode) el.parentNode.removeChild(el);
      document.documentElement.classList.remove("site-booting");
    }, 360 + 800);
  }
  el.addEventListener("click", finish);
  document.addEventListener("keydown", onKey);

  // The screen is held for a minimum beat AND until the fonts are ready
  // (otherwise the site wordmark "jumps" on reveal), the whole thing capped.
  var MIN = 1650, CAP = 3600, start = Date.now();
  capId = setTimeout(finish, CAP);
  function release() { setTimeout(finish, Math.max(0, MIN - (Date.now() - start))); }
  function whenLoaded() {
    var fonts = (document.fonts && document.fonts.ready) ? document.fonts.ready : Promise.resolve();
    fonts.then(release, release);
  }
  if (document.readyState === "complete") whenLoaded();
  else window.addEventListener("load", whenLoaded);
})();
