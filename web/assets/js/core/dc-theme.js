(function () {
  "use strict";

  window.DC = window.DC || {};

  // Theme: auto (default), light, dark.
  //
  // Loaded in <head> without defer, so data-theme lands before <body> first
  // paints and no wrong theme flashes. "auto" means no attribute at all, and
  // the CSS follows prefers-color-scheme through color-scheme/light-dark().
  //
  // Loads after core/dc-lang.js and the i18n tables: the button label is a
  // translated string.

  var THEME_KEY = "dc-theme";
  var ORDER = ["auto", "light", "dark"];

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

  function nextMode(mode) { return ORDER[(ORDER.indexOf(mode) + 1) % ORDER.length]; }

  function applyMode(mode) {
    var root = document.documentElement;
    try {
      if (mode === "auto") { root.removeAttribute("data-theme"); localStorage.removeItem(THEME_KEY); }
      else { root.setAttribute("data-theme", mode); localStorage.setItem(THEME_KEY, mode); }
    } catch (e) {
      // The attribute still has to land even when the choice cannot be stored.
      if (mode === "auto") root.removeAttribute("data-theme"); else root.setAttribute("data-theme", mode);
    }
  }

  function label(mode) {
    return { auto: window.DC.s("themeAuto"), light: window.DC.s("themeLight"), dark: window.DC.s("themeDark") }[mode];
  }

  function updateUi(btn) {
    var mode = currentMode();
    btn.textContent = label(mode);
    btn.setAttribute("data-theme-mode", mode);
    btn.setAttribute("aria-label", window.DC.s("themePrefix") + label(mode) + window.DC.s("themeSwitchTo") + label(nextMode(mode)));
  }

  // Rebindable: the router replaces the <body> and therefore the button.
  window.DC.initThemeToggle = function initThemeToggle() {
    var btn = document.querySelector("[data-theme-toggle]");
    if (!btn || btn.hasAttribute("data-theme-bound")) return;
    btn.setAttribute("data-theme-bound", "");
    updateUi(btn);
    btn.addEventListener("click", function () {
      applyMode(nextMode(currentMode()));
      updateUi(btn);
    });
  };
  document.addEventListener("DOMContentLoaded", window.DC.initThemeToggle);

  // Read once here rather than in each caller, so reveal.js and the router
  // agree on what the preference is.
  window.DC.reduceMotion = function reduceMotion() {
    return window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  };
})();
