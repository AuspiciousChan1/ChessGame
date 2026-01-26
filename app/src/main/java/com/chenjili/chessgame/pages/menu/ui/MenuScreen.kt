package com.chenjili.chessgame.pages.menu.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chenjili.chessgame.R
import com.chenjili.chessgame.pages.menu.ui.theme.ChessGameTheme

@Composable
fun MenuScreen(state: MenuState, onIntent: (MenuIntent) -> Unit) {
    ChessGameTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                val maxWidth = this.maxWidth
                val maxHeight = this.maxHeight
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.menu_title_bg),
                            contentDescription = null,
                            contentScale = ContentScale.Inside,
                            modifier = Modifier.matchParentSize()
                        )

                        Text(
                            text = stringResource(R.string.activity_menu_title),
                            fontSize = 25.sp,
                            color = Color(0xFF222222),
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    state.menuItems.forEach { item ->
                        Button(
                            onClick = { onIntent(MenuIntent.OnMenuItemClicked(item)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = maxWidth * 0.1f, vertical = 10.dp)
                        ) {
                            Text(text = stringResource(item.titleId))
                        }
                    }
                }

            }
        }
    }
}