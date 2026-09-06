# Défauts connus de DeepCrate

Relevé du 6 septembre 2026, branche `modularite`. Chaque ligne a été vérifiée par une commande le
jour où elle a été écrite. Ce qui est un choix assumé est marqué comme tel, le reste est à réparer.

## Ce qui est cassé et attend quelqu'un

Le `LICENSE` renvoie deux fois au site d'un autre mod, lignes 21 et 45, et le canal de signalement
qu'on y trouve a `lethalbreed` câblé en dur dans son adresse. Un redistributeur de DeepCrate est donc
tenu de créditer ce mod en pointant la page d'un autre, et une demande de modpack atterrit dans la
file du voisin. La réparation est dans le fichier lui-même, pas sur une page de boutique.

Le dépôt GitHub est privé. `gh repo view Dreyka-Oas/DeepCrate` rend `PRIVATE` et un `curl` anonyme
sur son adresse rend 404. Les deux pages de boutique ont donc été écrites sans aucun lien vivant, et
leur champ Source porte la consigne de le remplir le jour où le dépôt s'ouvre. Tant qu'il reste
fermé, publier ces pages revient à publier un mod dont personne ne peut lire la source. Ouvrir le
dépôt est une décision qui appartient à son propriétaire, pas au code.

`BlockEntityRendererRegistry` est déprécié dans l'API Fabric et reste appelé dans
`DeepCrateClient.onInitializeClient`. Le compilateur le dit à chaque passe, avec
`-Xlint:deprecation` pour le détail. Ça marche encore, mais la classe partira d'une version à
l'autre et le mod ne dessinera plus ses coffres.

`fabric.mod.json` range `lithium` dans `suggests`, aux côtés de huit autres mods d'optimisation.
C'est le seul de la liste qui coûte quelque chose : tant qu'il est là, un coffre répond 64 par case
à l'automatisation, ce que le README, les deux pages de boutique et le réglage
`limitAutomationWithLithium` expliquent chacun de leur côté. Le fichier de métadonnées, lui,
le recommande sans un mot.

## Six fichiers au-dessus du plafond de 150 lignes

```
404  inventory/DeepCrateMenu.java
324  client/screen/DeepCrateScreen.java
316  block/DeepCrateBlockEntity.java
294  inventory/CrateStorage.java
263  block/DeepCrateBlock.java
204  api/DeepCrateApi.java
```

Les trois premiers ont déjà été découpés une fois et sont retombés au-dessus. Les trois autres
n'ont jamais eu d'étape. La fiche `docs/superpowers/specs/2026-09-05-decoupes-restantes.md` dessine
six découpes, et un adversaire a montré qu'au moins une, `MenuModules`, ne tient pas telle qu'elle
est écrite. À relire avant de s'en servir.

## Ce qui est écrit exprès et se voit quand même

Un coffre de trois colonnes garde un panneau large comme un coffre de neuf, avec beaucoup de panneau
nu de chaque côté de la grille. C'est voulu : l'inventaire du joueur est dessiné sur le même panneau
et il lui faut ses neuf colonnes. La capture `0010_screen-three-columns` montre que le résultat ne va
pas pour autant. Aucun coffre livré ne fait trois colonnes, c'est un palier que le test client
invente lui-même, mais un addon qui en poserait un tomberait dessus.

Un coffre apparié cache un objet en 3x3 sur sa languette de module. Ce n'est pas une grille de
fabrication : c'est la case de module, dessinée à la taille d'une case ordinaire, et la languette
grandit avec le nombre de cases que les addons ont inscrites.

## Ce que la mesure de performance ne dit pas

`CrateHopperCostGameTest` mesure ce qu'une trémie paie pour interroger un coffre, et son plafond est
à 5,0. Ce n'est pas la vraie valeur, qui tourne autour de 2,2 depuis que la moitié d'une paire retient
quel côté porte son module. Le plafond est large parce que la même marche sur le même code a donné
2,86 un matin, puis 2,67 et 3,86 à deux minutes d'intervalle le lendemain : ce qui bougeait, c'était
la charge de la machine. Un seuil serré sur ce genre de mesure casse chez quelqu'un d'autre sans
qu'aucun code n'ait changé.
