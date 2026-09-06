(function () {
  "use strict";

  window.SITE = window.SITE || {};

  // SLOT, and the loudest one in the template: the four voices, stamp, sweep,
  // click and whoosh, that give the site its own sound identity. A new mod
  // rewrites what those four functions play, tuning A.burst()'s filter, peak
  // and decay numbers or wiring in raw oscillators as needed. Keep exporting
  // the same four names on window.SITE.sfx: router.js, toggle.js and the page
  // scripts call them by name, so renaming one breaks the call site. Reusing
  // another mod's exact sound palette here leaves the site dressed in someone
  // else's clothes, and one rule never bends regardless of what plays: nothing
  // may sound before a first real user gesture, a click or a key press,
  // because that is the browser's autoplay policy, not a house preference.

  // The dossier's four voices, published as SITE.sfx. Every one opens the same
  // way: ask the engine for a context, and get null where Web Audio is missing
  // or the user has muted. Nothing here holds state: engine.js owns all of it,
  // burst.js owns the one graph shape they share.
  //
  // Must be loaded AFTER fx/sound/burst.js.
  var A = window.SITE._audio;

  // A small bounded random variation. The same sound replayed identically on
  // every click ends up sounding like a machine, not a living dossier.
  function jitter(base, amount) { return base + (Math.random() * 2 - 1) * amount; }

  function volumeOf(opts) { return (opts && opts.volume) || 1; }

  // Ink stamp: low-pass filtered noise plus a deep sine thump, fast attack and
  // decay (the containment-level change, picking a tier, the action buttons).
  function stamp(opts) {
    var c = A.voiceCtx();
    if (!c) return;
    var now = c.currentTime, vol = volumeOf(opts);

    A.burst(c, now, {
      filter: "lowpass", from: jitter(1400, 200), to: 180, sweep: 0.09,
      peak: 0.5 * vol, floor: 0.001, decay: 0.11, stop: 0.12
    });

    // The thump is an oscillator, not noise: it is the weight of the stamp
    // hitting paper, and noise has no pitch to drop.
    var thump = c.createOscillator();
    thump.type = "sine";
    thump.frequency.setValueAtTime(jitter(140, 12), now);
    thump.frequency.exponentialRampToValueAtTime(60, now + 0.12);
    var tg = c.createGain();
    tg.gain.setValueAtTime(0.6 * vol, now);
    tg.gain.exponentialRampToValueAtTime(0.001, now + 0.16);
    thump.connect(tg).connect(A.master());
    thump.start(now);
    thump.stop(now + 0.17);
  }

  // Band-pass noise sweeping downward: radio interference being wiped away.
  // Used by the home page title reveal and the list rows.
  function sweep(opts) {
    var c = A.voiceCtx();
    if (!c) return;
    A.burst(c, c.currentTime, {
      filter: "bandpass", q: 0.8, from: jitter(2600, 400), to: jitter(500, 100), sweep: 0.18,
      start: 0.001, attack: 0.02, peak: 0.32 * volumeOf(opts), floor: 0.001, decay: 0.2, stop: 0.22
    });
  }

  // Navigation confirmation: a fingertip tap on paper, soft and low, meant to
  // be replayed often (the site's links) without wearing thin. Delegated
  // exactly once by router/router.js.
  function click() {
    var c = A.voiceCtx();
    if (!c) return;
    A.burst(c, c.currentTime, {
      filter: "bandpass", q: 1.2, from: jitter(1000, 150),
      peak: 0.11, floor: 0.001, decay: 0.045, stop: 0.05
    });
  }

  // Page turn, synchronised with the router's swap. Three layers: a crumple of
  // very short grains at random times, pitches and amplitudes (it is that
  // irregular grain, not a smooth whoosh, that reads as "paper" to the ear), the
  // rush of air beneath it, and a muffled tap as the page settles.
  function whoosh() {
    var c = A.voiceCtx();
    if (!c) return;
    var now = c.currentTime;

    var grains = 16 + Math.floor(Math.random() * 8);
    for (var i = 0; i < grains; i++) {
      // Drawn in this order on purpose: placement, pitch, amplitude, length.
      // The four come off one random stream, so reordering them re-rolls every
      // grain in the burst.
      var at = now + Math.pow(Math.random(), 1.6) * 0.26;   // biased to the start: the paper folds, then settles
      var pitch = 1700 + Math.random() * 3200;
      var peak = 0.04 + Math.random() * 0.08;
      var dur = 0.005 + Math.random() * 0.02;
      A.burst(c, at, {
        filter: "highpass", from: pitch,
        start: 0.0001, attack: 0.001, peak: peak,
        floor: 0.0001, decay: dur, stop: dur + 0.01
      });
    }

    A.burst(c, now, {
      filter: "bandpass", q: 0.7, from: 520, to: jitter(1600, 200), sweep: 0.24,
      start: 0.001, attack: 0.06, peak: 0.08, floor: 0.001, decay: 0.3, stop: 0.32
    });

    A.burst(c, now + 0.27, {
      filter: "lowpass", from: 850,
      peak: 0.08, floor: 0.0005, decay: 0.06, stop: 0.08
    });
  }

  window.SITE.sfx = {
    stamp: stamp,
    sweep: sweep,
    click: click,
    whoosh: whoosh
  };
})();
