(function () {
  "use strict";

  window.SITE = window.SITE || {};

  // SLOT. The one chart in the wiki (wiki/rows.html): how many slots a crate
  // has against how many row modules sit in its lower cell, drawn as inline
  // SVG with no library.
  //
  // The rule that matters is not the shape of the curves, it is where the
  // numbers come from. Both lines are the mod's own rule, base + 9 per module,
  // with the bases read off the tier table on wiki/crates.html. They end at 171
  // and 216, which is what the prose on this page says, digit for digit. A
  // chart that disagrees with the page around it is worse than no chart.
  //
  // The two series are the smallest and the largest crate on purpose: the point
  // is that the lines cross nothing, and a fully equipped copper crate still
  // ends up larger than a bare echo one.
  window.SITE.initWikiChart = function initWikiChart() {
    var host = document.getElementById("wiki-chart");
    if (!host || host.hasAttribute("data-chart-bound")) return;
    host.setAttribute("data-chart-bound", "");

    var names = window.SITE.s("tierNames");

    // The crate bases, copper and echo, and the nine slots a row module buys.
    var BASE_A = 27, BASE_B = 72, PER_MODULE = 9;
    var MODULE_MAX = 16, Y_MAX = 216;
    var W = 600, H = 240, ML = 38, MR = 16, MT = 16, MB = 34;
    var plotW = W - ML - MR, plotH = H - MT - MB;

    function x(modules) { return ML + (modules / MODULE_MAX) * plotW; }
    function y(v) { return MT + plotH - (Math.min(v, Y_MAX) / Y_MAX) * plotH; }
    function seriesA(m) { return BASE_A + PER_MODULE * m; }
    function seriesB(m) { return BASE_B + PER_MODULE * m; }

    // Two points are enough for a straight line, and drawing forty would only
    // suggest a curve that is not there.
    function curve(fn) {
      return [0, MODULE_MAX].map(function (m) { return x(m) + "," + y(fn(m)).toFixed(1); }).join(" ");
    }

    var vGrid = [0, 4, 8, 12, 16].map(function (m) {
      return '<line x1="' + x(m) + '" x2="' + x(m) + '" y1="' + MT + '" y2="' + (MT + plotH) + '" class="site-chart__grid"/>' +
        '<text x="' + x(m) + '" y="' + (MT + plotH + 18) + '" class="site-chart__label" text-anchor="middle">' + m + "</text>";
    }).join("");
    var hGrid = [0, 54, 108, 162, 216].map(function (v) {
      return '<line x1="' + ML + '" x2="' + (ML + plotW) + '" y1="' + y(v) + '" y2="' + y(v) + '" class="site-chart__grid"/>';
    }).join("");

    host.innerHTML =
      '<svg viewBox="0 0 ' + W + " " + H + '" role="presentation" focusable="false">' +
      hGrid + vGrid +
      '<polyline points="' + curve(seriesA) + '" class="site-chart__line site-chart__line--a"/>' +
      '<polyline points="' + curve(seriesB) + '" class="site-chart__line site-chart__line--b"/>' +
      '<line x1="' + x(16) + '" x2="' + x(16) + '" y1="' + MT + '" y2="' + (MT + plotH) + '" class="site-chart__marker"/>' +
      '<text x="' + (ML + plotW) + '" y="' + (MT + plotH + 18) + '" class="site-chart__label" text-anchor="end">' + window.SITE.s("chartAxis") + "</text>" +
      "</svg>" +
      '<div class="site-chart__legend">' +
      '<span><i class="site-chart__swatch site-chart__swatch--a"></i>' + names[0] + " (" + seriesA(0) + " > " + seriesA(MODULE_MAX) + ")</span>" +
      '<span><i class="site-chart__swatch site-chart__swatch--b"></i>' + names[5] + " (" + seriesB(0) + " > " + seriesB(MODULE_MAX) + ")</span>" +
      "</div>";
  };
  document.addEventListener("DOMContentLoaded", window.SITE.initWikiChart);
})();
