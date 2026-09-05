# Les six fichiers encore au-dessus de 150 lignes

Fiche du 5 septembre 2026, branche `modularite`, écrite après la tâche 4 du plan d'audit. Elle ne
contient aucun Java à recopier : elle dit, pour chacun des six fichiers, ce qu'il porte, ce qui peut
en sortir, et où ça s'arrête. Trois des six ne se découpent pas, et c'est une réponse, pas un aveu.

## La mesure

`find mod/src/main/java -name '*.java' | xargs wc -l | sort -rn`, tête de liste :

```
   404 inventory/DeepCrateMenu.java
   323 client/screen/DeepCrateScreen.java
   315 block/DeepCrateBlockEntity.java
   294 inventory/CrateStorage.java
   263 block/DeepCrateBlock.java
   204 api/DeepCrateApi.java
   143 init/RegistryInit.java
```

Le septième fichier tient sous le plafond, avec sept lignes de marge. Les six premiers sont ceux de
cette fiche, et les chiffres sont exactement ceux annoncés.

## Le verdict, avant le détail

Trois fichiers se découpent et restent quand même au-dessus de 150 : `DeepCrateMenu` tombe vers 252,
`DeepCrateScreen` vers 232, `DeepCrateBlock` vers 208. Ce ne sont pas des échecs de conception, ce
sont des sous-classes de `AbstractContainerMenu`, `AbstractContainerScreen` et `BaseEntityBlock`,
dont l'essentiel des lignes restantes est constitué de méthodes redéfinies qui ne peuvent pas
changer de classe.

`DeepCrateApi` perd une méthode et tombe à 190. Il ne descendra pas plus bas sans renommer ce que
les addons appellent.

Deux fichiers ne bougent pas du tout. `DeepCrateBlockEntity` a déjà rendu ses trois morceaux
détachables à la tâche 4, et 91 de ses 315 lignes sont désormais la facture de ce découpage, sous
forme de méthodes qui ne font que passer l'appel. `CrateStorage` est épinglé par ses tests : treize
méthodes publiques y sont appelées sur l'instance par `CrateStorageTest`, que je n'ai pas le droit
de modifier, donc chaque extraction candidate laisse un moignon derrière elle et rapporte dix lignes.

## Les dossiers avant travaux

Comptés fichier par fichier sous `mod/src/main/java` :

```
2  deepcrate            7  inventory
7  api                  3  inventory/module
5  api/module           2  inventory/slot
6  block                3  init
1  client               1  mixin
3  client/render        1  net
5  client/screen
4  client/sort
```

Deux dossiers sont pleins. `api/` est à 7, et `inventory/` aussi. Le second compte, parce que
`DeepCrateMenu` et `CrateStorage` y habitent tous les deux : aucun fichier neuf ne peut y atterrir,
tout ce qui sort d'eux doit trouver un sous-paquet.

## Vérification des noms contre le jeu

Les noms proposés plus bas ont été confrontés à la liste réelle des classes de Minecraft 1.21.11,
tirée des correspondances de loom
(`~/.gradle/caches/fabric-loom/1.21.11/loom.mappings.1_21_11.layered+hash.2198-v2/mappings.tiny`,
colonne `named`, 6035 classes de premier niveau), plus les 1094 classes de fabric-api 0.141.4.
Le jar `minecraft-merged.jar` du même cache est obfusqué et ne sert à rien pour ça : il donne `a`,
`aa`, `aab`.

Aucun des sept noms proposés n'existe dans les 9208 noms simples de cette liste. Le témoin est bon :
la même commande trouve `Registries` (`net.minecraft.core.registries.Registries`), qui est le nom
que la relecture avait refusé.

