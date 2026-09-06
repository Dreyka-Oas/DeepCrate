(function () {
  "use strict";

  window.SITE = window.SITE || {};

  // KEEP. Owns the single AudioContext, master gain, and mute switch that
  // voices.js and toggle.js reach through window.SITE._audio. Replacing or
  // reshaping this file breaks both of them at once.

  // The audio engine: one AudioContext, one master gain, one mute state, one
  // noise buffer, and the background drone. The voices (voices.js) and the mute
  // button (toggle.js) reach all of it through window.SITE._audio. That handle is
  // internal to fx/sound/, never part of the site's public SITE surface.
  //
  // Everything is synthesised, no external audio file, consistent with the rest
  // of the site (zero imported asset). A deliberately physical palette: ink
  // stamp, radio static, typewriter clatter. Never a generic interface "beep".
  // Sound only plays after a first user gesture (the browsers' rule) and can be
  // silenced for good.

  var STORAGE_KEY = "site-sound";
  var MASTER_VOLUME = 0.5;

  var ctx = null;
  var master = null;
  var noiseBuffer = null;
  var ambient = null;

  // Wrapped: a browser with storage blocked (private mode, cookies off) throws
  // on plain access, and an uncaught throw here would take the whole module
  // down: no SITE.sfx at all, and every guarded caller silently soundless.
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

  // Background drone, a server-room/lab hum, never a "musical" sound: heavily
  // filtered noise, slowly modulated by an LFO so the ear does not settle into
  // it the way it would with a fixed electrical buzz. Cut when the tab is not
  // visible (CPU/battery courtesy) and restarted on return, without ever
  // needing a fresh gesture to retrigger it.
  function startAmbient() {
    var c = ensureCtx();
    if (!c || ambient || document.hidden || !enabled) return;
    var src = noiseSource();
    var lp = c.createBiquadFilter();
    lp.type = "lowpass";
    lp.frequency.value = 220;
    lp.Q.value = 0.6;
    var lfo = c.createOscillator();
    lfo.frequency.value = 0.06;
    var lfoGain = c.createGain();
    lfoGain.gain.value = 55;
    lfo.connect(lfoGain).connect(lp.frequency);
    var g = c.createGain();
    g.gain.value = 0;
    g.gain.setTargetAtTime(0.035, c.currentTime, 3);
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
    // Short fade before the cut, avoiding an audible click.
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

  window.SITE._audio = {
    // Returns the live context, or null where Web Audio is unavailable and
    // muted while the user has the sound off: the single gate every voice
    // opens with.
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
