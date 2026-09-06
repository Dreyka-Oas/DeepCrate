(function () {
  "use strict";

  window.DC = window.DC || {};

  // The audio engine: one AudioContext, one master gain, one mute state, one
  // noise buffer, and the room tone underneath everything. The voices
  // (voices.js) and the mute button (toggle.js) reach all of it through
  // window.DC._audio, a handle internal to fx/sound/ and never part of the
  // site's public DC surface.
  //
  // Everything is synthesised, no audio file, like the rest of the site which
  // imports nothing. The palette is the mod's own material: pine, iron and cut
  // rock, never an interface beep. Nothing plays before a first user gesture
  // (the browsers' rule) and the mute survives a reload.

  var STORAGE_KEY = "dc-sound";
  var MASTER_VOLUME = 0.5;

  var ctx = null;
  var master = null;
  var noiseBuffer = null;
  var ambient = null;

  // Wrapped: a browser with storage blocked (private mode, cookies off) throws
  // on plain access, and an uncaught throw here would take the module down,
  // leaving no DC.sfx at all and every guarded caller silently soundless.
  function stored(key) {
    try { return localStorage.getItem(key); } catch (e) { return null; }
  }
  function store(key, value) {
    try { localStorage.setItem(key, value); } catch (e) { /* choice not persisted */ }
  }

  var enabled = stored(STORAGE_KEY) !== "off";

  function ensureCtx() {
    if (ctx) return ctx;
    var AC = window.AudioContext || window.webkitAudioContext;
    if (!AC) return null;
    ctx = new AC();
    master = ctx.createGain();
    master.gain.value = enabled ? MASTER_VOLUME : 0;
    master.connect(ctx.destination);
    noiseBuffer = ctx.createBuffer(1, ctx.sampleRate, ctx.sampleRate);
    var data = noiseBuffer.getChannelData(0);
    for (var i = 0; i < data.length; i++) data[i] = Math.random() * 2 - 1;
    return ctx;
  }

  function noiseSource() {
    var src = ctx.createBufferSource();
    src.buffer = noiseBuffer;
    src.loop = true;
    return src;
  }

  // Room tone rather than music: a storeroom underground, so heavily filtered
  // noise with a slow swell, cut lower than a machine hum would sit. The LFO
  // keeps the ear from settling into it the way it would with a fixed drone.
  // Stopped when the tab is hidden and restarted on return, without needing a
  // fresh gesture.
  function startAmbient() {
    var c = ensureCtx();
    if (!c || ambient || document.hidden || !enabled) return;
    var src = noiseSource();
    var lp = c.createBiquadFilter();
    lp.type = "lowpass";
    lp.frequency.value = 165;
    lp.Q.value = 0.7;
    var lfo = c.createOscillator();
    lfo.frequency.value = 0.045;
    var lfoGain = c.createGain();
    lfoGain.gain.value = 40;
    lfo.connect(lfoGain).connect(lp.frequency);
    var g = c.createGain();
    g.gain.value = 0;
    g.gain.setTargetAtTime(0.03, c.currentTime, 3.5);
    src.connect(lp).connect(g).connect(master);
    src.start();
    lfo.start();
    ambient = { src: src, lfo: lfo, gain: g };
  }

  function stopAmbient(hard) {
    if (!ambient) return;
    var a = ambient;
    ambient = null;
    if (hard) {
      try { a.src.stop(); a.lfo.stop(); } catch (e) { /* already stopped */ }
      return;
    }
    // Short fade before the cut, so there is no audible click.
    a.gain.gain.setTargetAtTime(0, ctx.currentTime, 0.15);
    setTimeout(function () { try { a.src.stop(); a.lfo.stop(); } catch (e) { /* noop */ } }, 600);
  }

  function resume() {
    var c = ensureCtx();
    if (c && c.state === "suspended") c.resume();
    startAmbient();
  }
  ["pointerdown", "keydown"].forEach(function (evt) {
    document.addEventListener(evt, resume, { once: true, passive: true });
  });

  document.addEventListener("visibilitychange", function () {
    if (document.hidden) stopAmbient(false);
    else startAmbient();
  });

  window.DC._audio = {
    // The live context, or null where Web Audio is missing and while the
    // visitor has the sound off: the single gate every voice opens with.
    voiceCtx: function voiceCtx() {
      if (!enabled) return null;
      return ensureCtx();
    },
    noiseSource: noiseSource,
    master: function () { return master; },
    isEnabled: function () { return enabled; },
    setEnabled: function setEnabled(next) {
      enabled = next;
      store(STORAGE_KEY, next ? "on" : "off");
      if (master) master.gain.setTargetAtTime(next ? MASTER_VOLUME : 0, ctx.currentTime, 0.03);
      if (next) startAmbient();
      else stopAmbient(true);
    }
  };
})();