Elle trouve aussi `PageButton`, dans `net.minecraft.client.gui.screens.inventory`. Le mod porte déjà
un `PageButton` sous `client/screen/`, posé à la tâche 1. Il compile et il continuera de compiler,
parce qu'une classe du même paquet l'emporte sur un import : `DeepCrateScreen` est dans le même
paquet que lui et n'importe pas celui du jeu. Le voisinage est gênant à la lecture, le paquet du jeu
étant celui d'où vient `AbstractContainerScreen` que l'écran étend. Ce n'est pas un défaut à corriger
dans cette fiche, mais c'est mesuré et écrit.

## Vérification des portées contre le jeu

Chaque découpe plus bas bute sur la portée d'un membre du jeu. Elles sont lues dans le jar nommé du
cache loom du dépôt, pas dans le `minecraft-merged.jar` obfusqué :

```
JAR=$(find mod/.gradle/loom-cache/minecraftMaven -name 'minecraft-merged-*-v2.jar' | head -1)

javap -p -classpath "$JAR" net.minecraft.world.inventory.AbstractContainerMenu
  protected net.minecraft.world.inventory.Slot addSlot(net.minecraft.world.inventory.Slot);
  protected net.minecraft.world.inventory.DataSlot addDataSlot(net.minecraft.world.inventory.DataSlot);
  public void broadcastChanges();
  protected boolean moveItemStackTo(net.minecraft.world.item.ItemStack, int, int, boolean);

javap -p -classpath "$JAR" net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
  protected int imageWidth;
  protected int leftPos;
  protected int topPos;

javap -p -classpath "$JAR" net.minecraft.client.gui.screens.Screen
  protected <T extends net.minecraft.client.gui.components.events.GuiEventListener & net.minecraft.client.gui.components.Renderable & net.minecraft.client.gui.narration.NarratableEntry> T addRenderableWidget(T);

javap -p -classpath "$JAR" net.minecraft.world.entity.player.Player
  public net.minecraft.world.inventory.AbstractContainerMenu containerMenu;
```

Un membre protégé hérité d'un autre paquet ne s'ouvre pas au voisinage. Une classe posée dans
`client/screen/`, à côté de `DeepCrateScreen`, ne lit pas `screen.leftPos` mieux qu'une classe posée
n'importe où ailleurs, parce que `leftPos` est déclaré dans
`net.minecraft.client.gui.screens.inventory` et que seul le sous-classeur y accède. Tout ce qui est
protégé dans cette liste se passe donc en argument, ou reste dans la classe qui hérite.

## DeepCrateMenu, 404 lignes

### Ce qu'il porte

Les lignes 1 à 34 sont les imports et la javadoc de classe. Ensuite, dans l'ordre du fichier :

36 à 46, neuf constantes qui ne font que réexporter `CratePanelGeometry`. Le commentaire de la ligne
36 dit que l'écran et les gametests les lisent sous `DeepCrateMenu` ; c'est faux pour les gametests.
`grep -rn "DeepCrateMenu\.[A-Z]" mod/src/test mod/src/gametest` ne remonte rien. Les seuls lecteurs
sont `DeepCrateScreen` (sept sites) et `CratePanel` (cinq), tous deux sous `client/screen/`.

48 à 70, les champs. 72 à 82, le constructeur client qui fabrique un `CrateContainer` à partir du
`CrateOpenData`. 84 à 156, le vrai constructeur, qui fait six choses de suite : choisir le conteneur
de cases (96 à 106), poser les emplacements de module (110 à 112), poser ceux du coffre avec leur
géométrie et leur page (114 à 128), poser l'inventaire du joueur (130 à 133), déclarer le `DataSlot`
qui synchronise la contenance (135 à 150), lire le nombre de rangées avant la première variation puis
ouvrir sur la première page (152 à 155).

158 à 200, les accès et `setPage`. 202 à 221, l'envoi d'un module clic-majuscule vers sa case.
223 à 238, `sort`. 240 à 244, `isSlotOnCurrentPage`. 246 à 260, `clicked`, qui refuse la touche
numérique sur une case au-dessus de la limite d'objet. 262 à 265, `stillValid`. 267 à 302,
`quickMoveStack`. 304 à 334, `removed`, dont la fin lâche par terre ce qui dépasse quand un module
part. 336 à 350, `anotherScreenIsOpen`. 352 à 403, la réaction à un changement de case :
`onModuleChanged`, `moduleStacks`, `reopenIfRowCountChanged`.

