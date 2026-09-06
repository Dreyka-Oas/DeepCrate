# Remise à niveau de DeepCrate sur le gabarit, plan d'exécution

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** ramener DeepCrate sur ce que `mod-blueprint` décrit, en réglant ce que l'audit du 5 septembre 2026 a mesuré : le travail en cours non posé, le code mort, trois duplications, six fichiers au-dessus du plafond, l'identité incomplète, l'absence de fichier de réglages et le dernier mur encore debout.

**Architecture:** l'ordre suit la règle du gabarit, fusionner avant découper. Le travail non commité part d'abord, sinon toute la suite s'empile dessus. Viennent le code mort et les résidus, puis les fusions, puis les découpes, qui sont des refactorisations pures : même comportement observable, tests relancés après chaque déplacement. L'identité, la boutique et la couche de réglages sont des ajouts sans effet sur le code existant. Le mur 5 et le README ferment le chantier de modularité entamé le 3 septembre.

**Tech Stack:** Fabric loader 0.19.3, fabric-api 0.141.4+1.21.11, fabric-loom 1.17.12, Gradle 9.5.1, Java 21, mappings officielles Mojang. JUnit 5 pour les règles pures, gametest pour ce qui a besoin d'un serveur, gametest client pour les captures.

## Global Constraints

Le répertoire de travail est `/run/media/dreykaoas/O.A.S/projects/mods/DeepCrate`, et toutes les commandes Gradle se lancent depuis `mod/`.

La branche courante est `modularite`. Ce plan ne la quitte pas et n'en ouvre pas d'autre.

Aucune ligne d'attribution dans un message de commit, aucun trailer, aucune mention d'outil. Les messages de commit sont en anglais, au format Conventional Commits, et suivent le ton de ceux déjà posés : une phrase qui dit ce qui change pour le lecteur, pas un gabarit.

Les commentaires de code sont en anglais et disent pourquoi, jamais quoi. Les identifiants sont en anglais. Le français reste dans `fr_fr.json`, dans ce fichier, dans `PLAN.md` et dans les fiches de `docs/`.

Tout texte qu'un joueur lit passe par `Component.translatable` et existe dans `en_us.json` et `fr_fr.json` avec la même clé et les mêmes marqueurs de format, écrits dans le même commit.

Un fichier Java tient sous 150 lignes, un dossier sous 8 fichiers. Les dossiers de ressources imposés par le format du jeu, un fichier par objet ou par recette, ne comptent pas.

Une classe de gametest ne tourne que si elle est nommée dans `mod/src/gametest/resources/fabric.mod.json`. Une classe ajoutée sans cette ligne ne s'exécute jamais et le test passe au vert sans rien avoir vérifié.

Le monde de test est toujours paisible et créatif.

Avant tout build ou lancement de client, vérifier `free -h` ; sous 2 Go disponibles, attendre plutôt qu'empiler. Tout serveur ou client lancé pendant une tâche est fermé à la fin de cette tâche. Pour voir le jeu tourner, `mod/scripts/headless-test.sh` ou `./gradlew runClientGameTest`, jamais `./gradlew runClient`, qui ouvre une vraie fenêtre sur le vrai écran.

Un renommage qui touche une clé de configuration, un identifiant NBT, un champ de Codec ou une clé de traduction est un nom déjà écrit sur le disque de quelqu'un. Il ne s'applique qu'avec un alias de compatibilité déjà en place.

## Structure des fichiers

Fichiers créés :

`mod/src/main/java/oas/dreyka/deepcrate/api/TagMatch.java` porte l'essai d'appartenance à une étiquette, aujourd'hui recopié trois fois.
`mod/src/main/java/oas/dreyka/deepcrate/api/Registries.java` porte l'inscription refusant les doublons, aujourd'hui recopiée quatre fois.
`mod/src/main/java/oas/dreyka/deepcrate/block/CrateDrops.java` porte le largage d'objets entiers, aujourd'hui recopié deux fois.
`mod/src/main/java/oas/dreyka/deepcrate/block/CrateModuleHolder.java` sort de `DeepCrateBlockEntity` tout ce qui touche aux cases et au partage d'une paire.
`mod/src/main/java/oas/dreyka/deepcrate/block/CrateLid.java` sort de `DeepCrateBlockEntity` le couvercle, les ouvreurs et le son.
`mod/src/main/java/oas/dreyka/deepcrate/block/CrateSave.java` sort de `DeepCrateBlockEntity` la lecture et l'écriture de la sauvegarde.
`mod/src/main/java/oas/dreyka/deepcrate/client/CratePanel.java` sort de `DeepCrateScreen` la peinture du panneau et ses constantes de texture.
`mod/src/main/java/oas/dreyka/deepcrate/client/CrateSearch.java` sort de `DeepCrateScreen` la recherche et le grisage.
`mod/src/main/java/oas/dreyka/deepcrate/inventory/CratePanelGeometry.java` sort de `DeepCrateMenu` les constantes de mise en page et les deux calculs de position.
`mod/src/main/java/oas/dreyka/deepcrate/client/CrateScreenArea.java`, `CrateScreenCallback.java` et `CrateTooltipCallback.java` sont la greffe sur l'écran, dont le code complet est écrit dans `docs/superpowers/plans/2026-09-03-modularite.md` à partir de la ligne 1813.
`mod/src/main/java/oas/dreyka/deepcrate/config/` porte la couche de réglages, sur le découpage de `LethalBreed`.
`mod/src/main/resources/assets/deepcrate/icon.png`, 128 par 128.
`store/curseforge.md` et `store/modrinth.md`.
`.gitattributes` à la racine du produit.

Fichiers modifiés en profondeur : `DeepCrateBlockEntity.java` passe de 502 à moins de 150 lignes, `DeepCrateScreen.java` de 453 à moins de 150, `DeepCrateMenu.java` de 426 à moins de 200.

---

### Task 1: poser le travail en cours

Le bouton de page qui porte un point vert est écrit, testé par la capture, et il n'est ni commité ni suivi. `DeepCrateScreen.java` est modifié et référence `PageButton`, donc un retour en arrière sur le seul fichier suivi casserait la compilation. Rien d'autre ne peut commencer avant que ça parte.

**Files:**
- Add: `mod/src/main/java/oas/dreyka/deepcrate/client/PageButton.java`
- Modify: `mod/src/main/java/oas/dreyka/deepcrate/client/DeepCrateScreen.java`
- Modify: `mod/src/gametest/java/oas/dreyka/deepcrate/gametest/CrateLookClientGameTest.java`
- Modify: dix-huit fichiers sous `mod/src/main/resources/assets/deepcrate/textures/entity/chest/`

**Interfaces:**
- Produces: `PageButton.SIZE` valant 16, et le constructeur `PageButton(int x, int y, Component, BooleanSupplier holdsItems, Runnable onOpen)`. La suite du plan lit `SIZE` depuis là.

- [ ] **Step 1: relire ce qui va partir**

```bash
cd /run/media/dreykaoas/O.A.S/projects/mods/DeepCrate
git status --porcelain=v1 -uall
git diff -- '*.java'
```

Attendu : deux fichiers Java modifiés, dix-huit PNG modifiés, `PageButton.java` non suivi. Rien d'autre.

- [ ] **Step 2: vérifier que tout compile et passe**

```bash
cd mod && ./gradlew build check --console=plain --max-workers=2
```

Attendu : `BUILD SUCCESSFUL`.

- [ ] **Step 3: séparer en trois commits par intention**

Les textures ne sont pas le bouton, et la scène du test client n'est pas non plus le bouton. Trois intentions, trois commits.

