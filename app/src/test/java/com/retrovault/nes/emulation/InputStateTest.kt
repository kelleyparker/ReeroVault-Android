package com.retrovault.nes.emulation

import org.junit.Assert.assertEquals
import org.junit.Test

class InputStateTest {
    @Test fun combinesTouchAndGamepadAndReleasesIndependently() {
        val state = InputState()
        state.setTouch(NesButtons.RIGHT, true)
        state.setGamepad(NesButtons.A, true)
        assertEquals(NesButtons.RIGHT or NesButtons.A, state.mask())
        state.setTouch(NesButtons.RIGHT, false)
        assertEquals(NesButtons.A, state.mask())
    }
}
