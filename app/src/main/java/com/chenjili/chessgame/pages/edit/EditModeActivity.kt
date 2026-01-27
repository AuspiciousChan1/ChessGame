package com.chenjili.chessgame.pages.edit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.chenjili.chessgame.pages.edit.ui.EditModeScreen
import com.chenjili.chessgame.pages.edit.ui.EditModeViewModel
import com.chenjili.chessgame.pages.edit.ui.theme.ChessGameTheme

class EditModeActivity : ComponentActivity() {
    private val viewModel: EditModeViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by viewModel.state.collectAsState()
            EditModeScreen(
                state=state,
                onIntent = viewModel::processIntent
            )
        }
    }
}