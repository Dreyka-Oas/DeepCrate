(function () {
  "use strict";

  window.SITE = window.SITE || {};

  // KEEP. Mounts site-nav, site-sidebar and site-footer; each one only calls
  // into site-chrome.js for its markup, so no mod identity lives here.
  // Rename a tag or an innerHTML call below and the load order breaks,
  // leaving an element blank at first paint.
  //
  // The site's native Web Components: <site-nav>, <site-sidebar>, <site-footer>.
  // They only mount: the HTML they render comes from site-chrome.js, and the
  // language and theme preferences from core/. <site-ledger> defines itself in
  // site-ledger.js, next to the markup it generates.
  //
  // Mandatory load order in <head>, without defer:
  //   core/* → fx/sound/* → site-boot → site-icons → site-chrome → site-ledger →
  //   components
  // Without defer, because the HTML parser must reach the <site-*> tags in <body>
  // with the elements already defined, or undefined content flashes.

  if (typeof customElements === "undefined") return;


  class SiteNav extends HTMLElement {
    connectedCallback() {
      this.innerHTML = window.SITE.navHtml(this.getAttribute("current"));
      if (window.SITE.initSoundToggle) window.SITE.initSoundToggle();
      if (window.SITE.initThemeToggle) window.SITE.initThemeToggle();
      if (window.SITE.initLangToggle) window.SITE.initLangToggle();
    }
  }

  class SiteSidebar extends HTMLElement {
    connectedCallback() {
      this.innerHTML = window.SITE.sidebarHtml(this.getAttribute("current"));
    }
  }

  class SiteFooter extends HTMLElement {
    connectedCallback() {
      this.innerHTML = window.SITE.footerHtml();
    }
  }

  if (!customElements.get("site-nav")) customElements.define("site-nav", SiteNav);
  if (!customElements.get("site-sidebar")) customElements.define("site-sidebar", SiteSidebar);
  if (!customElements.get("site-footer")) customElements.define("site-footer", SiteFooter);
})();
