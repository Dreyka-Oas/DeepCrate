(function () {
  "use strict";

  window.SITE = window.SITE || {};

  // SLOT brand-mark. Drawn inline at 32 by 32 so it inherits the text
  // colour. It ships as a neutral square and diamond, a placeholder rather
  // than decorative glyph: two stacked paths (frame / centre dot), slightly
  // offset from each other, for the same glitch effect as the rest of the
  // site. Fill rules (.brand-mark-back/-front) live in base.css, shared with
  // the watermark behind the wiki masthead (components/wiki/page.css).
  //
  // The site chrome (nav, sidebar, footer) rendered as HTML by the Web
  // Components in components.js. Kept separate from them because this is the
  // CONTENT (labels, page tree, brand marks): it changes when the wiki gains a
  // page, not when a component's behaviour changes.
  //
  // Must be loaded AFTER core/site-lang.js (SITE.s / SITE.langPrefix / SITE.stripLangPrefix),
  // AFTER the two tables in assets/js/i18n/, and AFTER chrome/site-icons.js
  // (the three nav buttons it drops into the bar).
  var s = window.SITE.s;
  var langPrefix = window.SITE.langPrefix;

  // Order and grouping only. The labels live in assets/js/i18n/, keyed by these
  // very paths, so a page that appears in both lists is worded once per language
  // instead of twice.
  var NAV_PATHS = [
    "/index.html",
    "/wiki/crates.html",
    "/wiki/capacity.html",
    "/wiki/screen.html",
    "/wiki/config.html",
    "/wiki/installation.html"
  ];

  var SIDEBAR_GROUPS = [
    ["groupMod", ["/wiki/crates.html", "/wiki/capacity.html", "/wiki/rows.html"]],
    ["groupScreen", ["/wiki/screen.html", "/wiki/hoppers.html"]],
    ["groupTuning", ["/wiki/config.html", "/wiki/installation.html", "/wiki/api.html"]]
  ];

  // A crate seen head on: the hollow box, the line its lid closes along, and
  // the module plate straddling that line. Two paths back, one front, so the
  // plate keeps the accent colour while the box follows the text. The same
  // three shapes are in assets/favicon.svg and in the hero, and the three have
  // to stay identical.
  var BRAND_MARK_SVG =
    '<svg class="site-nav__mark" viewBox="0 0 32 32" aria-hidden="true">' +
    '<path class="brand-mark-back" fill-rule="evenodd" d="M3 6h26v20H3V6Zm3 3v14h20V9H6Z"/>' +
    '<path class="brand-mark-back" d="M3 13h26v2H3z"/>' +
    '<path class="brand-mark-front" d="M13 11h6v5h-6z"/>' +
    "</svg>";

  // Root-relative-only comparisons: the pages themselves may still use
  // page-relative hrefs elsewhere (breadcrumbs, in-content links), but every
  // href these components render is root-relative so it never breaks across
  // directory depths (root vs wiki/) or after a router.js body swap.
  function normalize(path) {
    if (!path) return "";
    try {
      return new URL(path, location.origin + "/").pathname;
    } catch (e) {
      return path;
    }
  }

  // A page's "logical" path (without its /lang/<code> tree), used to identify
  // the current page independently of the language.
  function logical(path) {
    return window.SITE.stripLangPrefix(normalize(path)) || "/index.html";
  }

  window.SITE.navHtml = function navHtml(current) {
    var cur = logical(current);
    var pre = langPrefix(), labels = s("pages");
    var items = NAV_PATHS.map(function (path) {
      var isCurrent = path === cur;
      return "<li><a href=\"" + pre + path + "\"" + (isCurrent ? " aria-current=\"page\"" : "") + ">" + labels[path] + "</a></li>";
    }).join("");
    return (
      '<header class="site-nav">' +
      '<a href="' + pre + '/index.html" class="site-nav__brand">' + BRAND_MARK_SVG + "Deep<em>Crate</em></a>" +
      '<nav aria-label="' + s("navAria") + '"><ul class="site-nav__links">' + items + "</ul></nav>" +
      '<div class="site-nav__right">' + window.SITE.themeToggleHtml() + window.SITE.soundToggleHtml() + window.SITE.langToggleHtml() + "</div>" +
      "</header>"
    );
  };

  window.SITE.sidebarHtml = function sidebarHtml(current) {
    var cur = logical(current);
    var pre = langPrefix(), labels = s("pages");
    var groups = SIDEBAR_GROUPS.map(function (group) {
      var links = group[1].map(function (path) {
        var isCurrent = path === cur;
        return "<a href=\"" + pre + path + "\"" + (isCurrent ? " aria-current=\"page\"" : "") + ">" + labels[path] + "</a>";
      }).join("");
      return '<p class="wiki-sidebar__group">' + s(group[0]) + "</p>" + links;
    }).join("");
    return '<aside class="wiki-sidebar" aria-label="' + s("sidebarAria") + '">' + groups + "</aside>";
  };

  window.SITE.footerHtml = function footerHtml() {
    return (
      '<footer class="site-footer container">' +
      "<span>DeepCrate</span>" +
      "<span>© Dreyka Oas · " + s("footerRights") + "</span>" +
      // A link, not a decoration: it goes to the page saying who wrote this
      // and what the other mod is.
      '<a class="site-footer__sig" href="' + langPrefix() + '/oas.html">o.a.s</a>' +
      "</footer>"
    );
  };
})();
