package com.chenjili.chessgame.pages.chess.ui

import android.animation.ValueAnimator
import android.content.Context
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

    init {
        // 可按需从 attrs 读取自定义属性（这里用简单默认色）
        borderPaint.strokeWidth = borderWidthPx
    }

    fun setCellColors(light: Int, dark: Int) {
        lightColor = light
        darkColor = dark
        invalidate()
    }

    fun setBorder(color: Int, widthDp: Float) {
        borderColor = color
        borderWidthPx = widthDp * resources.displayMetrics.density
        borderPaint.color = borderColor
        borderPaint.strokeWidth = borderWidthPx
        invalidate()
    }

    fun setPerCellDuration(ms: Long) {
        perCellDurationMs = ms.coerceAtLeast(1L)
    }

    fun setPlayerColor(color: PlayerColor) {
        playerColor = color
        invalidate()
    }

    fun startDraw() {
        animator?.cancel()
        drawnCellsCount = 0
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
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // 强制正方形，取较小边
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
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 如果是黑方视角，需要先旋转画布 180 度（以视图中心为轴）以翻转棋盘位置，但单格绘制方向保持不变
        if (playerColor == PlayerColor.Black) {
            canvas.save()
            canvas.rotate(180f, width / 2f, height / 2f)
        }

        // 绘制已到达的格子（按行主序：row 0..7, col 0..7）
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

        // 如果动画尚未完成，仍需绘制已完成之前的格子边框/占位为透明时不绘制
        // 绘制全部格子边框（不随动画显示）
        borderPaint.style = Paint.Style.STROKE
        borderPaint.color = borderColor
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), borderPaint)

        // 恢复旋转（如有）
        if (playerColor == PlayerColor.Black) {
            canvas.restore()
        }
    }
}