### Ce qui sort

Quatre déplacements, du plus rentable au plus mince.

Le premier ne crée aucun fichier. Les neuf constantes des lignes 38 à 46 disparaissent, et les deux
lecteurs importent `CratePanelGeometry` directement. Les deux relais statiques `panelWidth(int)` et
`gridLeft(int, int)` des lignes 174 à 180 partent avec, personne ne les appelle depuis l'extérieur du
menu. Environ 18 lignes, à coût nul : aucun test ne lit ces noms.

Le deuxième est `MenuModules`, dans `inventory/module/`. Il prend l'initialisation de `capacity`
(92), le choix du conteneur de cases (96 à 106), le `DataSlot` (135 à 150), la lecture du nombre de
rangées avant la première variation (152 à 154) et le bloc 352 à 403 en entier. Il devient
propriétaire de `moduleContainer`, de `capacity` et de `rowModuleCount`.

Le point délicat est l'accès, et il vaut d'être posé membre par membre. Voici ce que les lignes
déplacées touchent sur le menu, avec la portée actuelle :

```
crate            private final Container                    lu en 146 et 357
crates           private final List<DeepCrateBlockEntity>   lu en 98, 106, 356, 365, 393, 397
player           private final Player                       lu deux fois en 393
capacity         private int                                lu en 140, 358, 366 ; écrit en 92, 145, 353
rowModuleCount   private int                                lu en 388 ; écrit en 154 et 392
moduleContainer  private final Container                    écrit en 98 ; lu en 372 à 374, et en 111
onModuleChanged  private void                               référencé en 103 et 106
moduleStacks     private List<ItemStack>                    appelé en 154, 353, 387
```

Une référence arrière ne règle pas ça. `CrateLid` en tient une sur le coffre, mais `CrateLid` vit
dans `block/`, le paquet de `DeepCrateBlockEntity`, donc elle atteint les membres de paquet.
`inventory/module/MenuModules` et `inventory/DeepCrateMenu` sont dans deux paquets différents : une
référence arrière n'y donnerait que le public, et les cinq premières lignes du tableau sont privées.
Les ouvrir ferait cinq accesseurs neufs sur le menu, et cinq accesseurs neufs disent que la couture
n'a pas été trouvée.

Elle l'est quand même, en passant les valeurs au constructeur au lieu de les lire à travers le menu.
`new MenuModules(container, crates, inventory.player, this)` se pose ligne 96, où `container`,
`crates` et `inventory.player` sont déjà en main, le champ `player` n'étant affecté qu'en 109.
`DeepCrateMenu` ne gagne aucun accesseur. Le quatrième argument sert à une seule chose, le test
d'identité `serverPlayer.containerMenu == menu`, et `containerMenu` est un champ public de `Player`.

Dans l'autre sens, `MenuModules` rend trois choses. `container()`, que le menu passe aux `ModuleSlot`
de la ligne 111 et qui remplace le champ supprimé. `capacitySlot()`, que le menu inscrit lui-même
parce que `addDataSlot` est protégé. `capacity()`, sur quoi le `capacity()` du menu se replie ; cette
délégation n'est pas facultative, `CrateMenuGameTest` lit `deepCrateMenu.capacity()` lignes 31 et 43
et je n'ai pas le droit de toucher au test.

Rien de déplacé n'appelle `broadcastChanges()`. Le seul site est la ligne 237, dans `sort`, qui reste
au menu.

Environ 90 lignes sortent, le solde des imports compris, et 5 rentrent : le champ, la construction,
son commentaire et l'inscription du `DataSlot`. Le fichier neuf fait de l'ordre de 137 lignes, treize
sous le plafond. C'est peu de marge pour un fichier qu'on vient de poser, et c'est le prix des quatre
valeurs injectées et des trois accesseurs rendus.

