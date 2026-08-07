# CozyStatues

Turns any Minecraft player's skin into a giant, full 3D block statue.

Run `/statue Notch 2` and a two-times-scale Notch is built one block in front of
you, standing on your floor level and looking straight back at you. The statue
is a real player model — head, torso, both arms, both legs, with the correct
vanilla proportions — textured from that player's actual skin, pixel by pixel,
with every skin colour matched to the closest suitable building block.

## Requirements

- Paper 1.21+ (built against Paper API 26.1.2)
- Java 25
- No dependencies. Skins are resolved through Paper's own profile API.

## Installation

1. Drop `CozyStatues.jar` into `plugins/`.
2. Restart the server. `plugins/CozyStatues/config.yml` is written on first start.
3. Grant `cozystatues.use` to whoever should be able to build statues.

## Command

```
/statue <name> <scale>
/statue undo
```

That is the whole command surface. `undo` incrementally restores the blocks
replaced by your most recently completed statue. Undo data is kept in memory
only and is cleared by a restart or reload.

- `<name>` — any Minecraft username. The player does not have to be online, or
  to have ever joined the server.
- `<scale>` — a positive whole number. One skin pixel becomes a `scale` ×
  `scale` × `scale` cube of blocks, so the statue is `32 × scale` blocks tall.
  Decimals, zero, negatives and anything above `max-scale` are rejected.

Example:

```
/statue Steve 2
```

> Building a statue of Steve at scale 2...
> Statue of Steve created (9344 blocks, 64 tall).

### Permission

| Node | Default | Grants |
| --- | --- | --- |
| `cozystatues.use` | op | `/statue` |

## Placement

- Only players can run the command; the statue is built relative to **you**, not
  to the player whose skin it wears.
- Your facing is snapped to the nearest compass direction, so the statue is
  always square to the world grid.
- The statue faces you: its front is the side nearest you, and it grows away
  from you.
- Its front layer starts one block beyond your hitbox, so nothing is ever placed
  inside you — even standing right on the edge of a block.
- It is centred left to right on you and stands on the block level your feet
  are on. No terrain is cleared: whatever is in the way is replaced.

## Configuration

`config.yml` is small on purpose. Missing keys are merged in from the packaged
defaults on every start, so upgrades never overwrite your changes.

| Key | Default | Meaning |
| --- | --- | --- |
| `max-scale` | `4` | Largest scale `/statue` accepts. Hard limit 16. |
| `blocks-per-tick` | `2500` | Blocks processed per tick while building or undoing. |
| `outer-layer` | `true` | Build the hat, jacket, sleeves and pants as a second, one-block-larger shell. |
| `skin-cache-minutes` | `60` | How long a resolved skin is reused before it is fetched again. |
| `palette.excluded` | `[]` | Material names to drop from the built-in palette. |
| `palette.extra` | `{}` | `MATERIAL_NAME: "#RRGGBB"` entries to add, or to retune an existing block's colour. |

### The block palette

Skin colours are matched in CIE L\*a\*b\* rather than raw RGB, so the match
follows how different two colours actually look. The built-in palette is around
75 curated blocks — the full concrete and terracotta ranges plus stones, ocean
greens, waxed copper and mineral blocks — chosen because they are opaque, look
the same on all six sides, and do not fall, burn, melt, oxidise, glow, power
redstone, hold an inventory or need support.

## Performance

Profile lookup, texture download, image decoding and the entire statue plan run
off the server thread. Block placement and undo restoration touch the world in
batches of `blocks-per-tick`, with physics suppressed. Nothing is placed until
the whole plan has been built successfully, so a failed lookup never leaves
half a statue standing.

The statue is a hollow shell — only the outside of each body box is placed, and
at scale `s` that shell is `s` blocks thick. A scale 4 statue is roughly 75,000
blocks and takes about half a minute of build time at the default rate.

## Skin service assumptions

- Usernames are resolved with `Bukkit.createProfile(name).complete(true, true)`.
  Paper resolves the account against Mojang's account and session services and
  caches the result in the server's own profile cache. `true, true` forces the
  online-mode lookup, so `/statue` works on offline-mode servers too.
- The skin texture is then downloaded over HTTPS from the URL in the profile —
  normally `textures.minecraft.net` — with a 5 second connect and 10 second read
  timeout, and a 2 MB response cap.
- Skins may be 64×64 (modern) or 64×32 (legacy, whose left limbs are the right
  ones mirrored). Higher resolution skins are supported if their dimensions are
  a whole multiple of the vanilla ones; each vanilla texel is averaged from its
  block of image pixels.
- The classic/slim arm shape comes from the profile's own texture metadata, and
  falls back to the vanilla UUID rule when a profile does not declare one.
- Failures — unknown names, an unreachable service, malformed texture data, a
  player with no skin — are reported to the player in one line and cached for a
  minute, so a mistyped name cannot be used to hammer Mojang or fill the console.
- If Mojang's services are unreachable, statues of players already in the cache
  keep working; anyone else fails cleanly.

## Not in this phase

Deliberately absent, and structured so they can be added cleanly later:
statue persistence and management, poses, held items, armour,
alternative palettes per statue, and any GUI. The command surface is meant to
stay exactly `/statue <name> <scale>` and `/statue undo`.
