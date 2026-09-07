plugins {
    id("fabric-loom") version "1.17.12"
    java
}

version = project.property("mod_version") as String
group = project.property("maven_group") as String

base {
    archivesName.set(project.property("archives_base_name") as String)
}

repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net/") { name = "Fabric" }
}

dependencies {
    minecraft("com.mojang:minecraft:${project.property("minecraft_version")}")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:${project.property("loader_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${project.property("fabric_version")}")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.test {
    useJUnitPlatform()
}

// Loom launches the game from the Gradle DAEMON, which keeps the environment it was first started
// with. A headless run therefore inherits the desktop compositor and opens its window on the real
// screen. Reading the socket through a provider goes around the daemon: the value travels with the
// build request, not with the daemon's own environment.
val headlessWayland = providers.gradleProperty("headlessWayland")

loom {
    runs {
        named("client") {
            runDir("run")
            // Same JVM tuning as the sibling mods: fixed heap, tight G1 pause target, pretouch.
            vmArgs(
                "-Xms8G",
                "-Xmx8G",
                "-XX:+UnlockExperimentalVMOptions",
                "-XX:+UseG1GC",
                "-XX:MaxGCPauseMillis=25",
                "-XX:G1NewSizePercent=40",
                "-XX:G1MaxNewSizePercent=50",
                "-XX:G1HeapRegionSize=16M",
                "-XX:G1ReservePercent=15",
                "-XX:+ParallelRefProcEnabled",
                "-XX:+PerfDisableSharedMem",
                "-XX:+AlwaysActAsServerClassMachine",
                "-XX:+AlwaysPreTouch",
                "-XX:+DisableExplicitGC",
                "-XX:+UseNUMA"
            )
            programArgs("--quickPlaySingleplayer", "DeepCrate")
            if (headlessWayland.isPresent) {
                environmentVariable("WAYLAND_DISPLAY", headlessWayland.get())
            }
        }
        named("server") {
            runDir("run/server")
            vmArgs("-Xms1G", "-Xmx3G", "-XX:+UseG1GC")
        }
    }
}

// Loom's runs { environmentVariable(...) } reaches the generated IDE configuration, not the gradle
// task, so a client launched from a headless compositor still inherited the desktop's socket and
// opened a real window. Setting it on the task is what actually moves it.
if (headlessWayland.isPresent) {
    tasks.withType<JavaExec>().configureEach {
        if (name == "runClient" || name == "runClientGameTest") {
            environment("WAYLAND_DISPLAY", headlessWayland.get())
        }
    }
}

// Server game tests: the screen is the one thing JUnit cannot reach, because a menu needs a real
// player and a real level. createSourceSet gives src/gametest its own mod metadata, so the shipped
// jar never declares an entry point it does not contain.
fabricApi {
    configureTests {
        createSourceSet = true
        modId = "deepcrate-gametest"
        enableGameTests = true
        // The screen and the crate model are the two things a headless server cannot answer for, so
        // the client tests drive a real client and photograph what it draws.
        enableClientGameTests = true
        eula = true
    }
}

tasks.check {
    dependsOn(tasks.named("compileGametestJava"))
}

tasks.jar {
    from("../LICENSE")
}
