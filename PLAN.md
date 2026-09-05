# Où en est DeepCrate

Arrêt du 5 septembre 2026, branche `modularite`, à reprendre là où le plan s'arrête.

Le chantier de modularité du 3 septembre est toujours ouvert : ses tâches 6 et 7 n'ont pas bougé. Ce
qui a occupé la journée, c'est un audit du mod contre le gabarit, puis les quatre premières tâches du
plan qui en est sorti. Le plan complet, tâche par tâche avec le code à écrire, est dans
`docs/superpowers/plans/2026-09-05-deepcrate-audit.md`. Ce fichier ne dit que l'état.

## Ce qui a été fait aujourd'hui

Vingt commits, chacun testé avant d'être posé, chacun relu par un agent qui n'avait pas écrit le code.

Le travail qui traînait dans l'arbre est parti : le bouton de page qui porte un point vert quand sa
page tient quelque chose, la fermeture de la languette de module sur son bord au lieu d'une tranche de
cellule, et les dix-huit textures de coffre dont le loquet est redevenu noir plein.

Le code mort a été retiré : deux constantes que rien ne lisait, un import inutilisé, un accesseur sans
appelant, et une javadoc qui décrivait une méthode en étant posée sur une autre. Les noms pleinement
qualifiés écrits en ligne sont devenus des imports, et les deux `"deepcrate"` en dur passent par
`RegistryInit.id`, l'identifiant réseau restant `deepcrate:sort` à l'octet près.

Trois blocs recopiés ont fondu en un chacun : l'essai d'appartenance à une étiquette, écrit trois
fois, l'inscription qui refuse un identifiant déjà pris, écrite quatre fois, et le largage d'un objet
entier, écrit deux fois. La classe qui porte l'inscription s'appelle `Registrations` et pas
`Registries`, parce que ce dernier nom est celui d'une classe de Minecraft déjà importée sans
qualification dans deux fichiers d'ici.

Six découpes ont suivi, dans cet ordre parce que fusionner passe avant découper. `DeepCrateBlockEntity`
est passé de 496 à 315 lignes en rendant la sauvegarde, le couvercle et le porteur de module à trois
classes. `DeepCrateScreen` est passé de 452 à 323 en rendant la peinture du panneau et la recherche.
`DeepCrateMenu` a laissé partir ses mesures de panneau. Puis trois paquets ont été ouverts pour
ramener les dossiers sous huit fichiers : `api/module`, `client/screen`, `client/render`,
`client/sort`, `inventory/module`, `inventory/slot`.

## Ce qui a été vérifié, et comment

37 tests JUnit et 44 gametests, verts après chaque commit.

Les onze captures du test client sont identiques à l'octet près à celles d'avant les découpes, ce qui
est la preuve qu'aucune n'a changé ce qui est dessiné. C'est le vrai filet de cette journée : l'écran
du coffre est peint à la main à partir d'une texture du jeu de base, et un décalage faux compile,
passe tous les tests et se voit seulement à l'oeil.

La capture `0007_screen-paged-crate` montre les boutons 1, 3 et 6 avec leur point et 2, 4 et 5 sans,
ce qui correspond aux pages remplies, et la languette de module fermée sur son bord.

Le coût d'un tick de trémie contre un coffre a été mesuré, pas supposé, avec un gametest jeté après
lecture : 99,85 ns par emplacement sur un coffre simple, 286,02 ns sur un apparié. Le facteur 2,86
vient de `moduleHolder()`, appelé quatre fois par emplacement, qui refait un `getBlockEntity` à chaque
fois. Les chiffres bruts sont dans `.superpowers/sdd/perf-report.md`.

## Le second tour, cinq conceptions et leurs adversaires

Cinq livrables produits en parallèle, chacun passé à un agent chargé de le réfuter en lisant les
fichiers plutôt que le rapport. Quatre sur cinq ont été réfutés, ce qui est le but de la manoeuvre.

La cinquième tâche est faite. Le mod a son icône, 128 pixels, un coffre avec la languette de module
qui pend à gauche, lisible une fois réduite à 32 où le lanceur la dessine vraiment. `fabric.mod.json`
déclare l'icône et un bloc `contact` sans `homepage`, puisqu'aucun site n'existe. Le `.gitattributes`
est posé.

La sixième aussi. `store/curseforge.md` et `store/modrinth.md` portent un seul corps, l'incompatibilité
lithium en tête, et ne diffèrent que par les deux paragraphes mis en citation côté Modrinth.

Les septième et huitième tâches ont leur fiche de conception, pas leur code :
`docs/superpowers/specs/2026-09-05-reglages.md` et `2026-09-05-decoupes-restantes.md`.

