package com.chenjili.chessgame.pages.chess.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.chenjili.chessgame.R
import kotlin.math.min

class StrokeChessBoardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var lightColor: Int = ContextCompat.getColor(context, R.color.gold)
    private var darkColor: Int = ContextCompat.getColor(context, R.color.saddle_brown)
    private var borderColor: Int = ContextCompat.getColor(context, android.R.color.darker_gray)
    private var borderWidthPx: Float = 4f * resources.displayMetrics.density

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    private val cellRects = mutableListOf<RectF>()
    private var cellSizePx: Float = 0f

    // 控制动画的已绘制格子数（0..64）
    private var drawnCellsCount: Int = 0
    private var animator: ValueAnimator? = null
    private var perCellDurationMs: Long = 20L // 每个格子绘制时长，可调整

    private var playerColor: PlayerColor = PlayerColor.White

    // 离屏缓存
    private var cacheBitmap: Bitmap? = null
    private var cacheCanvas: Canvas? = null
    private var cacheValid: Boolean = false

    init {
        borderPaint.strokeWidth = borderWidthPx
    }

    fun setCellColors(light: Int, dark: Int) {
        lightColor = light
        darkColor = dark
        invalidateCache()
    }

    fun setBorder(color: Int, widthDp: Float) {
        borderColor = color
        borderWidthPx = widthDp * resources.displayMetrics.density
        borderPaint.color = borderColor
        borderPaint.strokeWidth = borderWidthPx
        invalidateCache()
    }

    fun setPerCellDuration(ms: Long) {
        perCellDurationMs = ms.coerceAtLeast(1L)
    }

    fun setPlayerColor(color: PlayerColor) {
        playerColor = color
        invalidateCache()
    }

    private fun invalidateCache() {
        cacheValid = false
        invalidate()
    }

    fun startDraw() {
        animator?.cancel()
        drawnCellsCount = 0
        invalidateCache() // 动画时不要使用静态缓存
        val total = 8 * 8
        animator = ValueAnimator.ofInt(0, total).apply {
            duration = perCellDurationMs * total
            addUpdateListener { anim ->
                drawnCellsCount = anim.animatedValue as Int
                invalidate()
            }
            start()
        }
    }

    fun stopDraw() {
        animator?.cancel()
        animator = null
        // 动画结束后可以重建缓存以便后续绘制快速
        invalidateCache()
        // optional: buildCache()  // 延迟到下次 onDraw 构建
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
        cacheBitmap?.recycle()
        cacheBitmap = null
        cacheCanvas = null
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = MeasureSpec.getSize(heightMeasureSpec)
        val size = min(w, h)
        setMeasuredDimension(size, size)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        cellRects.clear()
        val size = min(w, h).toFloat()
        cellSizePx = size / 8f
        for (r in 0 until 8) {
            for (c in 0 until 8) {
                val left = c * cellSizePx
                val top = r * cellSizePx
                val rect = RectF(left, top, left + cellSizePx, top + cellSizePx)
                cellRects.add(rect)
            }
        }

        // 重建或释放缓存 Bitmap
        if (w > 0 && h > 0) {
            cacheBitmap?.recycle()
            cacheBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            cacheCanvas = Canvas(cacheBitmap!!)
            cacheValid = false
        } else {
            cacheBitmap?.recycle()
            cacheBitmap = null
            cacheCanvas = null
            cacheValid = false
        }
    }

    // 将完整棋盘绘制到 cacheCanvas
    private fun buildCache() {
        val cb = cacheCanvas ?: return
        cb.drawColor(0) // 清空
        // 考虑 playerColor 旋转
        if (playerColor == PlayerColor.Black) {
            cb.save()
            cb.rotate(180f, width / 2f, height / 2f)
        }

        // 绘制全部格子
        for (i in 0 until cellRects.size) {
            val rect = cellRects[i]
            val row = i / 8
            val col = i % 8
            val isLight = (row + col) % 2 == 0
            paint.style = Paint.Style.FILL
            paint.color = if (isLight) lightColor else darkColor
            cb.drawRect(rect, paint)
        }

        // 边框
        borderPaint.style = Paint.Style.STROKE
        borderPaint.color = borderColor
        cb.drawRect(0f, 0f, width.toFloat(), height.toFloat(), borderPaint)

        if (playerColor == PlayerColor.Black) {
            cb.restore()
        }
        cacheValid = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 如果没有动画并且缓存可用，直接绘制缓存 Bitmap（避免逐格绘制）
        if (animator == null) {
            if (!cacheValid) {
                buildCache()
            }
            cacheBitmap?.let {
                canvas.drawBitmap(it, 0f, 0f, null)
            }
            return
        }

        // 动画进行中：按原逻辑逐格绘制（缓存已被标记为无效）
        if (playerColor == PlayerColor.Black) {
            canvas.save()
            canvas.rotate(180f, width / 2f, height / 2f)
        }

        val totalToDraw = drawnCellsCount.coerceIn(0, cellRects.size)
        for (i in 0 until totalToDraw) {
            val rect = cellRects[i]
            val row = i / 8
            val col = i % 8
            val isLight = (row + col) % 2 == 0
            paint.style = Paint.Style.FILL
            paint.color = if (isLight) lightColor else darkColor
            canvas.drawRect(rect, paint)
        }

        borderPaint.style = Paint.Style.STROKE
        borderPaint.color = borderColor
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), borderPaint)

        if (playerColor == PlayerColor.Black) {
            canvas.restore()
        }
    }
}
