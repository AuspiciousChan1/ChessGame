package com.chenjili.chessgame.pages.loading

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.chenjili.chessgame.pages.chess.ui.CircularLoadingView

/**
 * 演示 CircularLoadingView 的 Activity
 */
class LoadingDemoActivity : AppCompatActivity() {

    private lateinit var loadingView: CircularLoadingView
    private lateinit var progressText: TextView
    private lateinit var segmentsText: TextView

    companion object {
        fun startActivity(context: Context) {
            val intent = Intent(context, LoadingDemoActivity::class.java)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 创建布局
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
        }

        // 标题
        val title = TextView(this).apply {
            text = "圆环加载进度视图示例"
            textSize = 20f
            setPadding(0, 0, 0, 40)
        }
        rootLayout.addView(title)

        // 创建 CircularLoadingView
        loadingView = CircularLoadingView(this).apply {
            layoutParams = LinearLayout.LayoutParams(800, 800)
        }
        rootLayout.addView(loadingView)

        // 进度文本
        progressText = TextView(this).apply {
            text = "进度: 0%"
            textSize = 16f
            setPadding(0, 40, 0, 20)
        }
        rootLayout.addView(progressText)

        // 进度滑块
        val progressSeekBar = SeekBar(this).apply {
            max = 100
            progress = 0
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val progressValue = progress / 100f
                    loadingView.setProgress(progressValue)
                    progressText.text = "进度: $progress%"
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        rootLayout.addView(progressSeekBar)

        // 分段数文本
        segmentsText = TextView(this).apply {
            text = "分段数: 8"
            textSize = 16f
            setPadding(0, 40, 0, 20)
        }
        rootLayout.addView(segmentsText)

        // 分段数滑块
        val segmentsSeekBar = SeekBar(this).apply {
            max = 20
            min = 3
            progress = 8
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    loadingView.setSegments(progress)
                    segmentsText.text = "分段数: $progress"
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        rootLayout.addView(segmentsSeekBar)

        // 添加按钮来演示不同的颜色配置
        val colorButton1 = Button(this).apply {
            text = "默认颜色方案"
            setOnClickListener {
                loadingView.setColorPairs(listOf(
                    CircularLoadingView.ColorPair(
                        android.graphics.Color.parseColor("#CCCCCC"),
                        android.graphics.Color.parseColor("#4CAF50")
                    ),
                    CircularLoadingView.ColorPair(
                        android.graphics.Color.parseColor("#FFCCBC"),
                        android.graphics.Color.parseColor("#FF5722")
                    ),
                    CircularLoadingView.ColorPair(
                        android.graphics.Color.parseColor("#C5CAE9"),
                        android.graphics.Color.parseColor("#3F51B5")
                    )
                ))
            }
        }
        rootLayout.addView(colorButton1)

        val colorButton2 = Button(this).apply {
            text = "单色方案 (蓝色)"
            setOnClickListener {
                loadingView.setSingleColorPair(
                    android.graphics.Color.parseColor("#BBDEFB"),
                    android.graphics.Color.parseColor("#2196F3")
                )
            }
        }
        rootLayout.addView(colorButton2)

        val colorButton3 = Button(this).apply {
            text = "彩虹色方案"
            setOnClickListener {
                loadingView.setColorPairs(listOf(
                    CircularLoadingView.ColorPair(
                        android.graphics.Color.parseColor("#FFCDD2"),
                        android.graphics.Color.parseColor("#F44336")
                    ),
                    CircularLoadingView.ColorPair(
                        android.graphics.Color.parseColor("#FFE0B2"),
                        android.graphics.Color.parseColor("#FF9800")
                    ),
                    CircularLoadingView.ColorPair(
                        android.graphics.Color.parseColor("#FFF9C4"),
                        android.graphics.Color.parseColor("#FFEB3B")
                    ),
                    CircularLoadingView.ColorPair(
                        android.graphics.Color.parseColor("#C8E6C9"),
                        android.graphics.Color.parseColor("#4CAF50")
                    ),
                    CircularLoadingView.ColorPair(
                        android.graphics.Color.parseColor("#B3E5FC"),
                        android.graphics.Color.parseColor("#03A9F4")
                    ),
                    CircularLoadingView.ColorPair(
                        android.graphics.Color.parseColor("#D1C4E9"),
                        android.graphics.Color.parseColor("#673AB7")
                    )
                ))
            }
        }
        rootLayout.addView(colorButton3)

        setContentView(rootLayout)
    }
}
