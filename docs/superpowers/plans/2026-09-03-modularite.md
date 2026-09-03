# Ouvrir les cinq murs de DeepCrate, plan d'exécution

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** rendre inscriptibles par un autre mod les cinq choses qui sont aujourd'hui des constantes ou des énumérations fermées : les cases à module, les ordres de tri, le nombre de colonnes, la règle de contenance, et la pose de widgets et d'infobulles sur l'écran.

**Architecture:** chaque mur tombe par le même mouvement, une valeur figée devient une liste inscrite dans `DeepCrateApi` ou un événement Fabric, et le code qui la lisait la relit à travers cette liste. Les deux sortes de cases livrées et les deux ordres de tri livrés se réinscrivent dans leur propre registre, ce qui prouve que le registre suffit. Le format de sauvegarde change une fois, en gardant la lecture des anciennes clés.

**Tech Stack:** Fabric loader 0.19.3, fabric-api 0.141.4+1.21.11, fabric-loom 1.17.12, Gradle 9.5.1, Java 21, mappings officielles Mojang. JUnit 5 pour ce qui n'a besoin ni de monde ni de client, gametest pour ce qui a besoin d'un serveur, gametest client pour les captures.

## Global Constraints

Le répertoire de travail est `/run/media/dreykaoas/O.A.S/projects/mods/DeepCrate`, et toutes les commandes Gradle se lancent depuis `mod/`.

Le nombre écrit dans un `DataSlot` est un short, donc `DeepCrateApi.MAX_CAPACITY` vaut `Short.MAX_VALUE` et rien ne le dépasse.

Aucune ligne d'attribution dans un message de commit, aucun trailer, aucune mention d'outil.

Les commentaires de code sont en anglais, les messages de commit sont en anglais, la documentation est en français. Un commentaire dit pourquoi, jamais quoi.

Un fichier Java du dépôt tient sous 150 lignes quand c'est possible ; dix le dépassent déjà et ce plan ne les aggrave pas.

Une classe de gametest ne tourne que si elle est nommée dans `mod/src/gametest/resources/fabric.mod.json`. Une classe ajoutée sans cette ligne ne s'exécute jamais et le test passe au vert sans rien avoir vérifié.

Le monde de test est toujours paisible et créatif.

Avant de lancer un build ou un client, vérifier `free -h` ; sous 2 Go disponibles, attendre plutôt qu'empiler. Tout serveur ou client lancé pendant une tâche est fermé à la fin de cette tâche.

## Structure des fichiers

Fichiers créés :

`mod/src/main/java/com/dreykaoas/deepcrate/api/CrateModuleSlot.java` décrit une sorte de case à module et rien d'autre.
`mod/src/main/java/com/dreykaoas/deepcrate/api/CrateModules.java` porte les piles qu'un coffre garde dans ses cases, indexées par identifiant.
`mod/src/main/java/com/dreykaoas/deepcrate/inventory/StoredModule.java` est la forme sauvegardée d'une de ces piles, comme `StoredSlot` l'est pour une case de coffre.
`mod/src/main/java/com/dreykaoas/deepcrate/api/CrateCapacityCallback.java` est l'événement de contenance.
`mod/src/main/java/com/dreykaoas/deepcrate/client/CrateSortOrder.java` et `CrateSortRule.java` décrivent un ordre de tri.
`mod/src/main/java/com/dreykaoas/deepcrate/client/DeepCrateClientApi.java` tient le registre des ordres et sait bâtir une liste d'objets à partir d'un ordre.
`mod/src/main/java/com/dreykaoas/deepcrate/client/CrateScreenCallback.java`, `CrateScreenArea.java` et `CrateTooltipCallback.java` sont la greffe sur l'écran.

Fichiers supprimés : `inventory/RowModuleSlot.java` fondu dans `ModuleSlot`, `client/CrateSort.java` remplacé par le registre.

---

### Task 1: le registre des sortes de cases, et la table du coffre

**Files:**
- Create: `mod/src/main/java/com/dreykaoas/deepcrate/api/CrateModuleSlot.java`
- Create: `mod/src/main/java/com/dreykaoas/deepcrate/api/CrateModules.java`
- Create: `mod/src/main/java/com/dreykaoas/deepcrate/inventory/StoredModule.java`
- Modify: `mod/src/main/java/com/dreykaoas/deepcrate/api/DeepCrateApi.java`
- Modify: `mod/src/main/java/com/dreykaoas/deepcrate/init/RegistryInit.java`
- Modify: `mod/src/main/java/com/dreykaoas/deepcrate/block/DeepCrateBlockEntity.java`
- Test: `mod/src/test/java/com/dreykaoas/deepcrate/CrateModulesTest.java`

**Interfaces:**
- Produces: `CrateModuleSlot(Identifier id, int order, int stackLimit, Identifier emptyIcon, Predicate<ItemStack> filter)` avec `boolean accepts(ItemStack)` et le statique `Predicate<ItemStack> tagged(TagKey<Item>)`.
- Produces: `CrateModules implements Iterable<ItemStack>` avec `ItemStack get(Identifier)`, `void set(Identifier, ItemStack)`, `boolean isEmpty()`, `void clear()`, `Set<Identifier> ids()`.
- Produces: `DeepCrateApi.registerModuleSlot(CrateModuleSlot)`, `DeepCrateApi.moduleSlots()`, `DeepCrateApi.moduleSlot(Identifier)`, `DeepCrateApi.capacityAmong(Iterable<ItemStack>)`, `DeepCrateApi.rowsAmong(Iterable<ItemStack>)`.
- Produces: `RegistryInit.CAPACITY_SLOT` et `RegistryInit.ROWS_SLOT`, deux `Identifier`.
- Produces: `DeepCrateBlockEntity.modules()` rendant le `CrateModules`, et `setModuleIn(Identifier, ItemStack)`.
- Consumes: rien.

- [ ] **Step 1: écrire le test qui échoue**

Créer `mod/src/test/java/com/dreykaoas/deepcrate/CrateModulesTest.java` :

```java
package com.dreykaoas.deepcrate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dreykaoas.deepcrate.api.CrateModules;
import net.minecraft.SharedConstants;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CrateModulesTest {
    private static final Identifier CAPACITY = Identifier.fromNamespaceAndPath("deepcrate", "capacity");
    private static final Identifier ROWS = Identifier.fromNamespaceAndPath("deepcrate", "rows");

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void anUntouchedTableAnswersEmptyForEveryName() {
        CrateModules crateModules = new CrateModules();

        assertTrue(crateModules.get(CAPACITY).isEmpty());
        assertTrue(crateModules.isEmpty());
        assertTrue(crateModules.ids().isEmpty());
    }

    @Test
    void aStackPutBackUnderItsNameComesBackWhole() {
        CrateModules crateModules = new CrateModules();

        crateModules.set(ROWS, new ItemStack(Items.CHEST, 7));

        assertEquals(7, crateModules.get(ROWS).getCount());
        assertFalse(crateModules.isEmpty());
        assertEquals(1, crateModules.ids().size());
    }

    @Test
    void anEmptyStackClearsItsNameRatherThanKeepingAHole() {
        CrateModules crateModules = new CrateModules();
        crateModules.set(CAPACITY, new ItemStack(Items.CHEST));

        crateModules.set(CAPACITY, ItemStack.EMPTY);

        assertTrue(crateModules.isEmpty());
        assertTrue(crateModules.ids().isEmpty());
    }

    @Test
    void walkingTheTableHandsBackEveryStackItHolds() {
        CrateModules crateModules = new CrateModules();
        crateModules.set(CAPACITY, new ItemStack(Items.CHEST));
        crateModules.set(ROWS, new ItemStack(Items.BARREL, 3));

        int total = 0;
        for (ItemStack itemStack : crateModules) {
            total += itemStack.getCount();
        }

        assertEquals(4, total);
    }
}
```

- [ ] **Step 2: lancer le test pour vérifier qu'il échoue**

Run: `cd mod && ./gradlew test --tests 'com.dreykaoas.deepcrate.CrateModulesTest'`
Expected: la compilation échoue, `package com.dreykaoas.deepcrate.api.CrateModules does not exist`.

- [ ] **Step 3: écrire `CrateModules`**

```java
package com.dreykaoas.deepcrate.api;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * What a crate keeps in its module slots, one stack per registered kind.
 *
 * A stack whose kind nobody registered stays in the table without being drawn: a world opened
 * without the mod that added that kind gives its module back when the mod returns, rather than
 * losing it on the first save.
 */
public final class CrateModules implements Iterable<ItemStack> {
    private final Map<Identifier, ItemStack> stacks = new LinkedHashMap<>();

    public ItemStack get(Identifier identifier) {
        return this.stacks.getOrDefault(identifier, ItemStack.EMPTY);
    }

    public void set(Identifier identifier, ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            this.stacks.remove(identifier);
        } else {
            this.stacks.put(identifier, itemStack);
        }
    }

    public boolean isEmpty() {
        return this.stacks.isEmpty();
    }

    public void clear() {
        this.stacks.clear();
    }

    public Set<Identifier> ids() {
        return Collections.unmodifiableSet(this.stacks.keySet());
    }

    @Override
    public Iterator<ItemStack> iterator() {
        return Collections.unmodifiableCollection(this.stacks.values()).iterator();
    }
}
```

- [ ] **Step 4: lancer le test pour vérifier qu'il passe**

Run: `cd mod && ./gradlew test --tests 'com.dreykaoas.deepcrate.CrateModulesTest'`
Expected: PASS, quatre tests.

- [ ] **Step 5: écrire `CrateModuleSlot`**

```java
package com.dreykaoas.deepcrate.api;

import java.util.function.Predicate;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * One cell of the tab hanging off the left of the crate screen.
 *
 * What the item sitting in it does is not decided here: a capacity module raises the crate from
 * whichever cell it sits in, a row module adds its rows from whichever cell it sits in. A kind that
 * matches neither is a cell the crate itself makes nothing of, and the mod that added it reads its
 * stack back through {@code DeepCrateBlockEntity.modules()}.
 *
 * @param order     place in the column, smallest at the top
 * @param emptyIcon the sprite drawn while the cell is empty, under {@code textures/gui/sprites}
 * @param filter    what the cell takes
 */
public record CrateModuleSlot(Identifier id, int order, int stackLimit, Identifier emptyIcon, Predicate<ItemStack> filter) {
    public CrateModuleSlot {
        if (stackLimit < 1) {
            throw new IllegalArgumentException("Module slot " + id + " needs a positive stack limit, got " + stackLimit);
        }
    }

    public boolean accepts(ItemStack itemStack) {
        return !itemStack.isEmpty() && this.filter.test(itemStack);
    }

    /** The common case: a cell that takes whatever an item tag names, with no Java on the other side. */
    public static Predicate<ItemStack> tagged(TagKey<Item> items) {
        return itemStack -> {
            try {
                return itemStack.is(items);
            } catch (IllegalStateException e) {
                // Tags bind when a world loads; anything asking earlier gets "no" rather than a crash.
                return false;
            }
        };
    }
}
```

