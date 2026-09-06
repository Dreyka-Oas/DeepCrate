(function () {
  "use strict";

  window.SITE = window.SITE || {};
  window.SITE.strings = window.SITE.strings || {};

  // SLOT strings. This table and assets/js/i18n/en.js are really one slot,
  // not two: tools/check-i18n.mjs fails the moment their keys stop matching
  // each other. Add a new string to both files in the same edit, never to
  // just one.
  //
  // Keys are English (the naming rule for every identifier in this tree) and
  // the values are the only thing translated. `pages` is keyed by the page's own
  // root-relative path, so a page listed in both the nav bar and the wiki
  // sidebar is worded once instead of twice per language.
  //
  // These values land in innerHTML, so an ampersand is written &amp; here,
  // exactly as it was in the markup the strings came from.
  window.SITE.strings.fr = {
    pages: {
      "/index.html": "Accueil",
      "/wiki/crates.html": "Les six coffres",
      "/wiki/capacity.html": "Capacité",
      "/wiki/rows.html": "Rangées",
      "/wiki/screen.html": "Tri, recherche et pages",
      "/wiki/hoppers.html": "Trémies et tuyaux",
      "/wiki/config.html": "Fichier de réglages",
      "/wiki/installation.html": "Installation",
      "/wiki/api.html": "Pour les autres mods"
    },

    groupMod: "Le mod",
    groupScreen: "L'écran",
    groupTuning: "Réglages",

    navAria: "Navigation principale",
    sidebarAria: "Navigation du guide",
    footerRights: "licence propre au mod, tous droits réservés.",

    soundToggleAria: "Activer ou couper le son",
    soundMuteAria: "Couper le son",
    soundEnableAria: "Activer le son",

    themeToggleAria: "Thème : auto, changer",
    themePrefix: "Thème : ",
    themeSwitchTo: " · passer en ",
    themeAuto: "auto (suit le système)",
    themeLight: "jour",
    themeDark: "nuit",

    langToggleAria: "Passer en anglais",

    ledgerLabel: "Inventaire",

    chartAxis: "modules de rangée",
    tier: "Palier",
    tierNames: ["Cuivre", "Fer", "Améthyste", "Prismarine", "Brise", "Écho"],
    tierDescs: [
      "27 cases. Un lingot de cuivre autour d'un coffre du jeu, et la première rangée de neuf arrive.",
      "36 cases. Un lingot de fer autour du cuivre. Le premier qui tient une récolte entière.",
      "45 cases. Un éclat d'améthyste autour du fer, et le premier module de capacité a de la place.",
      "54 cases. Des cristaux de prismarine, ramenés d'un monument sous l'eau.",
      "63 cases. Un bâton de brise, qui ne se trouve que dans un donjon d'essai.",
      "72 cases. Un fragment d'écho, au fond d'une cité antique. Le dernier palier, et celui que les autres mods étendent."
    ]
  };
})();
