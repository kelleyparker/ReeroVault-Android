package com.retrovault.nes.emulation

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Test

class GamepadMapperTest {
    @Test fun mapsXboxStyleFaceButtonsByDefault() {
        assertEquals(NesButtons.A, GamepadMapper.buttonForKey(KeyEvent.KEYCODE_BUTTON_A))
        assertEquals(NesButtons.B, GamepadMapper.buttonForKey(KeyEvent.KEYCODE_BUTTON_B))
    }

    @Test fun swapExchangesOnlyTheFaceButtons() {
        assertEquals(NesButtons.B, GamepadMapper.buttonForKey(KeyEvent.KEYCODE_BUTTON_A, swapAB = true))
        assertEquals(NesButtons.A, GamepadMapper.buttonForKey(KeyEvent.KEYCODE_BUTTON_B, swapAB = true))
        assertEquals(NesButtons.START, GamepadMapper.buttonForKey(KeyEvent.KEYCODE_BUTTON_START, swapAB = true))
        assertEquals(NesButtons.SELECT, GamepadMapper.buttonForKey(KeyEvent.KEYCODE_BUTTON_SELECT, swapAB = true))
    }

    @Test fun mapsDpadAndModeAndLeavesOthersUnmapped() {
        assertEquals(NesButtons.UP, GamepadMapper.buttonForKey(KeyEvent.KEYCODE_DPAD_UP))
        assertEquals(NesButtons.RIGHT, GamepadMapper.buttonForKey(KeyEvent.KEYCODE_DPAD_RIGHT))
        assertEquals(NesButtons.SELECT, GamepadMapper.buttonForKey(KeyEvent.KEYCODE_BUTTON_MODE))
        assertEquals(0, GamepadMapper.buttonForKey(KeyEvent.KEYCODE_BUTTON_X))
    }
}
