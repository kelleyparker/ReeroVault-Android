package com.retrovault.nes.emulation

object NesButtons {
    // libretro joypad IDs
    const val B = 1 shl 0
    const val SELECT = 1 shl 2
    const val START = 1 shl 3
    const val UP = 1 shl 4
    const val DOWN = 1 shl 5
    const val LEFT = 1 shl 6
    const val RIGHT = 1 shl 7
    const val A = 1 shl 8
}

class InputState {
    private val touch = java.util.concurrent.atomic.AtomicInteger(0)
    private val gamepad = java.util.concurrent.atomic.AtomicInteger(0)

    fun setTouch(button: Int, pressed: Boolean) = update(touch, button, pressed)
    fun setGamepad(button: Int, pressed: Boolean) = update(gamepad, button, pressed)
    fun clearGamepad() = gamepad.set(0)
    fun mask(): Int = touch.get() or gamepad.get()

    private fun update(target: java.util.concurrent.atomic.AtomicInteger, button: Int, pressed: Boolean) {
        while (true) {
            val old = target.get()
            val next = if (pressed) old or button else old and button.inv()
            if (target.compareAndSet(old, next)) return
        }
    }
}
