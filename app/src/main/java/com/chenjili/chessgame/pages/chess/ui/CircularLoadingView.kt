package com.chenjili.chessgame.pages.chess.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * 自定义圆环进度加载视图
 * 
 * 特性：
 * - 可配置的圆环分段数量 (segments)
 * - 从圆心指向圆环的箭头，起始向上，顺时针旋转
 * - 进度输入 (0.0 到 1.0)，100% = 一整圈
 * - 超过 1/n 的进度段会从暗色变为亮色
 * - 支持多组暗色/亮色配置，循环选取
 */
class CircularLoadingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // 配置属性
    private var segments: Int = 8 // 圆环分段数量
    private var progress: Float = 0f // 进度 0.0 - 1.0
    
    // 颜色配置 - 支持多组暗色/亮色对
    private val colorPairs = mutableListOf(
        ColorPair(Color.parseColor("#CCCCCC"), Color.parseColor("#4CAF50")), // 灰色/绿色
        ColorPair(Color.parseColor("#FFCCBC"), Color.parseColor("#FF5722")), // 浅橙/深橙
        ColorPair(Color.parseColor("#C5CAE9"), Color.parseColor("#3F51B5"))  // 浅蓝/深蓝
    )
    
    // 画笔
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    
    private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#333333")
    }
    
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.parseColor("#666666")
    }
    
    // 绘制缓存
    private val rectF = RectF()
    private val arrowPath = Path()
    
    /**
     * 设置圆环分段数量
     */
    fun setSegments(count: Int) {
        segments = count.coerceAtLeast(1)
        invalidate()
    }
    
    /**
     * 设置进度 (0.0 - 1.0)
     */
    fun setProgress(value: Float) {
        progress = value.coerceIn(0f, 1f)
        invalidate()
    }
    
    /**
     * 设置颜色对列表
     */
    fun setColorPairs(pairs: List<ColorPair>) {
        if (pairs.isNotEmpty()) {
            colorPairs.clear()
            colorPairs.addAll(pairs)
            invalidate()
        }
    }
    
    /**
     * 添加单个颜色对
     */
    fun addColorPair(darkColor: Int, lightColor: Int) {
        colorPairs.add(ColorPair(darkColor, lightColor))
        invalidate()
    }
    
    /**
     * 清除所有颜色对并设置单个
     */
    fun setSingleColorPair(darkColor: Int, lightColor: Int) {
        colorPairs.clear()
        colorPairs.add(ColorPair(darkColor, lightColor))
        invalidate()
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (width <= 0 || height <= 0) return
        
        val centerX = width / 2f
        val centerY = height / 2f
        val size = min(width, height).toFloat()
        
        // 圆环外半径和内半径
        val outerRadius = size * 0.4f
        val innerRadius = size * 0.25f
        
        // 绘制圆环的每个分段
        val anglePerSegment = 360f / segments
        
        for (i in 0 until segments) {
            // 计算起始角度 (从顶部开始，即 -90 度)
            val startAngle = -90f + i * anglePerSegment
            
            // 判断此分段是否应该高亮 (亮色)
            val segmentEndProgress = (i + 1).toFloat() / segments
            val isLit = progress >= segmentEndProgress
            
            // 根据分段索引循环选择颜色对
            val colorPairIndex = i % colorPairs.size
            val colorPair = colorPairs[colorPairIndex]
            val color = if (isLit) colorPair.lightColor else colorPair.darkColor
            
            // 绘制圆环分段 (使用圆弧)
            ringPaint.color = color
            
            // 使用路径绘制圆环分段
            val path = Path()
            
            // 外弧起点
            val outerStartX = centerX + outerRadius * cos(Math.toRadians(startAngle.toDouble())).toFloat()
            val outerStartY = centerY + outerRadius * sin(Math.toRadians(startAngle.toDouble())).toFloat()
            
            path.moveTo(outerStartX, outerStartY)
            
            // 外弧
            rectF.set(
                centerX - outerRadius,
                centerY - outerRadius,
                centerX + outerRadius,
                centerY + outerRadius
            )
            path.arcTo(rectF, startAngle, anglePerSegment)
            
            // 连线到内弧
            val innerEndAngle = startAngle + anglePerSegment
            val innerEndX = centerX + innerRadius * cos(Math.toRadians(innerEndAngle.toDouble())).toFloat()
            val innerEndY = centerY + innerRadius * sin(Math.toRadians(innerEndAngle.toDouble())).toFloat()
            path.lineTo(innerEndX, innerEndY)
            
            // 内弧 (反向)
            rectF.set(
                centerX - innerRadius,
                centerY - innerRadius,
                centerX + innerRadius,
                centerY + innerRadius
            )
            path.arcTo(rectF, innerEndAngle, -anglePerSegment)
            
            path.close()
            
            canvas.drawPath(path, ringPaint)
            
            // 绘制分段边框
            canvas.drawPath(path, strokePaint)
        }
        
        // 绘制箭头 (从圆心指向当前进度位置)
        drawArrow(canvas, centerX, centerY, innerRadius, outerRadius)
    }
    
    /**
     * 绘制箭头，从圆心指向进度对应的位置
     */
    private fun drawArrow(canvas: Canvas, centerX: Float, centerY: Float, innerRadius: Float, outerRadius: Float) {
        // 计算箭头指向的角度 (从顶部开始顺时针旋转)
        // 0% = -90度 (向上), 100% = 270度 (转一圈回到向上)
        val angle = -90f + progress * 360f
        
        arrowPath.reset()
        
        // 箭头从圆心开始，延伸到外圆环边缘
        val arrowLength = outerRadius + 10f
        val arrowWidth = 12f
        
        // 箭头尖端
        val tipX = centerX + arrowLength * cos(Math.toRadians(angle.toDouble())).toFloat()
        val tipY = centerY + arrowLength * sin(Math.toRadians(angle.toDouble())).toFloat()
        
        // 箭头基部宽度的两个点
        val baseAngle1 = angle - 150f
        val baseAngle2 = angle + 150f
        val baseRadius = arrowWidth
        
        val base1X = centerX + baseRadius * cos(Math.toRadians(baseAngle1.toDouble())).toFloat()
        val base1Y = centerY + baseRadius * sin(Math.toRadians(baseAngle1.toDouble())).toFloat()
        val base2X = centerX + baseRadius * cos(Math.toRadians(baseAngle2.toDouble())).toFloat()
        val base2Y = centerY + baseRadius * sin(Math.toRadians(baseAngle2.toDouble())).toFloat()
        
        arrowPath.moveTo(tipX, tipY)
        arrowPath.lineTo(base1X, base1Y)
        arrowPath.lineTo(centerX, centerY)
        arrowPath.lineTo(base2X, base2Y)
        arrowPath.close()
        
        canvas.drawPath(arrowPath, arrowPaint)
    }
    
    /**
     * 颜色对数据类
     */
    data class ColorPair(
        val darkColor: Int,
        val lightColor: Int
    )
}
