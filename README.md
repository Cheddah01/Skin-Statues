# SkinStatues

SkinStatues turns Minecraft player skins into towering, fully 3D block statues
built directly in the world. It is available for both Paper and Fabric.

## Features

- Full 3D player statues from any valid Minecraft username
- Classic and slim player models with modern skin texture mapping
- Hats, jackets, sleeves, pants, and other outer skin layers
- A thin one-block outer skin shell regardless of statue scale
- Configurable statue scale
- Refined block color matching with improved natural skin tones
- Incremental construction to spread world updates across server ticks
- Safe undo with restoration of the original blocks
- Conservative protection against overwriting newer or manual block changes
- Paper and Fabric support

## Quick Start

```text
/statue Steve 2
```

Creates a scale-2 statue using Steve's skin in front of you.

```text
/statue undo
```

Safely restores the blocks replaced by your most recently completed statue.

## Platforms

| Platform | Minecraft | SkinStatues | Client Required | GUI |
| -------- | --------- | ----------- | --------------- | --- |
| [Paper](paper/) | 26.1.2 (`26.1.2.build.70-stable`) | 1.1.0 | No | No |
| [Fabric](fabric/) | [26.2](fabric/26.2/) | 1.1.0 | No | Optional |
| [Fabric](fabric/) | [26.1.2](fabric/26.1.2/) | 1.1.0 | No | No |

Fabric builds are Minecraft-version-specific. Use the project and JAR that
exactly match the Minecraft version you are running.

## Optional Fabric Client Menu

Fabric 26.2 users can optionally install SkinStatues on the client to add a
small creation menu. Press `[` by default to open it; the key can be rebound in
Minecraft's Controls menu. The menu provides a **Player** field, a **Scale**
field, **Create Statue**, and **Undo Last Statue**.

**The client mod is optional.** A Fabric server can run SkinStatues without
requiring players to install it. The menu sends the existing `/statue`
commands and is currently included only in the Fabric 26.2 build.

## Commands

| Command | Description |
| ------- | ----------- |
| `/statue <name> <scale>` | Creates a statue using the specified Minecraft player's skin. |
| `/statue undo` | Safely restores your most recently created statue. |

## How It Works

SkinStatues:

1. Resolves the requested Minecraft skin.
2. Maps it onto a full 3D player model.
3. Matches visible skin pixels to a curated Minecraft block palette.
4. Builds the statue incrementally in the world.

Transparent pixels, classic and slim models, and outer skin layers are all
supported. The refined palette combines concrete, terracotta, wool, and a
small selection of natural-tone blocks for cleaner pale, cream, tan, and
shaded skin colors without relying on visually noisy materials.

## Safe Undo

`/statue undo` does not simply replace a statue with air. SkinStatues records
the block states that existed before construction and restores them safely.
Newer statues and manual block changes are handled conservatively so an old
undo does not blindly destroy subsequent edits.

## Downloads and Version Selection

Paper users should use the Paper build. Fabric users must use the JAR matching
their exact Minecraft version:

- `SkinStatues-Fabric-26.2.jar`
- `SkinStatues-Fabric-26.1.2.jar`

Generated JARs are build outputs and are not committed to this repository.

## Building From Source

Paper:

```bash
cd paper
mvn clean package
```

Fabric 26.2:

```bash
cd fabric/26.2
./gradlew build
```

Fabric 26.1.2:

```bash
cd fabric/26.1.2
./gradlew build
```

## Bugs and Suggestions

Found a bug or have an idea for SkinStatues? Open an issue on the
[GitHub repository](https://github.com/Cheddah01/Skin-Statues/issues).

For visual issues, include the Minecraft username, statue scale, platform,
Minecraft version, and a screenshot if possible.

## Detailed Platform Documentation

- [Paper documentation](paper/README.md)
- [Fabric documentation](fabric/README.md)
