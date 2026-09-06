(function () {
  "use strict";

  window.DC = window.DC || {};

  // The "prepare and install the document" half of the router: rewrite the
  // fetched document's links, carry the metadata over, replay the scripts
  // innerHTML never executes, and swap in the <body>.
  //
  // Kept apart from router.js, which decides when to navigate: click
  // interception and document correctness are two different reasons to change.

  // Every href/src of the fetched document is resolved against the URL it
  // actually came from and rewritten root-relative before injection. Without
  // this, "crates.html" or "../index.html" would resolve against the current
  // page's directory once moved between depths.
  function rootRelativize(doc, baseUrl) {
    doc.querySelectorAll("[href], [src]").forEach(function (el) {
      ["href", "src"].forEach(function (attr) {
        var raw = el.getAttribute(attr);
        if (!raw || raw.charAt(0) === "#" || /^(mailto|tel|javascript):/i.test(raw)) return;
        try {
          var abs = new URL(raw, baseUrl);
          if (abs.origin !== location.origin) return;
          el.setAttribute(attr, abs.pathname + abs.search + abs.hash);
        } catch (e) { /* leave untouched */ }
      });
    });
  }

  function setMeta(doc) {
    document.title = doc.title;
    // Only <body> is replaced, so the target page's language is copied onto
    // <html lang> by hand; otherwise the rendered components and the language
    // button would come back in the wrong language.
    var lang = doc.documentElement.getAttribute("lang");
    if (lang) document.documentElement.setAttribute("lang", lang);
    var incoming = doc.querySelector('meta[name="description"]');
    var current = document.querySelector('meta[name="description"]');
    if (!incoming) return;
    if (!current) {
      current = document.createElement("meta");
      current.setAttribute("name", "description");
      document.head.appendChild(current);
    }
    current.setAttribute("content", incoming.getAttribute("content") || "");
  }

  // Every page carries the same script set (tools/check-assets.mjs fails the
  // run if one drifts), so each of these is defined by the time a navigation
  // can happen. A missing one is a structural bug that should surface rather
  // than be swallowed by a guard.
  var REBIND = ["initReveal", "initThemeToggle", "initLangToggle"];

  function afterSwap() {
    for (var i = 0; i < REBIND.length; i++) window.DC[REBIND[i]]();
    window.scrollTo(0, 0);
  }

  // Scripts inserted through innerHTML never execute. That goes unnoticed as
  // long as every page carries the same tags, since the browser ran them on
  // first load. The moment a page has one of its own, it would arrive dead.
  // The registry is seeded with what has already run, so nothing re-executes.
  var executedScripts = null;

  function seedExecutedScripts() {
    executedScripts = {};
    var nodes = document.querySelectorAll("script[src]");
    for (var i = 0; i < nodes.length; i++) executedScripts[nodes[i].src] = true;
  }

  function runNewBodyScripts() {
    var nodes = document.body.querySelectorAll("script[src]");
    for (var i = 0; i < nodes.length; i++) {
      var src = nodes[i].src;
      if (executedScripts[src]) continue;
      executedScripts[src] = true;
      var fresh = document.createElement("script");
      for (var a = 0; a < nodes[i].attributes.length; a++) {
        fresh.setAttribute(nodes[i].attributes[a].name, nodes[i].attributes[a].value);
      }
      // Replaced in place rather than appended: otherwise the inert node from
      // the innerHTML stays alongside ours and the DOM shows the script twice,
      // only one of which ever ran.
      nodes[i].parentNode.replaceChild(fresh, nodes[i]);
    }
  }

  function swapBody(doc) {
    if (executedScripts === null) seedExecutedScripts();
    document.body.innerHTML = doc.body.innerHTML;
    document.body.className = doc.body.className;
    afterSwap();
    runNewBodyScripts();
  }

  window.DC._routerSwap = { rootRelativize: rootRelativize, setMeta: setMeta, swapBody: swapBody };
})();
