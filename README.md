# RetroVault NES

RetroVault NES is a native, offline Android NES emulator frontend built with Kotlin,
Jetpack Compose, JNI, and the FCEUmm libretro core. This first milestone packages one
authorized local NES backup to prove the full game-launch path before the library is
expanded.

## Current milestone

- One-game offline library and launch screen
- FCEUmm emulation on ARM64 Android devices
- Sharp 4:3 gameplay rendering in portrait and landscape
- Multitouch D-pad, A, B, Start, and Select controls
- Xbox/Android gamepad buttons and left-stick input
- Low-latency 48 kHz stereo audio
- Battery SRAM persistence plus manual state save/load
- Edge-to-edge Compose UI with safe-inset handling
- No Internet permission, analytics, advertising, or account

## ROM privacy

**No ROM is stored in this repository or any GitHub release.** ROM paths, supported
extensions, and the private folder layout are documented in [ROM_LIBRARY.md](ROM_LIBRARY.md).

For the current test build, place the authorized archive at:

```text
D:\RetroVault\roms\NES\Super Mario Bros. (World).zip
```

Then stage it locally from PowerShell:

```powershell
.\tools\stage-test-rom.ps1
```

The generated `app/src/main/assets/roms/` directory and common ROM/archive extensions
are excluded by `.gitignore`.

## Build

Requirements:

- JDK 17
- Android SDK Platform 35 and Build Tools 35.0.0
- Android NDK 27.2.12479018

Build and test:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`. The validated workspace
deliverable is copied to `output/RetroVault-NES-0.1.0-debug.apk`.

Install on an authorized connected device:

```powershell
adb install -r .\output\RetroVault-NES-0.1.0-debug.apk
adb shell am start -n com.retrovault.nes/.MainActivity
```

## Emulator core and license

This project vendors [libretro FCEUmm](https://github.com/libretro/libretro-fceumm) at
commit `ac0d00a7fc01166f84c7bac01d1084ebfd2b865e`. FCEUmm is GPLv2-licensed and supports
iNES/NES 2.0 content, automatic NTSC/PAL/Dendy detection, broad mapper coverage, SRAM,
and serialized save states. The upstream source is present in `third_party/libretro-fceumm`
and the GPLv2 text is in [LICENSE](LICENSE).

## Status

See [TEST_REPORT.md](TEST_REPORT.md) for exactly what has and has not been validated.
This is an early NES-only milestone; SNES, Game Boy, and Game Boy Color directory names
are reserved for later cores and are not yet playable.