```bash
cd /run/media/dreykaoas/O.A.S/projects/mods/DeepCrate
git add mod/src/main/resources/assets/deepcrate/textures/entity/chest/
git commit -m "fix(textures): the material now reads the same on all six crates"

git add mod/src/main/java/oas/dreyka/deepcrate/client/PageButton.java mod/src/main/java/oas/dreyka/deepcrate/client/DeepCrateScreen.java
git commit -m "feat(screen): a page button carries a dot while its page holds something"

git add mod/src/gametest/java/oas/dreyka/deepcrate/gametest/CrateLookClientGameTest.java
git commit -m "test(client): fill two far pages so one shot carries a button in both states"
```

Le message des textures doit dire ce qui a vraiment changé sur elles. Lire `git diff --stat` du premier commit et écrire d'après ce qu'on voit, pas d'après cette ligne.

- [ ] **Step 4: vérifier que l'arbre est propre**

```bash
git status --porcelain=v1 -uall
```

Attendu : sortie vide.

---

### Task 2: le code mort et les résidus

Deux constantes déclarées et jamais lues, un import jamais utilisé, un accesseur sans appelant, une javadoc posée sur la mauvaise méthode, trois noms pleinement qualifiés écrits en ligne là où tous les fichiers voisins importent, deux chaînes `"deepcrate"` écrites en dur là où `RegistryInit.id` existe, et deux lignes vides de trop.

**Files:**
- Modify: `mod/src/main/java/oas/dreyka/deepcrate/client/DeepCrateScreen.java:48-49`
- Modify: `mod/src/main/java/oas/dreyka/deepcrate/block/DeepCrateBlockEntity.java:45`
- Modify: `mod/src/main/java/oas/dreyka/deepcrate/inventory/ModuleSlot.java:18-20`
- Modify: `mod/src/main/java/oas/dreyka/deepcrate/inventory/DeepCrateMenu.java:392-398`
- Modify: `mod/src/main/java/oas/dreyka/deepcrate/init/CreativeTabInit.java:26,32`
- Modify: `mod/src/main/java/oas/dreyka/deepcrate/api/DeepCrateApi.java:36`
- Modify: `mod/src/main/java/oas/dreyka/deepcrate/net/CrateSortPayload.java:30`
- Modify: `mod/src/main/java/oas/dreyka/deepcrate/inventory/CrateStorage.java:270-271`
- Modify: `mod/src/main/java/oas/dreyka/deepcrate/mixin/HopperBlockEntityMixin.java:75`

**Interfaces:**
- Consumes: `RegistryInit.id(String)` de la tâche zéro, qui existe déjà.
- Produces: rien de nouveau. Aucune signature ne change.

- [ ] **Step 1: retirer les deux constantes mortes**

Dans `DeepCrateScreen.java`, supprimer les deux lignes :

```java
    private static final int SLOT_FRAME_U = 7;
    private static final int SLOT_FRAME_V = 17;
```

Le script de nommage les a trouvées. Vérifier qu'elles ne sont lues nulle part avant de couper :

```bash
cd /run/media/dreykaoas/O.A.S/projects/mods/DeepCrate
grep -rn 'SLOT_FRAME' mod/src
```

Attendu : les deux déclarations et rien d'autre. Si une lecture apparaît, ne pas couper et le dire.

- [ ] **Step 2: retirer l'import mort et l'accesseur sans appelant**

Dans `DeepCrateBlockEntity.java`, supprimer la ligne 45, `import org.jspecify.annotations.Nullable;`. Rien dans le fichier ne porte cette annotation. Vérifier avant de couper :

```bash
grep -n 'Nullable' mod/src/main/java/oas/dreyka/deepcrate/block/DeepCrateBlockEntity.java
```

Attendu : la seule ligne d'import. Le compilateur ne le signale pas, il n'y a pas de `-Xlint` dans le build.

Dans `ModuleSlot.java`, supprimer :

```java
    public CrateModuleSlot kind() {
        return this.kind;
    }
```

Les trois redéfinitions de la classe lisent le champ privé directement. `ModuleSlot` ne vit pas sous `api/`, donc ce n'est pas une surface publiée qu'un addon compile contre. Vérifier :

```bash
grep -rn '\.kind()' mod/src
```

Attendu : rien.

- [ ] **Step 3: remettre la javadoc sur sa méthode**

Dans `DeepCrateMenu.java`, le bloc qui parle de rouvrir le coffre est posé sur `moduleStacks`, qui ne rouvre rien, et `reopenIfRowCountChanged` neuf lignes plus bas n'a rien. Déplacer le premier bloc sur la seconde méthode :

```java
    /** The cells as a plain list, which is what the two api helpers walk. */
    private List<ItemStack> moduleStacks() {
        List<ItemStack> stacks = new ArrayList<>(this.moduleContainer.getContainerSize());
        for (int i = 0; i < this.moduleContainer.getContainerSize(); i++) {
            stacks.add(this.moduleContainer.getItem(i));
        }

        return stacks;
    }

    /**
     * A row module changes how many slots the screen has, and a menu's slot list is fixed once it is
     * built. So the crate is opened again, at the start of the next tick rather than inside the click
     * that caused it: closing a menu mid-click would put whatever the player is carrying on the
     * ground.
     */
    private void reopenIfRowCountChanged() {
```

- [ ] **Step 4: importer au lieu de qualifier en ligne**

Dans `CreativeTabInit.java`, ajouter aux imports :

```java
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
```

et écrire les deux boucles avec les noms courts :

```java
                for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(crateModule.items())) {
```

Dans `DeepCrateApi.java`, ajouter `import net.fabricmc.loader.api.FabricLoader;` et écrire :

```java
    public static final boolean AUTOMATION_LIMITED = FabricLoader.getInstance().isModLoaded("lithium");
```

- [ ] **Step 5: passer les deux identifiants par RegistryInit.id**

Dans `DeepCrateScreen.java`, remplacer :

```java
    private static final Identifier MODULE_TAB = Identifier.fromNamespaceAndPath("deepcrate", "textures/gui/module_tab.png");
```

par :

```java
    private static final Identifier MODULE_TAB = RegistryInit.id("textures/gui/module_tab.png");
```

et ajouter `import oas.dreyka.deepcrate.init.RegistryInit;`. Faire de même dans `CrateSortPayload.java` pour `Identifier.fromNamespaceAndPath("deepcrate", "sort")`, qui devient `RegistryInit.id("sort")`.

Le fichier ne change pas de valeur : `RegistryInit.id` construit exactement le même identifiant à partir de `DeepCrate.MOD_ID`. L'identifiant réseau `deepcrate:sort` reste le même octet pour octet, donc rien ne casse entre un client et un serveur de versions différentes.

- [ ] **Step 6: les deux lignes vides**

Dans `CrateStorage.java`, la méthode `splitForVanilla` a deux lignes vides avant son `return`. En laisser une. Dans `HopperBlockEntityMixin.java`, retirer la ligne vide qui précède l'accolade fermante de la classe.

- [ ] **Step 7: relancer et vérifier**

```bash
cd mod && ./gradlew build check --console=plain --max-workers=2
node ../../../.claude/skills/naming-audit/scripts/check-names.mjs ../
```

Attendu : `BUILD SUCCESSFUL`, et le script de nommage ne mentionne plus `SLOT_FRAME_U` ni `SLOT_FRAME_V`.

- [ ] **Step 8: commit**

```bash
cd /run/media/dreykaoas/O.A.S/projects/mods/DeepCrate
git add mod/src/main/java
git commit -m "refactor: drop two unread constants and fix a javadoc pointing at the wrong method"
```

---

### Task 3: fusionner les trois blocs recopiés

