(function () {
  "use strict";

  window.SITE = window.SITE || {};

  if (typeof customElements === "undefined") return;

  var s = window.SITE.s;

  // The section separator reused on every page: a row of short codes, a few of
  // which corrupt over time. Purely ambient (aria-hidden); it never carries
  // information anyone needs, which is why inventing the codes is allowed.
  //
  // SLOT ledger-tokens. The tokens below are neutral on purpose. A mod puts
  // its own two or three character codes here: item prefixes, biome
  // initials, whatever its players already read in game. Keep them the same
  // width, or the row jitters every time a cell corrupts.
  // The six crate materials plus the four things a crate is made of on the
  // screen: a module, a row, a slot, a page. Two characters each, or the row
  // jitters every time a cell corrupts.
  var LEDGER_BASES = ["CU", "FE", "AM", "PR", "BR", "EC", "MD", "RW", "SL", "PG"];
  var LEDGER_CORRUPT = ["██", "▓▓", "░░", "××"];
  var LEDGER_CELLS = 10;

  function ledgerRow(seed) {
    var cells = [];
    for (var i = 0; i < LEDGER_CELLS; i++) {
      var base = LEDGER_BASES[(seed + i * 7) % LEDGER_BASES.length];
      var num = ((seed + i * 13) % 89 + 10);
      var flagged = (i - seed) % 4 === 0;
      cells.push({ base: base, num: num, flagged: flagged, corrupt: false });
    }
    // One position corrupted from the outset, derived from the seed: not pure
    // randomness, and stable on first paint (no flash of different content when
    // the element mounts).
    cells[(seed * 3) % cells.length].corrupt = true;
    return cells;
  }

  function renderLedgerRow(cells) {
    return cells.map(function (c) {
      if (c.corrupt) return '<span class="corrupt">' + LEDGER_CORRUPT[0] + "</span>";
      var txt = c.base + "·" + c.num;
      return c.flagged ? '<span class="flag">' + txt + "</span>" : txt;
    }).join("  ");
  }

  class SiteLedger extends HTMLElement {
    connectedCallback() {
      var label = this.getAttribute("label") || s("ledgerLabel");
      var seed = 0;
      for (var i = 0; i < label.length; i++) seed = (seed + label.charCodeAt(i)) % 97;
      this._cells = ledgerRow(seed);
      this.innerHTML =
        '<div class="ledger"><p class="ledger__label">' + label + '</p>' +
        '<div class="ledger__row" aria-hidden="true">' + renderLedgerRow(this._cells) + "</div></div>";

      if (window.SITE.reduceMotion()) return;
      var self = this;
      var rowEl = this.querySelector(".ledger__row");
      // No sound on this ambient tick: it runs continuously even off-screen,
      // and scoring an animation the user never triggered is exactly the kind
      // of unwanted background noise this site sets out to avoid.
      this._ledgerInterval = setInterval(function () {
        if (!rowEl.isConnected) return;
        var idx = Math.floor(Math.random() * self._cells.length);
        self._cells.forEach(function (c) { c.corrupt = false; });
        self._cells[idx].corrupt = true;
        rowEl.innerHTML = renderLedgerRow(self._cells);
      }, 1800 + Math.random() * 800);
    }
    disconnectedCallback() {
      clearInterval(this._ledgerInterval);
    }
  }

  if (!customElements.get("site-ledger")) customElements.define("site-ledger", SiteLedger);
})();
