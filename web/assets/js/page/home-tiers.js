(function () {
  "use strict";

  window.SITE = window.SITE || {};

  // The tier table and its slider. Kept out of home.js because it is CONTENT
  // (the labels and their ordering) as much as behaviour.

  // SLOT. A preview of the first 15 tiers of whatever the mod ramps up: waves,
  // depths, difficulty steps, crafting levels. The labels live here rather than
  // in the i18n tables because a mod that names its tiers after something the
  // game itself prints ("Wave 3", "Level 3") wants one spelling in both
  // languages. If the names have to be translated, move them to
  // assets/js/i18n/ next to the descriptions, which are already read there as
  // SITE.s("tierDescs").
  //
  // Change the count and the rest follows: the ticks, the heat wash and PEAK_FROM
  // are all derived from the length of this list.
  // The six crates are named after the material that makes them, and the game
  // prints those names differently in the two languages, so the list lives in
  // assets/js/i18n/ beside the descriptions rather than here. Read at init
  // rather than at load: the language can change under a router swap.

  // From this tier on, the ticks take the accent colour: the reading is no
  // longer "rising", it is at the top of the range. Breeze and echo are the two
  // that need a trial chamber and an ancient city, so the change lands there.
  var PEAK_FROM = 4;

  function initTiers() {
    var sec = document.getElementById("home-tiers");
    if (!sec || sec.hasAttribute("data-home-bound")) return;
    sec.setAttribute("data-home-bound", "");
    var tierWord = window.SITE.s("tier");
    var TIER_NAMES = window.SITE.s("tierNames");
    var descriptions = window.SITE.s("tierDescs");
    var numEl = document.getElementById("home-tier-num"),
        nameEl = document.getElementById("home-tier-name"),
        descEl = document.getElementById("home-tier-desc"),
        ticks = document.getElementById("home-ticks"),
        wash = document.getElementById("home-tiers-wash");

    var pinned = 0;
    var tickButtons = [];
    var lastTickSfx = 0; // throttles the hover sound (a fast sweep across the tiers)
    function pad(n) { return (n + 1 < 10 ? "0" : "") + (n + 1); }

    function render(i, punch) {
      numEl.textContent = pad(i);
      nameEl.textContent = TIER_NAMES[i];
      descEl.textContent = descriptions[i];
      tickButtons.forEach(function (el, idx) {
        el.classList.toggle("on", idx <= i);
        el.classList.toggle("peak", idx <= i && idx >= PEAK_FROM);
        el.classList.toggle("sel", idx === i);
        el.setAttribute("aria-pressed", idx === pinned ? "true" : "false");
      });
      var heat = i / (TIER_NAMES.length - 1);
      wash.style.setProperty("--heat", (heat * heat).toFixed(3));
      // A small visual kick on the number when a tier is PINNED (not on a mere
      // hover). It tells browsing apart from confirming.
      if (punch && !window.SITE.reduceMotion()) {
        numEl.classList.remove("punch");
        void numEl.offsetWidth;
        numEl.classList.add("punch");
      }
    }

    for (var i = 0; i < TIER_NAMES.length; i++) {
      var b = document.createElement("button");
      b.type = "button";
      b.className = "home-tick";
      b.setAttribute("aria-label", tierWord + " " + pad(i));
      b.setAttribute("title", tierWord + " " + pad(i));
      (function (idx) {
        var hover = function () {
          render(idx, false);
          // The same timbre as the click at a lower volume: hovering scans,
          // clicking confirms. Throttled, or a fast sweep across the tiers
          // machine-guns the sound.
          var now = (window.performance && performance.now) ? performance.now() : Date.now();
          if (window.SITE.sfx && now - lastTickSfx > 70) {
            lastTickSfx = now;
            window.SITE.sfx.stamp({ volume: 0.45 });
          }
        };
        b.addEventListener("pointerenter", hover);
        b.addEventListener("focus", hover);
        b.addEventListener("click", function () {
          pinned = idx;
          render(idx, true);
          if (window.SITE.sfx) window.SITE.sfx.stamp();
        });
      })(i);
      ticks.appendChild(b);
    }
    tickButtons = [].slice.call(ticks.children);

    ticks.addEventListener("pointerleave", function () { render(pinned); });

    render(pinned);
  }

  window.SITE._initTiers = initTiers;
})();
