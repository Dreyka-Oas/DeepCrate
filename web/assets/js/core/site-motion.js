(function () {
  "use strict";

  window.SITE = window.SITE || {};

  // KEEP. The single prefers-reduced-motion check every animated part of
  // the site calls through SITE.reduceMotion(). Cache the result instead of
  // reading it live and a visitor who turns the setting on mid-visit still
  // gets a half-still site.
  //
  // Reduced motion, in one place.
  //
  // Seven modules gate on this setting, across eight call sites: the opening
  // screen, the ledger, the reveal observer, the hero backdrop, the
  // escalation punch, the home title (twice) and the router's page
  // transition. Each one used to carry its own copy of the matchMedia call,
  // half of them caching the answer at load time, which meant a visitor
  // turning the setting on mid-visit got a half-still site.
  //
  // Read live on every call: matchMedia().matches is a property read, cheap
  // enough for the places this is used (never inside a rAF loop), and it
  // follows the OS setting as it changes.
  window.SITE.reduceMotion = function reduceMotion() {
    return !!(window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches);
  };
})();
