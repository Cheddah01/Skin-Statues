# SkinStatues Fabric for Minecraft 26.1.2

> Turn any Minecraft player's skin into a towering, fully 3D block statue directly in your world.

This directory contains SkinStatues **1.1.0** for Fabric, targeting
**Minecraft 26.1.2**. For other Minecraft versions, see the
[Fabric overview](../README.md).

SkinStatues for Fabric converts modern or legacy Minecraft skins into full block
models with a head, torso, arms, and legs. It supports classic and slim arms,
transparent pixels, optional outer layers, refined natural-tone block matching,
incremental construction, and a conservative one-statue undo.

Minecraft 26.1.2 also includes the optional client-side convenience menu used
by the Minecraft 26.2 build. The commands remain the public interface and the
server remains authoritative for permissions, validation, skin lookup,
placement, and undo.

## Requirements

- Minecraft Java Edition 26.1.2
- Java 25
- Fabric Loader 0.19.3 or newer compatible 0.19.x release
- Fabric API 0.155.2+26.1.2 or newer release for Minecraft 26.1.2

This is a server-authoritative mod made entirely from vanilla blocks. The same
common server code also works in an integrated single-player server when the
mod and Fabric API are installed in that instance.

## Server-only installation

1. Install Fabric Loader for Minecraft 26.1.2 on the server.
2. Add Fabric API for Minecraft 26.1.2 to the server's `mods` directory.
3. Add `SkinStatues-Fabric-26.1.2.jar` to the same directory.
4. Start the server.

Connecting players do not need SkinStatues on their clients. Both
`/statue <name> <scale>` and `/statue undo` continue to work normally, and
everyone sees and interacts with the resulting vanilla blocks.

## Optional client installation

Players may also install the same `SkinStatues-Fabric-26.1.2.jar`, Fabric
Loader, and Fabric API in their client. Press `[` in-game to open the
SkinStatues menu. The default key can be changed under Minecraft's
**Controls > Key Binds** settings in the **SkinStatues** category.

The compact, centered menu provides a **Player** field, a **Scale** field,
**Create Statue**, and **Undo Last Statue**. It sends the normal
`statue <name> <scale>` or `statue undo` command to the connected server; it
does not download skins, plan statues, track undo state, or bypass server
permissions on the client.

Because the menu uses Minecraft's normal command tree and command dispatch,
it can also be used with a compatible server exposing the normal SkinStatues
`/statue` command, whether that server runs the Fabric or Paper edition. If the
server does not expose the command to the player, the menu shows it as
unavailable and disables both action buttons.

## Commands

| Command | Description |
| --- | --- |
| `/statue <name> <scale>` | Builds the named player's skin at the requested scale. |
| `/statue undo` | Restores the world state replaced by your latest completed statue. |

The command sender must be an in-game player. The target skin player may be
offline. The command requires the vanilla game-master permission level
(operator level 2), which is the lowest standard level appropriate for a
world-modifying command.

Example:

```text
/statue Steve 2
/statue undo
```

## Configuration

The first launch creates `config/skinstatues.json`:

```json
{
  "maxScale": 4,
  "blocksPerTick": 2500,
  "outerLayer": true,
  "skinCacheMinutes": 60,
  "palette": {
    "excluded": [],
    "extra": {}
  }
}
```

| Setting | Default | Description |
| --- | --- | --- |
| `maxScale` | `4` | Largest accepted scale. Values are safely limited to 1–16. |
| `blocksPerTick` | `2500` | Maximum build or undo snapshots processed per server tick. |
| `outerLayer` | `true` | Renders hats, jackets, sleeves, and pants one world block beyond the base model. |
| `skinCacheMinutes` | `60` | Successful skin-cache lifetime. `0` disables success caching; failures remain briefly cached. |
| `palette.excluded` | `[]` | Vanilla block identifiers removed from the built-in palette. |
| `palette.extra` | `{}` | Extra or retuned block colors as `"minecraft:block": "#RRGGBB"`. |

Malformed configuration is reported in the server log and replaced with safe
built-in defaults for that launch without overwriting the malformed file.
There is intentionally no reload command; restart the server after editing.

Undo data, ownership tracking, and resolved skins are kept in memory only and
are cleared when the server stops.

## Building from source

Java 25 is required. From the repository root:

```shell
cd fabric/26.1.2
./gradlew clean test
./gradlew build
```

The production mod is written to
`build/libs/SkinStatues-Fabric-26.1.2.jar`.
The similarly named `-sources.jar` is source code for development and is not
the server mod.

## Support and bugs

Use [GitHub Issues](https://github.com/Cheddah01/Skin-Statues/issues) to report
problems. Include the Minecraft, Fabric Loader, Fabric API, and Java versions,
plus relevant configuration and server logs.

## License

This project does not currently include an open-source license.
