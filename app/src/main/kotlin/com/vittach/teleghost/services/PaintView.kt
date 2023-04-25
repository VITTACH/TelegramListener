package com.vittach.teleghost.services

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.MotionEvent
import android.view.View
import com.vittach.teleghost.domain.FingerPath
import kotlinx.coroutines.*
import java.util.*
import kotlin.math.abs
import kotlin.math.max


class PaintView(context: Context?, attrs: AttributeSet? = null) : View(context, attrs) {

    companion object {
        var BRUSH_SIZE = 10
        const val DEFAULT_COLOR = Color.RED
        private const val TOUCH_TOLERANCE = 4f
    }

    private var mX = 0f
    private var mY = 0f

    private var path: Path? = null
    private val paint = Paint().apply {
        isAntiAlias = true
        isDither = true
        color = DEFAULT_COLOR
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        xfermode = null
        alpha = 0xff
    }

    private val paths = Collections.synchronizedList(ArrayList<FingerPath>())

    private var currentColor = 0

    private var strokeWidth = 0
    private var isEmboss = false
    private var isBlur = false
    private var isDrawing = false

    private val embossFilter = EmbossMaskFilter(floatArrayOf(1f, 1f, 1f), 0.4f, 6f, 3.5f)
    private val blur = BlurMaskFilter(5f, BlurMaskFilter.Blur.NORMAL)
    private val bitmapPaint = Paint(Paint.DITHER_FLAG)

    private lateinit var paintBitmap: Bitmap
    private lateinit var paintCanvas: Canvas

    override fun onDraw(canvas: Canvas) {
        canvas.save()

        paths.forEach {
            paint.color = it.color
            paint.strokeWidth = it.strokeWidth.toFloat()
            paint.maskFilter = when {
                it.emboss -> embossFilter
                it.blur -> blur
                else -> null
            }
            paintCanvas.drawPath(it.path, paint)
        }

        canvas.drawBitmap(paintBitmap, 0f, 0f, bitmapPaint)
        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchStart(x, y)
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                touchMove(x, y)
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                touchUp()
                invalidate()
            }
        }
        return true
    }

    fun init(metrics: DisplayMetrics) {
        val height = metrics.heightPixels
        val width = metrics.widthPixels

        paintBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        paintCanvas = Canvas(paintBitmap)
        currentColor = DEFAULT_COLOR
        strokeWidth = BRUSH_SIZE

        GlobalScope.launch {
            do {
                if (isDrawing) continue
                if (paths.size > 0) {
                    paths.removeFirst()
                    paintCanvas.drawColor(0, PorterDuff.Mode.CLEAR)
                    withContext(Dispatchers.Main) {
                        invalidate()
                    }
                }
                delay(2000L / max(1, paths.size))
            } while (true)
        }
    }

    fun normal() {
        isEmboss = false
        isBlur = false
    }

    fun emboss() {
        isEmboss = true
        isBlur = false
    }

    fun blur() {
        isEmboss = false
        isBlur = true
    }

    fun clear() {
        paths.clear()
        normal()
        invalidate()
    }

    private fun touchStart(x: Float, y: Float) {
        isDrawing = true
        path = Path()
        val fingerPath = FingerPath(currentColor, isEmboss, isBlur, strokeWidth, path!!)
        paths.add(fingerPath)
        path!!.reset()
        path!!.moveTo(x, y)
        mX = x
        mY = y
    }

    private fun touchMove(x: Float, y: Float) {
        val dx = abs(x - mX)
        val dy = abs(y - mY)
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            path!!.lineTo(x, y)
            mX = x
            mY = y
        }
    }

    private fun touchUp() {
        isDrawing = false
        path!!.lineTo(mX, mY)
    }
}