- [ ] **Step 6: écrire `StoredModule`**

```java
package com.dreykaoas.deepcrate.inventory;

import com.dreykaoas.deepcrate.api.DeepCrateApi;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;

/**
 * One saved module slot: which cell, what item, how many.
 *
 * The item is split from its count for the same reason {@link StoredSlot} does it: the vanilla stack
 * codec refuses a count above 99, and nothing stops an addon from opening a cell that takes more.
 */
public record StoredModule(Identifier id, ItemStack item, int count) {
    public static final Codec<StoredModule> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
                Identifier.CODEC.fieldOf("Id").forGetter(StoredModule::id),
                ItemStack.SINGLE_ITEM_CODEC.fieldOf("Item").forGetter(StoredModule::item),
                ExtraCodecs.intRange(1, DeepCrateApi.MAX_CAPACITY).fieldOf("Count").forGetter(StoredModule::count)
            )
            .apply(instance, StoredModule::new)
    );

    public static StoredModule of(Identifier identifier, ItemStack itemStack) {
        return new StoredModule(identifier, itemStack.copyWithCount(1), itemStack.getCount());
    }

    public ItemStack toStack() {
        return this.item.copyWithCount(this.count);
    }
}
```

- [ ] **Step 7: ouvrir le registre dans `DeepCrateApi`**

Ajouter le champ, à côté de `MODULES` et `ROW_MODULES` :

```java
    private static final List<CrateModuleSlot> MODULE_SLOTS = new ArrayList<>();
```

Ajouter les méthodes, après `registerRowModule` :

```java
    public static CrateModuleSlot registerModuleSlot(CrateModuleSlot crateModuleSlot) {
        for (CrateModuleSlot existing : MODULE_SLOTS) {
            if (existing.id().equals(crateModuleSlot.id())) {
                throw new IllegalStateException("Module slot " + crateModuleSlot.id() + " registered twice");
            }
        }

        MODULE_SLOTS.add(crateModuleSlot);
        MODULE_SLOTS.sort(Comparator.comparingInt(CrateModuleSlot::order));
        DeepCrate.LOGGER.info("[DeepCrate] module slot {}: up to {} at a time", crateModuleSlot.id(), crateModuleSlot.stackLimit());
        return crateModuleSlot;
    }

    public static List<CrateModuleSlot> moduleSlots() {
        return Collections.unmodifiableList(MODULE_SLOTS);
    }

    public static @Nullable CrateModuleSlot moduleSlot(Identifier identifier) {
        for (CrateModuleSlot crateModuleSlot : MODULE_SLOTS) {
            if (crateModuleSlot.id().equals(identifier)) {
                return crateModuleSlot;
            }
        }

        return null;
    }

    /**
     * The strongest capacity any of these stacks asks for. A crate whose cells hold two capacity
     * modules takes the better of the two rather than adding them, which is the rule a single cell
     * already followed between two tags.
     */
    public static int capacityAmong(Iterable<ItemStack> stacks) {
        int capacity = BASE_CAPACITY;
        for (ItemStack itemStack : stacks) {
            capacity = Math.max(capacity, capacityOf(itemStack));
        }

        return capacity;
    }

    /** Rows add up, because each row module is a row and two of them are two rows. */
    public static int rowsAmong(Iterable<ItemStack> stacks) {
        int rows = 0;
        for (ItemStack itemStack : stacks) {
            rows += rowsOf(itemStack);
        }

        return rows;
    }
```

Ajouter `import java.util.Comparator;` en tête du fichier.

- [ ] **Step 8: inscrire les deux sortes livrées dans `RegistryInit`**

Ajouter, après `ROW_MODULE_ITEM` :

```java
    /** The two cells the mod ships, in the order they are drawn. */
    public static final Identifier CAPACITY_SLOT = id("capacity");
    public static final Identifier ROWS_SLOT = id("rows");

    static {
        // The filters ask the module registries rather than a tag of their own, so adding an item to
        // deepcrate:module_512 still makes it placeable with no second data file to write.
        DeepCrateApi.registerModuleSlot(
            new CrateModuleSlot(CAPACITY_SLOT, 0, 1, id("container/slot/module"), itemStack -> DeepCrateApi.moduleFor(itemStack) != null)
        );
        DeepCrateApi.registerModuleSlot(
            new CrateModuleSlot(
                ROWS_SLOT, 1, RowModule.STACK_LIMIT, id("container/slot/row_module"), itemStack -> DeepCrateApi.rowModuleFor(itemStack) != null
            )
        );
    }
```

Ajouter `import com.dreykaoas.deepcrate.api.CrateModuleSlot;`.

- [ ] **Step 9: remplacer les deux champs du coffre par la table**

Dans `DeepCrateBlockEntity`, remplacer :

```java
    private ItemStack module = ItemStack.EMPTY;
    private ItemStack rowModules = ItemStack.EMPTY;
```

par :

```java
    private final CrateModules modules = new CrateModules();
```

Remplacer les quatre accesseurs par :

```java
    public CrateModules modules() {
        return this.modules;
    }

    public ItemStack module() {
        return this.modules.get(RegistryInit.CAPACITY_SLOT);
    }

    public void setModule(ItemStack itemStack) {
        this.setModuleIn(RegistryInit.CAPACITY_SLOT, itemStack);
    }

    public ItemStack rowModules() {
        return this.modules.get(RegistryInit.ROWS_SLOT);
    }

    public void setRowModules(ItemStack itemStack) {
        this.setModuleIn(RegistryInit.ROWS_SLOT, itemStack);
    }

    /**
     * Puts a stack in one cell and lets the whole table decide again. Capacity and rows are read
     * across every cell rather than from the one that changed: which cell an item sits in no longer
     * says what it does.
     */
    public void setModuleIn(Identifier identifier, ItemStack itemStack) {
        this.modules.set(identifier, itemStack);
        this.storage().setCapacity(DeepCrateApi.capacityAmong(this.modules));
        for (DeepCrateBlockEntity deepCrateBlockEntity : DeepCrateBlock.cratesFor(this)) {
            deepCrateBlockEntity.storage();
            deepCrateBlockEntity.setChanged();
        }

        this.setChanged();
    }
```

Remplacer `extraRows()` :

```java
    public int extraRows() {
        return DeepCrateApi.rowsAmong(this.moduleHolder().modules);
    }
```

Remplacer `hasAnyModule()` :

```java
    private boolean hasAnyModule() {
        return !this.modules.isEmpty();
    }
```

Dans `alignStorageWithTier`, remplacer `DeepCrateApi.rowsOf(this.moduleHolder().rowModules)` par `this.extraRows()`.

Dans `alignCapacityWithHolder`, remplacer `DeepCrateApi.capacityOf(holder.module())` par `DeepCrateApi.capacityAmong(holder.modules)`.

Ajouter les imports `com.dreykaoas.deepcrate.api.CrateModules` et `net.minecraft.resources.Identifier`.

- [ ] **Step 10: écrire et relire la table dans la sauvegarde**

Dans `saveAdditional`, remplacer les deux blocs `Module` et `RowModules` par :

```java
        if (!this.modules.isEmpty()) {
            ValueOutput.TypedOutputList<StoredModule> savedModules = valueOutput.list("Modules", StoredModule.CODEC);
            for (Identifier identifier : this.modules.ids()) {
                savedModules.add(StoredModule.of(identifier, this.modules.get(identifier)));
            }
        }
```

Dans `loadAdditional`, remplacer les deux lectures par :

```java
        this.modules.clear();
        for (StoredModule storedModule : valueInput.listOrEmpty("Modules", StoredModule.CODEC)) {
            this.modules.set(storedModule.id(), storedModule.toStack());
        }

        // A crate saved before the cells were a registry kept its two stacks under their own keys.
        // Read once and never written again, so a world upgrades on the first load of each crate.
        if (this.modules.isEmpty()) {
            this.modules.set(RegistryInit.CAPACITY_SLOT, valueInput.read("Module", ItemStack.CODEC).orElse(ItemStack.EMPTY));
            this.modules.set(RegistryInit.ROWS_SLOT, valueInput.read("RowModules", ItemStack.CODEC).orElse(ItemStack.EMPTY));
        }
```

Plus bas dans la même méthode, remplacer `DeepCrateApi.capacityOf(this.module)` par `DeepCrateApi.capacityAmong(this.modules)`.

Dans `removeComponentsFromTag`, ajouter `valueOutput.discard("Modules");` et garder les deux anciens `discard`.

Dans `preRemoveSideEffects`, remplacer les deux blocs par :

```java
        for (ItemStack itemStack : this.modules) {
            list.add(itemStack);
        }

        this.modules.clear();
```

- [ ] **Step 11: compiler et lancer toute la suite**

Run: `cd mod && ./gradlew test`
Expected: PASS, 29 tests, les 25 d'avant plus les quatre nouveaux.

Run: `cd mod && ./gradlew runGametest`
Expected: PASS, 36 tests. Ils appellent encore `setModule` et `setRowModules`, qui existent toujours.

- [ ] **Step 12: commit**

```bash
git add mod/src/main/java/com/dreykaoas/deepcrate/api/CrateModuleSlot.java \
        mod/src/main/java/com/dreykaoas/deepcrate/api/CrateModules.java \
        mod/src/main/java/com/dreykaoas/deepcrate/api/DeepCrateApi.java \
        mod/src/main/java/com/dreykaoas/deepcrate/inventory/StoredModule.java \
        mod/src/main/java/com/dreykaoas/deepcrate/init/RegistryInit.java \
        mod/src/main/java/com/dreykaoas/deepcrate/block/DeepCrateBlockEntity.java \
        mod/src/test/java/com/dreykaoas/deepcrate/CrateModulesTest.java
git commit -m "feat(api): module slots become a registry, and a crate keeps them in one table"
```

---

### Task 2: le menu et l'écran suivent le registre

**Files:**
- Modify: `mod/src/main/java/com/dreykaoas/deepcrate/inventory/ModuleContainer.java`
- Modify: `mod/src/main/java/com/dreykaoas/deepcrate/inventory/ModuleSlot.java`
- Delete: `mod/src/main/java/com/dreykaoas/deepcrate/inventory/RowModuleSlot.java`
- Modify: `mod/src/main/java/com/dreykaoas/deepcrate/inventory/DeepCrateMenu.java`
- Modify: `mod/src/main/java/com/dreykaoas/deepcrate/client/DeepCrateScreen.java`
- Test: `mod/src/gametest/java/com/dreykaoas/deepcrate/gametest/CrateModuleSlotGameTest.java`
- Modify: `mod/src/gametest/resources/fabric.mod.json`