Le troisième est `ModuleSpill`, dans `inventory/module/`. Il prend la fin de `removed` (309 à 333) et
`anotherScreenIsOpen` (336 à 350). Sa signature ne mentionne pas le menu : une liste de
`DeepCrateBlockEntity` et le joueur qui ferme, rien d'autre. Il appelle `CrateDrops.dropWhole`, déjà
sorti à la tâche 3. Il est dans `inventory/module/` et pas dans `block/` pour deux raisons : `block/`
n'a de la place que pour un seul fichier neuf, qui est pris plus bas par `CratePairing`, et ce que ce
code lâche par terre vient d'un module retiré, `overflow()` pour la contenance et `trimToRows()` pour
les rangées. Environ 40 lignes sortent, une seule rentre. Le fichier neuf fait environ 60 lignes.

Le quatrième est `CrateSlotLayout`, dans `inventory/slot/`, et c'est le plus discutable. Il prend la
boucle 114 à 128, qui calcule la largeur du panneau, le bord gauche de la grille, puis pour chaque
case sa colonne, sa ligne dans la page et son numéro de page. Il rend une `List<DeepCrateSlot>` dans
l'ordre, le menu la parcourt et appelle `addSlot`, qui est protégé. Environ 16 lignes sortent, 6
rentrent, gain net de 10 pour un fichier de 36. C'est le seul des quatre qui peut déplacer un pixel :
tout le reste est du branchement, celui-ci est de l'arithmétique de position. Si un seul des quatre
doit être abandonné, c'est celui-là.

### Où il s'arrête

404 moins 18, moins 85, moins 39, moins 10, soit environ 252 lignes. Il reste les imports allégés, la
javadoc de classe, les champs, les deux constructeurs, les accès, `moveModuleToItsSlot`, `sort`,
`setPage`, `isSlotOnCurrentPage`, et les quatre redéfinitions `clicked`, `stillValid`,
`quickMoveStack`, `removed`.

`quickMoveStack` fait 36 lignes et `moveModuleToItsSlot` 21, et aucun des deux ne peut sortir :
tous les deux appellent `moveItemStackTo`, qui est `protected` dans `AbstractContainerMenu`. Les
faire sortir demanderait de passer une lambda depuis le menu, c'est-à-dire de construire la coquille
qui délègue tout, ce que le plafond de 150 lignes cherche justement à éviter. Le menu reste à 252 et
il y reste.

## DeepCrateScreen, 323 lignes

### Ce qu'il porte

34 à 47, les constantes de l'écran, dont deux qui réexportent le menu et une, `ABBREVIATE_ABOVE`,
qui est un des quatre nombres que la tâche 7 doit envoyer dans le fichier de réglages. 49 à 61, les
champs, répartis en trois familles : la page, les boutons de tri, la recherche.

63 à 74, le constructeur, qui fixe la largeur et la hauteur du panneau. 76 à 139, `init`, qui fait
trois choses sans rapport entre elles : le champ de recherche (80 à 85), les boutons de tri avec leur
enroulement sur plusieurs lignes et la mémoire des sens de tri (87 à 113), les boutons de page avec
leur colonne de quatre (115 à 138).

141 à 151, `hasClickedOutside`, qui interroge trois zones. 153 à 157, la languette de module.
159 à 167, les boutons de tri. 169 à 177, ceux de page. 179 à 198, le tic et la relecture des pages
qui portent quelque chose. 200 à 206, `render`. 208 à 243, `renderBg`, qui décide combien de rangées
la page porte et enchaîne quatre appels à `CratePanel`. 245 à 260, `keyPressed`. 262 à 275,
`renderSlot`. 277 à 287, l'infobulle qui rétablit le compte exact. 289 à 304, `renderLabels`.
306 à 314, l'envoi de l'ordre de tri. 316 à 322, l'abréviation.

### Ce qui sort

