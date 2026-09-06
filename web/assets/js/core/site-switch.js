(function () {
  "use strict";

  // KEEP. The root index.html redirect: it checks localStorage first, then
  // the browser's language list, before replacing itself with that tree's
  // home page. Its storage key must stay "site-lang", the same literal
  // core/site-lang.js reads, or a visitor's saved choice stops sticking.
  //
  // The root page carries no content: it decides which tree the visitor lands
  // in and replaces itself. Loaded ONLY by index.html, and by nothing else, so
  // it must not touch window.SITE: the real pages never see it.
  //
  // location.replace and not href: the switch page must not sit in the history,
  // or the back button would bounce the visitor straight forward again.
  var STORED = "site-lang";

  // The mount this page is served from: "" at a domain root, "/deepcrate"
  // when the feedback hub mirrors the site under a sub-path. Read off our own
  // address because the sync script's data-base-path lands on the tag that
  // loads core/site-lang.js, and this page does not load it.
  var MOUNT = location.pathname.replace(/index\.html$/, "").replace(/\/$/, "");

  function chosen() {
    try {
      var pref = localStorage.getItem(STORED);
      if (pref === "fr" || pref === "en") return pref;
    } catch (e) { /* localStorage unavailable: fall through to the browser list */ }
    var list = navigator.languages || [navigator.language || ""];
    for (var i = 0; i < list.length; i++) {
      var code = String(list[i]).slice(0, 2).toLowerCase();
      if (code === "fr") return "fr";
      if (code === "en") return "en";
    }
    return "en";
  }

  location.replace(MOUNT + "/lang/" + chosen() + "/index.html" + location.search + location.hash);
})();
