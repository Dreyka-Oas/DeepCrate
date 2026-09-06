(function () {
  "use strict";

  // KEEP. Intercepts same-origin link clicks and turns them into a fetch-based
  // navigation that router-swap.js installs. Loosen isNavigableClick's guards
  // and ordinary clicks misfire: a ctrl or middle click meant to open a new
  // tab gets pulled into the in-page fetch, and so does a download link or a
  // link to another origin.

  // Progressive-enhancement client-side navigation: the site stays a plain
  // multi-page static export (every URL is a real, independently-loadable
  // .html file: no build step, no server, works with JS disabled). This
  // script only intercepts same-origin link clicks and swaps <body> via
  // fetch(), so <head> (CSS, fonts, this script itself) never re-downloads
  // or re-executes and the browser never does a full navigation for an
  // internal link. Falls back to a normal browser navigation for anything
  // it can't safely handle (external links, new tabs, failed fetch, no JS).

  if (!window.fetch || !window.history || !window.history.pushState) return;

  function sameOrigin(url) {
    try {
      return new URL(url, location.href).origin === location.origin;
    } catch (e) {
      return false;
    }
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
        return res.text().then(function (html) {
          return { html: html, finalUrl: res.url || url };
        });
      })
      .then(function (result) {
        if (requestId !== inFlight) return; // a newer navigation superseded this one
        var doc = new DOMParser().parseFromString(result.html, "text/html");
        window.SITE._routerSwap.rootRelativize(doc, result.finalUrl);
        window.SITE._routerSwap.setMeta(doc);
        if (push) history.pushState({ lbRouter: true }, "", url);

        var reduceMotion = window.SITE.reduceMotion();
        // The page whoosh accompanies the visual transition: it is not played
        // when motion is reduced (the swap is instant).
        if (window.SITE.sfx && !reduceMotion) window.SITE.sfx.whoosh();
        if (document.startViewTransition && !reduceMotion) {
          var transition = document.startViewTransition(function () { window.SITE._routerSwap.swapBody(doc); });
          // A visitor clicking faster than the 220ms cross-fade supersedes the
          // running transition, and the browser rejects ready/finished with
          // AbortError. That is the expected outcome, not a failure (the swap
          // itself has already happened), but left unhandled it surfaces as an
          // unhandled rejection in the console on every fast double click.
          var ignoreAbort = function () {};
          transition.ready.catch(ignoreAbort);
          transition.finished.catch(ignoreAbort);
        } else {
          window.SITE._routerSwap.swapBody(doc);
        }
      })
      .catch(function () {
        // Fetch/parse failed (offline, non-HTML response, CORS edge case…):
        // fall back to a real navigation rather than leaving the page stuck.
        location.href = url;
      });
  }

  document.addEventListener("click", function (e) {
    var a = e.target.closest("a[href]");
    if (!isNavigableClick(e, a)) return;
    e.preventDefault();
    if (window.SITE.sfx) window.SITE.sfx.click();
    loadPage(a.href, true);
  });

  window.addEventListener("popstate", function () {
    loadPage(location.href, false);
  });
})();
