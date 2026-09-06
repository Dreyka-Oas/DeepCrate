# DeepCrate, the mod itself

The Gradle root. What the crates do and what an addon can reach is in the README one level up; this
file is versions and commands.

## Versions

| | |
|---|---|
| Minecraft | 1.21.11 |
| Fabric loader | 0.19.3 |
| Fabric API | 0.141.4+1.21.11 |
| Java | 21, from the toolchain rather than a path |
| Loom | 1.17.12 |
| Mappings | official Mojang |

They live in `gradle.properties` and nowhere else. `mod_version` is repeated in
`src/main/resources/fabric.mod.json` and the two must not drift.

The package base is `oas.dreyka`, the resource namespace and `archives_base_name` are both
`deepcrate`. Those three are not the same string on purpose: the namespace is written into saves, the
package is not.

## Commands

    ./gradlew build           # jar in build/libs/, gametests compiled and run
    ./gradlew test            # 55 JUnit tests: storage, paging, save format, settings
    ./gradlew runGameTest     # 45 gametests, needs a world, no window
    ./gradlew runClientGameTest   # drives a real client and photographs it
    ./gradlew runClient       # a playable dev client, opens a window

`check` depends on `compileGametestJava`, so a gametest that stopped compiling fails the build
instead of surfacing the next time somebody launches a client.

`runClient` is for playing, not for verifying: it takes over the screen and the speakers. To watch
the mod run without either, `scripts/headless-test.sh start --both --timeout 300` puts the client in
an invisible sway session on a virtual output, still rendered by the graphics card, and
`scripts/headless-test.sh shot <name>` brings a picture back.

## Source sets

`main` is what ships. `test` holds the JUnit classes, `gametest` the ones that need a world and the
two client tests that take screenshots. Neither reaches the jar: `jar` packages `sourceSets.main`
alone, and the gametests carry their own mod metadata under `deepcrate-gametest`, so the shipped
`fabric.mod.json` never names an entry point the player's jar lacks.

    unzip -l build/libs/*.jar | grep -ciE '/dev/|gametest|Test\.class|junit'

That prints 0. It is a count rather than an exclude rule because the exclusion works by omission,
which a later edit could undo without anything complaining.

## Settings

`config/oas/deepcrate.json`, written on first launch under `run/` and `run/server/`. It is read
before the registries are filled, which matters: `rowModuleStackLimit` is read during
`RegistryInit`'s static initialisation, so a load happening after it would leave the option looking
functional and doing nothing.
