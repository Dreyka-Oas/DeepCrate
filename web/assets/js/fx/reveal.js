(function () {
  "use strict";

  window.DC = window.DC || {};

  // Reveal on scroll. Replayable after a router swap, because a reinjected
  // block carries no observer of its own.

  window.DC.initReveal = function initReveal() {
    var targets = document.querySelectorAll(".reveal:not([data-reveal-bound])");
    if (!targets.length) return;

    if (!("IntersectionObserver" in window)) {
      targets.forEach(function (el) {
        el.classList.add("in-view");
        el.setAttribute("data-reveal-bound", "");
      });
      return;
    }

    var observer = new IntersectionObserver(function (entries) {
      entries.forEach(function (entry) {
        if (!entry.isIntersecting) return;
        entry.target.classList.add("in-view");
        // The stagger only makes sense on appearance. Cleared once revealed so
        // it cannot hold back a later transition on the same element.
        entry.target.style.transitionDelay = "";
        observer.unobserve(entry.target);
      });
    }, { threshold: 0.15, rootMargin: "0px 0px -40px 0px" });

    targets.forEach(function (el, i) {
      el.setAttribute("data-reveal-bound", "");
      el.classList.add("reveal-ready");
      if (!window.DC.reduceMotion()) el.style.transitionDelay = Math.min(i * 55, 220) + "ms";
      observer.observe(el);
    });
  };

  document.addEventListener("DOMContentLoaded", window.DC.initReveal);
})();
