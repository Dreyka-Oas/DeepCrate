(function () {
  "use strict";

  window.DC = window.DC || {};

  // Two parallel trees under /lang/, fr and en, both hand-written. A page
  // declares its language through <html lang>, and everything rendered from JS
  // (nav, sidebar, footer, the theme button's label) reads DC.s(key) out of the
  // tables in assets/js/i18n/ and prefixes its links with /lang/<code>.
  //
  // First script on every page: it can redirect, and the rest reads DC.s off it.

  var LANG_KEY = "dc-lang";

  function pageLang() {
    var l = (document.documentElement.getAttribute("lang") || "fr").slice(0, 2).toLowerCase();
    return l === "en" ? "en" : "fr";
  }
  window.DC.lang = pageLang;

  function langPrefix() { return "/lang/" + pageLang(); }
  window.DC.langPrefix = langPrefix;

  // Both tables load on every page. tools/check-assets.mjs demands an identical
  // ordered asset list across the site, so a per-language script is not an
  // option, and loading both also rules out a page declaring one language in
  // <html lang> while carrying the other one's strings.
  window.DC.strings = window.DC.strings || {};

  // Falls back to French rather than to blank: a key still missing from en.js
  // shows the French wording, wrong but readable. tools/check-i18n.mjs is what
  // stops that from shipping in the first place.
  function s(key) {
    var table = window.DC.strings[pageLang()] || {};
    var value = table[key];
    if (value === undefined) value = (window.DC.strings.fr || {})[key];
    return value === undefined ? key : value;
  }
  window.DC.s = s;

  function stripLangPrefix(path) {
    return path.replace(/^\/lang\/(fr|en)(?=\/|$)/, "");
  }
  window.DC.stripLangPrefix = stripLangPrefix;

  // The same page in the other tree. A path carrying no /lang/<code> segment
  // (the gate at the root) maps to the target tree's home page.
  function counterpartPath(targetLang) {
    var base = stripLangPrefix(location.pathname || "/");
    if (base === "" || base === "/") base = "/index.html";
    return "/lang/" + targetLang + base;
  }
  window.DC.counterpartPath = counterpartPath;

  // An explicit preference wins over the tree a full load landed on, and the
  // switch happens before first paint since this runs inside <head>. The router
  // swaps <body> and never comes back through here, so only real entries and
  // reloads are affected.
  (function applyStoredLang() {
    try {
      var pref = localStorage.getItem(LANG_KEY);
      if ((pref === "fr" || pref === "en") && pref !== pageLang()) {
        location.replace(counterpartPath(pref) + location.search + location.hash);
      }
    } catch (e) { /* localStorage unavailable: stay on the page language */ }
  })();

  // The language button is a plain link to the counterpart page, so it works
  // with JS off; the router intercepts it for a smooth swap. Rebindable, since
  // the router replaces the <body> the button lives in.
  window.DC.initLangToggle = function initLangToggle() {
    var btn = document.querySelector("[data-lang-toggle]");
    if (!btn || btn.hasAttribute("data-lang-bound")) return;
    btn.setAttribute("data-lang-bound", "");
    btn.addEventListener("click", function (e) {
      // Same guard as the router's isNavigableClick: a modified click opens the
      // other tree in a new tab without this one following, so it must not flip
      // this tab's stored preference.
      if (e.defaultPrevented || e.button !== 0) return;
      if (e.metaKey || e.ctrlKey || e.shiftKey || e.altKey) return;
      try { localStorage.setItem(LANG_KEY, pageLang() === "en" ? "fr" : "en"); } catch (err) { /* ignore */ }
    });
  };
  document.addEventListener("DOMContentLoaded", window.DC.initLangToggle);
})();