Le gabarit demande de fusionner avant de découper : une découpe faite d'abord éparpille la duplication dans deux fichiers de plus. Trois groupes, mesurés.

Un quatrième existe et n'est pas traité ici : `isOverSortButtons` et `isOverPageButtons`, dans `DeepCrateScreen.java:197-215`, parcourent chacune leur liste de boutons pour la même réponse. La tâche 8 les rassemble avec la troisième zone dans une seule liste de `CrateScreenArea`, ce qui les fait disparaître. Les fusionner ici les ferait fusionner deux fois.

**Files:**
- Create: `mod/src/main/java/oas/dreyka/deepcrate/api/TagMatch.java`
- Create: `mod/src/main/java/oas/dreyka/deepcrate/api/Registries.java`
- Create: `mod/src/main/java/oas/dreyka/deepcrate/block/CrateDrops.java`
- Modify: `mod/src/main/java/oas/dreyka/deepcrate/api/CrateModule.java:22-34`
- Modify: `mod/src/main/java/oas/dreyka/deepcrate/api/RowModule.java:25-36`
- Modify: `mod/src/main/java/oas/dreyka/deepcrate/api/CrateModuleSlot.java:33-42`
- Modify: `mod/src/main/java/oas/dreyka/deepcrate/api/DeepCrateApi.java:64-107`
- Modify: `mod/src/main/java/oas/dreyka/deepcrate/client/DeepCrateClientApi.java:25-35`
- Modify: `mod/src/main/java/oas/dreyka/deepcrate/block/DeepCrateBlockEntity.java:386-392`
- Modify: `mod/src/main/java/oas/dreyka/deepcrate/inventory/DeepCrateMenu.java:363-372`
- Test: `mod/src/test/java/oas/dreyka/deepcrate/CrateModulesTest.java`

**Interfaces:**
- Produces: `TagMatch.matches(ItemStack, TagKey<Item>)` rendant un `boolean`. `Registries.addUnique(List<T>, T, Function<T, Identifier>, String kind)` rendant le `T` passé. `CrateDrops.dropWhole(Level, BlockPos, double yOffset, ItemStack)` sans retour.
- Consumes: rien des tâches précédentes.

- [ ] **Step 1: le test qui échoue sur TagMatch**

Ajouter dans `mod/src/test/java/oas/dreyka/deepcrate/CrateModulesTest.java` :

```java
    @Test
    void tagMatchSaysNoForAnEmptyStack() {
        assertFalse(TagMatch.matches(ItemStack.EMPTY, TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("deepcrate", "module_512"))));
    }
```

avec les imports qu'il faut. Un test unitaire ne peut pas lier une étiquette, donc c'est le cas de la pile vide qui est vérifiable ici sans monde ; le cas où l'étiquette n'est pas encore liée est déjà couvert par les gametests.

- [ ] **Step 2: le voir échouer**

```bash
cd mod && ./gradlew test --rerun --console=plain --max-workers=2
```

Attendu : échec de compilation, `TagMatch` n'existe pas.

- [ ] **Step 3: écrire TagMatch**

```java
package oas.dreyka.deepcrate.api;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Whether a stack carries an item tag, asked from three places that all have to survive an unbound tag. */
public final class TagMatch {
    private TagMatch() {}

    public static boolean matches(ItemStack itemStack, TagKey<Item> items) {
        if (itemStack.isEmpty()) {
            return false;
        }

        try {
            return itemStack.is(items);
        } catch (IllegalStateException e) {
            // Tags bind when a world loads. A creative tab build or an addon's own registration asks
            // earlier than that, and gets "no" rather than a crash.
            return false;
        }
    }
}
```

- [ ] **Step 4: router les trois appelants**

Dans `CrateModule.java`, le corps de `matches` devient :

```java
    public boolean matches(ItemStack itemStack) {
        return TagMatch.matches(itemStack, this.items);
    }
```

Même corps dans `RowModule.java`. Dans `CrateModuleSlot.java`, `tagged` devient :

```java
    /** The common case: a cell that takes whatever an item tag names, with no Java on the other side. */
    public static Predicate<ItemStack> tagged(TagKey<Item> items) {
        return itemStack -> TagMatch.matches(itemStack, items);
    }
```

Retirer les imports devenus inutiles dans les trois fichiers.

- [ ] **Step 5: vérifier**

```bash
cd mod && ./gradlew build check --console=plain --max-workers=2
```

Attendu : `BUILD SUCCESSFUL`, 37 tests JUnit verts.

- [ ] **Step 6: commit**

```bash
cd /run/media/dreykaoas/O.A.S/projects/mods/DeepCrate
git add mod/src
git commit -m "refactor(api): one place answers whether a stack carries a tag, instead of three"
```

- [ ] **Step 7: écrire Registries**

Quatre inscriptions font la même boucle de recherche de doublon avec un mot différent dans le message. Une seule suffit.

```java
package oas.dreyka.deepcrate.api;

import java.util.List;
import java.util.function.Function;
import net.minecraft.resources.Identifier;

/** Adds to a registry list and refuses a name already taken, which is what four registrations do. */
public final class Registries {
    private Registries() {}

    /** @param kind what to call the thing in the refusal, so the message names what the caller tried to add */
    public static <T> T addUnique(List<T> list, T entry, Function<T, Identifier> id, String kind) {
        Identifier identifier = id.apply(entry);
        for (T existing : list) {
            if (id.apply(existing).equals(identifier)) {
                throw new IllegalStateException(kind + " " + identifier + " registered twice");
            }
        }

        list.add(entry);
        return entry;
    }
}
```

- [ ] **Step 8: router les quatre inscriptions**

Dans `DeepCrateApi.java`, les trois qui bouclent :

```java
    public static CrateModule registerModule(CrateModule crateModule) {
        if (crateModule.capacity() > MAX_CAPACITY) {
            throw new IllegalArgumentException(
                "Crate module " + crateModule.id() + " asks for " + crateModule.capacity() + ", above the " + MAX_CAPACITY + " ceiling"
            );
        }

        Registries.addUnique(MODULES, crateModule, CrateModule::id, "Crate module");
        // Highest capacity first, so a stack matching two tags gets the better of the two.
        MODULES.sort((a, b) -> Integer.compare(b.capacity(), a.capacity()));
        DeepCrate.LOGGER.info("[DeepCrate] module {}: {} per slot", crateModule.id(), crateModule.capacity());
        return crateModule;
    }

    public static RowModule registerRowModule(RowModule rowModule) {
        Registries.addUnique(ROW_MODULES, rowModule, RowModule::id, "Row module");
        DeepCrate.LOGGER.info("[DeepCrate] row module {}: {} rows each", rowModule.id(), rowModule.rows());
        return rowModule;
    }

    public static CrateModuleSlot registerModuleSlot(CrateModuleSlot crateModuleSlot) {
        Registries.addUnique(MODULE_SLOTS, crateModuleSlot, CrateModuleSlot::id, "Module slot");
        MODULE_SLOTS.sort(Comparator.comparingInt(CrateModuleSlot::order));
        DeepCrate.LOGGER.info("[DeepCrate] module slot {}: up to {} at a time", crateModuleSlot.id(), crateModuleSlot.stackLimit());
        return crateModuleSlot;
    }
```

`registerTier` garde sa forme : elle écrit dans deux tables et détecte le doublon par la valeur rendue par `put`, ce qui n'est pas la même mécanique.

Dans `DeepCrateClientApi.java` :

```java
    public static CrateSortOrder registerSortOrder(CrateSortOrder crateSortOrder) {
        Registries.addUnique(SORT_ORDERS, crateSortOrder, CrateSortOrder::id, "Sort order");
        SORT_ORDERS.sort(Comparator.comparingInt(CrateSortOrder::order));
        return crateSortOrder;
    }
```

