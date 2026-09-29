# PackBridge

PackBridge is a client-side Fabric mod that loads classic Minecraft resource
packs on modern versions. Enable your pack normally; texture adaptation happens
as Minecraft reads the pack, without changing its files.

## Features

- Maps classic `textures/items` and `textures/blocks` paths and vanilla texture
  names to their current equivalents.
- Splits the classic GUI icon and widget sheets into modern HUD and button
  sprites, including hearts, armor, food, air, hotbar, and experience bars.
- Remaps classic chest texture faces and splits double-chest textures into
  modern left and right textures.
- Preserves high-resolution texture pixels and renamed animation metadata.
- Maps classic armor layers and enchantment glint to modern equipment paths.
- Updates classic item model texture references and retains display settings.
- Supports both zip and folder packs, including normal pack-stack priority.
- Keeps explicit modern textures when a pack includes both formats.
- Releases cached conversions when packs close or resources reload.

## Supported versions

- Minecraft 26.1.2 + Fabric
- Minecraft 26.2 + Fabric

Requires Fabric Loader 0.19.3 or newer and Java 25. Fabric API and Fabric
Language Kotlin are not required.

## Usage

1. Install the PackBridge JAR matching your exact Minecraft version.
2. Place a resource pack in Minecraft's `resourcepacks` folder.
3. Enable it through **Options → Resource Packs**.

PackBridge adapts packs declaring `pack_format` 1–4, covering the classic
Minecraft 1.6–1.14 layouts, with the primary focus on 1.7/1.8 texture packs.
Newer packs continue through Minecraft's normal loading path.

Texture adaptation is limited to the documented vanilla paths and sprite
layouts. OptiFine/MCPatcher CIT, connected textures, custom entity models,
shaders, and old block-state semantics require their own compatible tooling.
Packs with custom layouts or assets for blocks that no longer exist may need
manual updates. PackBridge does not create textures for content absent from
the pack. Files without valid `pack.mcmeta` are left to Minecraft to validate.

## Building

Build and test every supported target with Java 25:

```bash
./gradlew build releaseManifest
```

On Windows, use `.\gradlew.bat`.

The production JARs are written to:

```text
versions/mc26_1_2/build/libs/PackBridge-1.0.0+mc26.1.2.jar
versions/mc26_2/build/libs/PackBridge-1.0.0+mc26.2.jar
```

Shared behavior lives in `src/main/java`. `gradle/targets.properties` defines
the active targets. Each `versions/<target>` directory contains only its Gradle
marker file; API differences belong in `src/<minecraft-version>/java`.

Run the headless Fabric startup checks for both targets:

```bash
./gradlew :versions:mc26_1_2:runSmoke :versions:mc26_2:runSmoke
```

These checks exercise the actual supplier mixins and vanilla pack discovery
before the game opens a window. Their test-only mod is excluded from release
JARs.

## Releases

Only pushed `v*` tags publish releases. The tag must match
`v<mod_version>` from `gradle.properties`. The generated release manifest drives
both Minecraft builds and the individual Modrinth uploads. Normal branch pushes
and pull requests do not publish releases.

The workflow uses a GitHub-hosted Ubuntu runner and a `release` environment
with the `MODRINTH_TOKEN` secret and `MODRINTH_PROJECT_ID` variable. Run it
manually to check publishing configuration and build both targets without
uploading versions or creating a GitHub Release.
