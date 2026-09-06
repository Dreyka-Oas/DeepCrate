(function () {
  "use strict";

  window.SITE = window.SITE || {};

  // KEEP. Cycles data-theme through auto, light and dark, and applies the
  // result before first paint; the choice then persists under THEME_KEY.
  // Rename that attribute or its values and tokens.css, footer.css,
  // canvas.css and nav-toggles.css all stop matching the theme they are
  // meant to draw.
  //
  // Theme: auto (default) / day / night.
  //
  // The choice is applied as early as possible: this script is loaded in <head>
  // without defer, so data-theme is set before <body> first paints → no flash of
  // the wrong theme. "auto" = no attribute at all, and the CSS follows
  // prefers-color-scheme via color-scheme/light-dark() (tokens.css).
  //
  // Must be loaded AFTER core/site-lang.js and the tables in assets/js/i18n/: the
  // button labels go through SITE.s.
  var s = window.SITE.s;
  var THEME_KEY = "site-theme";
  var THEME_ORDER = ["auto", "light", "dark"];

  (function applyStoredTheme() {
    try {
      var stored = localStorage.getItem(THEME_KEY);
      if (stored === "light" || stored === "dark") document.documentElement.setAttribute("data-theme", stored);
    } catch (e) { /* localStorage unavailable: stay on auto */ }
  })();

  function currentMode() {
    var mode = document.documentElement.getAttribute("data-theme");
    return mode === "light" || mode === "dark" ? mode : "auto";
  }

  function nextMode(mode) {
    return THEME_ORDER[(THEME_ORDER.indexOf(mode) + 1) % THEME_ORDER.length];
  }

  function applyMode(mode) {
    var root = document.documentElement;
    try {
      if (mode === "auto") { root.removeAttribute("data-theme"); localStorage.removeItem(THEME_KEY); }
      else { root.setAttribute("data-theme", mode); localStorage.setItem(THEME_KEY, mode); }
    } catch (e) {
      // The attribute still has to land even if the choice cannot be persisted.
      if (mode === "auto") root.removeAttribute("data-theme"); else root.setAttribute("data-theme", mode);
    }
  }

  // Read through s() so the labels follow the page language.
  function modeLabel(mode) {
    return { auto: s("themeAuto"), light: s("themeLight"), dark: s("themeDark") }[mode];
  }

  function updateUi(btn) {
    var mode = currentMode();
    btn.setAttribute("data-theme-mode", mode);
    btn.setAttribute("aria-label", s("themePrefix") + modeLabel(mode) + s("themeSwitchTo") + modeLabel(nextMode(mode)));
  }

  // Rebindable: the router replaces the <body> (and therefore the button) on
  // every navigation, exactly as it does for the sound and language buttons.
  window.SITE.initThemeToggle = function initThemeToggle() {
    var btn = document.querySelector("[data-theme-toggle]");
    if (!btn || btn.hasAttribute("data-theme-bound")) return;
    btn.setAttribute("data-theme-bound", "");
    updateUi(btn);
    btn.addEventListener("click", function () {
      applyMode(nextMode(currentMode()));
      updateUi(btn);
      if (window.SITE.sfx) window.SITE.sfx.stamp({ volume: 0.5 });
    });
  };
  document.addEventListener("DOMContentLoaded", window.SITE.initThemeToggle);
})();
