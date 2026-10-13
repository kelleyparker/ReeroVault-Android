package com.retrovault.nes.emulation

object NativeBridge {
    init {
        System.loadLibrary("retro")
        System.loadLibrary("retrovault")
    }

    external fun nativeInit(romPath: String, systemDir: String, saveDir: String): Boolean
    external fun nativeRunFrame(pixels: IntArray, samples: ShortArray): Int
    external fun nativeSetInput(mask: Int)
    external fun nativeFrameRate(): Double
    external fun nativeSaveSram(path: String): Boolean
    external fun nativeLoadSram(path: String): Boolean
    external fun nativeSaveState(path: String): Boolean
    external fun nativeLoadState(path: String): Boolean
    external fun nativeReset()
    external fun nativeShutdown()
}