`CrateSortStrip`, dans `client/sort/`, à côté de `SortButton` qu'il fabrique. Il prend `SORT_GAP` et
`SORT_BUTTON_GAP`, la liste des boutons, la table `sortDirections`, le bloc 87 à 113, la méthode
`isOverSortButtons` et l'envoi 306 à 314. Quatre membres protégés barrent le chemin direct.
`addRenderableWidget` d'abord, donc la classe construit les boutons et l'écran les inscrit. Puis
`leftPos`, `topPos` et `imageWidth`, que le bloc 87 à 113 lit aux lignes 97, 105 et 106, et qui se
passent en trois `int` à la fabrique. Environ 52 lignes sortent, 8 rentrent. Fichier neuf, environ
77 lignes. `client/sort/` passe de 4 à 5.

`CratePageStrip`, dans `client/screen/`. Il prend `PAGE_BUTTONS_PER_COLUMN`, la liste des boutons, le
tableau `pagesHoldingItems`, le bloc 115 à 138, `isOverPageButtons` et `readPagesHoldingItems`. Mêmes
trois `int` et même mécanique d'inscription, le bloc 115 à 138 lisant `leftPos` et `imageWidth` en
130, `topPos` en 131. Atterrir dans le paquet de l'écran n'y change rien, comme dit plus haut.
Environ 46 lignes sortent, 8 rentrent. Fichier neuf, environ 71 lignes.

`CrateCountLabel`, dans `client/screen/`. Il prend `ABBREVIATE_ABOVE`, la méthode `abbreviate` et la
fabrication de la ligne d'infobulle `screen.deepcrate.count`. Onze lignes sortent, deux rentrent,
pour un fichier de 31. Le gain de lignes est faible et je le propose quand même, parce que la tâche 7
doit rendre le seuil réglable et que ce fichier est l'endroit où le réglage se lira. C'est le
troisième par ordre de priorité, à laisser tomber si la place manque.

### Où il s'arrête

323 moins 44, moins 38, moins 9, soit environ 232 lignes. Ce qui reste est neuf redéfinitions
d'`AbstractContainerScreen` : `init`, `hasClickedOutside`, `containerTick`, `render`, `renderBg`,
`keyPressed`, `renderSlot`, `getTooltipFromContainerItem`, `renderLabels`.

`renderBg` pèse 36 lignes à lui seul et c'est le plus gros bloc restant. Son corps pourrait
descendre en entier dans `CratePanel`, sous la forme d'un `renderBackground` qui prendrait les
rangées, les colonnes et la largeur. Ça ne règle rien : `CratePanel` fait 123 lignes, il passerait à
153 et deviendrait le septième fichier hors plafond. Le problème changerait de fichier, pas de
taille. L'écran reste à 232.

## DeepCrateBlock, 263 lignes

### Ce qu'il porte

Seize méthodes redéfinies de `BaseEntityBlock` et `BlockBehaviour`, trois propriétés d'état, deux
formes de collision, et un ensemble de règles d'appariement qui n'appartient à aucune de ces
catégories.

Les règles d'appariement occupent 80 à 88 (`connectedDirection` et `connectedPos`), 90 à 109
(`cratesFor`, qui remonte les deux moitiés dans l'ordre porteur d'abord), 111 à 114 (`containerFor`),
143 à 160 (la décision `SINGLE` / `LEFT` / `RIGHT` à la pose, avec le cas accroupi séparé du cas
normal), 188 à 198 (le mariage et le veuvage dans `updateShape`) et 258 à 262 (`partnerFacing`).
Soixante-sept lignes, sur un seul sujet, qui est comment deux coffres deviennent un.

Le reste est du redéfini : `codec`, `newBlockEntity`, `getTicker`, `getShape`, `useWithoutItem`,
`getStateForPlacement`, `updateShape`, `getFluidState`, `isPathfindable`,
`affectNeighborsAfterRemoval`, `tick`, `hasAnalogOutputSignal`, `getAnalogOutputSignal`, `rotate`,
`mirror`, `createBlockStateDefinition`.

