(function () {
  "use strict";

  window.SITE = window.SITE || {};

  // KEEP. Wires the nav's mute button to window.SITE._audio and is rerun
  // after every pjax swap under the initSoundToggle name that router-swap.js
  // calls by name. Remove the data-sound-bound guard and the click handler
  // stacks on every navigation, so a single click ends up toggling the mute
  // state twice.

  // The mute button in the nav. Rebindable like the other two toggles: the
  // router only ever replaces <body>, so this button is reinjected on every
  // navigation.
  //
  // Must be loaded AFTER fx/sound/voices.js (it plays a stamp on unmute) and
  // AFTER core/site-lang.js plus the tables in assets/js/i18n/ (the labels go
  // through SITE.s).
  var s = window.SITE.s;
  var A = window.SITE._audio;

  function updateUi(btn) {
    var on = A.isEnabled();
    btn.setAttribute("aria-pressed", on ? "true" : "false");
    btn.setAttribute("aria-label", on ? s("soundMuteAria") : s("soundEnableAria"));
    btn.classList.toggle("is-muted", !on);
  }

  window.SITE.initSoundToggle = function initSoundToggle() {
    var btn = document.querySelector("[data-sound-toggle]");
    if (!btn || btn.hasAttribute("data-sound-bound")) return;
    btn.setAttribute("data-sound-bound", "");
    updateUi(btn);
    btn.addEventListener("click", function () {
      A.setEnabled(!A.isEnabled());
      updateUi(btn);
      // Only on the way back on: an unmute that stays silent gives no
      // confirmation that it worked.
      if (A.isEnabled()) window.SITE.sfx.stamp({ volume: 0.6 });
    });
  };
  document.addEventListener("DOMContentLoaded", window.SITE.initSoundToggle);
})();
