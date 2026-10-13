package com.retrovault.nes.emulation

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max

class EmulatorEngine(private val context: Context) {
    val input = InputState()
    var frameConsumer: ((IntArray) -> Unit)? = null
    var errorConsumer: ((String) -> Unit)? = null

    private val executor = Executors.newSingleThreadExecutor()
    private val commandExecutor = Executors.newSingleThreadExecutor()
    private val running = AtomicBoolean(false)
    private val paused = AtomicBoolean(false)
    private val pixels = IntArray(256 * 240)
    private val samples = ShortArray(4096)
    private var audioTrack: AudioTrack? = null
    private lateinit var romId: String

    fun start() {
        if (running.getAndSet(true)) return
        executor.execute {
            try {
                val rom = installRom()
                romId = sha256(rom)
                val saveDir = File(context.filesDir, "saves/$romId").apply { mkdirs() }
                if (!NativeBridge.nativeInit(rom.absolutePath, context.filesDir.absolutePath, saveDir.absolutePath)) {
                    throw IllegalStateException("FCEUmm could not load the packaged ROM")
                }
                NativeBridge.nativeLoadSram(File(saveDir, "battery.srm").absolutePath)
                audioTrack = createAudioTrack().also { it.play() }
                val frameNanos = (1_000_000_000.0 / NativeBridge.nativeFrameRate()).toLong()
                var next = System.nanoTime()
                var lastAutoSave = next
                while (running.get()) {
                    if (paused.get()) {
                        Thread.sleep(10)
                        next = System.nanoTime()
                        continue
                    }
                    NativeBridge.nativeSetInput(input.mask())
                    val sampleCount = NativeBridge.nativeRunFrame(pixels, samples)
                    if (sampleCount > 0) audioTrack?.write(samples, 0, sampleCount, AudioTrack.WRITE_BLOCKING)
                    frameConsumer?.invoke(pixels)
                    val now = System.nanoTime()
                    if (now - lastAutoSave > 10_000_000_000L) {
                        NativeBridge.nativeSaveSram(File(saveDir, "battery.srm").absolutePath)
                        lastAutoSave = now
                    }
                    next += frameNanos
                    val remaining = next - System.nanoTime()
                    if (remaining > 1_000_000L) Thread.sleep(remaining / 1_000_000L, (remaining % 1_000_000L).toInt())
                    if (System.nanoTime() - next > frameNanos * 4) next = System.nanoTime()
                }
                NativeBridge.nativeSaveSram(File(saveDir, "battery.srm").absolutePath)
                NativeBridge.nativeShutdown()
            } catch (t: Throwable) {
                errorConsumer?.invoke(t.message ?: t.javaClass.simpleName)
            } finally {
                audioTrack?.run { stop(); release() }
                audioTrack = null
                running.set(false)
            }
        }
    }

    fun pause(value: Boolean) { paused.set(value); if (value) audioTrack?.pause() else audioTrack?.play() }

    fun stop() { running.set(false) }

    fun reset() = commandExecutor.execute { if (running.get()) NativeBridge.nativeReset() }

    fun saveState(slot: Int = 0, callback: (Boolean) -> Unit = {}) = commandExecutor.execute {
        callback(running.get() && NativeBridge.nativeSaveState(stateFile(slot).absolutePath))
    }

    fun loadState(slot: Int = 0, callback: (Boolean) -> Unit = {}) = commandExecutor.execute {
        callback(running.get() && NativeBridge.nativeLoadState(stateFile(slot).absolutePath))
    }

    private fun stateFile(slot: Int) = File(context.filesDir, "saves/$romId/state-$slot.rvs")

    private fun installRom(): File {
        val dir = File(context.filesDir, "roms").apply { mkdirs() }
        val file = File(dir, "super-mario-bros-world.nes")
        if (!file.exists() || file.length() == 0L) {
            context.assets.open("roms/super-mario-bros-world.nes").use { input ->
                val temp = File(dir, "${file.name}.tmp")
                temp.outputStream().use(input::copyTo)
                check(temp.renameTo(file)) { "Unable to install packaged ROM" }
            }
        }
        return file
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { stream ->
            val buffer = ByteArray(8192)
            while (true) {
                val read = stream.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun createAudioTrack(): AudioTrack {
        val min = AudioTrack.getMinBufferSize(48_000, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT)
        return AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            .setAudioFormat(AudioFormat.Builder().setSampleRate(48_000).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).build())
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(max(min, 4096 * 2))
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .build()
    }
}
