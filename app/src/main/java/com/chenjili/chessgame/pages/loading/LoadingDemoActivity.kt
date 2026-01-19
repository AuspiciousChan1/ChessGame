package com.chenjili.chessgame.pages.loading

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.chenjili.chessgame.pages.chess.ui.CircularLoadingView

/**
 * 演示 CircularLoadingView 的 Activity
 */
class LoadingDemoActivity : ComponentActivity() {

    private lateinit var loadingView: CircularLoadingView
    private lateinit var progressText: TextView
    private lateinit var segmentsText: TextView

    companion object {
        fun startActivity(context: Context) {
            val intent = Intent(context, LoadingDemoActivity::class.java)
            if (context !is Activity) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val TAG = "LoadingDemoActivity"

        // 创建布局并保护自定义 View 的构造与 setContentView
        try {
            val rootLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(40, 40, 40, 40)
            }

            val title = TextView(this).apply {
                text = "圆环加载进度视图示例"
                textSize = 20f
                setPadding(0, 0, 0, 40)
            }
            rootLayout.addView(title)

            // 保护 CircularLoadingView 的构造，避免它内部抛出未捕获异常
            try {
                loadingView = CircularLoadingView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(800, 800)
                }
                rootLayout.addView(loadingView)
            } catch (e: Exception) {
                Log.e(TAG, "创建 CircularLoadingView 失败", e)
                // 如果自定义视图失败，终止 Activity，或可改为展示占位视图
                finish()
                return
            }

            progressText = TextView(this).apply {
                text = "进度: 0%"
                textSize = 16f
                setPadding(0, 40, 0, 20)
            }
            rootLayout.addView(progressText)

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
                        try {
                            loadingView.setProgress(progressValue)
                        } catch (e: Exception) {
                            Log.e(TAG, "更新 loadingView 进度失败", e)
                        }
                        progressText.text = "进度: $progress%"
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                })
            }
            rootLayout.addView(progressSeekBar)

            segmentsText = TextView(this).apply {
                text = "分段数: 8"
                textSize = 16f
                setPadding(0, 40, 0, 20)
            }
            rootLayout.addView(segmentsText)

            val segmentsSeekBar = SeekBar(this).apply {
                max = 20
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    min = 3
                }
                progress = 8
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                        try {
                            loadingView.setSegments(progress)
                        } catch (e: Exception) {
                            Log.e(TAG, "设置 segments 失败", e)
                        }
                        segmentsText.text = "分段数: $progress"
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                })
            }
            rootLayout.addView(segmentsSeekBar)

            // 颜色按钮（省略重复异常处理，保持简洁）
            val colorButton1 = Button(this).apply {
                text = "默认颜色方案"
                setOnClickListener {
                    try {
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
                    } catch (e: Exception) {
                        Log.e(TAG, "设置颜色方案失败", e)
                    }
                }
            }
            rootLayout.addView(colorButton1)

            val colorButton2 = Button(this).apply {
                text = "单色方案 (蓝色)"
                setOnClickListener {
                    try {
                        loadingView.setSingleColorPair(
                            android.graphics.Color.parseColor("#BBDEFB"),
                            android.graphics.Color.parseColor("#2196F3")
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "设置单色方案失败", e)
                    }
                }
            }
            rootLayout.addView(colorButton2)

            val colorButton3 = Button(this).apply {
                text = "彩虹色方案"
                setOnClickListener {
                    try {
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
                    } catch (e: Exception) {
                        Log.e(TAG, "设置彩虹色方案失败", e)
                    }
                }
            }
            rootLayout.addView(colorButton3)

            setContentView(rootLayout)
        } catch (e: Exception) {
            Log.e("LoadingDemoActivity", "onCreate 初始化视图失败", e)
            finish()
        }
    }
}