**Interfaces:**
- Consumes: `DeepCrateApi.moduleSlots()`, `DeepCrateApi.capacityAmong`, `DeepCrateApi.rowsAmong`, `DeepCrateBlockEntity.modules()`, `DeepCrateBlockEntity.setModuleIn`, `RegistryInit.CAPACITY_SLOT`, `RegistryInit.ROWS_SLOT`.
- Produces: `DeepCrateMenu.crateSlotStart()` rendant un `int`, qui remplace la constante `CRATE_SLOT_START`.
- Produces: `ModuleSlot(Container, int, int, int, CrateModuleSlot)`.

- [ ] **Step 1: écrire le gametest qui échoue**

Créer `mod/src/gametest/java/com/dreykaoas/deepcrate/gametest/CrateModuleSlotGameTest.java` :

```java
package com.dreykaoas.deepcrate.gametest;

import com.dreykaoas.deepcrate.api.CrateModuleSlot;
import com.dreykaoas.deepcrate.api.DeepCrateApi;
import com.dreykaoas.deepcrate.block.DeepCrateBlockEntity;
import com.dreykaoas.deepcrate.init.RegistryInit;
import com.dreykaoas.deepcrate.inventory.DeepCrateMenu;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** The cells of the module tab, now that there is a registry behind them rather than two fields. */
public class CrateModuleSlotGameTest {
    private static final BlockPos CRATE = new BlockPos(1, 1, 1);

    @GameTest
    public void theMenuOpensOneSlotPerRegisteredKind(GameTestHelper gameTestHelper) {
        ServerPlayer serverPlayer = gameTestHelper.makeMockServerPlayerInLevel();
        gameTestHelper.setBlock(CRATE, RegistryInit.TIERS.get(0).block().defaultBlockState());
        DeepCrateBlockEntity deepCrateBlockEntity = gameTestHelper.getBlockEntity(CRATE, DeepCrateBlockEntity.class);

        DeepCrateMenu deepCrateMenu = (DeepCrateMenu) deepCrateBlockEntity.createMenu(1, serverPlayer.getInventory(), serverPlayer);

        int kinds = DeepCrateApi.moduleSlots().size();
        if (deepCrateMenu.crateSlotStart() != kinds) {
            gameTestHelper.fail("the crate slots should start after the " + kinds + " module cells, they start at " + deepCrateMenu.crateSlotStart());
        }

        gameTestHelper.succeed();
    }

    @GameTest
    public void aCapacityModuleRaisesTheCrateFromWhicheverCellItSitsIn(GameTestHelper gameTestHelper) {
        gameTestHelper.setBlock(CRATE, RegistryInit.TIERS.get(0).block().defaultBlockState());
        DeepCrateBlockEntity deepCrateBlockEntity = gameTestHelper.getBlockEntity(CRATE, DeepCrateBlockEntity.class);

        // The rows cell, not the capacity one: which cell an item sits in no longer says what it does.
        deepCrateBlockEntity.setModuleIn(RegistryInit.ROWS_SLOT, new ItemStack(RegistryInit.MODULE_ITEMS.get(2)));

        if (deepCrateBlockEntity.storage().capacity() != 512) {
            gameTestHelper.fail("the crate should hold 512 a slot, it holds " + deepCrateBlockEntity.storage().capacity());
        }

        gameTestHelper.succeed();
    }

    @GameTest
    public void theStrongestOfTwoModulesWins(GameTestHelper gameTestHelper) {
        gameTestHelper.setBlock(CRATE, RegistryInit.TIERS.get(0).block().defaultBlockState());
        DeepCrateBlockEntity deepCrateBlockEntity = gameTestHelper.getBlockEntity(CRATE, DeepCrateBlockEntity.class);

        deepCrateBlockEntity.setModuleIn(RegistryInit.CAPACITY_SLOT, new ItemStack(RegistryInit.MODULE_ITEMS.get(2)));
        deepCrateBlockEntity.setModuleIn(RegistryInit.ROWS_SLOT, new ItemStack(RegistryInit.MODULE_ITEMS.get(0)));

        if (deepCrateBlockEntity.storage().capacity() != 512) {
            gameTestHelper.fail("the better of 512 and 128 should win, the crate holds " + deepCrateBlockEntity.storage().capacity());
        }

        gameTestHelper.succeed();
    }

    @GameTest
    public void aCellNobodyRegisteredKeepsItsStackRatherThanLosingIt(GameTestHelper gameTestHelper) {
        gameTestHelper.setBlock(CRATE, RegistryInit.TIERS.get(0).block().defaultBlockState());
        DeepCrateBlockEntity deepCrateBlockEntity = gameTestHelper.getBlockEntity(CRATE, DeepCrateBlockEntity.class);
        Identifier stranger = Identifier.fromNamespaceAndPath("somemod", "filter");

        deepCrateBlockEntity.setModuleIn(stranger, new ItemStack(Items.HOPPER));

        if (deepCrateBlockEntity.modules().get(stranger).isEmpty()) {
            gameTestHelper.fail("a stack in an unregistered cell should stay in the table");
        }

        if (DeepCrateApi.moduleSlot(stranger) != null) {
            gameTestHelper.fail("nobody registered that cell, the api should not know it");
        }

        gameTestHelper.succeed();
    }

    @GameTest
    public void anAddonCellTakesWhatItsFilterAllowsAndNothingElse(GameTestHelper gameTestHelper) {
        CrateModuleSlot crateModuleSlot = new CrateModuleSlot(
            Identifier.fromNamespaceAndPath("somemod", "hoppers"), 9, 4, RegistryInit.id("container/slot/module"), itemStack -> itemStack.is(Items.HOPPER)
        );

        if (!crateModuleSlot.accepts(new ItemStack(Items.HOPPER)) || crateModuleSlot.accepts(new ItemStack(Items.STONE))) {
            gameTestHelper.fail("the cell should take a hopper and refuse a stone");
        }

        if (crateModuleSlot.accepts(ItemStack.EMPTY)) {
            gameTestHelper.fail("no cell takes an empty stack");
        }

        gameTestHelper.succeed();
    }
}
```

Ajouter la ligne `"com.dreykaoas.deepcrate.gametest.CrateModuleSlotGameTest"` dans le tableau `fabric-gametest` de `mod/src/gametest/resources/fabric.mod.json`. Sans elle la classe ne tourne pas et le vert ne veut rien dire.

- [ ] **Step 2: lancer le gametest pour vérifier qu'il échoue**

Run: `cd mod && ./gradlew runGametest`
Expected: la compilation échoue, `cannot find symbol: method crateSlotStart()`.

- [ ] **Step 3: fondre `RowModuleSlot` dans `ModuleSlot`**

Remplacer tout `ModuleSlot.java` par :

```java
package com.dreykaoas.deepcrate.inventory;

import com.dreykaoas.deepcrate.api.CrateModuleSlot;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** One cell of the module tab, taking whatever its registered kind allows. */
public class ModuleSlot extends Slot {
    private final CrateModuleSlot kind;

    public ModuleSlot(Container container, int i, int j, int k, CrateModuleSlot crateModuleSlot) {
        super(container, i, j, k);
        this.kind = crateModuleSlot;
    }

    public CrateModuleSlot kind() {
        return this.kind;
    }

    @Override
    public Identifier getNoItemIcon() {
        return this.kind.emptyIcon();
    }

    @Override
    public boolean mayPlace(ItemStack itemStack) {
        return this.kind.accepts(itemStack);
    }

    @Override
    public int getMaxStackSize() {
        return this.kind.stackLimit();
    }
}
```

Supprimer `mod/src/main/java/com/dreykaoas/deepcrate/inventory/RowModuleSlot.java`.

- [ ] **Step 4: faire lire le registre à `ModuleContainer`**

Remplacer les deux constantes et les méthodes qui les utilisent :

```java
    private final List<DeepCrateBlockEntity> crates;
    private final Runnable onChanged;

    public ModuleContainer(List<DeepCrateBlockEntity> crates, Runnable onChanged) {
        this.crates = crates;
        this.onChanged = onChanged;
    }

    @Override
    public int getContainerSize() {
        return DeepCrateApi.moduleSlots().size();
    }

    @Override
    public boolean isEmpty() {
        return this.crates.isEmpty() || this.crates.get(0).modules().isEmpty();
    }

    @Override
    public ItemStack getItem(int i) {
        Identifier identifier = idAt(i);
        return this.crates.isEmpty() || identifier == null ? ItemStack.EMPTY : this.crates.get(0).modules().get(identifier);
    }

    @Override
    public void setItem(int i, ItemStack itemStack) {
        Identifier identifier = idAt(i);
        if (!this.crates.isEmpty() && identifier != null) {
            this.crates.get(0).setModuleIn(identifier, itemStack);
        }

        this.setChanged();
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < this.getContainerSize(); i++) {
            this.setItem(i, ItemStack.EMPTY);
        }
    }

    /**
     * The widest cell of the lot. A slot answers its own limit through {@link ModuleSlot}; this one
     * is what the container promises, and promising less than a cell allows would clamp it.
     */
    @Override
    public int getMaxStackSize() {
        int limit = 1;
        for (CrateModuleSlot crateModuleSlot : DeepCrateApi.moduleSlots()) {
            limit = Math.max(limit, crateModuleSlot.stackLimit());
        }

        return limit;
    }

    private static @Nullable Identifier idAt(int i) {
        List<CrateModuleSlot> kinds = DeepCrateApi.moduleSlots();
        return i < 0 || i >= kinds.size() ? null : kinds.get(i).id();
    }
```

`removeItem` et `removeItemNoUpdate` restent tels quels. Les imports à ajouter : `com.dreykaoas.deepcrate.api.CrateModuleSlot`, `com.dreykaoas.deepcrate.api.DeepCrateApi`, `net.minecraft.resources.Identifier`, `org.jspecify.annotations.Nullable`. L'import `com.dreykaoas.deepcrate.api.RowModule` n'est plus utilisé et part.

- [ ] **Step 5: faire compter les cases au menu**

Dans `DeepCrateMenu`, supprimer `public static final int CRATE_SLOT_START = 2;` et `public static final int ROW_MODULE_Y`. Ajouter le champ, à côté de `crateSlotCount` :

```java
    /**
     * Where the crate's own slots begin, which is how many module cells were registered when this
     * menu was built. Read once: a mod loading a new cell mid-game must not move the slots of a
     * screen that is already open.
     */
    private final int crateSlotStart;
```

Dans le constructeur, remplacer les deux `addSlot` par :

```java
        List<CrateModuleSlot> kinds = DeepCrateApi.moduleSlots();
        this.crateSlotStart = kinds.size();
        this.moduleContainer = crates.isEmpty()
            ? new SimpleContainer(kinds.size()) {
                @Override
                public void setChanged() {
                    super.setChanged();
                    DeepCrateMenu.this.onModuleChanged();
                }
            }
            : new ModuleContainer(crates, this::onModuleChanged);

        container.startOpen(inventory.player);
        this.player = inventory.player;
        for (int i = 0; i < kinds.size(); i++) {
            this.addSlot(new ModuleSlot(this.moduleContainer, i, MODULE_X, MODULE_Y + i * 18, kinds.get(i)));
        }
```

