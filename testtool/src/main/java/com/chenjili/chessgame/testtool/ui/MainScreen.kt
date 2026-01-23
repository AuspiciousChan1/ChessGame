package com.chenjili.chessgame.testtool.ui

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chenjili.chessgame.testtool.ui.theme.ChessGameTheme

@Composable
fun mainScreen(
    uiState: MainState = MainState(),
    onIntent: (MainIntent) -> Unit = { }
) {
    ChessGameTheme {
        ChessGameApp(uiState, onIntent)
    }
}

@Composable
fun ChessGameApp(uiState: MainState = MainState(),
                 onIntent: (MainIntent) -> Unit = { }) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            Column(modifier = Modifier) {
                Text(
                    text = "Chess Game Test Tool",
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    fontSize = 21.sp,
                    textAlign = TextAlign.Center
                    )
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(uiState.targets.size) { index ->
                        Button(onClick = {
                            onIntent(MainIntent.NavigateTo(uiState.targets[index]))
                        }) {
                            Text(text= uiState.targets[index].label)
                        }
                    }
                }
            }
        }
    }
}