# Où en est DeepCrate

Arrêt du 6 septembre 2026, branche `modularite`. Ce fichier ne dit que l'état. Le plan tâche par
tâche est dans `docs/superpowers/plans/2026-09-05-deepcrate-audit.md`, et ce qui est cassé ou assumé
est dans `DEFAUTS-CONNUS.md`.

## Le plan de l'audit est fini

Ses dix tâches sont dans l'arbre et commitées. Les six premières l'étaient hier soir. Les quatre
dernières ont été posées ce matin, en quatre commits.

Les réglages existent : `config/oas/deepcrate.json`, cinq options, chacune bornée, écrite au premier
lancement et réécrite à chaque suivant pour qu'une option ajoutée par une mise à jour apparaisse
toute seule. La lecture passe avant `RegistryInit`, ce qui est le tout de l'affaire :
`rowModuleStackLimit` est lu pendant l'initialisation statique de ce registre, et lire le fichier
après aurait donné un réglage qui a l'air de marcher et ne fait rien. Les addons ont leur propre
passe, `onDeepCrateConfig()`, avant la lecture pour la même raison.

Les quatre constantes que ces options remplacent ont été retirées, pas dépréciées. `javac` recopie la
valeur d'une constante de compilation dans le fichier de classe de l'appelant, donc un addon compilé
contre une `BASE_CAPACITY` dépréciée aurait emporté 64 pour toujours.

La moitié d'une paire retient quel côté porte son module. Une trémie passait quatre fois par case par
`moduleHolder()`, et chacune de ces fois refaisait une recherche de block entity. La réponse est
maintenant gardée, et jetée quand l'état du bloc change, quand la moitié qu'elle nomme se déclare
retirée, ou quand un module atterrit dans une case, puisque le côté qui porte dépend de qui porte
quelque chose et que ça bouge sans que l'état du bloc bouge. Mesuré sur 216 cases : 2,17 fois une
caisse seule par case, contre 3,60 avant.

L'écran s'ouvre aux autres mods. `CrateScreenCallback` part à chaque mise en page et remet une
`CrateScreenArea` : le coin du panneau, sa taille, de quoi ajouter un widget, et `keepClickable`. Les
trois rectangles de la maison passent par cette même liste au lieu de trois tests écrits un par
espèce de widget. `CrateTooltipCallback` porte la case en plus de la pile, ce qui permet à un
écouteur de distinguer une case de coffre d'une case du joueur, et la ligne du compte réel du mod
s'inscrit dessus comme celle de n'importe qui.

La documentation dit ce qui est ouvert : douze points, avec la passe d'où chacun s'inscrit, là où le
README en nommait trois. Le douzième est `onTierRegistered`, qui ne répond pas au grep de la tâche
parce qu'il ne s'appelle pas `register` quelque chose, et qui est pourtant la seule façon d'apprendre
qu'un palier vient d'arriver. L'exemple d'addon qui accompagne la liste a été compilé contre cet
arbre, puis retiré, au lieu d'être recopié de mémoire. Ce qui reste fermé est écrit avec sa raison :
un palier porte un `Block`, le registre des blocs est gelé avant la première lecture de données, donc
inventer une espèce de coffre reste du Java.

Les deux sections périmées du README ont suivi. La capacité est la plus forte des cases et les
rangées s'additionnent, ce qui n'était plus dit depuis que les cases sont devenues un registre.
`DEFAUTS-CONNUS.md` a reçu ce qui traînait ailleurs : le coffre écho double qui perd son module,
l'automatisation bridée tant que lithium est là, et ce qu'une trémie payait avant le cache.

## Ce qui le prouve

45 gametests, 55 tests JUnit, verts après chaque commit. Le jar ne contient rien de développement,
le compte des quatre motifs rend 0. Les deux fichiers de langue portent les mêmes 23 clés, avec les
mêmes marqueurs de format : rien d'autre ne rattrape une clé écrite d'un seul côté, qui s'afficherait
telle quelle à l'écran.

