(function () {
  "use strict";

  window.SITE = window.SITE || {};

  // KEEP. This file wires the home page's behaviours to whatever markup is
  // there, and it names nothing. Delete a band from index.html and the hook
  // that went with it finds no host node and returns, so nothing here has to
  // be touched when the page loses a section.

  // Every hook below is idempotent and guarded by the presence of its host
  // node, so it is safe to call once on DOMContentLoaded AND again after each
  // pjax body-swap (router/router-swap.js). Continuous rAF/interval loops
  // self-terminate when their host element leaves the DOM after a navigation.

  // Page opening: the title is masked, then resolves letter by letter. Played
  // once on load, never on hover, and skipped entirely under reduced motion.
  function initTitle() {
    var t = document.getElementById("home-title");
    if (!t || t.hasAttribute("data-home-bound")) return;
    t.setAttribute("data-home-bound", "");
    if (window.SITE.reduceMotion()) return;
    if (window.SITE.sfx) window.SITE.sfx.sweep({ volume: 1.3 });
    t.classList.add("is-anim");
    t.querySelectorAll(".part").forEach(function (part, partIdx) {
      var txt = part.textContent;
      part.textContent = "";
      txt.split("").forEach(function (c, i) {
        var s = document.createElement("span");
        s.className = "ch";
        s.textContent = c === " " ? " " : c;
        s.style.animationDelay = (0.2 + partIdx * 0.5 + i * 0.035) + "s";
        part.appendChild(s);
      });
    });
  }

  // Discreet 3D tilt on hover over the appendix links, following the cursor
  // and returning flat on exit. Purely decorative (aria is untouched).
  function initTilt() {
    if (window.SITE.reduceMotion()) return;
    document.querySelectorAll(".home-toc__link:not([data-tilt-bound])").forEach(function (el) {
      el.setAttribute("data-tilt-bound", "");
      el.addEventListener("pointermove", function (e) {
        var r = el.getBoundingClientRect();
        var px = (e.clientX - r.left) / r.width - 0.5;
        var py = (e.clientY - r.top) / r.height - 0.5;
        el.style.setProperty("--tilt-x", (py * -8).toFixed(2) + "deg");
        el.style.setProperty("--tilt-y", (px * 8).toFixed(2) + "deg");
      });
      el.addEventListener("pointerleave", function () {
        el.style.setProperty("--tilt-x", "0deg");
        el.style.setProperty("--tilt-y", "0deg");
      });
    });
  }

  // Audio feedback on explicit action points (CTAs, list rows), never on
  // ordinary navigation links; reserved for gestures that deserve a
  // confirmation.
  function initSoundHooks() {
    document.querySelectorAll(".home-btn:not([data-sfx-bound])").forEach(function (btn) {
      btn.setAttribute("data-sfx-bound", "");
      btn.addEventListener("click", function () { if (window.SITE.sfx) window.SITE.sfx.stamp({ volume: 0.7 }); });
    });
    document.querySelectorAll(".home-row:not([data-sfx-bound])").forEach(function (row) {
      row.setAttribute("data-sfx-bound", "");
      var play = function () { if (window.SITE.sfx) window.SITE.sfx.sweep({ volume: 0.4 }); };
      row.addEventListener("pointerenter", play);
      row.addEventListener("focus", play);
    });
    // Timeline band: a softened stamp per step hovered or activated,
    // the same physical sound as the rest of the dossier (no more square beep).
    document.querySelectorAll(".home-tl:not([data-sfx-bound])").forEach(function (cell) {
      cell.setAttribute("data-sfx-bound", "");
      var play = function () { if (window.SITE.sfx) window.SITE.sfx.stamp({ volume: 0.4 }); };
      cell.addEventListener("pointerenter", play);
      cell.addEventListener("focus", play);
    });
  }

  window.SITE.initHome = function initHome() {
    if (!document.querySelector(".home")) return;
    initTitle();
    if (window.SITE._initBackdrop) window.SITE._initBackdrop();
    if (window.SITE._initTiers) window.SITE._initTiers();
    initSoundHooks();
    initTilt();
  };

  document.addEventListener("DOMContentLoaded", window.SITE.initHome);
})();