### Ce qui sort

`CratePairing`, dans `block/`, qui prend les soixante-sept lignes. Les quatre méthodes statiques
publiques changent simplement de classe d'accueil, sans relais : leurs seuls appelants sont
`CrateModuleHolder`, `CrateLid`, `DeepCrateRenderer` et `DeepCrateBlockEntity`, et aucun test ne les
appelle. Vérifié :

```
grep -rn "cratesFor\|containerFor\|connectedPos\|connectedDirection" mod/src --include=*.java
```

ne remonte que des fichiers de `main/`.

Le risque est quand même à nommer, parce qu'une surface publique bouge. Le grep ci-dessus ne voit que
ce dépôt : un addon écrit ailleurs qui appelle une des quatre casse au déplacement, et il n'y a pas
de relais derrière pour le rattraper. Ce qui autorise à ne pas en poser, c'est que rien de ce qui
fait face aux addons ne les nomme, le même motif lancé sur `README.md` et `store/*.md` ne remontant
rien. La même prudence vaut plus bas pour `DeepCrateApi.layoutFor`, à ceci près qu'il n'y a là qu'une
méthode.

Deux points demandent une signature un peu pensée. `partnerFacing` teste `blockState.is(this)`, donc
la version déplacée prend le bloc en argument. Et `getStateForPlacement` réaffecte `facing` en même
temps qu'il décide le `ChestType`, donc la méthode déplacée rend les deux ensemble ; un `record` de
deux champs suffit, `CratePairing.placement(block, context)`. Le `updateShape` déplacé rend un
`BlockState` nullable, et le bloc garde le repli sur `super`.

Environ 67 lignes sortent, 12 rentrent sous forme d'appels. Fichier neuf, de l'ordre de 102 lignes.
`block/` passe de 6 à 7.

### Où il s'arrête

263 moins 55, soit environ 208 lignes, toutes en redéfinitions et en déclarations d'état. Le bloc
comparateur (226 à 241) fait 16 lignes dont 6 de corps, et les deux méthodes sont redéfinies : le
sortir donnerait un fichier de trente lignes pour en économiser six. Le bloc reste à 208.

## DeepCrateApi, 204 lignes

### Ce qu'il porte

21 à 40, trois constantes publiques et leurs javadocs, vingt et une lignes à elles seules.
`BASE_CAPACITY`, `MAX_CAPACITY` et `AUTOMATION_LIMITED`, dont deux figurent dans la liste des quatre
nombres que la tâche 7 doit envoyer dans `config/oas/deepcrate.json`.

42 à 49, les six collections statiques et le constructeur privé. 51 à 93, les quatre inscriptions.
95 à 121 et 134 à 164, les recherches. 123 à 132, `rowsOf` et l'abonnement aux paliers. 166 à 188,
les deux sommes sur une suite de piles. 190 à 203, `layoutFor`.

### Ce qui sort

Une seule méthode, et sans créer de fichier. `layoutFor` (190 à 203) n'est ni une inscription ni une
recherche : elle déclenche `CrateLayoutCallback`, vérifie que ce que l'addon a rendu couvre bien les
rangées, et journalise un repli sinon. Sa place est sur `CrateLayout`, qui fait 23 lignes dans le
même dossier et porte déjà `balanced`. Sous le nom `CrateLayout.forTier(tier, rows)`, elle laisse
`DeepCrateApi` à 190 et met `CrateLayout` à 37. `api/` reste à 7 fichiers.

Le risque est nommé : `DeepCrateApi.layoutFor` est public, donc un addon hors dépôt qui l'appelle
casse. Le README ne le cite pas, il ne cite que `registerTier`, `registerModule` et
`CrateLayoutCallback.EVENT`, et les deux appelants internes sont dans `DeepCrateBlockEntity`.

### Pourquoi le reste ne bouge pas

