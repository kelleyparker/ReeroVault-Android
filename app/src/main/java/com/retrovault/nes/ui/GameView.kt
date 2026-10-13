package com.retrovault.nes.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import kotlin.math.min

class GameView(context: Context) : View(context) {
    private val bitmap = Bitmap.createBitmap(256, 240, Bitmap.Config.ARGB_8888)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = false }
    private val bitmapLock = Any()

    init { setBackgroundColor(android.graphics.Color.BLACK) }

    fun submitFrame(pixels: IntArray) {
        synchronized(bitmapLock) { bitmap.setPixels(pixels, 0, 256, 0, 0, 256, 240) }
        postInvalidateOnAnimation()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val scale = min(width / 4f, height / 3f)
        val drawWidth = (4f * scale).toInt()
        val drawHeight = (3f * scale).toInt()
        val left = (width - drawWidth) / 2
        val top = (height - drawHeight) / 2
        synchronized(bitmapLock) { canvas.drawBitmap(bitmap, null, Rect(left, top, left + drawWidth, top + drawHeight), paint) }
    }
}