Ajouter l'accesseur :

```java
    public int crateSlotStart() {
        return this.crateSlotStart;
    }
```

Remplacer les trois lectures de la constante : dans `moveModuleToItsSlot` la borne de boucle devient `this.crateSlotStart`, dans `clicked` et dans `quickMoveStack` `CRATE_SLOT_START` devient `this.crateSlotStart`.

Remplacer `onModuleChanged` et `reopenIfRowCountChanged` pour qu'ils lisent toutes les cases :

```java
    private void onModuleChanged() {
        this.capacity = DeepCrateApi.capacityAmong(this.moduleStacks());
        this.reopenIfRowCountChanged();
        ...
    }

    private void reopenIfRowCountChanged() {
        int rows = DeepCrateApi.rowsAmong(this.moduleStacks());
        ...
    }

    /** The cells as a plain list, which is what the two api helpers walk. */
    private List<ItemStack> moduleStacks() {
        List<ItemStack> stacks = new ArrayList<>(this.crateSlotStart);
        for (int i = 0; i < this.moduleContainer.getContainerSize(); i++) {
            stacks.add(this.moduleContainer.getItem(i));
        }

        return stacks;
    }
```

Dans le constructeur, remplacer la dernière lecture par `this.rowModuleCount = DeepCrateApi.rowsAmong(this.moduleStacks());`. Ajouter les imports `com.dreykaoas.deepcrate.api.CrateModuleSlot`. Les imports de `ModuleContainer` restent nécessaires.

- [ ] **Step 6: faire pousser la plaque de l'écran**

Dans `DeepCrateScreen`, remplacer `MODULE_TAB_HEIGHT` par les trois mesures de la texture et une hauteur calculée :

```java
    /** The tab is built as a cap, one cell, a foot: five rows, eighteen, five. */
    private static final int MODULE_TAB_CAP = 5;
    private static final int MODULE_TAB_CELL = 18;
```

Ajouter le champ `private final int moduleTabHeight;` et le poser dans le constructeur :

```java
        this.moduleTabHeight = MODULE_TAB_CAP * 2 + DeepCrateApi.moduleSlots().size() * MODULE_TAB_CELL;
```

Remplacer `renderModuleTab` :

```java
    private void renderModuleTab(GuiGraphics guiGraphics, int x, int y) {
        int tabX = x + DeepCrateMenu.MODULE_X - MODULE_TAB_MARGIN;
        int tabY = y + DeepCrateMenu.MODULE_Y - MODULE_TAB_MARGIN;
        int cells = DeepCrateApi.moduleSlots().size();

        blitTab(guiGraphics, tabX, tabY, 0, MODULE_TAB_CAP);
        for (int cell = 0; cell < cells; cell++) {
            blitTab(guiGraphics, tabX, tabY + MODULE_TAB_CAP + cell * MODULE_TAB_CELL, MODULE_TAB_CAP, MODULE_TAB_CELL);
        }

        blitTab(guiGraphics, tabX, tabY + MODULE_TAB_CAP + cells * MODULE_TAB_CELL, MODULE_TAB_CAP + MODULE_TAB_CELL, MODULE_TAB_CAP);
    }

    private static void blitTab(GuiGraphics guiGraphics, int x, int y, int v, int height) {
        guiGraphics.blit(
            RenderPipelines.GUI_TEXTURED, MODULE_TAB, x, y, 0.0F, (float) v, MODULE_TAB_WIDTH, height, MODULE_TAB_TEXTURE, MODULE_TAB_TEXTURE
        );
    }
```

Dans `isOverModuleTab`, remplacer `MODULE_TAB_HEIGHT` par `this.moduleTabHeight`.

Ajouter l'import `com.dreykaoas.deepcrate.api.DeepCrateApi`.

- [ ] **Step 7: lancer les tests**

Run: `cd mod && ./gradlew test runGametest`
Expected: PASS, 29 tests JUnit et 41 gametests, les 36 d'avant plus les cinq nouveaux.

- [ ] **Step 8: regarder l'écran**

Run: `cd mod && ./gradlew runClientGameTest`
Expected: PASS. Ouvrir `mod/build/run/clientGameTest/screenshots/screen-before-sorting.png` et vérifier que la plaque de gauche a deux cellules, avec son chapeau et son pied, comme avant le changement.

- [ ] **Step 9: commit**

```bash
git add mod/src/main/java/com/dreykaoas/deepcrate/inventory/ \
        mod/src/main/java/com/dreykaoas/deepcrate/client/DeepCrateScreen.java \
        mod/src/gametest/
git rm mod/src/main/java/com/dreykaoas/deepcrate/inventory/RowModuleSlot.java
git commit -m "feat(screen): the module tab grows one cell per registered kind"
```

---

### Task 3: le registre des ordres de tri

**Files:**
- Create: `mod/src/main/java/com/dreykaoas/deepcrate/client/CrateSortOrder.java`
- Create: `mod/src/main/java/com/dreykaoas/deepcrate/client/CrateSortRule.java`
- Create: `mod/src/main/java/com/dreykaoas/deepcrate/client/DeepCrateClientApi.java`
- Delete: `mod/src/main/java/com/dreykaoas/deepcrate/client/CrateSort.java`
- Modify: `mod/src/main/java/com/dreykaoas/deepcrate/client/SortButton.java`
- Modify: `mod/src/main/java/com/dreykaoas/deepcrate/client/DeepCrateScreen.java`
- Modify: `mod/src/main/java/com/dreykaoas/deepcrate/client/DeepCrateClient.java`
- Create: `mod/src/main/resources/assets/deepcrate/textures/gui/sort/name.png`
- Create: `mod/src/main/resources/assets/deepcrate/textures/gui/sort/count.png`
- Delete: `mod/src/main/resources/assets/deepcrate/textures/gui/sort_icons.png`
- Modify: `mod/src/gametest/java/com/dreykaoas/deepcrate/gametest/CrateLookClientGameTest.java`

**Interfaces:**
- Produces: `CrateSortRule` avec `Comparator<Item> comparator(Map<Item, Long> totals, Collator collator)`.
- Produces: `CrateSortOrder(Identifier id, int order, Identifier icon, CrateSortRule rule)` avec `Component label(boolean reversed)`.
- Produces: `DeepCrateClientApi.registerSortOrder(CrateSortOrder)`, `DeepCrateClientApi.sortOrders()`, `DeepCrateClientApi.order(CrateSortOrder, Container, boolean)` rendant `List<Item>`.
- Produces: `SortButton(int x, int y, CrateSortOrder crateSortOrder, boolean reversed, BiConsumer<CrateSortOrder, Boolean> onSort)` avec `CrateSortOrder order()` et `boolean isReversed()`.
- Consumes: rien des tâches précédentes.

- [ ] **Step 1: couper la planche d'icônes en deux fichiers**

La planche fait 32 sur 32 : les lettres en haut, les comptes en bas, le sens normal à gauche et l'inverse à droite. Chaque ordre veut désormais son propre fichier de 16 sur 32, le sens normal au-dessus de l'inverse.

Run:

```bash
cd mod && mkdir -p src/main/resources/assets/deepcrate/textures/gui/sort && python3 -c "
from PIL import Image
sheet = Image.open('src/main/resources/assets/deepcrate/textures/gui/sort_icons.png').convert('RGBA')
for name, row in (('name', 0), ('count', 16)):
    icon = Image.new('RGBA', (16, 32), (0, 0, 0, 0))
    icon.paste(sheet.crop((0, row, 16, row + 16)), (0, 0))
    icon.paste(sheet.crop((16, row, 32, row + 16)), (0, 16))
    icon.save('src/main/resources/assets/deepcrate/textures/gui/sort/%s.png' % name)
    print(name, icon.size)
"
```

Expected: `name (16, 32)` puis `count (16, 32)`.

Puis `git rm mod/src/main/resources/assets/deepcrate/textures/gui/sort_icons.png`.

- [ ] **Step 2: écrire `CrateSortRule`**

```java
package com.dreykaoas.deepcrate.client;

import java.text.Collator;
import java.util.Comparator;
import java.util.Map;
import net.minecraft.world.item.Item;

/**
 * How one order compares two kinds of item.
 *
 * The collator comes from the language the player reads, which is why an order lives on the client:
 * a server holds no language files, so it cannot know that this player reads Pierre where another
 * reads Stone.
 */
@FunctionalInterface
public interface CrateSortRule {
    /**
     * @param totals   how many of each kind the crate holds, every slot counted
     * @param collator built for the player's language, so accents land where a reader expects
     */
    Comparator<Item> comparator(Map<Item, Long> totals, Collator collator);
}
```

- [ ] **Step 3: écrire `CrateSortOrder`**

```java
package com.dreykaoas.deepcrate.client;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * One button above the crate.
 *
 * @param order place in the row, smallest first
 * @param icon  sixteen wide and thirty-two tall: the plain way up top, the reversed one under it
 */
public record CrateSortOrder(Identifier id, int order, Identifier icon, CrateSortRule rule) {
    /** The drawing shows what the next press will do, so the wording follows the same rule. */
    public Component label(boolean reversed) {
        return Component.translatable(
            "screen." + this.id.getNamespace() + ".sort." + this.id.getPath() + (reversed ? "_reversed" : "")
        );
    }
}
```

- [ ] **Step 4: écrire `DeepCrateClientApi`**

```java
package com.dreykaoas.deepcrate.client;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** What a mod can add to the crate screen: for now, an order the sort buttons offer. */
public final class DeepCrateClientApi {
    private static final List<CrateSortOrder> SORT_ORDERS = new ArrayList<>();

    private DeepCrateClientApi() {}

    public static CrateSortOrder registerSortOrder(CrateSortOrder crateSortOrder) {
        for (CrateSortOrder existing : SORT_ORDERS) {
            if (existing.id().equals(crateSortOrder.id())) {
                throw new IllegalStateException("Sort order " + crateSortOrder.id() + " registered twice");
            }
        }

        SORT_ORDERS.add(crateSortOrder);
        SORT_ORDERS.sort(Comparator.comparingInt(CrateSortOrder::order));
        return crateSortOrder;
    }

    public static List<CrateSortOrder> sortOrders() {
        return Collections.unmodifiableList(SORT_ORDERS);
    }

    public static @Nullable CrateSortOrder sortOrder(Identifier identifier) {
        for (CrateSortOrder crateSortOrder : SORT_ORDERS) {
            if (crateSortOrder.id().equals(identifier)) {
                return crateSortOrder;
            }
        }

        return null;
    }

    /**
     * Which items the crate should hold in which order. Worked out here rather than on the server:
     * only this side knows which name the player is reading.
     */
    public static List<Item> order(CrateSortOrder crateSortOrder, Container container, boolean reversed) {
        Map<Item, Long> totals = new LinkedHashMap<>();
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack itemStack = container.getItem(i);
            if (!itemStack.isEmpty()) {
                totals.merge(itemStack.getItem(), (long) itemStack.getCount(), Long::sum);
            }
        }

        Comparator<Item> comparator = crateSortOrder.rule().comparator(totals, Collator.getInstance(gameLocale()));
        List<Item> items = new ArrayList<>(totals.keySet());
        items.sort(reversed ? comparator.reversed() : comparator);
        return items;
    }

    /**
     * The plain name of the item, not the name of the stack: a renamed pile belongs with the rest of
     * its kind rather than under the letter someone typed on an anvil.
     */
    public static String nameOf(Item item) {
        return Component.translatable(item.getDescriptionId()).getString();
    }

    private static Locale gameLocale() {
        String[] parts = Minecraft.getInstance().options.languageCode.split("_");
        return parts.length < 2 ? Locale.of(parts[0]) : Locale.of(parts[0], parts[1].toUpperCase(Locale.ROOT));
    }
}
```

