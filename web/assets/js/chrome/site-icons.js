(function () {
  "use strict";

  window.SITE = window.SITE || {};

  // KEEP. Builds the sound, theme and language toggle buttons as inline SVG,
  // with the classes and data attributes the rest of the chrome depends on.
  // Rename an icon-* class or a data-*-toggle attribute here and
  // nav-toggles.css stops matching, or the click handlers in
  // core/site-theme.js and core/site-lang.js stop finding their button.
  //
  // The three navigation-bar buttons, as inline HTML: sound, theme, language.
  // Separated from site-chrome.js because these are reusable ATOMS (SVG paths
  // plus a11y labels) whereas site-chrome assembles the page.
  //
  // Must be loaded AFTER core/site-lang.js and the tables in assets/js/i18n/: it
  // reads SITE.s / SITE.lang / SITE.counterpartPath.
  var s = window.SITE.s;
  var pageLang = window.SITE.lang;

  window.SITE.soundToggleHtml = function soundToggleHtml() {
    return '<button class="sound-toggle" data-sound-toggle type="button" aria-pressed="true" aria-label="' +
      s("soundToggleAria") + '">' +
      '<span class="sound-toggle__icons" aria-hidden="true">' +
      '<svg class="sound-toggle__icon icon-on" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 9v6h4l5 4V5L8 9H4Z"/><path d="M17 8.5a5 5 0 0 1 0 7"/><path d="M19.5 6a8.5 8.5 0 0 1 0 12"/></svg>' +
      '<svg class="sound-toggle__icon icon-off" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 9v6h4l5 4V5L8 9H4Z"/><path d="M16 9l5 6M21 9l-5 6"/></svg>' +
      "</span></button>";
  };

  // Theme button (auto / day / night): a single button that cycles. The icon
  // shown reflects the current MODE: half-circle (auto), sun (day), moon
  // (night). CSS in components/chrome/nav-toggles.css.
  //
  // A function, not a constant: the router swaps <body> without re-running this
  // file, so a constant built at load time would carry the language of whatever
  // page was opened first across a FR↔EN navigation.
  window.SITE.themeToggleHtml = function themeToggleHtml() {
    return '<button class="theme-toggle" data-theme-toggle type="button" aria-label="' + s("themeToggleAria") + '">' +
      '<span class="theme-toggle__icons" aria-hidden="true">' +
      '<svg class="theme-toggle__icon icon-auto" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="9"/><path d="M12 3a9 9 0 0 0 0 18Z" fill="currentColor" stroke="none"/></svg>' +
      '<svg class="theme-toggle__icon icon-day" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="4"/><path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4"/></svg>' +
      '<svg class="theme-toggle__icon icon-night" viewBox="0 0 24 24" fill="currentColor" stroke="none"><path d="M20 14.5A8.5 8.5 0 1 1 9.5 4a7 7 0 0 0 10.5 10.5Z"/></svg>' +
      "</span></button>";
  };

  // Language button, a link (it works without JS) to the same page in the
  // other language. Globe plus the code of the TARGET language (what you switch
  // to, not what you are on).
  window.SITE.langToggleHtml = function langToggleHtml() {
    var target = pageLang() === "en" ? "fr" : "en";
    var href = window.SITE.counterpartPath(target);
    var label = s("langToggleAria");
    return (
      '<a class="lang-toggle" data-lang-toggle href="' + href + '" hreflang="' + target + '" ' +
      'aria-label="' + label + '" title="' + label + '">' +
      '<svg class="lang-toggle__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">' +
      '<circle cx="12" cy="12" r="9"/><path d="M3 12h18"/><path d="M12 3c2.6 2.8 2.6 15.2 0 18M12 3c-2.6 2.8-2.6 15.2 0 18"/></svg>' +
      '<span class="lang-toggle__code">' + target.toUpperCase() + "</span></a>"
    );
  };
})();