Vingt-cinq captures du test client. `0011_screen-with-an-addon-button` montre un bouton posé par un
écouteur avec la seule API publiée, contre le bord droit du panneau. `0024_13-the-real-count-under-an-abbreviated-one`
montre l'infobulle sortir sur une pile dessinée 2M. Le bouton est derrière un drapeau parce qu'un
événement Fabric ne se désinscrit pas et que les deux tests clients tournent dans le même jeu : sans
ça il s'invitait dans les treize captures de la planche-contact.

Le plafond du test de coût est à 5,0 et pas à la vraie valeur. La même marche sur le même code a
donné 2,86 un matin, puis 2,67 et 3,86 à deux minutes d'écart : ce qui bougeait était la charge de la
machine, pas le code.

## Deux choses réglées après coup

Le dépôt est public depuis le 6 septembre à 11h22. Les deux seules adresses du mod, dans
`fabric.mod.json`, rendaient 404 en anonyme ; elles rendent 200. Les pages de boutique ont récupéré
leur champ Source et la section licence dit maintenant lisible mais pas open source, ce qui est le
cas : le code se lit, la licence du jar est ce qui gouverne.

La base de paquet est passée de `com.dreykaoas` à `oas.dreyka`, ce que le gabarit demande. 102
fichiers, les deux listes de points d'entrée, le paquet des mixins et `maven_group`. Le namespace de
ressources reste `deepcrate`, donc aucun identifiant écrit dans une sauvegarde ne bouge. C'était la
dernière fenêtre : après une publication, le même renommage coûte une recompilation à chaque addon.

## Le site

`web/` ne tenait qu'un README pendant que le mod avait déjà toutes ses mécaniques. Il porte
maintenant une page d'accueil, huit pages de guide, les deux arbres de langue au complet, et six
vérifications sous `node` sans rien à installer. Les sept images viennent de `runClientGameTest` :
une mécanique que personne n'a vue tourner n'a pas de bande sur l'accueil.

La forme vient du site de LethalBreed, lu avant d'écrire une ligne : barre avec la marque à gauche
et les bascules à droite, hero avec pastille, titre, chapeau et deux boutons, sections ouvertes par
une étiquette, guide en `container wiki-layout` avec fil d'Ariane. Ce qui ne vient pas de lui, et ne
devait pas venir de lui : la palette passe au bleu-vert et à l'améthyste au lieu de l'ambre et de la
rouille, les trois polices changent, le glyphe est un coffre, et les bandes de l'accueil alternent
image et texte plutôt que d'empiler des blocs à mécanique propre.

Le test client a gagné une prise pour la trémie, qui n'en avait aucune. Une case à 96 sur un coffre
de fer, ce que le plafond du jeu aurait refusé.

Le site est en ligne sur `https://deepcrate.pages.dev`. Le projet Cloudflare Pages n'existait pas et
a été créé sur demande, la branche de production étant `modularite`. Pages retire le `.html` de
chaque adresse et redirige en 308 vers la forme courte, exactement comme pour LethalBreed : c'est le
comportement de la plateforme, pas un défaut du site.

## Ce qui reste

Rien du plan de l'audit. Ce qui traîne est dans `DEFAUTS-CONNUS.md`. Une de ces lignes ne regarde pas
le code : le `LICENSE` renvoie deux fois au site d'un autre mod, et le canal de signalement qu'on y
trouve a `lethalbreed` câblé en dur.

Restent aussi les six découpes de `docs/superpowers/specs/2026-09-05-decoupes-restantes.md`, dont un
adversaire a montré qu'au moins une, `MenuModules`, ne tient pas telle qu'elle est dessinée.

## Une chose à savoir avant de rouvrir ce dépôt

Une deuxième session a écrit dans `client/screen/` ce matin pendant que celle-ci y travaillait, sur
exactement la même tâche : trois classes du même nom, à un dossier près, et un champ ajouté par
dessus. Ça compilait par accident, parce que sa classe était dans le paquet de l'écran et ne
demandait pas d'import. Elle a été arrêtée et son travail retiré. Deux sessions sur le même fichier
finissent par le corrompre, et rien dans l'outillage ne prévient.
