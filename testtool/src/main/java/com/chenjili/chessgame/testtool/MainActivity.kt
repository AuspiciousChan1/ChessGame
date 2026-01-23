package com.chenjili.chessgame.testtool

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.chenjili.chessgame.testtool.ui.MainEffect
import com.chenjili.chessgame.testtool.ui.MainViewModel
import com.chenjili.chessgame.testtool.ui.mainScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    val viewModel: MainViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.effect.collect { effect ->
                    when (effect) {
                        is MainEffect.NavigateTo -> {
                            startActivity(
                                Intent(
                                    this@MainActivity,
                                    effect.destination.targetClass
                                )
                            )
                        }
                    }
                }
            }
        }
        setContent {
            val state by viewModel.uiState.collectAsState()
            mainScreen(uiState = state,
                onIntent = viewModel::processIntent)
        }
    }
}