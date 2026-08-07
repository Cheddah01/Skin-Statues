# Changelog

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
