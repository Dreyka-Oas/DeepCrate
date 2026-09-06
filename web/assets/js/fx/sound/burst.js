(function () {
  "use strict";

  // One shaped burst of noise: source, filter, gain envelope, master, then
  // discarded. Most of the graphs in voices.js are exactly this and differ only
  // in their numbers, so the shape lives here once and each voice is written as
  // the numbers that make it.
  //
  // The spec stays literal: every value a voice needs is spelled out by that
  // voice, envelope floor and stop offset included. Nothing is normalised
  // behind the caller's back, because these are sounds and a tidier shared
  // default would quietly retune them.
  //
  //   filter  biquad type ("lowpass" | "highpass" | "bandpass")
  //   q       filter Q, omitted to keep the node default
  //   from    filter frequency at the start of the burst
  //   to      filter frequency to sweep to, omitted for a fixed filter
  //   sweep   seconds the from-to sweep takes
  //   start   gain to open at, only with attack
  //   attack  seconds to swell from start to peak, omitted for an instant hit
  //   peak    loudest gain of the burst
  //   floor   gain the exponential decay aims at
  //   decay   seconds from the start of the burst to that floor
  //   stop    seconds from the start of the burst to releasing the source
  //
  // Must be loaded AFTER fx/sound/engine.js: it extends DC._audio, the handle
  // internal to fx/sound/.
  var A = window.DC._audio;

  A.burst = function burst(ctx, at, spec) {
    var src = A.noiseSource();

    var filter = ctx.createBiquadFilter();
    filter.type = spec.filter;
    if (spec.q !== undefined) filter.Q.value = spec.q;
    if (spec.to !== undefined) {
      filter.frequency.setValueAtTime(spec.from, at);
      filter.frequency.exponentialRampToValueAtTime(spec.to, at + spec.sweep);
    } else {
      filter.frequency.value = spec.from;
    }

    var gain = ctx.createGain();
    if (spec.attack !== undefined) {
      gain.gain.setValueAtTime(spec.start, at);
      gain.gain.linearRampToValueAtTime(spec.peak, at + spec.attack);
    } else {
      gain.gain.setValueAtTime(spec.peak, at);
    }
    gain.gain.exponentialRampToValueAtTime(spec.floor, at + spec.decay);

    src.connect(filter).connect(gain).connect(A.master());
    src.start(at);
    src.stop(at + spec.stop);
  };
})();
