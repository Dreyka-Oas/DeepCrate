# Paliers, modules et pages

Plan d'exécution de la deuxième version. Le descriptif de la première est dans `../specs/`.

## Ce qui change pour le joueur

Six coffres au lieu d'un, dans l'ordre du danger qu'il faut affronter pour trouver le matériau :
cuivre 27 cases, fer 36, améthyste 45, prismarine 54, souffle 63, écho 72. Chaque recette est le
coffre précédent au centre, huit fois le nouveau matériau autour. Deux coffres du même palier posés
côte à côte fusionnent, comme un coffre du jeu.

Une case tient 64 objets. Un module posé dans la case en haut à gauche de l'écran monte tout le
coffre à 128, 256, 512 ou 1024. Un seul module à la fois, poser le suivant rend le précédent. Quand
on retire un module et qu'on ferme l'écran, ce qui dépasse la nouvelle contenance tombe au sol.

Au-delà de six rangées, l'écran se coupe en pages. Les numéros s'empilent en haut à droite, il n'y a
pas de barre de défilement. Les pages se répartissent à parts égales : un coffre de huit rangées
donne deux pages de quatre, pas une pleine et une presque vide.

Casser un coffre lâche tout au sol, contenu et module.

Les entonnoirs remplissent jusqu'à la contenance du coffre, pas seulement 64. Cela demande de
modifier le comportement de l'entonnoir du jeu, que les mods d'optimisation modifient aussi.

## Ce que les autres mods peuvent faire

Trois choses, parce que la demande en nomme trois : ajouter un module, ajouter un palier de coffre,
changer le nombre de cases et de pages.

Les deux premières passent par un registre statique alimenté depuis un point d'entrée `deepcrate`
déclaré dans le `fabric.mod.json` de l'autre mod. Un module est décrit par un `HolderSet<Item>`,
donc une étiquette suffit et un mod qui veut promouvoir un objet existant n'enregistre rien.

La troisième passe par un événement, `CrateLayoutCallback`, appelé à l'ouverture. Sans lui, le
nombre de rangées par page serait figé dans une constante et le troisième tiers de la demande
manquerait.

## Découpage du code

Un paquet `api` avec ce qui est public et stable : `CrateTier`, `CrateModule`, `CrateLayout`,
`CrateLayoutCallback`, `DeepCrateApi`, `DeepCrateAddon`. Le reste du mod en dépend, jamais l'inverse.

`CrateStorage` prend une taille variable et garde sa nature : aucune dépendance au jeu, testable
seul. `StoredSlot` passe son champ de case en entier ; les octets déjà écrits se relisent, `NbtOps`
accepte tout nombre.

Le bloc devient une classe unique paramétrée par son palier, résolu depuis le registre plutôt que
porté en champ, pour garder `simpleCodec`. Il gagne `CHEST_TYPE` à côté de `HORIZONTAL_FACING`, et
recopie la logique d'appariement du coffre du jeu, dont les quatre fonctions utiles sont publiques
et statiques.

La paire est un `CompoundContainer` avec ses deux `getMaxStackSize` redéfinis, huit lignes. Le
module vit sur une seule des deux moitiés, celle que le jeu appelle la première, donc il ne peut ni
se dupliquer ni se perdre.

Le menu construit ses cases une fois, à l'ouverture, d'après la taille reçue. Changer de page ne
touche pas au serveur : une case hors page devient inactive et sort de l'écran. Le serveur ne lit
jamais l'état de page, donc aucun chemin ne permet de dupliquer en changeant de page.

L'écran dessine son fond à la taille voulue, pose la case de module en haut à gauche et les numéros
de page en haut à droite.

## Ordre de travail

L'API et le stockage d'abord, puisque tout en dépend. Le bloc et la paire ensuite, vérifiés en jeu
avant d'aller plus loin. Le menu et l'écran après, avec les tests de duplication. Les textures, les
recettes et les traductions en parallèle. Le mixin d'entonnoir en dernier, parce que c'est la seule
pièce qui peut se fâcher avec un autre mod.

## Ce qui est tranché sans demander

L'ancien identifiant `deepcrate:deep_crate` disparaît au profit d'un identifiant par palier. Le
dépôt est privé et daté du jour, personne n'a de monde à perdre, et garder un identifiant qui ne
nomme plus rien coûterait plus cher que le renommage.

Une case au-dessus de sa contenance après le retrait d'un module suit la règle du joueur : elle
déverse à la fermeture. C'est aussi ce qui arrivera aux caisses de l'ancien monde de test, qui
tiennent 128 sans module.

Casser un double coffre d'écho plein lâche deux mille piles d'un coup. C'est le prix de la règle
demandée, et il est écrit ici plutôt que corrigé en douce.
