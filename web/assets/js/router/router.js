(function () {
  "use strict";

  // Client-side navigation as progressive enhancement. The site stays a plain
  // multi-page static export: every URL is a real .html file that loads on its
  // own, with no build step and no server. This script intercepts same-origin
  // link clicks and swaps <body> through fetch(), so <head> never re-downloads
  // and the browser never does a full navigation for an internal link. Anything
  // it cannot handle safely falls back to a normal navigation.

  if (!window.fetch || !window.history || !window.history.pushState) return;

  function sameOrigin(url) {
    try { return new URL(url, location.href).origin === location.origin; }
    catch (e) { return false; }
  }

  function isNavigableClick(e, a) {
    if (!a || !a.href) return false;
    if (a.target && a.target !== "" && a.target !== "_self") return false;
    if (a.hasAttribute("download")) return false;
    if (a.getAttribute("href").charAt(0) === "#") return false;
    if (e.defaultPrevented || e.button !== 0) return false;
    if (e.metaKey || e.ctrlKey || e.shiftKey || e.altKey) return false;
    return sameOrigin(a.href) && a.href !== location.href;
  }

  var inFlight = 0;

  function loadPage(url, push) {
    var requestId = ++inFlight;
    fetch(url)
      .then(function (res) {
        if (!res.ok) throw new Error("HTTP " + res.status);
        return res.text().then(function (html) { return { html: html, finalUrl: res.url || url }; });
      })
      .then(function (result) {
        if (requestId !== inFlight) return; // a newer navigation superseded this one
        var doc = new DOMParser().parseFromString(result.html, "text/html");
        window.DC._routerSwap.rootRelativize(doc, result.finalUrl);
        window.DC._routerSwap.setMeta(doc);
        if (push) history.pushState({ dcRouter: true }, "", url);

        if (document.startViewTransition && !window.DC.reduceMotion()) {
          var transition = document.startViewTransition(function () { window.DC._routerSwap.swapBody(doc); });
          // A visitor clicking faster than the cross-fade supersedes the running
          // transition and the browser rejects ready/finished with AbortError.
          // Expected, since the swap itself already happened, but left unhandled
          // it surfaces as an unhandled rejection on every fast double click.
          var ignoreAbort = function () {};
          transition.ready.catch(ignoreAbort);
          transition.finished.catch(ignoreAbort);
        } else {
          window.DC._routerSwap.swapBody(doc);
        }
      })
      .catch(function () {
        // Offline, a non-HTML response, a CORS edge case: fall back to a real
        // navigation rather than leaving the visitor on a stuck page.
        location.href = url;
      });
  }

  document.addEventListener("click", function (e) {
    var a = e.target.closest("a[href]");
    if (!isNavigableClick(e, a)) return;
    e.preventDefault();
    loadPage(a.href, true);
  });

  window.addEventListener("popstate", function () { loadPage(location.href, false); });
})();
