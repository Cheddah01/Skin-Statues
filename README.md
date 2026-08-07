# SkinStatues

> Turn any Minecraft player's skin into a towering, fully 3D block statue directly in your world.

SkinStatues is a Paper server plugin that converts Minecraft player skins into
large block structures. Each statue uses full player geometry, including the
head, body, arms, legs, and optional outer skin layers.

## Features

- Generate a statue from any valid Minecraft username, even when the player is offline.
- Build a fully 3D player model rather than a flat skin image.
- Support classic and slim player models with modern and legacy skin layouts.
- Render hats, jackets, sleeves, and pants as a clean one-block outer shell at every scale.
- Respect transparent skin pixels.
- Choose statue scale within a configurable limit.
- Match skin colors against a curated Minecraft block palette.
- Place statues on the world grid facing the player who creates them.
- Process skins asynchronously and build statues incrementally instead of editing the world in one large tick.
- Cache resolved skins in memory for faster repeated use.
- Undo the latest completed statue with `/statue undo`.
- Restore original block states while protecting newer or manually changed blocks from stale undo operations.

## Example

```text
/statue Steve 2
```

This creates a scale-2 statue using Steve's skin in front of you, facing you.

```text
/statue undo
```

This safely restores the area occupied by your most recently completed statue.

## Commands

| Command | Description |
| --- | --- |
| `/statue <name> <scale>` | Creates a statue using the specified Minecraft skin. |
| `/statue undo` | Safely restores your most recently created statue. |

Only players can use these commands. Each skin pixel becomes a
`scale × scale × scale` cube of blocks, making a statue `32 × scale` blocks tall.

## Permissions

| Permission | Default | Description |
| --- | --- | --- |
| `skinstatues.use` | Server operators | Allows the player to create and undo statues with `/statue`. |

## Installation

1. Download the latest `SkinStatues.jar`.
2. Place it in your Paper server's `plugins` directory.
3. Restart the server.
4. Use `/statue <name> <scale>` in game.

SkinStatues declares Bukkit API version `1.21`, is built against Paper API
`26.1.2.build.70-stable`, and requires Java 25. Other server implementations
are not officially supported.

## Configuration

SkinStatues creates `plugins/SkinStatues/config.yml` on first start. Missing
settings are restored from the packaged defaults without overwriting your
existing values.

| Setting | Default | Description |
| --- | --- | --- |
| `max-scale` | `4` | Largest accepted statue scale. The built-in hard limit is 16. |
| `blocks-per-tick` | `2500` | Blocks processed per tick while building or undoing. |
| `outer-layer` | `true` | Enables hats, jackets, sleeves, and pants as a second skin shell. |
| `skin-cache-minutes` | `60` | Minutes a resolved skin remains cached. Set to `0` to disable normal caching. |
| `palette.excluded` | `[]` | Material names removed from the built-in block palette. |
| `palette.extra` | `{}` | Additional or replacement `MATERIAL_NAME: "#RRGGBB"` color entries. |

## How statue generation works

SkinStatues resolves the requested player's skin, maps its pixels onto a full
3D player model, matches visible colors to suitable Minecraft blocks, and
builds the result incrementally in the world. Transparent pixels are skipped,
slim arms are supported, and skin overlays are rendered as a thin outer shell.

The statue is aligned to the nearest cardinal direction, placed in front of
the creator, and faces back toward them.

## Undo safety

`/statue undo` records the original block states replaced by the player's most
recently completed statue and restores them incrementally. If blocks have since
been changed or superseded by a newer statue, undo handles them conservatively
instead of blindly overwriting the newer world state. Undo history is kept in
memory and is cleared when the server restarts or the plugin reloads.

## Building from source

Java 25 and Maven are required.

```shell
mvn clean package
```

The packaged plugin is written to `target/SkinStatues.jar`.

## Support and bugs

Use [GitHub Issues](https://github.com/Cheddah01/Skin-Statues/issues) to report
bugs or request changes. Include the Paper version, Java version, relevant
configuration, and server log details when reporting a problem.

## License

This project does not currently include an open-source license.
