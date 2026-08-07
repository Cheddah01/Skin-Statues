# SkinStatues for Fabric

> Turn any Minecraft player's skin into a towering, fully 3D block statue directly in your world.

SkinStatues 1.1.0 is available for multiple Minecraft versions. Download the build
matching the Minecraft version used by your server or game.

> **Warning:** Fabric builds are version-specific. A JAR built for one
> Minecraft version will not load on another. Always download the JAR intended
> for your Minecraft version.

## Supported Versions

| Minecraft | SkinStatues | Optional Client GUI |
| --------- | ----------- | ------------------- |
| [26.2](./26.2/) | 1.1.0 | Yes |
| [26.1.2](./26.1.2/) | 1.1.0 | No |

Minecraft 26.2 includes an optional client-side command menu with a rebindable
`[` shortcut. Fabric 26.1.2 does not include that client menu yet. Both builds
continue to support server-only installation and their normal commands.

Each version directory contains its own documentation, including installation,
commands, configuration, and build-from-source instructions.

## Features

- Full 3D skin geometry with head, torso, arms, and legs
- Classic and slim arm model support
- Optional outer skin layers (hats, jackets, sleeves, pants)
- Refined 53-block color palette with improved natural skin-tone matching
- Incremental, server-friendly construction and safe statue undo
- Server-only operation on both versions; connecting players need no client mod
- Optional command-backed client menu on Minecraft 26.2

## Maintenance model

Each Minecraft version is maintained as its own independent Fabric project
that builds on its own, without shared Gradle configuration or cross-version
modules. Minecraft internals and Fabric dependencies can change substantially
between versions, so implementations are allowed to diverge when necessary.

Adding support for a new Minecraft version generally means:

1. Copy the nearest compatible existing version directory.
2. Update the Minecraft, Fabric Loader, Fabric API, and Loom versions.
3. Port any Minecraft API changes.
4. Run the full test suite.
5. Build the version-specific JAR.
6. Keep older supported versions intact.
