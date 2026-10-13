# Local ROM library

ROM files are private build/runtime inputs. They are deliberately excluded from Git,
GitHub releases, source archives, and issue attachments. Only use backups that you are
legally authorized to use.

## Recommended layout

Keep the library outside this repository. For the current workspace the root is
`D:\RetroVault\roms`:

```text
D:\RetroVault\roms\
|-- NES\   (.nes, .zip, .7z)
|-- SNES\  (.sfc, .smc, .zip, .7z)
|-- GB\    (.gb, .zip, .7z)
`-- GBC\   (.gbc, .zip, .7z)
```

Archives should normally contain one ROM. Multi-file archives will be indexed as
multiple entries in the future library importer rather than silently discarding files.

## Current NES milestone

The first milestone expects this authorized archive:

```text
D:\RetroVault\roms\NES\Super Mario Bros. (World).zip
```

It must contain `Super Mario Bros. (World).nes`. For a local APK build, stage the ROM as:

```text
app\src\main\assets\roms\super-mario-bros-world.nes
```

That staging directory and every known ROM/archive extension are ignored by `.gitignore`.
Before any push, run `git status --ignored` and verify no game data is staged.

SNES, Game Boy, and Game Boy Color folders are reserved for later milestones; the current
APK only contains an NES/Famicom core and cannot launch those systems yet.
