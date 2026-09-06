(function () {
  "use strict";

  window.SITE = window.SITE || {};
  window.SITE.strings = window.SITE.strings || {};

  // SLOT strings. This table is the mirror of assets/js/i18n/fr.js: together
  // they are one slot, not two, and tools/check-i18n.mjs fails the moment
  // they stop matching key for key. Add a new string to this file and to
  // fr.js in the same edit, never to just one.
  window.SITE.strings.en = {
    pages: {
      "/index.html": "Home",
      "/wiki/crates.html": "The six crates",
      "/wiki/capacity.html": "Capacity",
      "/wiki/rows.html": "Rows",
      "/wiki/screen.html": "Sorting, search and pages",
      "/wiki/hoppers.html": "Hoppers and pipes",
      "/wiki/config.html": "Settings file",
      "/wiki/installation.html": "Install",
      "/wiki/api.html": "For other mods"
    },

    groupMod: "The mod",
    groupScreen: "The screen",
    groupTuning: "Settings",

    navAria: "Main navigation",
    sidebarAria: "Guide navigation",
    footerRights: "the mod's own licence, all rights reserved.",

    soundToggleAria: "Toggle sound",
    soundMuteAria: "Mute sound",
    soundEnableAria: "Enable sound",

    themeToggleAria: "Theme: auto, change",
    themePrefix: "Theme: ",
    themeSwitchTo: " · switch to ",
    themeAuto: "auto (follows system)",
    themeLight: "day",
    themeDark: "night",

    langToggleAria: "Switch to French",

    ledgerLabel: "Inventory",

    chartAxis: "row modules",
    tier: "Tier",
    tierNames: ["Copper", "Iron", "Amethyst", "Prismarine", "Breeze", "Echo"],
    tierDescs: [
      "27 slots. A copper ingot around a vanilla chest, and the first row of nine arrives.",
      "36 slots. An iron ingot around the copper one. The first that holds a whole harvest.",
      "45 slots. An amethyst shard around the iron one, and the first capacity module has somewhere to sit.",
      "54 slots. Prismarine crystals, carried back from a monument under the sea.",
      "63 slots. A breeze rod, which only drops in a trial chamber.",
      "72 slots. An echo shard, from the floor of an ancient city. The last tier, and the one other mods extend."
    ]
  };
})();
