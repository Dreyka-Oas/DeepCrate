(function () {
  "use strict";

  window.DC = window.DC || {};

  // Nav, wiki sidebar and footer as native custom elements: the three blocks
  // repeated on every page, written once. They render on connect, read their
  // wording through DC.s and prefix every link with the tree the page sits in,
  // so nothing here has to be duplicated per language.

  // The crate glyph: a hollow box, the line of its lid, and the module plate
  // straddling it. Paths filled by base.css rather than an <img>, so it
  // repaints with the theme. Kept in one string because the hero pages and the
  // favicon carry the same shapes, and the three have to stay identical.
  var MARK = '<svg viewBox="0 0 24 24" aria-hidden="true">' +
    '<path class="brand-mark-back" fill-rule="evenodd" d="M2 4h20v16H2V4Zm2.6 2.6v10.8h14.8V6.6H4.6Z"/>' +
    '<path class="brand-mark-back" d="M2 9.4h20v2H2z"/>' +
    '<path class="brand-mark-front" d="M10 8.6h4v3.6h-4z"/></svg>';

  var NAV = [
    ["/index.html", "navHome"],
    ["/wiki/installation.html", "navInstall"],
    ["/wiki/crates.html", "navWiki"]
  ];

  var SIDEBAR = [
    ["sideGroupMod", [
      ["/wiki/installation.html", "pageInstall"],
      ["/wiki/crates.html", "pageCrates"],
      ["/wiki/capacity.html", "pageCapacity"],
      ["/wiki/rows.html", "pageRows"]
    ]],
    ["sideGroupScreen", [
      ["/wiki/screen.html", "pageScreen"],
      ["/wiki/hoppers.html", "pageHoppers"]
    ]],
    ["sideGroupTuning", [
      ["/wiki/config.html", "pageConfig"],
      ["/wiki/addons.html", "pageAddons"]
    ]]
  ];

  var s = function (key) { return window.DC.s(key); };
  var href = function (path) { return window.DC.langPrefix() + path; };

  // Compared against the path below the tree, so the aria-current mark does not
  // depend on which language is open.
  function isCurrent(path) {
    var here = window.DC.stripLangPrefix(location.pathname || "");
    return here === path || (path === "/index.html" && (here === "" || here === "/"));
  }

  function link(path, key) {
    var a = document.createElement("a");
    a.href = href(path);
    a.textContent = s(key);
    if (isCurrent(path)) a.setAttribute("aria-current", "page");
    return a;
  }

  function toggles() {
    var box = document.createElement("div");
    box.className = "site-nav__toggles";

    var lang = document.createElement("a");
    lang.className = "site-nav__toggle";
    lang.setAttribute("data-lang-toggle", "");
    lang.href = window.DC.counterpartPath(window.DC.lang() === "en" ? "fr" : "en");
    lang.setAttribute("hreflang", window.DC.lang() === "en" ? "fr" : "en");
    lang.textContent = s("langOther");
    box.appendChild(lang);

    var theme = document.createElement("button");
    theme.type = "button";
    theme.className = "site-nav__toggle";
    theme.setAttribute("data-theme-toggle", "");
    theme.textContent = s("themeAuto");
    box.appendChild(theme);

    // Wording and state are set by fx/sound/toggle.js, which owns the mute and
    // reads it from storage. Rendering the label here too would show "son" for
    // a beat on a visitor who muted three sessions ago.
    var sound = document.createElement("button");
    sound.type = "button";
    sound.className = "site-nav__toggle";
    sound.setAttribute("data-sound-toggle", "");
    box.appendChild(sound);

    return box;
  }

  customElements.define("dc-nav", class extends HTMLElement {
    connectedCallback() {
      var nav = document.createElement("nav");
      nav.className = "site-nav";
      nav.setAttribute("aria-label", s("navLabel"));

      var brand = document.createElement("a");
      brand.className = "site-nav__brand";
      brand.href = href("/index.html");
      brand.innerHTML = MARK + "<span>Deep<em>Crate</em></span>";
      nav.appendChild(brand);

      var links = document.createElement("div");
      links.className = "site-nav__links";
      NAV.forEach(function (entry) { links.appendChild(link(entry[0], entry[1])); });
      nav.appendChild(links);

      nav.appendChild(toggles());
      this.appendChild(nav);
    }
  });

  customElements.define("dc-sidebar", class extends HTMLElement {
    connectedCallback() {
      var aside = document.createElement("aside");
      aside.className = "wiki-sidebar";
      aside.setAttribute("aria-label", s("sideLabel"));

      SIDEBAR.forEach(function (group) {
        var title = document.createElement("p");
        title.className = "wiki-sidebar__group";
        title.textContent = s(group[0]);
        aside.appendChild(title);

        var list = document.createElement("ul");
        list.className = "wiki-sidebar__list";
        group[1].forEach(function (entry) {
          var li = document.createElement("li");
          li.appendChild(link(entry[0], entry[1]));
          list.appendChild(li);
        });
        aside.appendChild(list);
      });

      this.appendChild(aside);
    }
  });

  customElements.define("dc-footer", class extends HTMLElement {
    connectedCallback() {
      var footer = document.createElement("footer");
      footer.className = "site-footer";

      var box = document.createElement("div");
      box.className = "container";

      var name = document.createElement("span");
      name.textContent = "DeepCrate";
      box.appendChild(name);

      var licence = document.createElement("span");
      licence.textContent = s("footerLicence");
      box.appendChild(licence);

      // The signature is a real link, not a decoration: it goes to the page
      // saying who wrote this and what the other mod is.
      var sig = document.createElement("a");
      sig.className = "site-footer__sig";
      sig.href = href("/oas.html");
      sig.textContent = "o.a.s";
      sig.setAttribute("aria-label", s("footerSigAria"));
      if (isCurrent("/oas.html")) sig.setAttribute("aria-current", "page");
      box.appendChild(sig);

      footer.appendChild(box);
      this.appendChild(footer);
    }
  });
})();