`DeepCrateApi` est lui-même la surface que les addons compilent. L'exemple du README, ligne 113,
écrit `DeepCrateApi.registerTier(...)`. Couper la moitié paliers de la moitié modules revient à
renommer ce que les addons appellent, ou à laisser derrière la coquille de délégations. Il n'y a pas
de troisième option pour une classe dont tous les membres sont publics et statiques.

Ce qui reste au-dessus du plafond après le déplacement, ce sont 27 lignes de trois parcours linéaires
presque identiques (`moduleSlot`, `rowModuleFor`, `moduleFor`), 21 lignes de javadoc de constantes
que la tâche 7 déplacera, et l'état du registre. Aucun des trois n'est une responsabilité séparable
du reste. Le fichier tombera à 190 ici, et sous 170 quand la tâche 7 emportera les constantes.

## DeepCrateBlockEntity, 315 lignes, ne se découpe pas

C'est le cas exact que la consigne décrit. La classe étend `BaseContainerBlockEntity` et implémente
`LidBlockEntity` et `ExtendedScreenHandlerFactory`, ce qui fait vingt-deux méthodes redéfinies.

La tâche 4 a déjà sorti les trois morceaux détachables, et la facture est visible dans le fichier :

```
32 lignes (76 à 107)   délégations vers CrateModuleHolder
33 lignes (250 à 282)  délégations vers CrateLid
26 lignes (205 à 230)  délégations vers CrateSave
```

Quatre-vingt-onze lignes sur 315 ne font plus que passer l'appel. Elles ne peuvent pas disparaître.
Les cinq délégations de module sont épinglées par les gametests, que je n'ai pas le droit de
modifier : `setModule` est appelé depuis cinq classes de gametest, `setRowModules` depuis cinq aussi,
`setModuleIn` depuis `CrateModuleSlotGameTest` et `ModuleContainer`, `module()` depuis
`CrateMenuGameTest` ligne 247, `extraRows()` depuis `CrateRowModuleGameTest` ligne 25. Les six
délégations de couvercle sont des méthodes d'interface, `startOpen`, `stopOpen`,
`getEntitiesWithContainerOpen`, `triggerEvent`, `getOpenNess`, plus le `lidAnimateTick` que
`DeepCrateBlock` passe au `BlockEntityTicker`. Elles sont là par définition.

Un seul bloc peut encore changer de maison : `alignStorageWithTier` (283 à 299, 17 lignes). Sa
formule est `tier.slotCount() + extraRows() * tier.columns()`, c'est-à-dire les rangées ajoutées par
les modules, et son jumeau `alignCapacityWithHolder` vit déjà sur `CrateModuleHolder`. Le déplacer
mettrait les deux alignements au même endroit, ferait passer `CrateModuleHolder` de 121 à 137 lignes,
et laisserait `DeepCrateBlockEntity` à 299. Ça ne change pas le verdict et je ne le recommande pas
comme un découpage : c'est un rangement, à faire ou pas selon l'humeur du jour.

## CrateStorage, 294 lignes, ne se découpe pas

Le motif est différent des autres. `CrateStorage` n'implémente aucune interface du jeu, ne touche ni
niveau ni menu ni joueur, et pourrait en théorie se couper en deux. Ce qui l'en empêche est le banc
d'essai.

`CrateStorageTest` appelle treize méthodes sur l'instance : `accepts`, `automationCapacityFor`,
`capacityFor`, `extract`, `get`, `grow`, `insert`, `isEmpty`, `overflow`, `set`, `setCapacity`,
`size`, `splitForVanilla`. `CrateSorterTest` y ajoute `restore`. Le reste du mod y ajoute `trimTo`,
`slots`, `replaceSlots`, `capacity`, `setTier` et `clear`. La consigne interdit de modifier un seul
test. Toute méthode extraite doit donc rester joignable sous son nom actuel sur cette classe, ce qui
veut dire un moignon de délégation pour chacune.

Deux extractions ont été chiffrées.