- [ ] **Step 5: inscrire les deux ordres livrés**

Dans `DeepCrateClient.onInitializeClient`, avant les deux appels qui y sont déjà :

```java
        DeepCrateClientApi.registerSortOrder(
            new CrateSortOrder(RegistryInit.id("name"), 0, RegistryInit.id("textures/gui/sort/name.png"), (totals, collator) ->
                Comparator.comparing(DeepCrateClientApi::nameOf, collator))
        );
        DeepCrateClientApi.registerSortOrder(
            new CrateSortOrder(RegistryInit.id("count"), 1, RegistryInit.id("textures/gui/sort/count.png"), (totals, collator) ->
                Comparator.<Item, Long>comparing(totals::get).reversed().thenComparing(Comparator.comparing(DeepCrateClientApi::nameOf, collator)))
        );
```

Supprimer `mod/src/main/java/com/dreykaoas/deepcrate/client/CrateSort.java`.

- [ ] **Step 6: faire porter un ordre au bouton**

Dans `SortButton`, remplacer le champ `crateSort` par `private final CrateSortOrder order;`, supprimer la constante `SHEET`, et remplacer le corps du dessin et du libellé :

```java
    /** Each order brings its own drawing: sixteen wide, thirty-two tall, plain over reversed. */
    private static final int SHEET_HEIGHT = 32;

    public CrateSortOrder order() {
        return this.order;
    }

    @Override
    protected void renderContents(GuiGraphics guiGraphics, int i, int j, float f) {
        this.renderDefaultSprite(guiGraphics);
        guiGraphics.blit(
            RenderPipelines.GUI_TEXTURED,
            this.order.icon(),
            this.getX(),
            this.getY(),
            0.0F,
            this.reversed ? SIZE : 0.0F,
            SIZE,
            SIZE,
            SIZE,
            SHEET_HEIGHT
        );
    }

    private void tellWhatIsNext() {
        Component component = this.order.label(this.reversed);
        this.setMessage(component);
        this.setTooltip(Tooltip.create(component));
    }
```

Le constructeur devient `public SortButton(int x, int y, CrateSortOrder crateSortOrder, boolean reversed, BiConsumer<CrateSortOrder, Boolean> onSort)`, et `onPress` appelle `this.onSort.accept(this.order, this.reversed)`. Les imports `java.util.Locale` et `net.minecraft.resources.Identifier` ne servent plus.

- [ ] **Step 7: poser un bouton par ordre inscrit**

Dans `DeepCrateScreen`, remplacer les deux champs `nameSort` et `countSort` par `private final List<SortButton> sortButtons = new ArrayList<>();` et un champ `private final Map<Identifier, Boolean> sortDirections = new HashMap<>();`.

Dans `init`, remplacer les six lignes qui posent les deux boutons par :

```java
        // Read by name and not by position: a mod loaded since the last build would otherwise shift
        // every direction by one.
        for (SortButton sortButton : this.sortButtons) {
            this.sortDirections.put(sortButton.order().id(), sortButton.isReversed());
        }

        this.sortButtons.clear();
        List<CrateSortOrder> orders = DeepCrateClientApi.sortOrders();
        int perRow = Math.max(1, this.imageWidth / (SortButton.SIZE + SORT_BUTTON_GAP));
        this.sortRows = (orders.size() + perRow - 1) / perRow;
        for (int i = 0; i < orders.size(); i++) {
            CrateSortOrder crateSortOrder = orders.get(i);
            int column = i % perRow;
            int row = this.sortRows - 1 - i / perRow;
            this.sortButtons.add(
                this.addRenderableWidget(
                    new SortButton(
                        this.leftPos + column * (SortButton.SIZE + SORT_BUTTON_GAP),
                        this.topPos - (row + 1) * (SortButton.SIZE + SORT_BUTTON_GAP) - SORT_GAP + SORT_BUTTON_GAP,
                        crateSortOrder,
                        this.sortDirections.getOrDefault(crateSortOrder.id(), false),
                        this::sort
                    )
                )
            );
        }
```

Ajouter le champ `private int sortRows = 1;`.

Remplacer `isOverSortButtons` par une version qui interroge les boutons posés, ce qui est juste quelle que soit la disposition :

```java
    private boolean isOverSortButtons(double d, double e) {
        for (SortButton sortButton : this.sortButtons) {
            if (sortButton.isMouseOver(d, e)) {
                return true;
            }
        }

        return false;
    }
```

Adapter l'appel dans `hasClickedOutside`, qui n'a plus besoin des deux derniers arguments.

Remplacer `sort` :

```java
    private void sort(CrateSortOrder crateSortOrder, boolean reversed) {
        ClientPlayNetworking.send(
            new CrateSortPayload(this.menu.containerId, DeepCrateClientApi.order(crateSortOrder, this.menu.getContainer(), reversed))
        );
    }
```

- [ ] **Step 8: adapter le test client, qui cherche les boutons par leur libellé**

Dans `CrateLookClientGameTest`, les deux appels `press(context, "screen.deepcrate.sort.name")` et `press(context, "screen.deepcrate.sort.count")` restent valides, puisque les clés de traduction ne changent pas. Aucune modification n'est nécessaire ; le vérifier en relisant le fichier plutôt qu'en le supposant.

- [ ] **Step 9: lancer les tests**

Run: `cd mod && ./gradlew test runGametest`
Expected: PASS, 29 et 41.

Run: `cd mod && ./gradlew runClientGameTest`
Expected: PASS. Ouvrir `mod/build/run/clientGameTest/screenshots/screen-sorted-by-name.png` et `screen-sorted-by-count.png` : les deux boutons doivent être au même endroit qu'avant, avec les mêmes dessins, et le coffre trié.

- [ ] **Step 10: commit**

```bash
git add mod/src/main/java/com/dreykaoas/deepcrate/client/ mod/src/main/resources/assets/deepcrate/textures/gui/
git rm mod/src/main/java/com/dreykaoas/deepcrate/client/CrateSort.java \
       mod/src/main/resources/assets/deepcrate/textures/gui/sort_icons.png
git commit -m "feat(sort): orders become a registry, one button per registered order"
```

---

### Task 4: la largeur devient une propriété du palier

**Files:**
- Modify: `mod/src/main/java/com/dreykaoas/deepcrate/api/CrateTier.java`
- Modify: `mod/src/main/java/com/dreykaoas/deepcrate/inventory/CrateOpenData.java`
- Modify: `mod/src/main/java/com/dreykaoas/deepcrate/inventory/DeepCrateMenu.java`
- Modify: `mod/src/main/java/com/dreykaoas/deepcrate/block/DeepCrateBlockEntity.java`
- Modify: `mod/src/main/java/com/dreykaoas/deepcrate/client/DeepCrateScreen.java`
- Test: `mod/src/test/java/com/dreykaoas/deepcrate/CrateTierTest.java`
- Test: `mod/src/gametest/java/com/dreykaoas/deepcrate/gametest/CrateColumnsGameTest.java`
- Modify: `mod/src/gametest/resources/fabric.mod.json`
- Modify: `mod/src/gametest/java/com/dreykaoas/deepcrate/gametest/CrateLookClientGameTest.java`

**Interfaces:**
- Produces: `CrateTier(Identifier id, int rows, int columns, Block block)` avec le constructeur court `CrateTier(Identifier, int, Block)` qui donne neuf colonnes, et `CrateTier.DEFAULT_COLUMNS` remplaçant `CrateTier.COLUMNS`.
- Produces: `CrateOpenData(int slotCount, int rowsPerPage, int pageCount, int capacity, int columns)`.
- Produces: `DeepCrateMenu(int, Inventory, Container, List<DeepCrateBlockEntity>, CrateLayout, int columns)` et `DeepCrateMenu.columns()`.
- Consumes: `DeepCrateMenu.crateSlotStart()` de la tâche 2.

- [ ] **Step 1: écrire le test JUnit qui échoue**

Créer `mod/src/test/java/com/dreykaoas/deepcrate/CrateTierTest.java` :

```java
package com.dreykaoas.deepcrate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dreykaoas.deepcrate.api.CrateTier;
import net.minecraft.SharedConstants;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CrateTierTest {
    private static final Identifier ID = Identifier.fromNamespaceAndPath("deepcrate", "test_crate");

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void theShortFormStaysNineWide() {
        CrateTier crateTier = new CrateTier(ID, 3, Blocks.CHEST);

        assertEquals(CrateTier.DEFAULT_COLUMNS, crateTier.columns());
        assertEquals(27, crateTier.slotCount());
    }

    @Test
    void aWiderTierCountsItsOwnColumns() {
        CrateTier crateTier = new CrateTier(ID, 3, 12, Blocks.CHEST);

        assertEquals(36, crateTier.slotCount());
    }

    @Test
    void aTierWithNoColumnIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new CrateTier(ID, 3, 0, Blocks.CHEST));
    }
}
```

- [ ] **Step 2: lancer le test pour vérifier qu'il échoue**

Run: `cd mod && ./gradlew test --tests 'com.dreykaoas.deepcrate.CrateTierTest'`
Expected: la compilation échoue, `cannot find symbol: DEFAULT_COLUMNS`.

- [ ] **Step 3: ouvrir `CrateTier`**

```java
package com.dreykaoas.deepcrate.api;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

/**
 * One kind of crate: how large its grid is, and which block carries it.
 *
 * @param id      namespaced name, also the block and item name
 * @param rows    rows of slots, before any page split
 * @param columns slots to a row; the screen panel is built to whatever this says
 * @param block   the block registered for this tier
 */
public record CrateTier(Identifier id, int rows, int columns, Block block) {
    /** What a chest is wide, and what a tier gets when it does not say. */
    public static final int DEFAULT_COLUMNS = 9;

    public CrateTier {
        if (rows < 1) {
            throw new IllegalArgumentException("Crate tier " + id + " needs at least one row, got " + rows);
        }

        if (columns < 1) {
            throw new IllegalArgumentException("Crate tier " + id + " needs at least one column, got " + columns);
        }
    }

    public CrateTier(Identifier identifier, int rows, Block block) {
        this(identifier, rows, DEFAULT_COLUMNS, block);
    }

    public int slotCount() {
        return this.rows * this.columns;
    }
}
```

