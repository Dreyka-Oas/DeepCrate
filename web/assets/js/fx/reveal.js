(function () {
  "use strict";

  window.SITE = window.SITE || {};

  // KEEP. Runs the IntersectionObserver that adds in-view to .reveal blocks,
  // rerun after each pjax swap under the initReveal name that router-swap.js
  // calls by name; the reveal distances and easing curves a mod actually tunes
  // live in motion.css, not here. Drop the data-reveal-bound guard and a
  // second call to initReveal on the same nodes attaches a second observer per
  // element, so the reveal fires twice and the stagger delay math doubles up.

  // Reveal-on-scroll: .reveal blocks appear as they enter the viewport.
  // Replayable after a pjax swap (router/router-swap.js), because reinjected blocks
  // carry no observer of their own.

  window.SITE.initReveal = function initReveal() {
    var targets = document.querySelectorAll(".reveal:not([data-reveal-bound])");
    if (!targets.length) return;

    if (!("IntersectionObserver" in window)) {
      targets.forEach(function (el) { el.classList.add("in-view"); el.setAttribute("data-reveal-bound", ""); });
      return;
    }

    var observer = new IntersectionObserver(
      function (entries) {
        entries.forEach(function (entry) {
          if (entry.isIntersecting) {
            entry.target.classList.add("in-view");
            // The stagger delay only makes sense on appearance: once revealed
            // it is cleared, so it cannot hold back any later transition
            // (hover and the like) on the same element.
            entry.target.style.transitionDelay = "";
            observer.unobserve(entry.target);
          }
        });
      },
      { threshold: 0.15, rootMargin: "0px 0px -40px 0px" }
    );

    targets.forEach(function (el, i) {
      el.setAttribute("data-reveal-bound", "");
      el.classList.add("reveal-ready");
      if (!window.SITE.reduceMotion()) {
        el.style.transitionDelay = Math.min(i * 60, 240) + "ms";
      }
      observer.observe(el);
    });
  };

  document.addEventListener("DOMContentLoaded", window.SITE.initReveal);
})();
