(function () {
  "use strict";

  window.DC = window.DC || {};

  // The four voices, published as DC.sfx. Each opens the same way: ask the
  // engine for a context, and get null where Web Audio is missing or the
  // visitor has muted. Nothing here holds state, engine.js owns all of it and
  // burst.js owns the graph shape they share.
  //
  // The four jobs are the ones any page needs: confirm a press, mark a reveal,
  // answer a link, cover a page swap. Only the numbers belong to this mod, and
  // they are pine, iron and cut rock rather than paper and ink.
  //
  // Must be loaded AFTER fx/sound/burst.js.
  var A = window.DC._audio;

  // A small bounded variation. The same sound replayed identically on every
  // click ends up sounding like a machine rather than a room.
  function jitter(base, amount) { return base + (Math.random() * 2 - 1) * amount; }

  function volumeOf(opts) { return (opts && opts.volume) || 1; }

  // Iron latch dropping into a pine lid: a short bright tick over a low thud.
  // The confirmation voice, used by the buttons and by the unmute.
  function latch(opts) {
    var c = A.voiceCtx();
    if (!c) return;
    var now = c.currentTime, vol = volumeOf(opts);

    A.burst(c, now, {
      filter: "bandpass", q: 2.4, from: jitter(2300, 260), to: 900, sweep: 0.05,
      peak: 0.34 * vol, floor: 0.001, decay: 0.07, stop: 0.08
    });

    // The thud is an oscillator, not noise: it is the weight landing on wood,
    // and noise has no pitch to drop.
    var body = c.createOscillator();
    body.type = "sine";
    body.frequency.setValueAtTime(jitter(168, 14), now);
    body.frequency.exponentialRampToValueAtTime(74, now + 0.11);
    var bg = c.createGain();
    bg.gain.setValueAtTime(0.5 * vol, now);
    bg.gain.exponentialRampToValueAtTime(0.001, now + 0.15);
    body.connect(bg).connect(A.master());
    body.start(now);
    body.stop(now + 0.16);
  }

  // An echo shard ringing thin, the one bright thing in the room: filtered
  // noise opening upward under a high partial that fades faster than it. Marks
  // a reveal, so it stays rare.
  function shard(opts) {
    var c = A.voiceCtx();
    if (!c) return;
    var now = c.currentTime, vol = volumeOf(opts);

    A.burst(c, now, {
      filter: "bandpass", q: 1.6, from: jitter(900, 120), to: jitter(3400, 300), sweep: 0.2,
      start: 0.001, attack: 0.03, peak: 0.2 * vol, floor: 0.001, decay: 0.26, stop: 0.28
    });

    var ring = c.createOscillator();
    ring.type = "triangle";
    ring.frequency.value = jitter(1870, 90);
    var rg = c.createGain();
    rg.gain.setValueAtTime(0.0001, now);
    rg.gain.linearRampToValueAtTime(0.09 * vol, now + 0.04);
    rg.gain.exponentialRampToValueAtTime(0.0001, now + 0.34);
    ring.connect(rg).connect(A.master());
    ring.start(now);
    ring.stop(now + 0.35);
  }

  // Knuckle on a lid: soft, low, meant to be replayed on every internal link
  // without wearing thin. Delegated exactly once by router/router.js.
  function tap() {
    var c = A.voiceCtx();
    if (!c) return;
    A.burst(c, c.currentTime, {
      filter: "lowpass", from: jitter(760, 120),
      peak: 0.13, floor: 0.001, decay: 0.05, stop: 0.06
    });
  }

  // A crate dragged across stone and set down, synchronised with the router's
  // swap. Three layers: the grain of the drag, made of short irregular grains
  // because a smooth whoosh reads as air rather than as stone; the low body of
  // the travel; and the corner settling at the end.
  function slide() {
    var c = A.voiceCtx();
    if (!c) return;
    var now = c.currentTime;

    var grains = 14 + Math.floor(Math.random() * 8);
    for (var i = 0; i < grains; i++) {
      // Drawn in this order on purpose: placement, pitch, amplitude, length.
      // The four come off one random stream, so reordering them re-rolls every
      // grain in the drag.
      var at = now + Math.pow(Math.random(), 1.4) * 0.24;
      var pitch = 900 + Math.random() * 1700;
      var peak = 0.03 + Math.random() * 0.06;
      var dur = 0.006 + Math.random() * 0.024;
      A.burst(c, at, {
        filter: "bandpass", q: 0.9, from: pitch,
        start: 0.0001, attack: 0.002, peak: peak,
        floor: 0.0001, decay: dur, stop: dur + 0.01
      });
    }

    A.burst(c, now, {
      filter: "lowpass", from: 240, to: jitter(520, 80), sweep: 0.22,
      start: 0.001, attack: 0.07, peak: 0.1, floor: 0.001, decay: 0.3, stop: 0.32
    });

    A.burst(c, now + 0.26, {
      filter: "lowpass", from: 380,
      peak: 0.1, floor: 0.0005, decay: 0.07, stop: 0.09
    });
  }

  window.DC.sfx = {
    latch: latch,
    shard: shard,
    tap: tap,
    slide: slide
  };
})();
