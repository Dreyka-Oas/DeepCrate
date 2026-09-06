(function () {
  "use strict";

  // The root carries no content of its own. It sends the visitor into one of the
  // two trees, reading the stored preference first and the browser's languages
  // after it. With JavaScript off nothing runs and the page shows both links,
  // which is why they are written in the HTML rather than rendered here.

  var LANG_KEY = "dc-lang";

  function stored() {
    try {
      var pref = localStorage.getItem(LANG_KEY);
      return pref === "fr" || pref === "en" ? pref : null;
    } catch (e) {
      return null;
    }
  }

  function fromBrowser() {
    var list = navigator.languages || [navigator.language || "fr"];
    for (var i = 0; i < list.length; i++) {
      var code = String(list[i]).slice(0, 2).toLowerCase();
      if (code === "fr" || code === "en") return code;
    }
    return "fr";
  }

  // replace() rather than assign(): the gate has nothing to come back to, and
  // leaving it in the history traps the back button on this page.
  location.replace("/lang/" + (stored() || fromBrowser()) + "/index.html");
})();
