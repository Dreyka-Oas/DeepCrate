# Où en est la modularité de DeepCrate

Arrêt du 3 septembre 2026 au soir, branche `modularite`, à reprendre demain.

Le dessin est dans `docs/superpowers/specs/2026-09-03-modularite-design.md` et le plan détaillé,
tâche par tâche et pas à pas, dans `docs/superpowers/plans/2026-09-03-modularite.md`. Ce fichier ne
dit que ce qui est fait et ce qui reste.

## Ce qui est fait

Cinq commits sur la branche, chacun testé avant d'être posé.

Les cases à module sont devenues un registre. `CrateModuleSlot` décrit une case, `CrateModules` porte
les piles d'un coffre indexées par nom, `StoredModule` les écrit dans la sauvegarde sous la clé
`Modules`. Les anciennes clés `Module` et `RowModules` sont encore relues, donc un monde de la
version précédente s'ouvre sans rien perdre. Ce que fait un objet ne dépend plus de la case où il
est posé : la contenance est la plus forte de toutes les cases, les rangées en sont la somme. Le
menu ouvre une case par sorte inscrite et la plaque de gauche pousse d'une cellule à chaque fois.
`RowModuleSlot` a fondu dans `ModuleSlot`.

Les ordres de tri sont devenus un registre côté client, `DeepCrateClientApi`. Un ordre porte son nom,
son rang, son image de 16 sur 32 et sa règle de comparaison. Les deux ordres livrés se sont
réinscrits dedans sans changer de clé de traduction, et la planche d'icônes a été coupée en
`textures/gui/sort/name.png` et `count.png`. Il y a un bouton par ordre inscrit, repliés sur une
seconde ligne s'ils dépassent la largeur du panneau.

La largeur est passée dans le palier. `CrateTier` a un champ `columns`, neuf par défaut, et le
constructeur court existe toujours. Le nombre voyage jusqu'au client dans `CrateOpenData`. Le fond de
l'écran se bâtit maintenant en bord gauche, cellules répétées, bord droit, avec du panneau nu de
chaque côté quand la grille ne remplit pas l'intérieur. Le panneau ne descend jamais sous la largeur
d'un coffre, parce que l'inventaire du joueur y est dessiné aussi, et les deux grilles s'y centrent.

La contenance passe par `CrateCapacityCallback`. Il reçoit le palier, la pile et la valeur proposée.
Zéro veut dire que le coffre refuse l'objet, et le refus ne détruit rien : la valeur rendue au
stockage ne descend jamais sous un, le refus est la question séparée `accepts`, et ce qui est déjà
rangé dans un coffre qui se met à le refuser y reste.

## Ce qui a été vérifié, et comment

36 tests JUnit et 44 gametests, tous verts au moment de l'arrêt.

Trois défauts trouvés par la comparaison de captures et corrigés avant commit :

1. La bande nue sous l'inventaire du joueur remplissait de noir les deux coins bas du panneau, que
   l'ancien code laissait transparents. Quatre-vingts pixels d'écart.
2. Un coffre de trois colonnes rendait le panneau plus étroit que l'inventaire du joueur, qui
   débordait des deux côtés.
3. Le pied du panneau manquait sur les côtés d'un coffre de douze colonnes, laissant le bas ouvert.

La capture à neuf colonnes est aujourd'hui identique au pixel près à celle d'avant tout le chantier,
ce qui est la preuve que rien du cas courant n'a bougé. Les captures sont dans
`mod/build/run/clientGameTest/screenshots/`, dont `screen-twelve-columns` et `screen-three-columns`.

## Ce qui reste

La tâche 6 du plan, la greffe sur l'écran, à peine commencée et remise à zéro pour ne pas laisser du
code à moitié écrit. Trois fichiers à créer, dont le code complet est déjà écrit dans le plan :
`CrateScreenArea`, `CrateScreenCallback` et `CrateTooltipCallback`. Puis dans `DeepCrateScreen`,
rassembler en une seule liste les rectangles que `hasClickedOutside` doit épargner, aujourd'hui
éclatés en trois méthodes, faire partir l'événement d'écran à la fin de `init`, et faire passer la
ligne du compte réel par son propre événement d'infobulle plutôt que par la méthode qui l'écrit en
dur. Une capture avec un bouton inscrit depuis le test client sert de preuve.

La tâche 7, la documentation. Le README promet trois points d'extension parce que la demande
d'origine en nommait trois ; il y en a huit maintenant. Sa section sur la contenance décrit aussi une
case unique, ce qui n'est plus vrai.

## Ce qui restera fermé, et pourquoi

Un palier a besoin d'un bloc inscrit dans le registre des blocs, et ce registre se ferme avant qu'un
fichier de données soit lu. Créer une sorte de coffre restera du Java.