avec `import oas.dreyka.deepcrate.api.Registries;`.

Les messages d'exception gardent leur texte mot pour mot. Un gametest existant peut les lire.

- [ ] **Step 9: vérifier et commiter**

```bash
cd mod && ./gradlew build check --console=plain --max-workers=2
cd .. && git add mod/src && git commit -m "refactor(api): one registration helper refuses a name twice, instead of four copies"
```

- [ ] **Step 10: écrire CrateDrops et router ses deux appelants**

Les deux largages ne diffèrent que par la hauteur, un demi-bloc contre un bloc entier.

```java
package oas.dreyka.deepcrate.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Puts a stack on the ground whole.
 *
 * Containers.dropItemStack cuts each stack into ten to thirty pieces. A full double echo crate losing
 * its module spills two thousand stacks, which through that helper is nearer seven thousand entities
 * in one tick.
 */
public final class CrateDrops {
    private CrateDrops() {}

    public static void dropWhole(Level level, BlockPos blockPos, double yOffset, ItemStack itemStack) {
        ItemEntity itemEntity = new ItemEntity(level, blockPos.getX() + 0.5, blockPos.getY() + yOffset, blockPos.getZ() + 0.5, itemStack);
        itemEntity.setDefaultPickUpDelay();
        level.addFreshEntity(itemEntity);
    }
}
```

Dans `DeepCrateBlockEntity.preRemoveSideEffects`, la boucle devient :

```java
        for (ItemStack itemStack : list) {
            CrateDrops.dropWhole(this.level, blockPos, 0.5, itemStack);
        }
```

Dans `DeepCrateMenu`, supprimer `dropWholeStack` et appeler `CrateDrops.dropWhole(player.level(), deepCrateBlockEntity.getBlockPos(), 1.0, itemStack)`. Les deux hauteurs restent celles d'aujourd'hui : un coffre cassé lâche à sa propre hauteur, un module retiré lâche au-dessus du coffre qui est toujours là.

- [ ] **Step 11: vérifier et commiter**

```bash
cd mod && ./gradlew build check --console=plain --max-workers=2
cd .. && git add mod/src && git commit -m "refactor(block): both spills go through one drop, the height being what differs"
```

---

### Task 4: découper les six fichiers au-dessus du plafond

Six fichiers de `main/java` dépassent 150 lignes, et le plus gros en fait 502. La découpe se fait par responsabilité, jamais à la ligne 150. Après la tâche 3, les blocs qui se ressemblaient ont fondu, donc rien ne se recopie ici.

C'est une refactorisation pure. Le comportement observable ne change pas et la suite complète tourne après chaque déplacement, pas seulement à la fin.

**Files:**
- Create: `mod/src/main/java/oas/dreyka/deepcrate/block/CrateModuleHolder.java`
- Create: `mod/src/main/java/oas/dreyka/deepcrate/block/CrateLid.java`
- Create: `mod/src/main/java/oas/dreyka/deepcrate/block/CrateSave.java`
- Create: `mod/src/main/java/oas/dreyka/deepcrate/client/CratePanel.java`
- Create: `mod/src/main/java/oas/dreyka/deepcrate/client/CrateSearch.java`
- Create: `mod/src/main/java/oas/dreyka/deepcrate/inventory/CratePanelGeometry.java`
- Modify: `mod/src/main/java/oas/dreyka/deepcrate/block/DeepCrateBlockEntity.java`
- Modify: `mod/src/main/java/oas/dreyka/deepcrate/client/DeepCrateScreen.java`
- Modify: `mod/src/main/java/oas/dreyka/deepcrate/inventory/DeepCrateMenu.java`

**Interfaces:**
- Produces: `CratePanelGeometry.PANEL_BORDER`, `CELL`, `MIN_PANEL_WIDTH`, `COLUMNS_OF_A_PLAYER`, `GRID_LEFT`, `GRID_TOP`, `MODULE_X`, `MODULE_Y`, `MODULE_SPACING`, plus `panelWidth(int columns)` et `gridLeft(int panelWidth, int cells)`. `DeepCrateMenu` garde des délégations vers ces noms, parce que `DeepCrateScreen` et les gametests les lisent sous `DeepCrateMenu.*`.
- Consumes: `CrateDrops.dropWhole` de la tâche 3.

- [ ] **Step 1: prendre l'empreinte avant de bouger**

Une découpe qui change une capture est une découpe qui a changé un comportement. La capture d'avant est la preuve.

```bash
cd mod && ./gradlew runClientGameTest --console=plain --max-workers=2
cp -r build/run/clientGameTest/screenshots /tmp/opencode/deepcrate-avant
```

- [ ] **Step 2: sortir la géométrie du menu**

Créer `CratePanelGeometry.java` avec les neuf constantes et les deux méthodes statiques copiées mot pour mot depuis `DeepCrateMenu.java:36-53,181-191`. Dans `DeepCrateMenu`, les remplacer par des délégations :

```java
    public static final int PANEL_BORDER = CratePanelGeometry.PANEL_BORDER;
    public static final int CELL = CratePanelGeometry.CELL;
    public static final int MIN_PANEL_WIDTH = CratePanelGeometry.MIN_PANEL_WIDTH;
    public static final int COLUMNS_OF_A_PLAYER = CratePanelGeometry.COLUMNS_OF_A_PLAYER;
    public static final int GRID_LEFT = CratePanelGeometry.GRID_LEFT;
    public static final int GRID_TOP = CratePanelGeometry.GRID_TOP;
    public static final int MODULE_X = CratePanelGeometry.MODULE_X;
    public static final int MODULE_Y = CratePanelGeometry.MODULE_Y;
    public static final int MODULE_SPACING = CratePanelGeometry.MODULE_SPACING;

    public static int panelWidth(int columns) {
        return CratePanelGeometry.panelWidth(columns);
    }

    public static int gridLeft(int panelWidth, int cells) {
        return CratePanelGeometry.gridLeft(panelWidth, cells);
    }
```

Les délégations restent parce que `DeepCrateScreen` et les gametests lisent ces noms sous `DeepCrateMenu`. Les déplacer aussi ferait une deuxième découpe dans le même commit, et une capture qui bouge ne dirait plus laquelle des deux est en cause.

- [ ] **Step 3: relancer la suite et comparer les captures**

```bash
cd mod && ./gradlew build check runClientGameTest --console=plain --max-workers=2
for f in build/run/clientGameTest/screenshots/*.png; do
  cmp -s "$f" "/tmp/opencode/deepcrate-avant/$(basename "$f")" || echo "DIFFERENT: $f"
done
```

Attendu : `BUILD SUCCESSFUL` et aucune ligne `DIFFERENT`. Une capture qui bouge veut dire que la découpe a changé quelque chose ; revenir en arrière et chercher quoi avant de continuer.

- [ ] **Step 4: commit**

```bash
cd .. && git add mod/src && git commit -m "refactor(menu): the panel measurements live on their own, away from the slot logic"
```

- [ ] **Step 5: sortir la peinture du panneau**

Créer `CratePanel.java` portant `BACKGROUND`, `PANEL_FOOT`, `PANEL_FOOT_V`, `BARE_PANEL_V`, `BARE_PANEL_HEIGHT`, `PLAYER_PANEL_V`, `PLAYER_PANEL_HEIGHT`, `HEADER_HEIGHT`, les sept constantes de la languette, et les méthodes `blitBand`, `fillBare`, `fillBarePanel`, `renderModuleTab`, `blitTab`, `blit` de `DeepCrateScreen.java:282-328,432-444`. La largeur d'image, qui était lue sur le champ `this.imageWidth`, devient un paramètre.

