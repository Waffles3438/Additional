# Additional — Minecraft 1.8.9 / Ornithe

Client-side nametag options, configurable jump delay, and Hypixel Bedwars/Duels stat commands.

## Installation

Use a Minecraft **1.8.9 Ornithe, generation 2, Fabric Loader** instance with **Java 25**.
Install the following in that instance:

- Fabric Loader 0.19.3 or newer through the [Ornithe installer](https://ornithemc.net/).
- [OneConfig v1](https://github.com/Polyfrost/OneConfig/tree/legacy), **1.2.3 for 1.8.9 Ornithe**, with its required dependencies (including Lenis 0.1.4+ and Ornithe Standard Libraries).
- `additional-3.1.2+1.8.9-ornithe.jar` from `build/libs`.

The published OneConfig 1.2.3 platform classes require Java 25. This port does not use Forge, LaunchWrapper, or the old `cc.polyfrost` OneConfig wrapper. Use the Ornithe release of OneConfig, not a modern Minecraft or Forge jar.

## Usage

Open OneConfig with Right Shift in-game and select **Additional**.

- **C** toggles the nametag features; change this binding in OneConfig.
- Nametag options include sneaking, invisible players, extended range, and rendering behind walls. The master switch takes priority over Legit Mode.
- No jump delay has a configurable cooldown in ticks.
- `/bw [player]` and `/d [player]` show Bedwars and Duels statistics. Omitting the player checks your own account. Tab completion offers players in the current tab list; other usernames can still be entered.
- Clear the stat cache in OneConfig. Restart after changing its maximum size.

Settings use OneConfig v1's configuration storage; the old Forge/OneConfig v0 JSON is not automatically imported. The old Forge-only RenderLib occlusion and PolyNametag 1.0.x reflection bridges are not included. Legit Mode preserves vanilla labels; there is no RenderLib-specific label recovery on Ornithe. The through-wall fallback uses the vanilla entity-rendering pass and retains player/bot/frustum checks.

## Building

Set `JAVA_HOME` to a JDK 25 installation, then run:

```text
./gradlew build
```

On Windows, use `gradlew.bat build`. The distributable is `build/libs/additional-3.1.2+1.8.9-ornithe.jar`; the `-sources.jar` is for development.

```text
./gradlew runClient
```

The build uses pinned Loom/Ploceus versions, Feather generation 2 mappings, and OneConfig 1.2.3. The existing provider coordination tests run as part of `build`.