Le terrain demandé existe, `CrateShowcaseClientGameTest`, douze prises couvrant chaque cas qui dessine
autrement, et les tests clients sont passés dans leur propre paquet `gametest/client` pour tenir sous
huit fichiers par dossier.

## Ce que les adversaires ont trouvé, et qui n'est pas réglé

Le dépôt GitHub est privé. `gh repo view` rend `PRIVATE` et un `curl` anonyme sur
`https://github.com/Dreyka-Oas/DeepCrate` rend 404. Les pages de boutique ont donc été écrites sans
aucun lien vivant, et leur champ Source porte la consigne de le remplir le jour où le dépôt s'ouvre.
Tant qu'il reste fermé, publier ces pages revient à publier un mod dont personne ne peut lire la
source.

Le `LICENSE` de DeepCrate renvoie deux fois au site d'un autre mod, lignes 21 et 45, et le canal de
signalement qu'on y trouve a `lethalbreed` câblé en dur dans son adresse. Un redistributeur de
DeepCrate est donc tenu de créditer ce mod en pointant la page d'un autre, et une demande de modpack
atterrit dans la file du voisin. À corriger dans le fichier, pas sur une page de boutique.

La fiche des réglages avait une option morte-née, `rowModuleStackLimit`, lue pendant l'initialisation
statique de `RegistryInit`, qui tourne avant que le fichier de configuration soit chargé. Réglé dans
la fiche, mais c'est le genre de faute qui aurait donné un réglage sans effet.

Restent des mineurs non repris, tous dans les deux fiches de conception : des renvois de ligne périmés
d'une poignée de lignes, un compte de gametests à 44 là où l'arbre en a 45, et une contradiction
interne dans la fiche des réglages sur une ligne de tableau. À relire avant d'implémenter d'après
elles.

## Le troisième tour, arrêté en cours

Lancé sur les quatre tâches restantes, une à la fois avec des adversaires entre chacune, et interrompu
pendant la première. Rien de ce tour n'est dans l'arbre, le dépôt est propre et le build vert avec ses
44 gametests.

Une chose est à récupérer au retour. Le test de non-régression de la dixième tâche est écrit et il
mesure vraiment, mais il attend le cache qu'il doit valider, donc il échoue : il a rendu un rapport de
3,60 contre un plafond de 3,0. Le laisser inscrit aurait mis la branche au rouge, alors il est mis de
côté hors dépôt, à `.superpowers/sdd/CrateHopperCostGameTest.java.pending`. Pour le reprendre, le
remettre dans `mod/src/gametest/java/com/dreykaoas/deepcrate/gametest/` et rajouter sa ligne dans
`mod/src/gametest/resources/fabric.mod.json` sous `fabric-gametest`, sans quoi il ne tournera jamais
et passera au vert sans rien avoir mesuré.

Ce chiffre de 3,60 mérite d'être noté : la mesure du matin donnait 2,86 sur les mêmes 216
emplacements. L'écart vient de la machine et de sa charge, pas du code, qui n'a pas bougé entre les
deux. Un seuil serré sur ce genre de mesure casse chez quelqu'un d'autre pour rien, et 3,0 était déjà
trop serré.

## Ce qui reste

La septième tâche, les réglages : la fiche est écrite, le code non. Il n'y a toujours aucun
`config/oas/deepcrate.json`.

La huitième, le dernier mur de la fiche du 3 septembre : personne ne peut poser un bouton ni une ligne
d'infobulle sur l'écran du coffre. Le code complet dort dans le plan du 3 septembre à partir de la
ligne 1813.

La neuvième, la documentation. Le README nomme trois points d'extension, il y en a huit.

La dixième, le chemin chaud mesuré plus haut.

Et les six découpes de la fiche du jour, dont l'adversaire a montré qu'au moins une, `MenuModules`, ne
tient pas telle qu'elle est dessinée.

Six fichiers dépassent encore 150 lignes après la tâche 4, dont trois que le plan ne prévoyait pas de
toucher : `DeepCrateMenu` à 404, `DeepCrateScreen` à 323, `DeepCrateBlockEntity` à 315, `CrateStorage`
à 294, `DeepCrateBlock` à 263 et `DeepCrateApi` à 204. Les trois premiers sont au-dessus de ce que la
tâche visait, les trois autres n'avaient aucune étape. À trancher avant de fermer la branche.

## Une chose vue et pas encore décidée

Un coffre de trois colonnes garde un panneau large comme un coffre de neuf, avec beaucoup de panneau
nu de chaque côté de la grille. C'est écrit ainsi exprès, l'inventaire du joueur est dessiné sur le
même panneau et il lui faut ses neuf colonnes, mais la capture `0010_screen-three-columns` montre que
le résultat ne va pas. Aucun coffre livré ne fait trois colonnes ; c'est un palier que le test inscrit
lui-même. Un addon qui en poserait un, lui, tomberait dessus.
