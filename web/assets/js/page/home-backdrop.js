(function () {
  "use strict";

  window.SITE = window.SITE || {};

  // SLOT: the second layer of the hero backdrop, and a free one.
  //
  // What ships is columns of glyphs drifting slowly upward on a full-screen
  // canvas, coloured from the theme tokens. It is one drawing among many. A mod
  // may swap the glyphs and stop there, redraw draw() entirely (falling embers,
  // a slow rain, a scrolling contour map), or delete the canvas from the pages
  // and keep only the CSS scene: the two layers know nothing about each other.
  //
  // Whatever replaces draw(), the plumbing around it stays, because it is what
  // makes the layer cheap:
  //   - reduceMotion() returns before anything paints, no animation at all;
  //   - the loop stops when the hero leaves the viewport and when the canvas
  //     leaves the DOM after a pjax navigation;
  //   - colours are re-read from the tokens in refreshStyle(), never written as
  //     literals, so the layer follows light, dark and the theme button;
  //   - no image and no external asset, only what the 2D context draws.
  //
  // The CSS scene behind it is the other half of the backdrop, in
  // assets/css/components/home/hero/scene.css.
  //
  // The set below is deliberately abstract: it means nothing, which is the
  // point of a placeholder. Replace it with characters the mod actually uses.
  // Stack counts rather than machine noise: what a crate is actually full of.
  // Drifting upward reads as a cell filling, which is the mod in one gesture.
  var BACKDROP_GLYPHS = ["6", "4", "1", "2", "5", "▪"];
  function initBackdrop() {
    var c = document.getElementById("home-backdrop");
    if (!c || c.hasAttribute("data-home-bound")) return;
    c.setAttribute("data-home-bound", "");
    if (window.SITE.reduceMotion()) return;
    var ctx = c.getContext("2d"), W, H, cols = [];
    var FONT_SIZE = 13;
    // Font and colours re-read from the effective theme (no hard-coded values
    // → it follows light/dark and the button's override). Refreshed inside
    // size(), often enough to cover a theme change via a resize, without a
    // getComputedStyle on every frame.
    var fontStr = "13px monospace", colNormal = "#8a7a52", colFlag = "#8a3a2a";
    function refreshStyle() {
      var cs = getComputedStyle(document.documentElement);
      fontStr = FONT_SIZE + "px " + (cs.getPropertyValue("--font-mono").trim() || "monospace");
      colNormal = cs.getPropertyValue("--muted").trim() || colNormal;
      colFlag = cs.getPropertyValue("--accent-text").trim() || colFlag;
    }
    function size() {
      W = c.width = innerWidth;
      H = c.height = innerHeight;
      refreshStyle();
      var n = Math.ceil(W / (FONT_SIZE * 1.6));
      cols = [];
      for (var i = 0; i < n; i++) {
        cols.push({
          x: i * FONT_SIZE * 1.6 + FONT_SIZE * 0.5,
          y: Math.random() * H,
          v: 0.25 + Math.random() * 0.4,
          glyph: BACKDROP_GLYPHS[Math.floor(Math.random() * BACKDROP_GLYPHS.length)],
          flagged: Math.random() < 0.08,
          flip: 60 + Math.random() * 140
        });
      }
    }
    size();
    window.addEventListener("resize", size);

    // Paused off-screen or on a hidden tab: the canvas is fixed and full-screen
    // but sits behind the content; there is no point painting it once the hero
    // has scrolled out of view. The IntersectionObserver starts and stops the
    // loop; everything is detached once the canvas leaves the DOM (a pjax
    // navigation).
    var running = false, tick0 = 0, io = null;
    function cleanup() {
      running = false;
      window.removeEventListener("resize", size);
      if (io) { io.disconnect(); io = null; }
    }
    function draw() {
      ctx.clearRect(0, 0, W, H);
      ctx.font = fontStr;
      tick0++;
      for (var i = 0; i < cols.length; i++) {
        var col = cols[i];
        col.y -= col.v;
        if (col.y < -FONT_SIZE) col.y = H + FONT_SIZE;
        if (tick0 % Math.round(col.flip) === 0) {
          col.glyph = BACKDROP_GLYPHS[Math.floor(Math.random() * BACKDROP_GLYPHS.length)];
          col.flagged = Math.random() < 0.08;
        }
        ctx.globalAlpha = 0.16 + 0.1 * Math.sin(col.y * 0.01 + i);
        ctx.fillStyle = col.flagged ? colFlag : colNormal;
        ctx.fillText(col.glyph, col.x, col.y);
      }
      ctx.globalAlpha = 1;
    }
    function loop() {
      if (!c.isConnected) { cleanup(); return; }
      if (!running) return;
      draw();
      requestAnimationFrame(loop);
    }
    function start() { if (!running && c.isConnected) { running = true; requestAnimationFrame(loop); } }
    function stop() { running = false; }
    if ("IntersectionObserver" in window) {
      io = new IntersectionObserver(function (es) {
        es.forEach(function (e) { e.isIntersecting ? start() : stop(); });
      });
      io.observe(document.querySelector(".home-hero") || c);
    } else {
      start();
    }
  }

  window.SITE._initBackdrop = initBackdrop;
})();