La règle de contenance, lignes 49 à 78, trente et une lignes : `capacityFor`, `accepts`, `limitFor`
et `automationCapacityFor`. C'est la seule partie du fichier qui regarde dehors, puisque c'est là que
`CrateCapacityCallback` se déclenche et là que `DeepCrateApi.AUTOMATION_LIMITED` se lit. Une classe
`CrateSlotLimits` sous `api/module/`, à côté du callback, serait cohérente. Environ 24 lignes
sortiraient, 14 rentreraient en quatre méthodes de délégation. Gain net de dix lignes pour un fichier
de plus. C'est la coquille, en petit.

Le découpage en piles de taille vanilla, lignes 274 à 285, treize lignes : `split` est privé et
statique, il partirait sans laisser de trace, mais `inventory/` est plein, donc il faudrait ouvrir un
sous-paquet pour treize lignes qui en rapportent treize.

Ni l'une ni l'autre ne vaut le fichier qu'elle coûte. `CrateStorage` raconte une seule histoire, les
cases d'un coffre et ce qu'elles retiennent, et son banc d'essai la traite déjà comme un seul objet.
Il reste à 294.

## Les dossiers après travaux

Si les sept fichiers proposés sont posés :

```
api            7 -> 7   (layoutFor rejoint un fichier existant)
api/module     5 -> 5
block          6 -> 7   CratePairing
client/render  3 -> 3
client/screen  5 -> 7   CratePageStrip, CrateCountLabel
client/sort    4 -> 5   CrateSortStrip
inventory      7 -> 7   (plein, rien n'y entre)
inventory/module 3 -> 5 MenuModules, ModuleSpill
inventory/slot 2 -> 3   CrateSlotLayout
```

Aucun dossier ne franchit huit, et la tâche 8 passe elle aussi. Elle prévoit trois fichiers de greffe
sur l'écran, `CrateScreenArea`, `CrateScreenCallback` et `CrateTooltipCallback`, et le plan d'audit
les pose dans `client/`, pas dans `client/screen/` :

```
grep -n "Create: .*CrateScreen\|Create: .*CrateTooltip" docs/superpowers/plans/2026-09-05-deepcrate-audit.md
920:- Create: `mod/src/main/java/com/dreykaoas/deepcrate/client/CrateScreenArea.java`
921:- Create: `mod/src/main/java/com/dreykaoas/deepcrate/client/CrateScreenCallback.java`
922:- Create: `mod/src/main/java/com/dreykaoas/deepcrate/client/CrateTooltipCallback.java`
```

`client/` ne porte aujourd'hui qu'un fichier, `DeepCrateClient.java`, et passe donc à 4. Le paquet de
l'écran reste à 7, `CrateCountLabel` n'a personne à qui céder sa place, et aucun sous-paquet neuf
n'est à ouvrir.

## Ce qui vérifie que rien n'a bougé

Les deux filets de la tâche 4 s'appliquent tels quels, et le second n'est pas négociable.

Les 37 tests JUnit et les 44 gametests tournent après chaque déplacement, sans qu'aucun soit
retouché. Un test modifié pour suivre un découpage annule la preuve que le découpage n'a rien changé.

Les onze captures du gametest client sont comparées octet par octet avant et après chaque
déplacement, une par une : `crates-in-the-world`, `double-crate-beside-a-chest`,
`single-crate-close`, `double-crate-close`, `screen-before-sorting`, `screen-sorted-by-name`,
`screen-sorted-by-count`, `screen-paged-crate`, `single-crate-after-sorting`, et les deux du
gametest de colonnes. L'écran du coffre est peint à la main à partir de `generic_54`, un décalage
faux compile et passe tous les tests, et trois des déplacements proposés touchent des coordonnées :
`CrateSlotLayout` pour les cases du coffre, `CrateSortStrip` pour l'enroulement des boutons sur
plusieurs lignes, `CratePageStrip` pour la colonne de quatre. Ces trois-là sont à poser dans trois
commits séparés, avec une comparaison de captures chacun.
