# Défauts connus de DeepCrate

Relevé du 6 septembre 2026, branche `modularite`. Chaque ligne a été vérifiée par une commande le
jour où elle a été écrite. Ce qui est un choix assumé est marqué comme tel, le reste est à réparer.

## Ce qui est cassé et attend quelqu'un

Le `LICENSE` renvoie deux fois au site d'un autre mod, lignes 21 et 45, et le canal de signalement
qu'on y trouve a `lethalbreed` câblé en dur dans son adresse. Un redistributeur de DeepCrate est
donc tenu de créditer ce mod en pointant la page d'un autre, et une demande de modpack atterrit dans
la file du voisin. La réparation est dans le fichier lui-même, pas sur une page de boutique.

`BlockEntityRendererRegistry` est déprécié dans l'API Fabric et reste appelé dans
`DeepCrateClient.onInitializeClient`. Le compilateur le dit à chaque passe, avec
`-Xlint:deprecation` pour le détail. Ça marche encore, mais la classe partira d'une version à
l'autre et le mod ne dessinera plus ses coffres.

`fabric.mod.json` range `lithium` dans `suggests`, aux côtés de huit autres mods d'optimisation.
C'est le seul de la liste qui coûte quelque chose : tant qu'il est là, un coffre répond 64 par case
à l'automatisation, ce que le README, les deux pages de boutique et le réglage
`limitAutomationWithLithium` expliquent chacun de leur côté. Le fichier de métadonnées, lui, le
recommande sans un mot.

## Six fichiers au-dessus du plafond de 150 lignes

```
404  inventory/DeepCrateMenu.java
324  client/screen/DeepCrateScreen.java
316  block/DeepCrateBlockEntity.java
294  inventory/CrateStorage.java
263  block/DeepCrateBlock.java
204  api/DeepCrateApi.java
```

Les trois premiers ont déjà été découpés une fois et sont retombés au-dessus. Les trois autres n'ont
jamais eu d'étape. La fiche `docs/superpowers/specs/2026-09-05-decoupes-restantes.md` dessine six
découpes, et un adversaire a montré qu'au moins une, `MenuModules`, ne tient pas telle qu'elle est
écrite. À relire avant de s'en servir.

Le commentaire de `CrateDrops` annonce deux mille piles pour un coffre écho double qui perd son
module. Le compte est de mille huit, la moitié : 144 cases, 448 objets en trop dans chacune, coupés
en piles de 64. Le nombre a l'air d'avoir été compté une fois par moitié de la paire alors que les
144 cases sont déjà les deux. Le comportement décrit est le bon, c'est le chiffre qui est faux.

## Deux dossiers d'assets au-dessus de huit fichiers

`assets/deepcrate/items` en tient dix et `assets/deepcrate/textures/entity/chest` dix-huit. Ni l'un
ni l'autre ne se découpe : le jeu va chercher `items/<identifiant>.json` à cet endroit précis, et les
dix-huit textures sont les six paliers en trois états, seul, moitié gauche, moitié droite. Le plafond
de huit vaut pour du code qu'on organise, pas pour un dossier dont le jeu impose le contenu.

## Ce qui est écrit exprès et se voit quand même

Un coffre écho double plein qui perd son module 512 vide 1 008 piles sur le sol en un tick. Les
comptes : 144 cases pour la paire, chacune retombe de 512 à 64, donc 448 objets en trop, 64 512 en
tout, coupés en piles de 64 parce que c'est tout ce qu'une entité d'objet peut porter. Avec les
seize modules de rangée dans la case du dessous, la paire fait 432 cases et le même geste en vide 3
024. `CrateDrops` les pose entières au lieu de passer par `Containers.dropItemStack`, qui recoupe
chaque pile en morceaux de dix à trente et triplerait le nombre d'entités, mais le tick reste lourd
et personne ne ramasse tout ça avant la disparition à cinq minutes. Le choix est de rendre plutôt
que d'avaler : cette capacité en trop n'a nulle part où être sauvegardée, et l'avaler en silence
détruirait des objets sans le dire. Vider un coffre avant d'en retirer le module reste le seul ordre
sûr.