- [ ] **Step 4: lancer le test pour vérifier qu'il passe**

Run: `cd mod && ./gradlew test --tests 'com.dreykaoas.deepcrate.CrateTierTest'`
Expected: PASS, trois tests.

- [ ] **Step 5: faire voyager la largeur**

Dans `CrateOpenData`, ajouter un cinquième champ `int columns` et le cinquième `ByteBufCodecs.VAR_INT, CrateOpenData::columns,` avant `CrateOpenData::new`.

Dans `DeepCrateBlockEntity`, remplacer les quatre lectures de `CrateTier.COLUMNS` :

- ligne du champ : `new CrateStorage(CrateTier.DEFAULT_COLUMNS, DeepCrateApi.BASE_CAPACITY)`, la taille de départ avant que le bloc soit connu ;
- `trimToRows` : `this.storage().trimTo(crateTier.slotCount() + this.extraRows() * crateTier.columns())` ;
- `getScreenOpeningData` et `createMenu` : `container.getContainerSize() / this.tier().columns()`, et `getScreenOpeningData` rend `new CrateOpenData(..., container.getMaxStackSize(), this.tier().columns())` ;
- `alignStorageWithTier` : `crateTier.slotCount() + this.extraRows() * crateTier.columns()` ;
- `loadAdditional` : `CrateTier.DEFAULT_COLUMNS` aux deux endroits.

`createMenu` passe la largeur au menu : `new DeepCrateMenu(i, inventory, container, crates, crateLayout, this.tier().columns())`.

- [ ] **Step 6: faire lire la largeur au menu**

Dans `DeepCrateMenu`, ajouter le champ `private final int columns;` et l'accesseur `public int columns()`. Le constructeur client devient :

```java
    public DeepCrateMenu(int i, Inventory inventory, CrateOpenData crateOpenData) {
        this(
            i,
            inventory,
            new CrateContainer(crateOpenData.slotCount(), crateOpenData.capacity()),
            List.of(),
            new CrateLayout(crateOpenData.rowsPerPage(), crateOpenData.pageCount()),
            crateOpenData.columns()
        );
    }
```

Dans la boucle qui pose les cases, `CrateTier.COLUMNS` devient `columns`. La ligne de l'inventaire du joueur devient :

```java
        // The player keeps nine columns whatever the crate is: their inventory is not the crate's.
        // Centred rather than flush left, so a crate wider than a chest does not look lopsided.
        int playerLeft = GRID_LEFT + (columns - CrateTier.DEFAULT_COLUMNS) * 9;
        this.addStandardInventorySlots(inventory, playerLeft, GRID_TOP + crateLayout.rowsPerPage() * 18 + 13);
```

- [ ] **Step 7: bâtir le fond en trois morceaux**

Dans `DeepCrateScreen`, ajouter les mesures et le champ :

```java
    /** The panel of generic_54: seven pixels of border, nine cells of eighteen, seven more. */
    private static final int PANEL_BORDER = 7;
    private static final int PANEL_WIDTH = 176;
    private static final int CELL = 18;

    private final int columns;
```

Dans le constructeur :

```java
        this.columns = deepCrateMenu.columns();
        this.imageWidth = PANEL_BORDER * 2 + this.columns * CELL;
```

Remplacer les blits de `renderBg` et `fillBarePanel` par des appels à une bande tuilée :

```java
    private void blitBand(GuiGraphics guiGraphics, int x, int y, int v, int height) {
        blit(guiGraphics, x, y, 0, v, PANEL_BORDER, height);
        for (int column = 0; column < this.columns; column++) {
            blit(guiGraphics, x + PANEL_BORDER + column * CELL, y, PANEL_BORDER, v, CELL, height);
        }

        blit(guiGraphics, x + PANEL_BORDER + this.columns * CELL, y, PANEL_WIDTH - PANEL_BORDER, v, PANEL_BORDER, height);
    }
```

`renderBg` devient :

```java
        blitBand(guiGraphics, x, y, 0, HEADER_HEIGHT + rowsOnPage * 18);
        if (rowsOnPage < this.rows) {
            this.fillBarePanel(guiGraphics, x, y + HEADER_HEIGHT + rowsOnPage * 18, (this.rows - rowsOnPage) * 18);
        }

        blitBand(guiGraphics, x, y + HEADER_HEIGHT + this.rows * 18, PLAYER_PANEL_V, PLAYER_PANEL_HEIGHT);
        this.renderModuleTab(guiGraphics, x, y);
```

et `fillBarePanel` appelle `blitBand` au lieu de `blit`.

La bande du joueur pose un problème que la tuile ne règle pas : ses cases sont dessinées dans la texture aux colonnes 7 à 169, et un coffre plus large les répéterait. Elle se dessine donc en deux temps, un fond nu sur toute la largeur puis la bande de l'inventaire, entière, centrée :

```java
        blitBand(guiGraphics, x, y + HEADER_HEIGHT + this.rows * 18, BARE_PANEL_V, PLAYER_PANEL_HEIGHT);
        int playerLeft = x + (this.imageWidth - PANEL_WIDTH) / 2;
        blit(guiGraphics, playerLeft, y + HEADER_HEIGHT + this.rows * 18, 0, PLAYER_PANEL_V, PANEL_WIDTH, PLAYER_PANEL_HEIGHT);
```

À neuf colonnes `playerLeft` vaut `x` et le second blit recouvre exactement le premier, donc le résultat ne bouge pas d'un pixel.

- [ ] **Step 8: écrire le gametest de largeur**

Créer `mod/src/gametest/java/com/dreykaoas/deepcrate/gametest/CrateColumnsGameTest.java` :

```java
package com.dreykaoas.deepcrate.gametest;

import com.dreykaoas.deepcrate.api.CrateTier;
import com.dreykaoas.deepcrate.block.DeepCrateBlockEntity;
import com.dreykaoas.deepcrate.init.RegistryInit;
import com.dreykaoas.deepcrate.inventory.DeepCrateMenu;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** A crate is as wide as its tier says, and the six shipped tiers say nine. */
public class CrateColumnsGameTest {
    private static final BlockPos CRATE = new BlockPos(1, 1, 1);

    @GameTest
    public void theShippedTiersAreNineWide(GameTestHelper gameTestHelper) {
        for (CrateTier crateTier : RegistryInit.TIERS) {
            if (crateTier.columns() != CrateTier.DEFAULT_COLUMNS) {
                gameTestHelper.fail(crateTier.id() + " should be nine wide, it is " + crateTier.columns());
            }
        }

        gameTestHelper.succeed();
    }

    @GameTest
    public void theMenuTellsTheClientHowWideTheCrateIs(GameTestHelper gameTestHelper) {
        ServerPlayer serverPlayer = gameTestHelper.makeMockServerPlayerInLevel();
        gameTestHelper.setBlock(CRATE, RegistryInit.TIERS.get(0).block().defaultBlockState());
        DeepCrateBlockEntity deepCrateBlockEntity = gameTestHelper.getBlockEntity(CRATE, DeepCrateBlockEntity.class);

        DeepCrateMenu deepCrateMenu = (DeepCrateMenu) deepCrateBlockEntity.createMenu(1, serverPlayer.getInventory(), serverPlayer);

        if (deepCrateMenu.columns() != CrateTier.DEFAULT_COLUMNS) {
            gameTestHelper.fail("the menu should say nine columns, it says " + deepCrateMenu.columns());
        }

        if (deepCrateBlockEntity.getScreenOpeningData(serverPlayer).columns() != CrateTier.DEFAULT_COLUMNS) {
            gameTestHelper.fail("the opening packet should carry nine columns");
        }

        gameTestHelper.succeed();
    }

    @GameTest
    public void rowsAddAsManySlotsAsTheTierIsWide(GameTestHelper gameTestHelper) {
        gameTestHelper.setBlock(CRATE, RegistryInit.TIERS.get(0).block().defaultBlockState());
        DeepCrateBlockEntity deepCrateBlockEntity = gameTestHelper.getBlockEntity(CRATE, DeepCrateBlockEntity.class);

        deepCrateBlockEntity.setRowModules(new ItemStack(RegistryInit.ROW_MODULE_ITEM, 2));

        int expected = RegistryInit.TIERS.get(0).slotCount() + 2 * RegistryInit.TIERS.get(0).columns();
        if (deepCrateBlockEntity.storage().size() != expected) {
            gameTestHelper.fail("two row modules should give " + expected + " slots, the crate has " + deepCrateBlockEntity.storage().size());
        }

        gameTestHelper.succeed();
    }
}
```

Ajouter `"com.dreykaoas.deepcrate.gametest.CrateColumnsGameTest"` dans le tableau `fabric-gametest`.

- [ ] **Step 9: lancer les tests**

Run: `cd mod && ./gradlew test runGametest`
Expected: PASS, 32 tests JUnit et 44 gametests.

- [ ] **Step 10: photographier un coffre étroit et un coffre large**

Le seul moyen de regarder une largeur autre que neuf est d'inscrire un palier pour la durée du test. Dans `CrateLookClientGameTest`, ajouter avant les captures d'écran de coffre :

```java
            context.takeScreenshot("screen-nine-columns");
```

juste après `screen-before-sorting`, puis à la fin de `runTest`, une scène de plus :

```java
            // A width other than nine has no shipped tier, so the panel is checked through the menu
            // the client builds from an opening packet that claims one.
            context.runOnClient(minecraft -> minecraft.setScreen(new DeepCrateScreen(
                new DeepCrateMenu(97, minecraft.player.getInventory(), new CrateOpenData(36, 3, 1, 512, 12)),
                minecraft.player.getInventory(),
                Component.literal("Twelve columns")
            )));
            context.waitTicks(20);
            context.takeScreenshot("screen-twelve-columns");

            context.runOnClient(minecraft -> minecraft.setScreen(new DeepCrateScreen(
                new DeepCrateMenu(98, minecraft.player.getInventory(), new CrateOpenData(9, 3, 1, 64, 3)),
                minecraft.player.getInventory(),
                Component.literal("Three columns")
            )));
            context.waitTicks(20);
            context.takeScreenshot("screen-three-columns");

            context.setScreen(() -> null);
```

Ajouter les imports `com.dreykaoas.deepcrate.inventory.CrateOpenData` et `com.dreykaoas.deepcrate.inventory.DeepCrateMenu`.

Run: `cd mod && ./gradlew runClientGameTest`
Expected: PASS. Comparer `screen-nine-columns.png` avec la capture d'avant le changement : les deux doivent être identiques. Regarder `screen-twelve-columns.png` et `screen-three-columns.png` : le cadre doit être fermé des deux côtés, la grille pleine, l'inventaire du joueur centré et entier.

- [ ] **Step 11: commit**

```bash
git add mod/src/main/java/com/dreykaoas/deepcrate/ mod/src/test/ mod/src/gametest/
git commit -m "feat(api): a tier says how wide it is, and the panel is built to that width"
```

