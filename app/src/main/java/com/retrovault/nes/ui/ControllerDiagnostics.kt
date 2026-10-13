package com.retrovault.nes.ui

import android.util.Log
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.abs

/**
 * Records what Android reports for a connected controller so unsupported hardware can be
 * diagnosed. Everything is also written to Logcat under [TAG]:
 * `adb logcat -s RetroVaultPad`.
 */
class ControllerDiagnostics {
    var enabled by mutableStateOf(false)
    var device by mutableStateOf("No controller input yet")
        private set
    var lastKey by mutableStateOf("-")
        private set
    var axes by mutableStateOf("-")
        private set

    private var lastDeviceId = Int.MIN_VALUE

    fun onKey(event: KeyEvent) {
        if (!enabled) return
        describeDevice(event.device)
        val name = KeyEvent.keyCodeToString(event.keyCode)
        val action = if (event.action == KeyEvent.ACTION_DOWN) "down" else "up"
        lastKey = "$name $action (code ${event.keyCode}, source 0x${event.source.toString(16)})"
        Log.d(TAG, "key $name action=$action code=${event.keyCode} source=0x${event.source.toString(16)} device=${event.device?.name}")
    }

    fun onMotion(event: MotionEvent) {
        if (!enabled) return
        val dev = event.device
        describeDevice(dev)
        val active = dev?.motionRanges.orEmpty()
            .map { it.axis }
            .filter { abs(event.getAxisValue(it)) > 0.1f }
            .joinToString("  ") { "${MotionEvent.axisToString(it)}=${"%.2f".format(event.getAxisValue(it))}" }
        axes = active.ifEmpty { "(all centered)" }
        if (active.isNotEmpty()) Log.d(TAG, "motion $active")
    }

    private fun describeDevice(dev: InputDevice?) {
        if (dev == null || dev.id == lastDeviceId) return
        lastDeviceId = dev.id
        val axisNames = dev.motionRanges.joinToString(", ") { MotionEvent.axisToString(it.axis) }
        device = "${dev.name}\nvendor 0x${dev.vendorId.toString(16)} product 0x${dev.productId.toString(16)} " +
            "sources 0x${dev.sources.toString(16)}\naxes: ${axisNames.ifEmpty { "none" }}"
        Log.d(TAG, "device name=${dev.name} vendor=0x${dev.vendorId.toString(16)} product=0x${dev.productId.toString(16)} " +
            "sources=0x${dev.sources.toString(16)} axes=[$axisNames]")
    }

    companion object {
        const val TAG = "RetroVaultPad"
    }
}
