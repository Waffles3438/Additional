# Additional

Client-side quality-of-life features for **Minecraft 1.8.9 on Ornithe**, using **OneConfig V1**.

## Installation

Use an [Ornithe 1.8.9 instance](https://ornithemc.net/). Run it with **Java 25 or newer**, and install the Ornithe versions of [OneConfig](https://modrinth.com/mod/oneconfig) **1.2.18 or newer** and [Ornithe Standard Libraries](https://modrinth.com/mod/osl) **0.21.1 or newer**, including OneConfig's required dependencies (such as Pylon). Copy the built Additional jar into the instance's `mods` folder.

Open OneConfig with Right Shift and select Additional. The default nametag toggle is **C**. Features include no jump delay, sneaking/invisible nametags, extended nametag range, and nametags behind walls. Legit Mode preserves vanilla nametag positioning while the nametag master switch is off.

Use `/bw [username]` for Bedwars stats and `/d [username]` for Duels stats. Omitting the username checks your own account. Both commands suggest tab-list players and accept other valid usernames. Stats expire after five minutes; the Clear Cache button also invalidates outstanding lookups.

## Building and development

Install JDK 25 and point `JAVA_HOME` to it. Gradle 9.5.0, Fabric Loom, and Ornithe Ploceus use Feather mappings with Calamus generation 2. Dependency versions are pinned in `gradle.properties`.

```powershell
./gradlew.bat build
./gradlew.bat runClient
```

On Linux/macOS use `./gradlew` instead. The distributable is `build/libs/additional-3.1.2+mc1.8.9-ornithe.jar`; the sources jar is for development. Runtime dependencies are installed separately, and Gradle supplies them automatically for `runClient`.

## Port compatibility

This build replaces Forge metadata, events, and LaunchWrapper with Fabric client initialization and Minecraft mixins. The Forge-only RenderLib occlusion fallback and the deferred-render bridge for old Forge PolyNametag were removed. The late nametag pass calls the normal player renderer, allowing inline renderer modifications to apply; compatibility with other Ornithe nametag/culling mods still needs multiplayer testing. Reconfigure options in OneConfig V1 when moving from a Forge instance; automatic conversion of V0 configuration files is not provided.

Implementation references: [OneConfig V1 setup](https://docsv1.polyfrost.org/introduction/getting-started), [OneConfig source](https://github.com/Polyfrost/OneConfig), and [Ornithe's development guide](https://wiki.ornithemc.net/wiki/Setting_up_a_Mod_Development_Environment).
