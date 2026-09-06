(function () {
  "use strict";

  window.SITE = window.SITE || {};

  // ============================================================
  // KEEP. Decides the page's active language, exposing it through SITE.s /
  // SITE.lang / SITE.langPrefix, and redirects to the stored preference
  // before first paint. Touch the storage key or the redirect logic and
  // every page drifts out of sync with site-switch.js and its own language
  // toggle.
  //
  //  Language: fr and en, two parallel trees under /lang/. Neither is a
  //  mirror of the other any more: both are hand-written sources.
  //  Every page declares its language through <html lang>. The rendered
  //  components (nav, sidebar, footer, log, ledger) and the dynamic content
  //  (page/home.js) read SITE.s(key) to pick their wording out of the tables in
  //  assets/js/i18n/, and prefix their links with /lang/<code>, matching the
  //  page's own tree. The .html files remain the source of truth for the
  //  prose: the site stays a static multi-page export.
  //
  //  First script on every page: it may redirect (see applyStoredLang), and
  //  everything else reads SITE.s / SITE.lang off it.
  // ============================================================

  // Mount prefix: empty on the standard Pages deployment (the domain root); set
  // when DeepCrate is served under a sub-path (/deepcrate on the feedback
  // hub's Worker) by outils/feedback-hub/scripts/sync-project.mjs, which writes
  // data-base-path onto THIS script's tag.
  //
  // An attribute rather than an inline <script>: the host serves these pages
  // under a CSP with script-src 'self' and no 'unsafe-inline'.
  //
  // Read off document.currentScript, and only that: a querySelector for the
  // attribute would run before the parser has reached any *later* <script> tag,
  // so tagging any other file silently yields "" and every rendered link points
  // at the host's root instead of the mount. The sync script and this line are
  // one contract. Change them together.
  var BASE_PATH = (document.currentScript && document.currentScript.dataset.basePath) || "";

  var LANG_KEY = "site-lang";

  function pageLang() {
    var l = (document.documentElement.getAttribute("lang") || "fr").slice(0, 2).toLowerCase();
    return l === "en" ? "en" : "fr";
  }
  window.SITE.lang = pageLang;

  // Every rendered link is prefixed with the page's own tree. The two trees are
  // strictly parallel, so the prefix is the only thing that differs between a
  // French link and its English counterpart.
  function langPrefix() { return BASE_PATH + "/lang/" + pageLang(); }
  window.SITE.langPrefix = langPrefix;

  // Translated strings: assets/js/i18n/fr.js and assets/js/i18n/en.js each
  // register their own table here. BOTH load on every page. check-assets.mjs
  // requires an identical ordered asset list across the site, so a per-language
  // <script> is not an option, and loading both also removes any chance of a
  // page declaring one language in <html lang> and loading the other's table.
  window.SITE.strings = window.SITE.strings || {};

  // One string by key, in the page's language. Falls back to French rather than
  // to blank: a key still missing from en.js then shows the French wording,
  // which is wrong but readable. tools/check-i18n.mjs is what stops that from
  // shipping in the first place.
  function s(key) {
    var table = window.SITE.strings[pageLang()] || {};
    var value = table[key];
    if (value === undefined) value = (window.SITE.strings.fr || {})[key];
    return value === undefined ? key : value;
  }
  window.SITE.s = s;

  // Strip a leading /lang/<code> segment off a path, shared with site-chrome.js
  // (which loads after this script on every page) so the two never drift on
  // what counts as the "logical" part of a path if a third language arrives.
  function stripLangPrefix(path) {
    return path.replace(/^\/lang\/(fr|en)(?=\/|$)/, "");
  }
  window.SITE.stripLangPrefix = stripLangPrefix;

  // The same page in the other tree: the /lang/<code> segment is swapped. A
  // path that carries no such segment (the switch page at the root) maps to the
  // target tree's home page.
  function counterpartPath(targetLang) {
    var p = location.pathname || "/";
    if (BASE_PATH && p.indexOf(BASE_PATH) === 0) p = p.slice(BASE_PATH.length) || "/";
    var base = stripLangPrefix(p);
    if (base === "" || base === "/") base = "/index.html";
    return BASE_PATH + "/lang/" + targetLang + base;
  }
  window.SITE.counterpartPath = counterpartPath;

  // Explicit preference: if the user has already chosen a language and a full
  // load drops them on the other one, the switch happens BEFORE the first paint
  // (we are still inside <head>, so there is no flash). The router (a <body>
  // swap) never comes back through here: only real entries and reloads count.
  // Consequence: after a language switch, pressing Back restores the previous
  // tree by body swap alone, so the visible language disagrees with the
  // stored preference until the next full load runs this again.
  (function applyStoredLang() {
    try {
      var pref = localStorage.getItem(LANG_KEY);
      if ((pref === "fr" || pref === "en") && pref !== pageLang()) {
        window.SITE._langRedirecting = true;
        location.replace(counterpartPath(pref) + location.search + location.hash);
      }
    } catch (e) { /* localStorage unavailable: stay on the page language */ }
  })();

  // Language button: a link to the same page in the other language (it works
  // without JS; the router intercepts it for a smooth swap). On click the
  // target is stored so the choice persists (see applyStoredLang).
  //
  // Rebindable: the router replaces the <body> (and therefore the button) on
  // every navigation, exactly as it does for the theme and sound buttons.
  window.SITE.initLangToggle = function initLangToggle() {
    var btn = document.querySelector("[data-lang-toggle]");
    if (!btn || btn.hasAttribute("data-lang-bound")) return;
    btn.setAttribute("data-lang-bound", "");
    btn.addEventListener("click", function (e) {
      // Same guard as router.js's isNavigableClick: a modified click or a
      // non-primary button opens the other tree in a new tab without this
      // tab following it, so it must not flip this tab's stored preference.
      if (e.defaultPrevented || e.button !== 0) return;
      if (e.metaKey || e.ctrlKey || e.shiftKey || e.altKey) return;
      var target = pageLang() === "en" ? "fr" : "en";
      try { localStorage.setItem(LANG_KEY, target); } catch (e) { /* ignore */ }
      if (window.SITE.sfx) window.SITE.sfx.stamp({ volume: 0.5 });
    });
  };
  document.addEventListener("DOMContentLoaded", window.SITE.initLangToggle);
})();