---

### Task 5: la contenance passe par un événement

**Files:**
- Create: `mod/src/main/java/com/dreykaoas/deepcrate/api/CrateCapacityCallback.java`
- Modify: `mod/src/main/java/com/dreykaoas/deepcrate/inventory/CrateStorage.java`
- Modify: `mod/src/main/java/com/dreykaoas/deepcrate/inventory/DeepCrateSlot.java`
- Modify: `mod/src/main/java/com/dreykaoas/deepcrate/block/DeepCrateBlockEntity.java`
- Test: `mod/src/test/java/com/dreykaoas/deepcrate/CrateStorageTest.java`

**Interfaces:**
- Produces: `CrateCapacityCallback.EVENT` de type `Event<CrateCapacityCallback>` avec `int capacity(@Nullable CrateTier tier, ItemStack itemStack, int proposed)`.
- Produces: `CrateStorage.accepts(ItemStack)` rendant un `boolean`, et `CrateStorage.setTier(@Nullable CrateTier)`.
- Consumes: `CrateTier` de la tâche 4.

- [ ] **Step 1: écrire les tests qui échouent**

Ajouter à `CrateStorageTest` :

```java
    @Test
    void anEventCanRaiseWhatOneSlotHolds() {
        CrateStorage crateStorage = new CrateStorage(9, 64);
        CrateCapacityCallback listener = (tier, itemStack, proposed) -> itemStack.is(Items.DIAMOND) ? proposed * 4 : proposed;
        CrateCapacityCallback.EVENT.register(listener);

        try {
            assertEquals(256, crateStorage.capacityFor(new ItemStack(Items.DIAMOND)));
            assertEquals(64, crateStorage.capacityFor(new ItemStack(Items.COBBLESTONE)));
        } finally {
            CrateCapacityCallback.EVENT.unregister(listener);
        }
    }

    @Test
    void aRefusedItemIsNeverStoredAndNeverDestroyed() {
        CrateStorage crateStorage = new CrateStorage(9, 64);
        CrateCapacityCallback listener = (tier, itemStack, proposed) -> itemStack.is(Items.GUNPOWDER) ? 0 : proposed;
        CrateCapacityCallback.EVENT.register(listener);

        try {
            ItemStack leftover = crateStorage.insert(new ItemStack(Items.GUNPOWDER, 30));

            assertEquals(30, leftover.getCount());
            assertTrue(crateStorage.isEmpty());
            assertFalse(crateStorage.accepts(new ItemStack(Items.GUNPOWDER)));
            // Never zero: a slot told it holds nothing would write a stack of nothing, which is how
            // items get destroyed rather than refused.
            assertEquals(1, crateStorage.capacityFor(new ItemStack(Items.GUNPOWDER)));
        } finally {
            CrateCapacityCallback.EVENT.unregister(listener);
        }
    }

    @Test
    void whatIsAlreadyStoredSurvivesTheCrateRefusingIt() {
        CrateStorage crateStorage = new CrateStorage(9, 64);
        crateStorage.set(0, new ItemStack(Items.GUNPOWDER, 30));
        CrateCapacityCallback listener = (tier, itemStack, proposed) -> itemStack.is(Items.GUNPOWDER) ? 0 : proposed;
        CrateCapacityCallback.EVENT.register(listener);

        try {
            assertTrue(crateStorage.overflow().isEmpty());
            assertEquals(30, crateStorage.get(0).getCount());
        } finally {
            CrateCapacityCallback.EVENT.unregister(listener);
        }
    }
```

Ajouter les imports `com.dreykaoas.deepcrate.api.CrateCapacityCallback` et `static org.junit.jupiter.api.Assertions.assertFalse`.

Fabric's `Event` n'a pas de `unregister`. Le remplacer par une bascule que le test pose et retire lui-même : un champ statique `AtomicReference<CrateCapacityCallback>` dans la classe de test, inscrit une seule fois dans `@BeforeAll`, et que chaque test remplit puis vide dans son `finally`.

```java
    private static final AtomicReference<CrateCapacityCallback> RULE = new AtomicReference<>();

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        // Fabric events never let go of a listener, so the tests swap the rule behind one listener
        // rather than adding one each time.
        CrateCapacityCallback.EVENT.register((tier, itemStack, proposed) -> {
            CrateCapacityCallback rule = RULE.get();
            return rule == null ? proposed : rule.capacity(tier, itemStack, proposed);
        });
    }
```

Chaque test pose `RULE.set(...)` et rend `RULE.set(null)` dans son `finally`.

- [ ] **Step 2: lancer les tests pour vérifier qu'ils échouent**

Run: `cd mod && ./gradlew test --tests 'com.dreykaoas.deepcrate.CrateStorageTest'`
Expected: la compilation échoue, `package com.dreykaoas.deepcrate.api.CrateCapacityCallback does not exist`.

- [ ] **Step 3: écrire l'événement**

```java
package com.dreykaoas.deepcrate.api;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Last word on what one crate slot holds for one item.
 *
 * Without this the rule is frozen: whatever does not stack keeps its own limit, everything else takes
 * the module's number. A crate that refuses gunpowder, or one that holds four times as much ore, has
 * no way in.
 */
@FunctionalInterface
public interface CrateCapacityCallback {
    Event<CrateCapacityCallback> EVENT = EventFactory.createArrayBacked(
        CrateCapacityCallback.class,
        listeners -> (tier, itemStack, proposed) -> {
            int current = proposed;
            for (CrateCapacityCallback listener : listeners) {
                current = listener.capacity(tier, itemStack, current);
            }

            return current;
        }
    );

    /**
     * @param tier     the crate asking, null while a crate is being read from a save and its block is
     *                 not bound yet
     * @param proposed what the previous listener decided, starting from the crate's own rule
     * @return the limit for this item; zero or less means this crate refuses it
     */
    int capacity(@Nullable CrateTier tier, ItemStack itemStack, int proposed);
}
```

- [ ] **Step 4: faire passer le stockage par l'événement**

Dans `CrateStorage`, ajouter le champ et son poseur :

```java
    private @Nullable CrateTier tier;

    /** Set when the crate lines up with its block, which is the first moment the tier is known. */
    public void setTier(@Nullable CrateTier crateTier) {
        this.tier = crateTier;
    }
```

Remplacer `capacityFor` et ajouter `accepts` :

```java
    public int capacityFor(ItemStack itemStack) {
        // Never below one. A slot told it holds nothing would write a stack of nothing, and that is
        // how an item gets destroyed rather than refused; refusing is what accepts is for.
        return Math.max(1, this.limitFor(itemStack));
    }

    /** Whether this crate takes the item at all. */
    public boolean accepts(ItemStack itemStack) {
        return this.limitFor(itemStack) > 0;
    }

    private int limitFor(ItemStack itemStack) {
        int proposed = !itemStack.isEmpty() && itemStack.getMaxStackSize() <= 1 ? itemStack.getMaxStackSize() : this.capacity;
        return CrateCapacityCallback.EVENT.invoker().capacity(this.tier, itemStack, proposed);
    }
```

Dans `automationCapacityFor`, rendre zéro pour un objet refusé :

```java
    public int automationCapacityFor(ItemStack itemStack) {
        if (!this.accepts(itemStack)) {
            return 0;
        }

        int limit = this.capacityFor(itemStack);
        return DeepCrateApi.AUTOMATION_LIMITED ? Math.min(limit, VANILLA_LIMIT) : limit;
    }
```

Dans `insert`, refuser d'entrée :

```java
        if (itemStack.isEmpty() || !this.accepts(itemStack)) {
            return itemStack;
        }
```

Dans `overflow`, ne cracher que ce qui dépasse une limite qui existe : la boucle utilise déjà `capacityFor`, qui ne descend pas sous un, donc une pile déjà rangée d'un objet refusé garde sa limite d'avant et ne sort pas. Vérifier en relisant que `overflow` appelle bien `capacityFor` et non `limitFor`.

Ajouter les imports `com.dreykaoas.deepcrate.api.CrateCapacityCallback`, `com.dreykaoas.deepcrate.api.CrateTier`, `org.jspecify.annotations.Nullable`.

- [ ] **Step 5: faire refuser la case du menu**

Dans `DeepCrateSlot`, ajouter :

```java
    @Override
    public boolean mayPlace(ItemStack itemStack) {
        return !(this.container instanceof DeepCrateBlockEntity deepCrateBlockEntity) || deepCrateBlockEntity.storage().accepts(itemStack);
    }
```

Le conteneur d'une paire est un `CratePairContainer` et non un coffre : la question passe alors par le conteneur, qui la renvoie à sa première moitié. Ajouter dans `CratePairContainer` :

```java
    @Override
    public boolean canPlaceItem(int i, ItemStack itemStack) {
        return this.holder.canPlaceItem(i, itemStack);
    }
```

et dans `DeepCrateBlockEntity` :

```java
    @Override
    public boolean canPlaceItem(int i, ItemStack itemStack) {
        return this.storage().accepts(itemStack);
    }
```

`DeepCrateSlot.mayPlace` devient alors simplement `return this.container.canPlaceItem(this.getContainerSlot(), itemStack);`, ce que `Slot.mayPlace` fait déjà par défaut : la seule ligne à écrire est celle du coffre et celle de la paire.

- [ ] **Step 6: poser le palier sur le stockage**

Dans `DeepCrateBlockEntity.alignStorageWithTier`, après le test de nullité :

```java
        this.storage.setTier(crateTier);
```

- [ ] **Step 7: lancer les tests**

Run: `cd mod && ./gradlew test runGametest`
Expected: PASS, 35 tests JUnit et 44 gametests.

- [ ] **Step 8: commit**

```bash
git add mod/src/main/java/com/dreykaoas/deepcrate/ mod/src/test/
git commit -m "feat(api): an event has the last word on what a slot holds, and on what a crate refuses"
```

---

### Task 6: la greffe sur l'écran

**Files:**
- Create: `mod/src/main/java/com/dreykaoas/deepcrate/client/CrateScreenArea.java`
- Create: `mod/src/main/java/com/dreykaoas/deepcrate/client/CrateScreenCallback.java`
- Create: `mod/src/main/java/com/dreykaoas/deepcrate/client/CrateTooltipCallback.java`
- Modify: `mod/src/main/java/com/dreykaoas/deepcrate/client/DeepCrateScreen.java`
- Modify: `mod/src/gametest/java/com/dreykaoas/deepcrate/gametest/CrateLookClientGameTest.java`

**Interfaces:**
- Produces: `CrateScreenArea` avec `int left()`, `int top()`, `int width()`, `int height()`, `<T extends AbstractWidget> T addWidget(T)`, `void keepClickable(int x, int y, int width, int height)`.
- Produces: `CrateScreenCallback.EVENT` avec `void onScreenInit(DeepCrateScreen screen, CrateScreenArea area)`.
- Produces: `CrateTooltipCallback.EVENT` avec `void addLines(DeepCrateMenu menu, ItemStack itemStack, List<Component> lines)`.
- Consumes: les boutons de tri de la tâche 3, la largeur de la tâche 4.

