package com.retrovault.nes.emulation

import android.view.KeyEvent

object GamepadMapper {
    /**
     * Maps an Android gamepad keycode to a NES button, or 0 when the key is unmapped.
     *
     * Android reports Xbox-style controllers by physical position (bottom face button is
     * BUTTON_A). Nintendo controllers print the labels the other way round, so [swapAB]
     * exchanges the two face-button mappings.
     */
    fun buttonForKey(keyCode: Int, swapAB: Boolean = false): Int = when (keyCode) {
        KeyEvent.KEYCODE_BUTTON_A -> if (swapAB) NesButtons.B else NesButtons.A
        KeyEvent.KEYCODE_BUTTON_B -> if (swapAB) NesButtons.A else NesButtons.B
        KeyEvent.KEYCODE_BUTTON_START -> NesButtons.START
        KeyEvent.KEYCODE_BUTTON_SELECT, KeyEvent.KEYCODE_BUTTON_MODE -> NesButtons.SELECT
        KeyEvent.KEYCODE_DPAD_UP -> NesButtons.UP
        KeyEvent.KEYCODE_DPAD_DOWN -> NesButtons.DOWN
        KeyEvent.KEYCODE_DPAD_LEFT -> NesButtons.LEFT
        KeyEvent.KEYCODE_DPAD_RIGHT -> NesButtons.RIGHT
        else -> 0
    }
}
