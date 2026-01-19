package com.chenjili.chessgame

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chenjili.chessgame.pages.chess.ui.ChessActivity
import com.chenjili.chessgame.pages.loading.LoadingDemoActivity
import com.chenjili.chessgame.ui.theme.ChessGameTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChessGameTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainMenu(
                        modifier = Modifier.padding(innerPadding),
                        onChessClick = { ChessActivity.startActivity(this) },
                        onLoadingDemoClick = { LoadingDemoActivity.startActivity(this) }
                    )
                }
            }
        }
    }
}

@Composable
fun MainMenu(
    modifier: Modifier = Modifier,
    onChessClick: () -> Unit = {},
    onLoadingDemoClick: () -> Unit = {}
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "ChessGame Demos",
            modifier = Modifier.padding(bottom = 32.dp)
        )
        Button(
            onClick = onChessClick,
            modifier = Modifier.padding(8.dp)
        ) {
            Text("Chess Game")
        }
        Button(
            onClick = onLoadingDemoClick,
            modifier = Modifier.padding(8.dp)
        ) {
            Text("Loading View Demo")
        }
    }
}