- [ ] **Step 1: écrire `CrateScreenArea`**

```java
package com.dreykaoas.deepcrate.client;

import net.minecraft.client.gui.components.AbstractWidget;

/**
 * The room a mod is given on the crate screen.
 *
 * A widget dropped outside the panel has to be named through {@link #keepClickable}: the game counts
 * a click outside a container screen as a click into the world, and releasing one there throws on the
 * ground whatever the player is carrying.
 */
public interface CrateScreenArea {
    int left();

    int top();

    int width();

    int height();

    <T extends AbstractWidget> T addWidget(T widget);

    void keepClickable(int x, int y, int width, int height);
}
```

- [ ] **Step 2: écrire les deux événements**

```java
package com.dreykaoas.deepcrate.client;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/** Called once each time a crate screen is laid out, which is also each time the window is resized. */
@FunctionalInterface
public interface CrateScreenCallback {
    Event<CrateScreenCallback> EVENT = EventFactory.createArrayBacked(
        CrateScreenCallback.class,
        listeners -> (screen, area) -> {
            for (CrateScreenCallback listener : listeners) {
                listener.onScreenInit(screen, area);
            }
        }
    );

    void onScreenInit(DeepCrateScreen screen, CrateScreenArea area);
}
```

```java
package com.dreykaoas.deepcrate.client;

import com.dreykaoas.deepcrate.inventory.DeepCrateMenu;
import java.util.List;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** The lines of a tooltip on a crate screen, before they are drawn. */
@FunctionalInterface
public interface CrateTooltipCallback {
    Event<CrateTooltipCallback> EVENT = EventFactory.createArrayBacked(
        CrateTooltipCallback.class,
        listeners -> (menu, itemStack, lines) -> {
            for (CrateTooltipCallback listener : listeners) {
                listener.addLines(menu, itemStack, lines);
            }
        }
    );

    /**
     * @param lines the lines as they stand, writable: adding at index 1 puts a line right under the
     *              item's name
     */
    void addLines(DeepCrateMenu menu, ItemStack itemStack, List<Component> lines);
}
```

- [ ] **Step 3: rassembler les rectangles à épargner**

Dans `DeepCrateScreen`, remplacer les trois méthodes `isOverPageButtons`, `isOverModuleTab` et `isOverSortButtons` par une liste et un seul test :

```java
    /** Everything drawn outside the panel, so a click released on it is not a click into the world. */
    private final List<int[]> clickable = new ArrayList<>();

    private void keepClickable(int x, int y, int width, int height) {
        this.clickable.add(new int[] {x, y, width, height});
    }

    @Override
    protected boolean hasClickedOutside(double d, double e, int i, int j) {
        if (!super.hasClickedOutside(d, e, i, j)) {
            return false;
        }

        for (int[] area : this.clickable) {
            if (d >= area[0] && d < area[0] + area[2] && e >= area[1] && e < area[1] + area[3]) {
                return false;
            }
        }

        return true;
    }
```

Dans `init`, vider la liste au début et y déclarer les trois choses de la maison : la plaque de module, chaque bouton de tri, chaque bouton de page.

```java
        this.clickable.clear();
        this.keepClickable(
            this.leftPos + DeepCrateMenu.MODULE_X - MODULE_TAB_MARGIN,
            this.topPos + DeepCrateMenu.MODULE_Y - MODULE_TAB_MARGIN,
            MODULE_TAB_WIDTH,
            this.moduleTabHeight
        );
```

et, après chaque `addRenderableWidget` de bouton, `this.keepClickable(button.getX(), button.getY(), button.getWidth(), button.getHeight());`.

- [ ] **Step 4: faire partir les deux événements**

À la fin de `init`, après la boucle des boutons de page :

```java
        CrateScreenCallback.EVENT.invoker().onScreenInit(this, this.area);
```

avec le champ, posé une fois dans le constructeur :

```java
    private final CrateScreenArea area = new CrateScreenArea() {
        @Override
        public int left() {
            return DeepCrateScreen.this.leftPos;
        }

        @Override
        public int top() {
            return DeepCrateScreen.this.topPos;
        }

        @Override
        public int width() {
            return DeepCrateScreen.this.imageWidth;
        }

        @Override
        public int height() {
            return DeepCrateScreen.this.imageHeight;
        }

        @Override
        public <T extends AbstractWidget> T addWidget(T widget) {
            return DeepCrateScreen.this.addRenderableWidget(widget);
        }

        @Override
        public void keepClickable(int x, int y, int width, int height) {
            DeepCrateScreen.this.keepClickable(x, y, width, height);
        }
    };
```

Et `getTooltipFromContainerItem` devient :

```java
    @Override
    protected List<Component> getTooltipFromContainerItem(ItemStack itemStack) {
        List<Component> lines = new ArrayList<>(super.getTooltipFromContainerItem(itemStack));
        CrateTooltipCallback.EVENT.invoker().addLines(this.menu, itemStack, lines);
        return lines;
    }
```

La ligne du compte réel, aujourd'hui écrite dans cette méthode, devient le premier abonné de son propre événement. L'inscrire dans `DeepCrateClient.onInitializeClient` :

```java
        // The mod's own line goes through the event rather than around it, which is how we know the
        // event is enough for anyone else's.
        CrateTooltipCallback.EVENT.register((menu, itemStack, lines) -> {
            if (itemStack.getCount() > DeepCrateScreen.ABBREVIATE_ABOVE) {
                lines.add(1, Component.translatable("screen.deepcrate.count", itemStack.getCount()));
            }
        });
```

`ABBREVIATE_ABOVE` passe de `private` à `public static final`. La condition sur `hoveredSlot instanceof DeepCrateSlot` disparaît de la méthode et n'a pas besoin d'être portée : une pile de l'inventaire du joueur ne dépasse jamais 999.

- [ ] **Step 5: écrire le test client de la greffe**

Dans `CrateLookClientGameTest`, avant l'ouverture du coffre, inscrire un bouton de passage et le photographier :

```java
            // An addon's button, registered from the test itself: if a mod can put one there, so can
            // anyone, and the picture is the proof.
            context.runOnClient(minecraft -> CrateScreenCallback.EVENT.register((screen, area) -> {
                Button button = Button.builder(Component.literal("+"), ignored -> {})
                    .bounds(area.left() + area.width() + 3, area.top() + area.height() - 20, 20, 20)
                    .build();
                area.addWidget(button);
                area.keepClickable(button.getX(), button.getY(), button.getWidth(), button.getHeight());
            }));
```

Après `screen-before-sorting`, ajouter `context.takeScreenshot("screen-with-an-addon-button");`.

Ajouter les imports `com.dreykaoas.deepcrate.client.CrateScreenCallback` et `net.minecraft.client.gui.components.Button`.

- [ ] **Step 6: lancer les tests**

Run: `cd mod && ./gradlew test runGametest`
Expected: PASS, 35 et 44.

Run: `cd mod && ./gradlew runClientGameTest`
Expected: PASS. Sur `screen-with-an-addon-button.png`, un bouton portant un plus se tient contre le bord droit du panneau, en bas. Sur `screen-before-sorting.png`, l'infobulle du compte doit toujours sortir quand le pointeur est sur une pile de plus de 999.

- [ ] **Step 7: commit**

```bash
git add mod/src/main/java/com/dreykaoas/deepcrate/client/ mod/src/gametest/
git commit -m "feat(screen): a mod can hang a widget and a tooltip line on the crate screen"
```

---

### Task 7: la documentation dit ce qui est ouvert

**Files:**
- Modify: `README.md`

**Interfaces:**
- Consumes: tout ce que les six tâches précédentes ont produit.

- [ ] **Step 1: réécrire la section "For other mods"**

Elle promet trois choses parce que la demande d'origine en nommait trois. Il y en a huit désormais. Remplacer le bloc de code par un exemple qui les touche toutes, en gardant le ton du fichier :

```java
public class MyAddon implements DeepCrateAddon {
    @Override
    public void onDeepCrateInit() {
        DeepCrateApi.registerTier(new CrateTier(id("my_crate"), 9, 12, MY_BLOCK));
        DeepCrateApi.registerModule(new CrateModule(id("my_module"), 4096, MY_TAG));
        DeepCrateApi.registerModuleSlot(
            new CrateModuleSlot(id("my_filter"), 2, 1, id("container/slot/module"), CrateModuleSlot.tagged(MY_FILTER_TAG))
        );
        CrateLayoutCallback.EVENT.register((tier, rows, layout) -> new CrateLayout(3, (rows + 2) / 3));
        CrateCapacityCallback.EVENT.register((tier, stack, proposed) -> stack.is(Items.GUNPOWDER) ? 0 : proposed);
    }
}
```

Dire ensuite, en prose, que le client a son propre point d'entrée pour un ordre de tri et pour la greffe sur l'écran, et rappeler qu'un palier a besoin d'un bloc et qu'un bloc ne vient pas d'un fichier de données.

- [ ] **Step 2: corriger la section "Capacity", qui parle d'une seule case**

La phrase "One module at a time, inserting another hands the previous one back" décrit une case, pas la règle. Elle devient : une case ne prend qu'un module, mais un coffre a autant de cases que de sortes inscrites, et c'est la plus forte contenance de toutes ses cases qui vaut.

- [ ] **Step 3: relire le fichier en entier**

Vérifier qu'aucune phrase ne promet neuf colonnes, deux cases ou deux ordres de tri comme des vérités du mod.

- [ ] **Step 4: commit**

```bash
git add README.md
git commit -m "docs: the readme names the eight ways in, not three"
```

## Self-review

Couverture de la fiche : les cases à module sont les tâches 1 et 2, les ordres de tri la tâche 3, la largeur la tâche 4, la contenance la tâche 5, la greffe la tâche 6, la documentation la tâche 7. La section "Ce qui est vérifié, et comment" de la fiche demandait des captures d'un coffre large et d'un coffre étroit : c'est l'étape 10 de la tâche 4.

Un écart avec la fiche, assumé : la fiche donnait `accepts` comme une étiquette d'objets. Le plan en fait un `Predicate<ItemStack>`, avec le statique `CrateModuleSlot.tagged` pour le cas de l'étiquette. La raison est que les deux cases livrées n'acceptent pas une étiquette mais tout ce qu'un registre de modules connaît ; forcer l'étiquette aurait obligé un mod ajoutant un module de contenance à écrire deux fichiers de données au lieu d'un.

Noms vérifiés d'une tâche à l'autre : `crateSlotStart()` est produit par la tâche 2 et consommé par la tâche 4, `CrateTier.DEFAULT_COLUMNS` par la tâche 4 et consommé par les tâches 5 et 6, `moduleTabHeight` posé par la tâche 2 et relu par la tâche 6, `capacityAmong` et `rowsAmong` posés par la tâche 1 et relus par la tâche 2.
