# DeepCrate, design

## But

Un coffre qui retient 128 exemplaires d'un même objet par case, au lieu de 64, sans changer la
limite de pile du reste du jeu.

## Cadre technique

Fabric loader, Minecraft 1.21.11, Java 21, mêmes versions que LethalBreed. Le mod est requis sur le
serveur et sur chaque client : sans lui, un client ne connaît ni le bloc ni son écran. La quantité
d'une case, elle, passe telle quelle dans les paquets de coffre du jeu, qui écrivent le nombre en
entier variable sans le plafonner.

## Le bloc

Un bloc unique, la caisse renforcée, avec son entité de bloc. Il se pose, s'oriente et s'ouvre comme
un coffre de base, et affiche 27 cases. Il lâche son contenu quand on le casse, découpé en piles de
64 que le jeu sait écrire.

Le rendu reprend le modèle de coffre du jeu, couvercle animé compris, sur une texture couleur fer.
Deux caisses côte à côte ne fusionnent pas en grand coffre. Le contenu ne voyage pas dans le
composant d'inventaire de l'objet : celui-ci passe par le codec de pile vanilla, qui refuse une case
au-dessus de 99.

## La quantité par case

La case du coffre accepte jusqu'à 128 objets d'un même type. Le nombre s'affiche sur l'icône, en
deux caractères quand il tient, en forme abrégée au-delà de 99.

Le jeu de base ne transporte pas plus de 64 dans la main du joueur. Les règles de manipulation sont
donc :

- clic gauche sur une case pleine, le joueur prend 64, la case retombe à 64
- clic droit, le joueur prend la moitié, plafonnée à 64
- dépôt d'une pile, la case avale jusqu'à sa limite et rend le reste
- maj-clic depuis l'inventaire, remplit les cases déjà entamées avant les cases vides

Un entonnoir, un tuyau ou tout automate extérieur voit la caisse par l'interface d'inventaire
habituelle. Il remplit jusqu'à 64 par case et retire par piles de 64 : le test de fusion du jeu
compare à la taille de pile de l'objet, et le déplacer demanderait de modifier l'entonnoir lui-même,
que les mods d'optimisation modifient déjà.

## La recette

Un coffre au centre de l'établi, huit lingots de fer autour.

## Découpage du code

Quatre unités séparées, chacune testable seule :

- le bloc et son entité de bloc, qui gèrent la pose, la casse et la sauvegarde
- le stockage, une liste de 27 emplacements avec leur quantité, et les opérations insérer, extraire,
  fusionner, sans aucune dépendance à l'écran
- l'écran et son menu, qui traduisent les clics en opérations de stockage
- l'enregistrement du mod, qui déclare le bloc, l'objet, le type d'entité de bloc, le menu et la
  recette

Les erreurs se traitent au bord : une quantité nulle ou négative en retrait est refusée par le
stockage avec une exception, pas corrigée en silence.

Aucun calcul lourd n'entre ici. Vingt-sept cases se parcourent en quelques microsecondes, et le
chemin GPU des mods voisins coûterait plus cher que le travail lui-même.

## Tests

Le stockage se teste sans le jeu : insérer 200 objets dans une case vide laisse 72 en retour,
extraire d'une case à 128 rend 64 et laisse 64, deux types différents ne fusionnent pas, la
sauvegarde puis la relecture rendent le même état. L'écran et le bloc se vérifient en jeu par le
skill `headless-mod-test`.

## Hors du cadre

Pas de tri, pas de recherche, pas de verrouillage de case sur un type, pas de version double du
coffre, pas de capacité réglable. Ces sujets pourront revenir plus tard.
