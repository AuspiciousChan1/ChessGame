package com.chenjili.chessgame.pages.chess.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.chenjili.chessgame.pages.chess.ui.theme.ChessGameTheme
import kotlin.getValue

class ChessActivity : ComponentActivity() {
    private val viewModel: ChessViewModel by viewModels()
    companion object {
        const val TAG = "ChessActivity"
        fun startActivity(activity: Activity, map: Map<String, String>? = null) {
            val intent = Intent(activity, ChessActivity::class.java)
            map?.forEach {
                intent.putExtra(it.key, it.value)
            }
            activity.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val pieces by viewModel.pieces.collectAsState()
            val color by viewModel.color.collectAsState()
            ChessScreen(application=this@ChessActivity.application, playerColor=color, pieces = pieces,
                onBoardLayoutChanged = { x: Dp, y: Dp, width: Dp, height: Dp ->
                    Log.i(TAG, "x=$x, y=$y, width=$width, height=$height")
                }, onPlayerColorChanged = viewModel::onPlayerColorChanged
            )
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ChessGameTheme {
        Greeting("Android")
    }
}