`DeepCrateScreen.renderBg` appelle alors `CratePanel` et ne fait plus que décider quoi peindre.

- [ ] **Step 6: relancer, comparer, commiter**

Mêmes commandes qu'à l'étape 3. Le panneau est ce que la capture montre le plus, donc une différence ici se voit immédiatement.

```bash
cd .. && git add mod/src && git commit -m "refactor(screen): the panel is painted by its own class, the screen decides what to paint"
```

- [ ] **Step 7: sortir la recherche**

Créer `CrateSearch.java` portant le champ `query`, `onQueryChanged`, `dims` et `SEARCH_HEIGHT`, avec la construction de la `SearchBox`. `DeepCrateScreen` lui demande si un emplacement est grisé.

Relancer et comparer comme à l'étape 3, puis :

```bash
cd .. && git add mod/src && git commit -m "refactor(screen): search and dimming move out of the screen class"
```

- [ ] **Step 8: découper le block entity en trois**

Dans l'ordre, une découpe et un cycle de tests chacune.

`CrateSave.java` prend les corps de `saveAdditional`, `loadAdditional`, `removeComponentsFromTag` et `collectImplicitComponents`. Les méthodes restent des redéfinitions dans `DeepCrateBlockEntity`, elles délèguent leur corps.

`CrateLid.java` prend le `ChestLidController`, le `ContainerOpenersCounter` anonyme, `startOpen`, `stopOpen`, `getEntitiesWithContainerOpen`, `recheckOpen`, `lidAnimateTick`, `triggerEvent`, `getOpenNess` et `playSound`.

`CrateModuleHolder.java` prend `module`, `setModule`, `rowModules`, `setRowModules`, `setModuleIn`, `extraRows`, `trimToRows`, `moduleHolder`, `hasAnyModule` et `alignCapacityWithHolder`.

Après chacune : `./gradlew build check runClientGameTest`, comparaison des captures, un commit.

- [ ] **Step 9: mesurer**

```bash
cd mod && wc -l $(find src/main/java -name '*.java') | sort -rn | head -12
```

Attendu : aucun fichier de `main/java` au-dessus de 150 lignes. Si un reste au-dessus, dire lequel et pourquoi la découpe suivante n'a pas été faite, plutôt que de couper à la ligne.

- [ ] **Step 10: mesurer les dossiers**

`api/`, `client/` et `inventory/` étaient déjà à 10, 11 et 11 fichiers avant cette tâche, et elle en ajoute. Compter :

```bash
cd mod/src/main/java/oas/dreyka/deepcrate && for d in */; do echo "$(ls "$d" | wc -l)  $d"; done
```

Un dossier au-dessus de huit se sous-découpe par domaine, pas par lettre : `client/screen/` pour l'écran et ses morceaux, `client/render/` pour le renderer et les matériaux, `client/sort/` pour le registre d'ordres et ses boutons. Un commit par sous-découpe, la suite relancée à chaque fois, parce qu'un déplacement de paquet touche tous les imports.

---

### Task 5: l'identité du mod

Le mod n'a pas d'icône, donc le lanceur dessine un carré vide dans la liste. `fabric.mod.json` n'a pas de bloc `contact`. Le produit n'a pas de `.gitattributes`, que le mod voisin a.

**Files:**
- Create: `mod/src/main/resources/assets/deepcrate/icon.png`
- Create: `.gitattributes`
- Modify: `mod/src/main/resources/fabric.mod.json`

**Interfaces:**
- Produces: rien de programmatique.

- [ ] **Step 1: dessiner l'icône**

128 par 128 pixels, PNG. Une seule forme, deux ou trois couleurs, fort contraste, lisible à 32 pixels puisque c'est la taille où elle est réellement dessinée. Le sujet du mod plutôt qu'une boîte générique : un coffre vu de face avec sa profondeur marquée, ou la languette de module qui pend sur le côté, qui est ce que le mod ajoute à un coffre.

Ce qui rate à cette taille : du texte, une capture d'écran réduite, du détail fin, un dégradé qui tourne à la bouillie, et la même image que l'en-tête du site sans redessin.

`mod/scripts/make_chest_textures.py` montre comment les textures de coffre du mod sont produites et donne les couleurs de chaque palier.

- [ ] **Step 2: la déclarer**

Dans `fabric.mod.json`, après la ligne `license` :

```json
  "contact": {
    "homepage": "https://deepcrate.pages.dev",
    "issues": "https://github.oas/dreyka/DeepCrate/issues"
  },
  "icon": "assets/deepcrate/icon.png",
```

Vérifier les deux adresses avant de les écrire. Le dépôt distant se lit avec `git remote -v` ; si le site n'est pas déployé, ne pas écrire `homepage` du tout plutôt qu'inventer une adresse qui ne répond pas.

- [ ] **Step 3: le .gitattributes**

Une ligne, la même que chez `LethalBreed` :

```
* -text
```

Le dépôt est aujourd'hui entièrement en LF, vérifié. Cette ligne empêche qu'un jour git réécrive les fins de ligne au passage et produise un diff de tout le dépôt.

- [ ] **Step 4: vérifier que l'icône voyage dans le jar**

```bash
cd mod && ./gradlew build --console=plain --max-workers=2
unzip -l build/libs/deepcrate-1.0.0.jar | grep -i icon
```

Attendu : une ligne `assets/deepcrate/icon.png`. Aujourd'hui la commande ne rend rien.

- [ ] **Step 5: commit**

```bash
cd .. && git add .gitattributes mod/src/main/resources
git commit -m "feat(meta): the mod list shows a crate instead of a blank square"
```

---

### Task 6: les deux pages de boutique

`store/` n'existe pas. Les deux fichiers portent un seul corps, et ils sont moins chers à écrire pendant que le mod l'est qu'un jour de sortie.

**Files:**
- Create: `store/curseforge.md`
- Create: `store/modrinth.md`

**Interfaces:**
- Consumes: la description de `fabric.mod.json` et le corps du `README.md`, qui doivent dire la même chose.

- [ ] **Step 1: écrire le corps commun**

En anglais. L'ordre qui marche : le nom, une ligne en gras qui dit la chose d'un souffle, une ligne disant où le mod est requis, puis l'incompatibilité, avant tout le reste, puisque c'est ce qui fait rater une installation. Ici c'est lithium, qui coupe le remplissage par tuyaux, et le README l'explique déjà avec la mesure derrière.

Ensuite une section par mécanique, écrite comme ce qui arrive au joueur : les six paliers, la contenance et ce qu'un module retiré fait tomber, les rangées, le tri, les pages, les tuyaux. Puis la licence, la réponse sur les modpacks, et le lien du site.

Écrire pour quelqu'un qui décide en quinze secondes. Deuxième personne, présent, chiffres réels. Le README contient déjà tous ces chiffres.

- [ ] **Step 2: le bloc de champs de formulaire**

Chaque fichier ouvre sur un bloc de commentaires HTML, qui ne rend rien et se colle donc tel quel dans l'éditeur de la boutique, réglé sur Markdown et pas sur son mode visuel.

```markdown
<!--
Summary       Six tiers of chest holding up to 512 an item, with modules, pages and a sort button.
Licence       custom, see LICENSE in the repository
Environment   required on the server and on every client
Categories    storage, utility
Links         website, source
Rendering     CurseForge keeps inline style, Modrinth strips it; the only difference between the two files
-->
```

Le résumé de Modrinth tient sous 256 caractères. Compter avant de poser :

```bash
python3 -c "print(len(open('store/modrinth.md').read().split('Summary')[1].split('\n')[0].strip()))"
```