Le remplissage par trémie et par tuyau est éteint tant que lithium est installé. Lithium remplace la
trémie en entier et garde sa propre copie de l'inventaire visé ; contre un conteneur qui répond plus
de 64, il sort les objets de la trémie et ne les écrit jamais. Mesuré, pas supposé : huit blocs de
terre détruits par passage, avec ou sans le correctif. Un coffre annonce donc 64 par case à
l'automatisation tant que lithium est là, ce qui coûte la fonction et garde les objets. La main du
joueur n'est pas concernée, elle passe par l'écran. Le propriétaire du serveur peut reprendre la
décision avec `limitAutomationWithLithium` à false, et perdre ces objets en connaissance de cause.

Une trémie contre un coffre double n'atteint que la moitié qu'elle touche, comme avec un tonneau.
Réunir les deux moitiés est pourtant ce que le jeu fait pour un coffre, mais lithium recaste ce
résultat en block entity et le serveur tombe au premier tick.

Un coffre de trois colonnes garde un panneau large comme un coffre de neuf, avec beaucoup de panneau
nu de chaque côté de la grille. C'est voulu : l'inventaire du joueur est dessiné sur le même panneau
et il lui faut ses neuf colonnes. La capture `0010_screen-three-columns` montre que le résultat ne
va pas pour autant. Aucun coffre livré ne fait trois colonnes, c'est un palier que le test client
invente lui-même, mais un addon qui en poserait un tomberait dessus.

Un coffre apparié cache un objet en 3x3 sur sa languette de module. Ce n'est pas une grille de
fabrication : c'est la case de module, dessinée à la taille d'une case ordinaire, et la languette
grandit avec le nombre de cases que les addons ont inscrites.

## Ce que la mesure de performance ne dit pas

D'où vient le chiffre : une trémie demande deux fois par case si le coffre est plein, et ces deux
appels passent chacun par `storage()`, qui appelait `moduleHolder()` deux fois. Quatre recherches de
block entity par case sur la moitié d'une paire, aucune sur un coffre seul, qui rend `this` dès la
première ligne. Mesuré sur un coffre écho à seize modules de rangée, 216 cases : 99,85 ns par case
seul, 286,02 ns par case en paire, soit 2,86 fois. La réponse est gardée depuis. Le commit qui la
pose a relevé 2,17, et la passe de 11h27 ce matin 1,56, avec 132,20 ns seul et 205,78 ns en paire :
même code, même machine, une heure d'écart.

Le reste vient d'un seul appel, et il est localisé. `alignCapacityWithHolder` finit par
`DeepCrateApi.capacityAmong(holder.modules())`, qui relit la table des modules à chaque passage. En
le remplaçant par une constante, tout le reste étant identique, trois passes donnent 0,78, 1,16 et
1,27 là où les trois passes de référence juste avant donnaient 2,25, 2,36 et 2,18. Autrement dit,
sans cet appel, la moitié d'une paire coûte à la case ce que coûte un coffre seul. L'expérience a
été défaite, rien n'en reste dans l'arbre, et personne n'a demandé d'aller plus loin : c'est écrit
ici pour que la prochaine personne qui veut ce facteur sache où creuser plutôt que de rechercher
elle-même.

`CrateHopperCostGameTest` mesure ce qu'une trémie paie pour interroger un coffre, et son plafond est
à 5,0. Ce n'est pas la vraie valeur, qui tombe entre 1,6 et 2,2 depuis que la moitié d'une paire
retient quel côté porte son module. Le plafond est large parce que la même marche sur le même code a
donné 2,86 un matin, puis 2,67 et 3,86 à deux minutes d'intervalle le lendemain : ce qui bougeait,
c'était la charge de la machine. Un seuil serré sur ce genre de mesure casse chez quelqu'un d'autre
sans qu'aucun code n'ait changé.
