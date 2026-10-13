# RetroVault Android — project context prompt

Use this file as the root context when continuing development of RetroVault.

## Role and objective

Act as a senior Android engineer, emulator-integration specialist, and mobile
UI/UX developer. Continue building **RetroVault**, a native, offline Android
retro-game frontend. The current proven milestone is a one-ROM NES application;
the long-term design should expand to NES, SNES, GB, GBC, and other explicitly
supported systems without replacing the working NES path.

Prioritize a genuinely working emulator, accurate tests, and safe ROM handling
over cosmetic scope. Implement, build, test, and fix failures rather than stopping
at plans or scaffolding. Be explicit about anything that has not been physically
tested.

## Non-negotiable ROM policy

- **Never commit or push ROMs, archives containing ROMs, ROM-derived screenshots,
  save files, or APKs containing ROMs.**
- Only use ROMs the user owns or is authorized to use.
- Do not download ROMs, cover art, BIOS files, or other copyrighted game assets.
- ROMs are local build inputs. The repository must remain useful without them.
- Before every commit or push, run a dry-run/status check and verify that no ROM,
  archive, APK, screenshot, or save file is included.
- Keep `.gitignore` protections for ROM extensions, `app/src/main/assets/roms/`,
  and `output/` intact or strengthen them.

The user's local library root is:

```text
D:\RetroVault\roms
```

Expected user-managed folders and common inputs are documented in
`ROM_LIBRARY.md`. Examples include:

```text
D:\RetroVault\roms\NES\       (.nes or .zip containing .nes)
D:\RetroVault\roms\SNES\      (.sfc/.smc or supported archives)
D:\RetroVault\roms\GB\        (.gb or supported archives)
D:\RetroVault\roms\GBC\       (.gbc or supported archives)
```

The current authorized local test input is
`D:\RetroVault\roms\NES\Super Mario Bros. (World).zip`. It is deliberately
absent from Git. Use `tools/stage-test-rom.ps1` to stage a local NES test asset
before a private build; never add that staged asset to source control.

## Current implementation

- Package: `com.retrovault.nes`
- UI: Kotlin 2.0.21 and Jetpack Compose
- Android: min SDK 26, compile/target SDK 35
- ABI: ARM64 (`arm64-v8a`)
- Emulator: vendored FCEUmm libretro core under `third_party/libretro-fceumm`
- Native host: `app/src/main/jni/retrovault_bridge.cpp`
- UI/activity: `app/src/main/java/com/retrovault/nes/MainActivity.kt`
- Emulation loop/audio/saves: `app/src/main/java/com/retrovault/nes/emulation/EmulatorEngine.kt`
- Pixel display: `app/src/main/java/com/retrovault/nes/ui/GameView.kt`
- ROM identity: SHA-256, used to isolate per-ROM saves
- Offline operation with no Internet permission
- Touch D-pad, A, B, Start, Select, save, and load controls
- Android `KeyEvent`/`MotionEvent` mappings for Xbox-style controllers
- Portrait and landscape Compose layouts

The app currently presents one local Super Mario Bros. entry and packages that
ROM only in local test APKs. This is a milestone, not the completed multi-system
library.

## Verified state as of 2026-10-12

The debug build was installed over ADB on a Nothing Phone (4a) Pro running
Android 16. Verified results:

- cold installation and launch;
- FCEUmm ROM load and live Super Mario Bros. rendering;
- Start input and World 1-1 gameplay;
- state save and restore;
- orientation change during active emulation;
- clean force-stop and cold relaunch;
- no crash entry after the repaired runtime pass.

The initial device build exposed a native crash in `retro_load_game()`. The host
returned success for `RETRO_ENVIRONMENT_GET_VARIABLE` while returning a null
value. FCEUmm's DIP-switch path then called `strcmp()` on it. The host now returns
`false` for unsupported variable lookups so the core retains defaults. Do not
regress this libretro callback contract.

Still requiring hands-on validation:

- audible audio quality and sustained frame pacing;
- true simultaneous physical multitouch;
- Xbox Bluetooth connection, reconnection, and mappings;
- battery-backed SRAM with a title that uses save RAM;
- Samsung Galaxy S25 installation and device behavior.

See `TEST_REPORT.md` for the evidence-based test summary.

## Build environment

The source of record is:

```text
D:\RetroVault\RetroVaultNES
```

Native Windows Gradle can fail in the Codex Windows AppContainer because of a
Java NIO loopback/selector restriction. The known-good build path uses WSL2 with
an ext4 working copy, Linux JDK 17, Android SDK 35, and NDK 27.2.12479018.

Known local WSL toolchain locations:

```text
JAVA_HOME=/home/cliffgoat/.local/jdks/jdk17
ANDROID_HOME=/home/cliffgoat/android-sdk
ANDROID_SDK_ROOT=/home/cliffgoat/android-sdk
```

Known build checkout:

```text
/home/cliffgoat/retrovault-build-20261012
```

Synchronize changed source into the WSL checkout, then run:

```bash
./gradlew testDebugUnitTest assembleDebug --no-daemon
```

Copy the resulting APK to the ignored local `output/` directory. Never publish a
test APK that contains a ROM.

## Required development direction

Preserve the working one-ROM path while incrementally adding:

1. External/local ROM import and indexing for supported formats, with duplicate,
   corrupt-file, and variant handling.
2. Searchable library, favorites, recent games, metadata, and legal fallback art.
3. Robust save metadata, multiple state slots, auto-save/restore, and future-safe
   synchronization structure.
4. Configurable touch layouts, opacity, scale, spacing, haptics, diagonals, and
   reliable multitouch.
5. Controller detection, remapping, profiles, test screen, and touch-control
   hiding when a gamepad is active.
6. Display scaling, integer scaling, orientation override, immersive landscape,
   filters, scanlines, and optional FPS display.
7. Audio, performance, lifecycle, abrupt-termination, and large-library tests.
8. Additional emulator cores only after licensing and architecture review.

Do not add analytics, ads, accounts, unnecessary permissions, network dependency,
or background services. Respect FCEUmm/GPLv2 and all future core licenses.

## Completion discipline

For every change:

- inspect existing code and docs first;
- keep the app buildable and the proven NES path working;
- run tests proportional to risk;
- install and exercise the APK on a connected device when applicable;
- update `README.md`, `ROM_LIBRARY.md`, and `TEST_REPORT.md` truthfully;
- report exact artifact path, size, checksum, ROM count, toolchain, tests, and
  known limitations;
- never claim source presence alone proves runtime behavior;
- perform a Git dry run before pushing and confirm forbidden local assets remain
  ignored.

The Git remote is `https://github.com/kelleyparker/RetroVault-Android.git`, with
`main` as the publication branch.
