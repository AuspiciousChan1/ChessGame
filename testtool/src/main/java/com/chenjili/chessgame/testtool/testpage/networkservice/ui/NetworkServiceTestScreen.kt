package com.chenjili.chessgame.testtool.testpage.networkservice.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chenjili.chessgame.testtool.testpage.networkservice.ui.theme.ChessGameTheme

@Composable
fun networkServiceTestScreen(
    state: NetworkServiceTestState,
    onIntent: (NetworkServiceTestIntent) -> Unit
) {
    ChessGameTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Box(modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)) {
                Layout(state = state, onIntent = onIntent)
            }
        }
    }
}

@Composable
fun Layout(state: NetworkServiceTestState, onIntent: (NetworkServiceTestIntent) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Network Service Test",
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth(),
            fontSize = 21.sp,
            textAlign = TextAlign.Center
        )
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(2),
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalItemSpacing = 8.dp
        ) {
            items(state.items.size) { index ->
                val item = state.items[index]
                Button(onClick = {
                    onIntent(NetworkServiceTestIntent.CallApi(item.apiType))
                }) {
                    Text(
                        text = "${item.apiType}: ${item.description}",
                        fontSize = 13.sp,
                    )
                }
            }
        }
    }
}