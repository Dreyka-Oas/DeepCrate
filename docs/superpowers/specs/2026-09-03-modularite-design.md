# Ouvrir les murs de DeepCrate

## But

Un inventaire des points d'extension du mod, fait le 3 septembre 2026, a compté 86 endroits ouverts
et 51 fermés. Les endroits ouverts se répartissent entre ce qui demande du Java (inscrire un palier,
un module, une rangée, répondre à l'événement de découpage en pages) et ce qui n'en demande pas du
tout (étiquettes d'objets, recettes, butin, langues, textures). C'est déjà large.

Les 51 fermés se ramènent à cinq murs. Aucun ne se contourne par un mod extérieur, quel que soit le
Java qu'il écrive, parce que les valeurs sont des constantes ou des énumérations à valeurs fixes.
Cette fiche décrit comment les cinq tombent.

Les cinq murs, dans l'ordre où ils seront abattus :

1. Le coffre a exactement deux cases à module, et personne ne peut en ajouter une troisième.
2. Il y a exactement deux ordres de tri, et personne ne peut en ajouter un troisième.
3. Un coffre fait exactement neuf colonnes.
4. La règle qui décide ce qu'une case retient est écrite en dur et ne se consulte pas.
5. Rien ne permet de poser un bouton ou une ligne d'infobulle sur l'écran du coffre.

## Ce qui reste fermé, et pourquoi

Un palier a besoin d'un bloc inscrit dans le registre des blocs, et un registre de blocs se ferme
avant qu'un fichier de données soit lu. Créer une sorte de coffre restera donc du Java, comme
aujourd'hui. Cette fiche ne cherche pas à changer ça.

## Les cases à module

### Ce qui existe

Le coffre garde deux piles dans deux champs, `module` et `rowModules`. `ModuleContainer` répond 2 à
`getContainerSize`, avec deux indices nommés en constantes. Le menu pose ses deux emplacements puis
démarre les cases du coffre à l'indice 2, valeur écrite en dur dans `CRATE_SLOT_START` et relue par
le déplacement rapide. L'écran dessine une plaque de 46 pixels de haut, qui est la hauteur de deux
cellules plus les bords.

### Ce qui le remplace

Une sorte de case à module devient une donnée inscriptible :

```java
public record CrateModuleSlot(Identifier id, int order, int stackLimit, TagKey<Item> accepts)
```

`order` décide de la place dans la colonne, du plus petit au plus grand. `accepts` est une étiquette
d'objets, comme pour `CrateModule`, ce qui garde la voie sans Java : ajouter un objet à l'étiquette
suffit à le rendre posable dans la case.

Le mod inscrit ses deux sortes, `deepcrate:capacity` au rang 0 et `deepcrate:rows` au rang 1, avec
les mêmes étiquettes qu'aujourd'hui et les mêmes limites de pile.

Le coffre remplace ses deux champs par une table de l'identifiant vers la pile. Une case dont
personne n'a inscrit la sorte garde sa pile en mémoire sans la dessiner : un monde ouvert sans le
mod qui l'avait ajoutée ne perd rien, et le module réapparaît quand ce mod revient.

### Ce que l'objet posé provoque

Aujourd'hui la case de contenance décide de la capacité et la case de rangées décide des rangées.
Cette liaison disparaît. La capacité d'un coffre est la plus forte que propose l'une quelconque de
ses piles de module, et ses rangées sont la somme de ce que proposent toutes ses piles. Deux modules
de contenance posés en même temps donnent la plus forte des deux, ce qui répond au sixième point
fermé de l'inventaire sans lui écrire de code à part.

Une sorte de case ajoutée par quelqu'un d'autre, dont l'étiquette ne croise ni les modules de
contenance ni ceux de rangée, ne change donc rien au coffre. Son mod lit la pile avec
`crate.moduleIn(id)` et en fait ce qu'il veut.

### La sauvegarde

Une nouvelle clé `Modules`, liste de couples identifiant et pile. À la lecture, si la clé manque,
les anciennes clés `Module` et `RowModules` sont relues vers `deepcrate:capacity` et
`deepcrate:rows`. Un coffre sauvé par la version précédente s'ouvre donc sans rien perdre. Les deux
anciennes clés ne sont plus écrites, et restent listées dans `removeComponentsFromTag` pour que le
nettoyage d'un coffre ramassé les efface encore.

### L'écran

La plaque de gauche est déjà bâtie en trois bandes : les cinq premières lignes de sa texture sont le
chapeau, les dix-huit suivantes une cellule, les cinq dernières le pied. Le dessin devient un
chapeau, autant de cellules qu'il y a de sortes inscrites, un pied. La hauteur vaut
`10 + cases * 18`, ce qui redonne 46 pour deux cases.

`CRATE_SLOT_START` devient le nombre de sortes inscrites, lu une fois à la construction du menu et
gardé dans un champ, jamais relu ailleurs. Le déplacement rapide et le garde contre l'échange par
touche numérotée s'appuient sur ce champ.

## Les ordres de tri

### Ce qui existe

`CrateSort` est une énumération de deux valeurs qui porte elle-même sa règle de comparaison, dans une
condition `this == NAME ? ... : ...`. Le bouton lit `crateSort == CrateSort.NAME ? 0 : SIZE` pour
choisir sa ligne dans une planche de quatre icônes. La clé de traduction est bâtie sur le nom de la
valeur d'énumération.

### Ce qui le remplace

```java
public record CrateSortOrder(Identifier id, int order, Identifier icon, CrateSortRule rule)

public interface CrateSortRule {
    Comparator<Item> comparator(Map<Item, Long> totals, Collator collator);
}
```

L'image fait 16 de large sur 32 de haut : le sens normal au-dessus, l'inverse en dessous. La planche
actuelle se coupe en deux fichiers, `textures/gui/sort/name.png` et `textures/gui/sort/count.png`.

Le registre vit côté client, dans `DeepCrateClientApi`, parce que la règle a besoin du comparateur de
la langue du joueur et qu'un serveur n'a pas de fichiers de langue. Le paquet envoyé au serveur ne
change pas : il porte déjà la liste d'objets et non le nom d'un ordre, ce qui veut dire qu'un ordre
ajouté par un mod client fonctionne contre un serveur qui l'ignore.

La clé de traduction devient `screen.<namespace>.sort.<path>`, avec `_reversed` pour l'inverse. Pour
les deux ordres livrés cela redonne `screen.deepcrate.sort.name` et `screen.deepcrate.sort.count`,
les clés qui existent déjà dans les deux fichiers de langue.

### L'écran

Un bouton par ordre inscrit, dans l'ordre de leur rang, posés de gauche à droite au-dessus du
panneau. S'ils dépassent la largeur du panneau, une seconde ligne s'ouvre au-dessus de la première.
Le sens de chaque bouton est relu quand l'écran se reconstruit, par identifiant plutôt que par
position, pour qu'un mod chargé entre-temps ne décale pas les sens.

Le rectangle que `hasClickedOutside` doit épargner devient l'enveloppe des boutons réellement posés,
calculée à partir de leur nombre plutôt qu'écrite pour deux.

## La largeur

### Ce qui existe

`CrateTier.COLUMNS` vaut 9 et sert à onze endroits. Cinq divisent une taille de conteneur pour
retrouver un nombre de rangées, quatre multiplient un nombre de rangées pour retrouver des cases,
deux servent au placement dans la grille. Le fond de l'écran est découpé dans `generic_54.png`, qui
fait exactement neuf cases de large, en un seul appel par bande.

### Ce qui le remplace

`CrateTier` gagne un champ `columns`. Le constructeur à trois arguments reste, et donne neuf, pour
qu'aucun mod déjà écrit ne casse. `slotCount()` multiplie par ce champ. Les onze endroits lisent le
palier plutôt que la constante. La constante reste, sous le nom `DEFAULT_COLUMNS`, parce que le
coffre a besoin d'une taille de départ avant de connaître son bloc.

Le nombre de colonnes voyage jusqu'au client dans `CrateOpenData`, qui gagne un cinquième champ. Sans
lui le client ne peut pas retrouver les rangées à partir de la taille du conteneur.

Les deux moitiés d'un coffre double sont du même palier, donc de la même largeur, et rien n'est à
faire de ce côté.

### Le fond

Le panneau de `generic_54.png` se lit ainsi : 7 pixels de bord à gauche, neuf cellules de 18, 7
pixels de bord à droite, soit les 176 de sa largeur. Le découpage passe donc de une bande à trois :
le bord gauche pris à `u = 0` sur 7 pixels, une cellule de 18 prise à `u = 7` et répétée autant de
fois qu'il y a de colonnes, le bord droit pris à `u = 169` sur 7 pixels. La même règle vaut pour
l'en-tête, pour les rangées de cases, pour la bande nue et pour le panneau du joueur.

À neuf colonnes, le résultat doit être identique au pixel près à celui d'aujourd'hui. C'est la façon
de vérifier que le changement n'a rien cassé, et une capture comparée à une capture d'avant le
prouve.

`imageWidth` vaut `14 + colonnes * 18`, ce qui redonne 176 à neuf colonnes. L'inventaire du joueur
garde ses neuf colonnes et se centre dans le panneau : son abscisse devient
`GRID_LEFT + (colonnes - 9) * 9`, qui vaut 8 dans le cas courant et ne bouge donc pas d'un pixel. Le
panneau du joueur se dessine sur toute la largeur, ses bandes de cases centrées de la même façon.

## La contenance

### Ce qui existe

`CrateStorage.capacityFor` décide seule : une pile qui ne s'empile pas garde sa propre limite, tout
le reste prend la capacité du coffre. Rien ne permet à un mod de dire qu'un coffre refuse la poudre à
canon, ou qu'un coffre à minerai en retient quatre fois plus.

### Ce qui le remplace

Un événement Fabric :

```java
public interface CrateCapacityCallback {
    int capacity(@Nullable CrateTier tier, ItemStack itemStack, int proposed);
}
```

Le stockage garde le palier de son coffre dans un champ, posé quand le coffre s'aligne sur son bloc.
Il vaut nul tant que le bloc n'est pas connu, ce qui arrive à la lecture de la sauvegarde, et
l'événement le reçoit tel quel.

Zéro ou moins veut dire que ce coffre refuse cet objet. Le refus ne détruit jamais rien : la valeur
que `capacityFor` rend au reste du code ne descend pas sous un, et le refus devient une question
séparée, `accepts(ItemStack)`, consultée par la pose dans une case, par l'insertion et par la limite
annoncée à l'automatisation. Un objet déjà rangé dans un coffre qui se met à le refuser y reste, et
sort par la main.

L'événement part une fois par appel, dans une méthode appelée en boucle par `insert`. La valeur est
sortie de la boucle, comme elle l'est déjà.

## La greffe sur l'écran

Deux événements, côté client seulement.

Le premier part à la fin de `init`, avec l'écran et un objet qui donne les coins du panneau et sait
poser un widget. Un widget posé hors du panneau doit être épargné par `hasClickedOutside`, sinon
relâcher un clic dessus lâche par terre ce que la main porte : l'objet gardera donc la liste des
rectangles à épargner, et les boutons de page, la plaque de module et les boutons de tri passeront
par la même liste au lieu de trois méthodes séparées.

Le second passe la liste des lignes d'une infobulle de case, avec la case et la pile, avant que la
liste parte au dessin. Il vaut pour les cases du coffre comme pour celles du joueur, et la ligne du
compte réel qu'ajoute déjà l'écran devient le premier abonné de son propre événement, ce qui prouve
que l'événement suffit.

## Ce qui est vérifié, et comment

Les règles de stockage, la contenance et son refus, la table des modules et sa relecture d'une
ancienne sauvegarde se testent en JUnit, sans monde ni client, comme le reste de `CrateStorage`.

Le nombre de cases du menu, la largeur, la croissance par rangées et le tri se testent en gametest,
qui a un serveur.

Le fond à neuf colonnes, la plaque à deux cases et les boutons de tri se vérifient par capture, avec
le test client qui existe déjà. Une capture d'un coffre à douze colonnes et d'un coffre à trois
s'ajoute à la scène, avec une plaque de trois cases, pour que le cas large et le cas étroit soient
regardés et non supposés.

## Ordre des travaux

Les cases à module d'abord, parce qu'elles touchent la sauvegarde et que tout le reste s'appuie
dessus. Les ordres de tri ensuite, qui ne touchent que le client. La largeur en troisième, la plus
longue et la plus visible. La contenance, courte. La greffe en dernier, parce qu'elle a besoin que
les rectangles à épargner soient déjà rassemblés en une liste.
