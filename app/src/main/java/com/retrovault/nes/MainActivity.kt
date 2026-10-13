package com.retrovault.nes

import android.content.res.Configuration
import android.os.Bundle
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.retrovault.nes.emulation.EmulatorEngine
import com.retrovault.nes.emulation.NesButtons
import com.retrovault.nes.ui.GameView
import kotlin.math.abs

class MainActivity : ComponentActivity() {
    private lateinit var engine: EmulatorEngine
    private var playing by mutableStateOf(false)
    private var status by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        engine = EmulatorEngine(this).apply { errorConsumer = { runOnUiThread { status = it } } }
        setContent { RetroTheme { if (playing) GameScreen() else LibraryScreen() } }
    }

    override fun onPause() { super.onPause(); if (playing) engine.pause(true) }
    override fun onResume() { super.onResume(); if (playing) engine.pause(false) }
    override fun onDestroy() { engine.stop(); super.onDestroy() }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (!playing || !event.isGamepadEvent()) return super.dispatchKeyEvent(event)
        val button = when (event.keyCode) {
            KeyEvent.KEYCODE_BUTTON_A -> NesButtons.A
            KeyEvent.KEYCODE_BUTTON_B -> NesButtons.B
            KeyEvent.KEYCODE_BUTTON_START -> NesButtons.START
            KeyEvent.KEYCODE_BUTTON_SELECT, KeyEvent.KEYCODE_BUTTON_MODE -> NesButtons.SELECT
            KeyEvent.KEYCODE_DPAD_UP -> NesButtons.UP
            KeyEvent.KEYCODE_DPAD_DOWN -> NesButtons.DOWN
            KeyEvent.KEYCODE_DPAD_LEFT -> NesButtons.LEFT
            KeyEvent.KEYCODE_DPAD_RIGHT -> NesButtons.RIGHT
            else -> 0
        }
        if (button == 0) return super.dispatchKeyEvent(event)
        engine.input.setGamepad(button, event.action == KeyEvent.ACTION_DOWN)
        return true
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        if (!playing || event.action != MotionEvent.ACTION_MOVE || event.source and InputDevice.SOURCE_JOYSTICK != InputDevice.SOURCE_JOYSTICK) return super.dispatchGenericMotionEvent(event)
        val x = event.getAxisValue(MotionEvent.AXIS_X)
        val y = event.getAxisValue(MotionEvent.AXIS_Y)
        val hatX = event.getAxisValue(MotionEvent.AXIS_HAT_X)
        val hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y)
        engine.input.setGamepad(NesButtons.LEFT, x < -0.45f || hatX < -0.5f)
        engine.input.setGamepad(NesButtons.RIGHT, x > 0.45f || hatX > 0.5f)
        engine.input.setGamepad(NesButtons.UP, y < -0.45f || hatY < -0.5f)
        engine.input.setGamepad(NesButtons.DOWN, y > 0.45f || hatY > 0.5f)
        return abs(x) > 0.1f || abs(y) > 0.1f || abs(hatX) > 0.1f || abs(hatY) > 0.1f || super.dispatchGenericMotionEvent(event)
    }

    @Composable
    private fun LibraryScreen() {
        Box(Modifier.fillMaxSize().background(Color(0xFF111113)).windowInsetsPadding(WindowInsets.safeDrawing)) {
            Column(Modifier.fillMaxSize().padding(24.dp)) {
                Text("RETROVAULT", color = Color(0xFFE34B4B), fontSize = 13.sp)
                Text("NES", color = Color.White, fontSize = 38.sp)
                Text("1 game · Offline library", color = Color(0xFF96969E), fontSize = 14.sp)
                Spacer(Modifier.height(32.dp))
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF212126)), shape = RoundedCornerShape(22.dp)) {
                    Column(Modifier.fillMaxWidth().padding(24.dp)) {
                        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(14.dp)).background(Color(0xFF2E2E34)), contentAlignment = Alignment.Center) {
                            Text("SMB", color = Color(0xFFE34B4B), fontSize = 48.sp)
                        }
                        Spacer(Modifier.height(18.dp))
                        Text("Super Mario Bros.", color = Color.White, fontSize = 22.sp)
                        Text("World · Nintendo Entertainment System", color = Color(0xFFAAAAAF), fontSize = 13.sp)
                        Spacer(Modifier.height(18.dp))
                        Button(onClick = { playing = true; status = null; window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); engine.start() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD84343))) {
                            Text("PLAY")
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun GameScreen() {
        val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
        val message = status
        Box(Modifier.fillMaxSize().background(Color(0xFF0B0B0D)).windowInsetsPadding(WindowInsets.safeDrawing)) {
            if (landscape) {
                Row(Modifier.fillMaxSize()) {
                    DPad(Modifier.weight(0.25f).fillMaxHeight())
                    GameDisplay(Modifier.weight(0.5f).fillMaxHeight())
                    ActionPad(Modifier.weight(0.25f).fillMaxHeight())
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    GameDisplay(Modifier.fillMaxWidth().weight(0.48f))
                    Row(Modifier.fillMaxWidth().weight(0.38f)) {
                        DPad(Modifier.weight(1f).fillMaxHeight())
                        ActionPad(Modifier.weight(1f).fillMaxHeight())
                    }
                    SystemButtons(Modifier.fillMaxWidth().weight(0.14f))
                }
            }
            if (landscape) SystemButtons(Modifier.align(Alignment.BottomCenter).width(220.dp).height(54.dp))
            if (message != null) Snackbar(Modifier.align(Alignment.TopCenter).padding(16.dp)) { Text(message) }
        }
    }

    @Composable
    private fun GameDisplay(modifier: Modifier) {
        AndroidView(
            modifier = modifier,
            factory = { context -> GameView(context).also { view -> engine.frameConsumer = view::submitFrame } },
            update = { engine.frameConsumer = it::submitFrame }
        )
    }

    @Composable
    private fun DPad(modifier: Modifier = Modifier) {
        Box(modifier.padding(16.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ControlButton("▲", NesButtons.UP)
                Row { ControlButton("◀", NesButtons.LEFT); Spacer(Modifier.size(52.dp)); ControlButton("▶", NesButtons.RIGHT) }
                ControlButton("▼", NesButtons.DOWN)
            }
        }
    }

    @Composable
    private fun ActionPad(modifier: Modifier = Modifier) {
        Box(modifier.padding(16.dp), contentAlignment = Alignment.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RoundButton("B", NesButtons.B, Modifier.offset(y = 18.dp))
                Spacer(Modifier.width(18.dp))
                RoundButton("A", NesButtons.A, Modifier.offset(y = (-18).dp))
            }
        }
    }

    @Composable
    private fun SystemButtons(modifier: Modifier = Modifier) {
        Row(modifier, horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            SmallButton("SELECT", NesButtons.SELECT)
            Spacer(Modifier.width(12.dp))
            SmallButton("START", NesButtons.START)
            Spacer(Modifier.width(12.dp))
            TextButton(onClick = { engine.saveState { runOnUiThread { status = if (it) "State saved" else "Save failed" } } }) { Text("SAVE", color = Color.LightGray, fontSize = 11.sp) }
            TextButton(onClick = { engine.loadState { runOnUiThread { status = if (it) "State loaded" else "No saved state" } } }) { Text("LOAD", color = Color.LightGray, fontSize = 11.sp) }
        }
    }

    @Composable
    private fun ControlButton(label: String, button: Int) = Box(
        Modifier.size(52.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xCC333338)).pressButton(button),
        contentAlignment = Alignment.Center
    ) { Text(label, color = Color(0xFFD5D5D7), fontSize = 18.sp) }

    @Composable
    private fun RoundButton(label: String, button: Int, modifier: Modifier = Modifier) = Box(
        modifier.size(72.dp).clip(CircleShape).background(Color(0xD9B93238)).pressButton(button),
        contentAlignment = Alignment.Center
    ) { Text(label, color = Color.White, fontSize = 24.sp) }

    @Composable
    private fun SmallButton(label: String, button: Int) = Box(
        Modifier.width(62.dp).height(30.dp).clip(RoundedCornerShape(15.dp)).background(Color(0xCC3A3A40)).pressButton(button),
        contentAlignment = Alignment.Center
    ) { Text(label, color = Color.White, fontSize = 10.sp) }

    private fun Modifier.pressButton(button: Int) = pointerInput(button) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            engine.input.setTouch(button, true)
            try { do { val event = awaitPointerEvent() } while (event.changes.any { it.pressed }) }
            finally { engine.input.setTouch(button, false) }
        }
    }

    private fun KeyEvent.isGamepadEvent() = source and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD || source and InputDevice.SOURCE_DPAD == InputDevice.SOURCE_DPAD
}

@Composable
private fun RetroTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFFD84343), background = Color(0xFF111113), surface = Color(0xFF212126)), content = content)
}
