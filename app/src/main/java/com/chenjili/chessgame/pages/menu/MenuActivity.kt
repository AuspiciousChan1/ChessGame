package com.chenjili.chessgame.pages.menu

import android.app.Activity
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
import com.chenjili.chessgame.pages.menu.ui.MenuEffect
import com.chenjili.chessgame.pages.menu.ui.MenuScreen
import com.chenjili.chessgame.pages.menu.ui.MenuViewModel
import kotlinx.coroutines.launch

class MenuActivity : ComponentActivity() {
    companion object {
        fun startActivity(activity: Activity) {
            val intent = android.content.Intent(activity, MenuActivity::class.java)
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP)
            activity.startActivity(intent)
        }
    }

    private val viewModel: MenuViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        collectEffects()
        setContent {
            val state by viewModel.state.collectAsState()
            MenuScreen(state, viewModel::processIntent)
        }
    }

    private fun collectEffects() {
        // 这里可以收集 ViewModel 的副作用流(effect)，并进行处理，比如导航等
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.effect.collect { effect ->
                    when (effect) {
                        is MenuEffect.NavigateTo -> {
                            val intent = Intent(this@MenuActivity, effect.target)
                            if (effect.flags > 0) {
                                intent.addFlags(effect.flags)
                            }
                            this@MenuActivity.startActivity(intent)
                        }
                    }
                }
            }
        }
    }
}