- [ ] **Step 3: la seule différence entre les deux**

CurseForge rend `style="color:…"` en ligne, Modrinth le retire et imprime le texte nu. Un paragraphe qui doit rester visible, ici l'avertissement sur lithium et celui sur les objets qui tombent quand on retire un module, se met en citation avec `>` côté Modrinth et se laisse nu côté CurseForge. Rien d'autre ne diffère, et les deux blocs de commentaires le disent.

- [ ] **Step 4: ne pas inventer d'adresse**

Le mod n'est pas publié. Il n'y a donc pas de page CurseForge ni Modrinth, et le lien est absent ou marqué à venir. Ne jamais deviner l'une de ces deux adresses.

- [ ] **Step 5: commit**

```bash
git add store/
git commit -m "docs(store): one description for both shops, with the lithium warning up front"
```

---

### Task 7: les réglages

Aucun fichier de réglages n'existe, et aucun paquet `config` non plus. Six nombres décidés dans le code peuvent raisonnablement se discuter sur un serveur, et un mod sans réglage est un mod dont l'auteur a décidé pour tout le monde.

Le découpage est celui de `reference/config.md` et de `LethalBreed`, qui en donne un exemple complet en 42 fichiers. DeepCrate a moins d'options, donc moins de fichiers, mais le même chemin de lecture et d'écriture.

**Files:**
- Create: `mod/src/main/java/oas/dreyka/deepcrate/config/DeepCrateConfig.java`
- Create: `mod/src/main/java/oas/dreyka/deepcrate/config/ConfigAccess.java`
- Create: `mod/src/main/java/oas/dreyka/deepcrate/config/ConfigBounds.java`
- Create: `mod/src/main/java/oas/dreyka/deepcrate/config/ConfigBoundsTable.java`
- Create: `mod/src/main/java/oas/dreyka/deepcrate/config/domain/CrateConfig.java`
- Create: `mod/src/main/java/oas/dreyka/deepcrate/config/domain/ScreenConfig.java`
- Create: `mod/src/main/java/oas/dreyka/deepcrate/config/schema/ConfigSchema.java`
- Create: `mod/src/main/java/oas/dreyka/deepcrate/config/io/ConfigLoader.java`
- Create: `mod/src/main/java/oas/dreyka/deepcrate/config/io/ConfigWriter.java`
- Modify: `mod/src/main/java/oas/dreyka/deepcrate/DeepCrateMod.java`
- Modify: `mod/src/main/java/oas/dreyka/deepcrate/api/DeepCrateApi.java:18,36`
- Modify: `mod/src/main/java/oas/dreyka/deepcrate/api/CrateLayout.java:10`
- Modify: `mod/src/main/java/oas/dreyka/deepcrate/client/DeepCrateScreen.java:77`
- Test: `mod/src/test/java/oas/dreyka/deepcrate/config/ConfigLoaderTest.java`

**Interfaces:**
- Produces: `DeepCrateConfig.load(Path gameDir)`, appelée en premier dans `onInitialize`. `CrateConfig.baseCapacity`, `CrateConfig.limitAutomationWithLithium`, `CrateConfig.maxRowsPerPage`, `ScreenConfig.abbreviateAbove`.
- Consumes: rien des tâches précédentes.

- [ ] **Step 1: les deux porteurs d'options**

Champs publics statiques non finaux, un porteur par domaine. Le schéma est l'énumération réflexive de ces champs dans l'ordre de déclaration, donc ajouter une option est ajouter un champ, sans code de sérialisation ni deuxième liste à oublier.

```java
package oas.dreyka.deepcrate.config.domain;

/** What a crate holds and how automation reaches it. */
public final class CrateConfig {
    private CrateConfig() {}

    /** A slot with no module, which is what the rest of the game holds. */
    public static int baseCapacity = 64;
    /**
     * Whether a crate tells hoppers and pipes 64 while lithium is installed.
     *
     * Lithium replaces the hopper wholesale and keeps its own copy of the target inventory. Against a
     * container answering more than 64 it takes items out of the hopper and never writes them in:
     * measured, eight blocks of dirt destroyed per run. Turning this off gets the feature back and
     * loses those items, which is a server owner's call and not the mod's.
     */
    public static boolean limitAutomationWithLithium = true;
    /** Rows on one page before the screen splits. */
    public static int maxRowsPerPage = 4;
}
```

```java
package oas.dreyka.deepcrate.config.domain;

/** What the crate screen draws. */
public final class ScreenConfig {
    private ScreenConfig() {}

    /** Past this a count runs out of its cell, so it is shortened and the tooltip carries the truth. */
    public static int abbreviateAbove = 999;
}
```

- [ ] **Step 2: le test qui échoue sur la lecture**

```java
package oas.dreyka.deepcrate.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import oas.dreyka.deepcrate.config.domain.CrateConfig;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigLoaderTest {
    @Test
    void anAbsentFileIsWrittenWithTheDefaults(@TempDir Path gameDir) throws Exception {
        DeepCrateConfig.load(gameDir);

        Path written = gameDir.resolve("config/oas/deepcrate.json");
        assertEquals(true, Files.exists(written));
        assertEquals(64, CrateConfig.baseCapacity);
    }

    @Test
    void aValueOutsideItsRangeIsClamped(@TempDir Path gameDir) throws Exception {
        Path file = gameDir.resolve("config/oas/deepcrate.json");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "{\"crate\":{\"baseCapacity\":99999}}");

        DeepCrateConfig.load(gameDir);

        assertEquals(32767, CrateConfig.baseCapacity);
    }
}
```

Chaque morceau prend son chemin en paramètre au lieu de le résoudre, ce qui rend toute la lecture et l'écriture vérifiable contre un dossier temporaire sans jeu qui tourne.

- [ ] **Step 3: le voir échouer**

```bash
cd mod && ./gradlew test --rerun --console=plain --max-workers=2
```

Attendu : échec de compilation, `DeepCrateConfig` n'existe pas.

- [ ] **Step 4: écrire la couche**

`ConfigSchema` énumère par réflexion les champs publics statiques non finaux des porteurs inscrits, dans l'ordre de déclaration. `ConfigLoader` lit le fichier s'il est là, applique chaque champ qu'il nomme par-dessus les valeurs du code, puis fait réécrire le fichier entier, de sorte qu'une option ajoutée par une mise à jour apparaît d'elle-même avec sa valeur par défaut et que l'administrateur voit qu'elle existe. `ConfigWriter` écrit dans un fichier temporaire puis renomme, sinon un plantage en cours d'écriture laisse un fichier tronqué et aucun retour possible. `ConfigBoundsTable` est une table de données pure, une plage par nom de champ : `baseCapacity` entre 1 et `Short.MAX_VALUE`, `maxRowsPerPage` entre 1 et 32, `abbreviateAbove` entre 9 et `Integer.MAX_VALUE`.

Les options sont groupées sous leur catégorie, catégories triées par ordre alphabétique, options gardant l'ordre du schéma dans une catégorie. Le fichier étant réécrit à chaque lancement, un ordre non déterminé produirait un diff neuf à chaque fois, ce qui apprend aux gens à ignorer les diffs.

- [ ] **Step 5: brancher les quatre lectures**

`DeepCrateApi.BASE_CAPACITY` devient une lecture de `CrateConfig.baseCapacity`. `AUTOMATION_LIMITED` devient une méthode plutôt qu'une constante, parce qu'une constante figée au chargement de classe ne peut plus suivre un réglage :

```java
    /** Whether automation may go past a vanilla stack. */
    public static boolean automationLimited() {
        return CrateConfig.limitAutomationWithLithium && FabricLoader.getInstance().isModLoaded("lithium");
    }
```

