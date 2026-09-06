(function () {
  "use strict";

  // Wording of everything rendered from JS: the nav, the wiki sidebar, the
  // footer and the two toggles. The pages themselves carry their own prose,
  // hand-written in each tree. tools/check-i18n.mjs fails the run when a key
  // here has no counterpart in en.js.

  window.DC = window.DC || {};
  window.DC.strings = window.DC.strings || {};

  window.DC.strings.fr = {
    navLabel: "Navigation du site",
    navHome: "Accueil",
    navInstall: "Installation",
    navWiki: "Guide",

    langOther: "EN",
    themeAuto: "auto",
    themeLight: "clair",
    themeDark: "sombre",
    themePrefix: "Thème actuel : ",
    themeSwitchTo: ", cliquer pour passer en ",

    sideLabel: "Pages du guide",
    sideGroupMod: "Le mod",
    sideGroupScreen: "L'écran",
    sideGroupTuning: "Réglages",

    pageInstall: "Installation",
    pageCrates: "Les six coffres",
    pageCapacity: "Capacité",
    pageRows: "Rangées",
    pageScreen: "Tri, recherche et pages",
    pageHoppers: "Trémies et tuyaux",
    pageConfig: "Fichier de réglages",
    pageAddons: "Pour les autres mods",

    footerLicence: "Dreyka Oas, licence propre au mod, tous droits réservés.",
    footerEcho: "il reste de la place au fond."
  };
})();
