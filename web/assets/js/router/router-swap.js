(function () {
  "use strict";

  window.SITE = window.SITE || {};

  // KEEP. The link rewriting, script replay, and body swap here are pjax
  // plumbing shared by every page. Adding a page only means appending its
  // init function to REBIND below; touching the rewrite or replay order
  // breaks navigation for the whole site.

  // The "prepare and install the document" half of the pjax router: rewrite the
  // fetched document's links, carry the metadata over, replay the scripts that
  // innerHTML never executes, and swap in the <body>.
  //
  // Kept apart from router.js (which decides WHEN to navigate) because these are
  // two distinct reasons to change: click interception is about navigation UX,
  // document preparation is about DOM correctness.

  // Rewrite every href/src in the fetched document to a root-relative path
  // (resolved against the ACTUAL fetched URL) before it gets injected into
  // the current document. Otherwise relative paths like "mechanic-1.html" or
  // "../index.html" would resolve against the wrong base once moved between
  // directory depths (root vs wiki/).
  function rootRelativize(doc, baseUrl) {
    doc.querySelectorAll("[href], [src]").forEach(function (el) {
      ["href", "src"].forEach(function (attr) {
        var raw = el.getAttribute(attr);
        if (!raw || raw.charAt(0) === "#" || /^(mailto|tel|javascript):/i.test(raw)) return;
        try {
          var abs = new URL(raw, baseUrl);
          if (abs.origin !== location.origin) return;
          el.setAttribute(attr, abs.pathname + abs.search + abs.hash);
        } catch (e) {
          /* leave untouched */
        }
      });
    });
  }

  function setMeta(doc) {
    document.title = doc.title;
    // The router only replaces <body>, so the target page's language is copied
    // onto <html lang> by hand, otherwise the components (nav, ledger…) and the
    // language button would render in the wrong language after the swap.
    var lang = doc.documentElement.getAttribute("lang");
    if (lang) document.documentElement.setAttribute("lang", lang);
    var newDesc = doc.querySelector('meta[name="description"]');
    var curDesc = document.querySelector('meta[name="description"]');
    if (newDesc) {
      if (!curDesc) {
        curDesc = document.createElement("meta");
        curDesc.setAttribute("name", "description");
        document.head.appendChild(curDesc);
      }
      curDesc.setAttribute("content", newDesc.getAttribute("content") || "");
    }
  }

  // Everything the swapped-in <body> needs re-bound. Every page carries the same
  // script set (tools/check-assets.mjs fails the run if one drifts), so
  // each of these is defined by the time a navigation can happen, and a missing
  // one is a structural bug that should surface, not be swallowed by a guard.
  var REBIND = [
    "initReveal",
    "initSoundToggle",
    "initThemeToggle",
    "initLangToggle",
    "initHome",
    "initWikiChart"
  ];

  function afterSwap() {
    for (var i = 0; i < REBIND.length; i++) window.SITE[REBIND[i]]();
    // Reset the scroll position last, after the rebinds have had a chance to
    // touch layout, so the swapped-in page opens at the top.
    window.scrollTo(0, 0);
  }

  // Scripts inserted through innerHTML NEVER execute. As long as every page
  // carries the same <script> tags at the end of body this goes unnoticed: they
  // have been loaded since the first visit. But the moment a page has a script
  // of its own, that script would never run after an internal navigation, and
  // the page would arrive crippled.
  //
  // So the external scripts of the swapped-in body that have not yet run are
  // replayed. The registry is seeded on the first load with the scripts the
  // browser has already executed, precisely so they are never re-run.
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
      // The data-* attributes carry the script's configuration (data-base-path,
      // for one), so everything is copied across, not just src.
      for (var a = 0; a < nodes[i].attributes.length; a++) {
        fresh.setAttribute(nodes[i].attributes[a].name, nodes[i].attributes[a].value);
      }
      // Replaced in place rather than appended at the end of body: otherwise the
      // inert node that came from the innerHTML stays alongside ours, and the DOM
      // shows the same script twice, only one of which ever ran.
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
  window.SITE._routerSwap = {
    rootRelativize: rootRelativize,
    setMeta: setMeta,
    swapBody: swapBody
  };
})();