`CrateStorage.automationCapacityFor` appelle la méthode. `CrateLayout.MAX_ROWS_PER_PAGE` et `DeepCrateScreen.ABBREVIATE_ABOVE` suivent leurs champs de la même façon.

`BASE_CAPACITY` et `MAX_ROWS_PER_PAGE` sont publics et un addon peut déjà les lire. Les garder comme constantes dépréciées rendant la valeur par défaut, ou les retirer en le disant dans le README : le choix se pose au moment de le faire, et il se note dans le commit.

- [ ] **Step 6: charger avant tout le reste**

Dans `DeepCrateMod.onInitialize`, en première ligne :

```java
        DeepCrateConfig.load(FabricLoader.getInstance().getGameDir());
```

Avant `RegistryInit.register()`, puisque le palier lit la contenance de base.

- [ ] **Step 7: vérifier**

```bash
cd mod && ./gradlew build check --console=plain --max-workers=2
```

Attendu : `BUILD SUCCESSFUL`, les deux tests de configuration verts, les 36 anciens toujours verts.

- [ ] **Step 8: commit**

```bash
cd .. && git add mod/src && git commit -m "feat(config): four numbers a server owner may want different move to config/oas/deepcrate.json"
```

---

### Task 8: le dernier mur, la greffe sur l'écran

C'est la tâche 6 du plan du 3 septembre, à peine commencée puis remise à zéro pour ne pas laisser du code à moitié écrit. Son code complet est déjà écrit dans `docs/superpowers/plans/2026-09-03-modularite.md` à partir de la ligne 1813. Le lire là-bas et le suivre.

**Files:**
- Create: `mod/src/main/java/oas/dreyka/deepcrate/client/CrateScreenArea.java`
- Create: `mod/src/main/java/oas/dreyka/deepcrate/client/CrateScreenCallback.java`
- Create: `mod/src/main/java/oas/dreyka/deepcrate/client/CrateTooltipCallback.java`
- Modify: `mod/src/main/java/oas/dreyka/deepcrate/client/DeepCrateScreen.java`
- Test: `mod/src/gametest/java/oas/dreyka/deepcrate/gametest/CrateLookClientGameTest.java`

**Interfaces:**
- Consumes: `CrateSearch` et `CratePanel` de la tâche 4, puisque `DeepCrateScreen` a changé de forme depuis que ce plan a été écrit. Relire les trois méthodes `isOver*` dans leur état d'après la tâche 4 avant de les rassembler.
- Produces: `CrateScreenCallback.EVENT`, `CrateTooltipCallback.EVENT`, `CrateScreenArea`.

- [ ] **Step 1: lire la tâche 6 du plan précédent**

```bash
sed -n '1813,2058p' docs/superpowers/plans/2026-09-03-modularite.md
```

- [ ] **Step 2: les trois fichiers**

Tels qu'écrits là-bas.

- [ ] **Step 3: rassembler les rectangles à épargner**

`hasClickedOutside` épargne aujourd'hui trois zones par trois méthodes séparées, `isOverPageButtons`, `isOverModuleTab` et `isOverSortButtons`. Elles deviennent une seule liste de `CrateScreenArea`, à laquelle un mod extérieur ajoute la sienne. Sans ça, un bouton posé par un addon hors du panneau fait tomber par terre ce que le joueur porte quand il relâche le clic dessus.

- [ ] **Step 4: faire partir l'événement d'écran**

À la fin de `init`, une fois les widgets du mod posés, pour qu'un addon en ajoute après.

- [ ] **Step 5: la ligne du compte réel par son propre événement**

`getTooltipFromContainerItem` écrit aujourd'hui la ligne du compte en dur à l'indice 1. Elle passe par `CrateTooltipCallback`, et le mod inscrit la sienne comme n'importe qui d'autre, ce qui est la preuve que l'événement suffit.

- [ ] **Step 6: la preuve par capture**

Inscrire depuis `CrateLookClientGameTest` un bouton et une ligne d'infobulle, et photographier l'écran avec. Une capture montrant le bouton d'un addon est ce qui prouve que la greffe tient.

```bash
cd mod && ./gradlew build check runClientGameTest --console=plain --max-workers=2
ls build/run/clientGameTest/screenshots/
```

- [ ] **Step 7: commit**

```bash
cd .. && git add mod/src && git commit -m "feat(api): another mod can put a button and a tooltip line on the crate screen"
```

---

### Task 9: la documentation dit ce qui est ouvert

Le README nomme trois points d'extension. Il y en a huit : inscrire un palier, un module de contenance, un module de rangée, une sorte de case, un ordre de tri, plus les événements de découpage en pages, de contenance, et après la tâche 8 celui de l'écran et celui de l'infobulle. Sa section sur la contenance décrit aussi une case unique, ce qui n'est plus vrai depuis que les cases sont un registre.

**Files:**
- Modify: `README.md`
- Modify: `PLAN.md`
- Create: `DEFAUTS-CONNUS.md`

**Interfaces:**
- Consumes: tout ce que les tâches 1 à 8 ont posé.

- [ ] **Step 1: refaire la section "For other mods"**

En anglais, puisque le README fait face aux joueurs et aux deux boutiques. Lister les huit points, avec l'exemple d'addon complet, et dire ce qui reste fermé et pourquoi : un palier a besoin d'un bloc inscrit dans le registre des blocs, et ce registre se ferme avant qu'un fichier de données soit lu, donc créer une sorte de coffre restera du Java.

Vérifier la liste contre le code plutôt que contre cette page :

```bash
grep -rn 'public static .*register\|Event<' mod/src/main/java/oas/dreyka/deepcrate/api mod/src/main/java/oas/dreyka/deepcrate/client/DeepCrateClientApi.java --include='*.java' | grep -v ' \*'
```

- [ ] **Step 2: corriger la section "Capacity"**

Elle parle d'une case unique et d'un module à la fois. Depuis le registre, la contenance d'un coffre est la plus forte que propose l'une quelconque de ses cases, et les rangées en sont la somme. Réécrire ces deux paragraphes.

- [ ] **Step 3: la section des réglages**

Le README ne mentionne aucun fichier de réglages, et il y en a un depuis la tâche 7. Une section courte : le chemin `config/oas/deepcrate.json`, les quatre options, et ce que change celle sur lithium.

- [ ] **Step 4: réécrire PLAN.md**

En français. Il dit aujourd'hui que les tâches 6 et 7 du chantier de modularité restent ; elles sont faites. Le réécrire sur l'état réel de la branche, court, comme un fichier réécrit chaque soir.

- [ ] **Step 5: ouvrir DEFAUTS-CONNUS.md**

En français, daté. Ce qui est cassé et ce qui est un choix, avec la mesure derrière chaque ligne. Deux entrées existent déjà, écrites dans le README et dans les commentaires du code : un coffre echo double perdant son module de 512 lâche 69 000 objets d'un coup, et le remplissage par tuyaux est coupé quand lithium est là. Les sortir du README, qui n'est pas fait pour ça, et les mettre là avec la mesure.

- [ ] **Step 6: relire la parité des langues avant de finir**

Rien dans cette tâche ne touche aux fichiers de langue, mais c'est le dernier moment où les vérifier avant de fermer :

```bash
cd mod/src/main/resources/assets/deepcrate/lang
python3 -c "import json; a=set(json.load(open('en_us.json'))); b=set(json.load(open('fr_fr.json'))); print('en seulement', a-b, 'fr seulement', b-a)"
```

Attendu : deux ensembles vides.

- [ ] **Step 7: commit**

