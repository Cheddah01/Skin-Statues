# Changelog

## 1.1.0 — Color and platform update

- Refined the shared 53-block palette and improved pale, cream, tan,
  Steve-range, and shaded skin-tone rendering.
- Added Fabric support for Minecraft 26.2 while retaining the independent
  Minecraft 26.1.2 project.
- Added an optional, command-backed client menu to Fabric 26.2 and 26.1.2 with
  a rebindable `[` shortcut, compact responsive layout, create action, and undo
  action. Paper remains command-only.
- Standardized Fabric production artifact names by platform and Minecraft
  compatibility: `SkinStatues-Fabric-26.2.jar` and
  `SkinStatues-Fabric-26.1.2.jar`.

## 1.0.0 — Initial release

First release.

- `/statue <name> <scale>` builds a full 3D block statue of any Minecraft
  player's skin one block in front of the command sender, facing them.
- Complete vanilla player model: head, torso, both arms and both legs, with
  classic and slim arm shapes detected from the profile.
- Correct skin mapping on all six faces of every body part, including mirrored
  back faces, the vertically flipped underside, and legacy 64x32 skins whose
  left limbs are the right ones mirrored.
- Outer skin layer (hat, jacket, sleeves, pants) as a second, one-block-larger
  shell; toggleable with `outer-layer`.
- CIE L\*a\*b\* nearest-colour matching against a curated palette of ~75 solid,
  non-directional blocks, extendable from `config.yml`.
- Skins resolved through Paper's profile API with caching, working for offline
  players; all lookup, download, decoding and planning happen off the server
  thread, and blocks are placed in configurable per-tick batches.
- `/statue undo` restores the original block-state snapshots for the player's
  latest completed statue in batches, while protecting newer or manually
  changed blocks from stale undo operations.
