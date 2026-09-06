(function () {
  "use strict";

  window.DC = window.DC || {};

  // The mute button in the nav. Rebindable like the theme one: the router only
  // ever replaces <body>, so this button is reinjected on every navigation.
  //
  // Must be loaded AFTER fx/sound/voices.js (it plays a latch on unmute) and
  // AFTER core/dc-lang.js plus the tables in assets/js/i18n/, since the labels
  // go through DC.s.
  var A = window.DC._audio;

  function updateUi(btn) {
    var on = A.isEnabled();
    btn.textContent = on ? window.DC.s("soundOn") : window.DC.s("soundOff");
    btn.setAttribute("aria-pressed", on ? "true" : "false");
    btn.setAttribute("aria-label", on ? window.DC.s("soundMuteAria") : window.DC.s("soundEnableAria"));
    btn.classList.toggle("is-muted", !on);
  }

  window.DC.initSoundToggle = function initSoundToggle() {
    var btn = document.querySelector("[data-sound-toggle]");
    if (!btn || btn.hasAttribute("data-sound-bound")) return;
    btn.setAttribute("data-sound-bound", "");
    updateUi(btn);
    btn.addEventListener("click", function () {
      A.setEnabled(!A.isEnabled());
      updateUi(btn);
      // Only on the way back on: an unmute that stays silent gives no
      // confirmation that it worked.
      if (A.isEnabled()) window.DC.sfx.latch({ volume: 0.6 });
    });
  };
  document.addEventListener("DOMContentLoaded", window.DC.initSoundToggle);
})();
