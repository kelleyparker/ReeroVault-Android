# Test report

Date: 2026-10-12

## Passed

- Gradle 8.10.2 project configuration
- Kotlin 2.0.21 / Jetpack Compose compilation
- FCEUmm ARM64 native compilation and linking
- JNI frontend compilation and linking
- Unit test: simultaneous touch/gamepad input state composition
- Debug APK assembly
- APK v2 signature verification
- Package metadata: `com.retrovault.nes`, min SDK 26, target SDK 35
- APK content: `libretro.so`, `libretrovault.so`, and locally staged NES asset
- No requested Internet permission
- Wireless ADB install on Nothing Phone (4a) Pro (`FroggerPro`, Android 16)
- Cold launch with `MainActivity` focused and process running
- FCEUmm boot of Super Mario Bros. (World) and live first-frame rendering
- On-screen Start input and World 1-1 gameplay progression
- Save-state creation (`state-0.rvs`, 13,758 bytes) and successful load
- Portrait-to-landscape reflow during an active emulation session
- Clean force-stop and cold relaunch after gameplay
- Empty Android crash buffer after the repaired build's runtime pass
- USB ADB connection using the signed Google Android driver
- Xbox Wireless Controller detected over Bluetooth as an Android gamepad/joystick
- Physical Xbox D-pad/left-stick events received during gameplay and Mario movement observed
- Android Xbox A/B/Menu/View key translation matches the app's NES mappings

## Runtime defect found and fixed

The first device build crashed in `retro_load_game()` because the frontend returned
success for `RETRO_ENVIRONMENT_GET_VARIABLE` while supplying a null value. FCEUmm's
DIP-switch path correctly treats a successful lookup as a valid string and called
`strcmp()` on it. The frontend now returns `false` for unsupported option lookups,
allowing FCEUmm to retain its defaults. The rebuilt APK passed the device tests above.

## Still requires hands-on validation

- Audio output and frame pacing
- Touch multitouch during gameplay
- Button-by-button physical Xbox input capture, disconnect/reconnect behavior, and remapping UI
- Battery-backed SRAM with a cartridge that actually uses save RAM

These items require listening to the phone or interacting with physical multi-touch and
Bluetooth hardware; they are not claimed as passed from ADB evidence alone.

## Tested APK

- File: `output/RetroVault-NES-0.1.0-debug.apk`
- Size: 14,250,710 bytes
- SHA-256: `7CEB68C7A628E16AF9D9C6579A9244E01BBEE1900533223E8A584A8FD69F1CF6`
