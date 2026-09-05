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

## Ce qui reste

Six tâches du plan, dans l'ordre où il les pose.

La cinquième, l'identité : le mod n'a toujours pas d'icône, donc le lanceur dessine un carré vide dans
la liste. Il manque aussi le bloc `contact` dans `fabric.mod.json` et le `.gitattributes` que le mod
voisin porte.

La sixième, les deux pages de boutique. `store/` n'existe pas.

La septième, les réglages. Il n'y a aucun `config/oas/deepcrate.json`, et quatre nombres décidés dans
le code se discutent : la contenance de base, le comportement avec lithium, les rangées par page et le
seuil d'abréviation.

La huitième, le dernier mur de la fiche du 3 septembre : personne ne peut poser un bouton ni une ligne
d'infobulle sur l'écran du coffre. Le code complet dort dans le plan du 3 septembre à partir de la
ligne 1813.

La neuvième, la documentation. Le README nomme trois points d'extension, il y en a huit.

La dixième, le chemin chaud mesuré plus haut.

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