```bash
cd /run/media/dreykaoas/O.A.S/projects/mods/DeepCrate
git add README.md PLAN.md DEFAUTS-CONNUS.md
git commit -m "docs: the readme names all eight extension points, not the three the first ask had"
```

---

### Task 10: le chemin chaud de la trémie

Ajoutée le 5 septembre après que le gabarit a gagné un huitième contrôle, la performance mesurée. Le chiffre est mesuré, pas supposé, et il vient d'un gametest jeté après lecture dont la sortie brute est dans `.superpowers/sdd/perf-report.md`.

`storage()`, [DeepCrateBlockEntity.java:100](../../../mod/src/main/java/oas/dreyka/deepcrate/block/DeepCrateBlockEntity.java), ressemble à un accesseur et n'en est pas un : chaque appel relance `alignStorageWithTier()` et `alignCapacityWithHolder()`, qui appellent chacun `moduleHolder()`, lequel fait un `getBlockEntity` sur la position voisine quand le coffre est apparié. La boucle du mixin appelle `getItem(i)` puis `getMaxStackSize(itemStack)` par emplacement, et les deux passent par `storage()`, donc quatre `moduleHolder()` par emplacement.

Mesuré sur 5000 parcours de 216 emplacements après 500 tours de chauffe : 99,85 ns par emplacement sur un coffre simple, 286,02 ns sur un apparié, un facteur 2,86. Une trémie contre un demi coffre echo plein coûte 62 µs par tick, et cent trémies contre cent grands coffres tournent autour de 6 ms sur les 50 ms du tick.

Rien n'est alloué dans cette boucle. `cratesFor` et `containerFor` allouent, mais leurs appelants sont l'ouverture d'écran, le comparateur et le changement de module.

**Files:**
- Modify: `mod/src/main/java/oas/dreyka/deepcrate/block/CrateModuleHolder.java`
- Modify: `mod/src/main/java/oas/dreyka/deepcrate/block/DeepCrateBlockEntity.java`
- Test: `mod/src/gametest/java/oas/dreyka/deepcrate/gametest/CrateHopperGameTest.java`

**Interfaces:**
- Consumes: `CrateModuleHolder` de la tâche 4, qui porte alors `moduleHolder()` et `alignCapacityWithHolder()`.
- Produces: rien de public. Aucune signature ne change, ce qui est la condition pour qu'un addon ne voie rien.

Cette tâche vient après la tâche 4, puisqu'elle modifie un fichier que la tâche 4 crée.

- [ ] **Step 1: reproduire la mesure de départ**

Refaire tourner le gametest de mesure décrit dans `.superpowers/sdd/perf-report.md`, cette fois en le gardant. Il devient un test de non-régression : un chiffre qui remonte plus tard veut dire que quelqu'un a remis du travail dans `storage()`.

Le nommer `CrateHopperCostGameTest`, l'ajouter à `mod/src/gametest/resources/fabric.mod.json` sous `fabric-gametest`, sinon il ne tourne jamais et passe au vert sans rien avoir mesuré.

Il asserte, il ne se contente pas d'imprimer : le coût par emplacement d'un coffre apparié reste sous trois fois celui d'un coffre simple. Un seuil large exprès, parce qu'un chiffre serré sur une machine de développement casse chez quelqu'un d'autre pour rien.

- [ ] **Step 2: le voir passer avant tout changement**

```bash
cd mod && ./gradlew check --console=plain --max-workers=2
```

Attendu : vert, avec le rapport 2,86 imprimé. C'est la ligne de base.

- [ ] **Step 3: mémoriser le holder pour la durée du tick**

`moduleHolder()` refait le même `getBlockEntity` quatre fois par emplacement alors que la réponse ne peut pas changer entre deux emplacements du même parcours. Garder le résultat dans un champ, invalidé quand le voisinage change.

Les deux moments où il doit être invalidé, et rien d'autre ne le touche : `setBlockState`, que le jeu appelle quand la propriété `TYPE` du coffre change, donc quand une paire se forme ou se défait ; et `setRemoved`, quand le coffre disparaît. Les deux sont des redéfinitions de `BlockEntity`.

```java
    private @Nullable DeepCrateBlockEntity cachedHolder;

    @Override
    public void setBlockState(BlockState blockState) {
        super.setBlockState(blockState);
        // The pairing itself lives in the TYPE property, so a crate marrying or losing its partner
        // arrives here. Keeping a stale holder past that point would send a module's capacity to a
        // crate that is no longer part of the pair.
        this.cachedHolder = null;
    }
```

L'autre moitié de la paire doit être invalidée aussi quand celle-ci change, sinon elle garde un pointeur vers un coffre retiré. Le faire dans `setRemoved`, en allant chercher le voisin avant de disparaître.

- [ ] **Step 4: vérifier que le comportement n'a pas bougé**

Le cache est une optimisation, donc les 44 gametests et les 36 tests JUnit doivent passer sans qu'aucun soit modifié. Un test qu'il faut retoucher pour passer est un test qui dit que le comportement a changé.

```bash
cd mod && ./gradlew build check --console=plain --max-workers=2
```

Attendu : `BUILD SUCCESSFUL`, 36 JUnit et 45 gametests verts, dont le nouveau.

- [ ] **Step 5: relire le chiffre**

Le rapport imprimé par `CrateHopperCostGameTest` doit être descendu nettement sous 2,86. S'il n'a pas bougé, le cache n'est pas sur le chemin mesuré : le dire et chercher où, plutôt que de committer une optimisation qui n'optimise rien.

- [ ] **Step 6: commit**

```bash
cd /run/media/dreykaoas/O.A.S/projects/mods/DeepCrate
git add mod/src
git commit -m "perf(block): a paired crate resolves its module holder once, not four times a slot"
```

---

## Ce que ce plan ne fait pas

Les dossiers de ressources qui passent huit fichiers, `items/` à 10, `recipe/` à 10, `advancement/recipes/` à 10 et `textures/entity/chest/` à 18, restent tels quels. Le format du jeu impose un fichier par objet et par recette, et les sous-dossiers y changeraient les chemins que le jeu lit.

`registerTier` garde sa forme au lieu de passer par le helper de la tâche 3 : elle écrit dans deux tables et détecte le doublon par la valeur rendue par `put`, ce qui n'est pas la même mécanique que les trois autres.

Le site sous `web/` reste un README d'une ligne. `stack-dev` et `site-template` couvrent ce chantier, et il ne se mêle pas à celui-ci.

## Self-review

Couverture : les sept mesures de l'audit ont chacune leur tâche. Code mort et duplication, tâches 2 et 3. Tailles et dossiers, tâche 4. Texte joueur en dur, rien à faire, les deux occurrences sont autorisées. Parité des langues, verte, vérifiée une dernière fois à l'étape 6 de la tâche 9. Nommage, tâche 2. Murs, tâche 8. Options, tâche 7. Le travail non commité passe en tâche 1, l'identité en tâche 5, la boutique en tâche 6, la documentation en tâche 9.

Cohérence des noms : `CrateDrops.dropWhole` porte le même nom à la tâche 3 et à la tâche 4. `CratePanelGeometry` garde les neuf constantes sous les mêmes noms qu'elles ont aujourd'hui dans `DeepCrateMenu`, et les délégations les laissent lisibles depuis là, ce que `DeepCrateScreen` et les gametests font. `automationLimited()` remplace `AUTOMATION_LIMITED` en tâche 7 seulement, et la tâche 2 ne fait que corriger l'import de la constante, sans la renommer.

Ordre : fusionner avant découper, tâche 3 avant tâche 4, comme le gabarit le demande. La tâche 8 vient après la tâche 4 parce que `DeepCrateScreen` aura changé de forme entre-temps, et son interface le dit.
