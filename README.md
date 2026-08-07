# SkinStatues

> Turn Minecraft player skins into towering, fully 3D block statues.

SkinStatues is organized as separate platform implementations so each version
can integrate naturally with its own Minecraft ecosystem.

## Implementations

- **[Paper](paper/)** — available as a Paper server plugin.
- **[Fabric](fabric/)** — available as a server-authoritative Fabric mod.

Both implementations provide full 3D skin geometry, classic and slim model
support, configurable block palettes and scale, incremental construction, and
safe statue undo. See the [Paper documentation](paper/README.md) or
[Fabric documentation](fabric/README.md) for platform-specific installation,
configuration, and build instructions.
