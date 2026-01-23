package com.chenjili.chessgame.testtool.ui

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chenjili.chessgame.testtool.testpage.networkservice.NetworkServiceTestActivity
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AppDestinations(
    val label: String,
    val targetClass: Class<out Activity>,
) {
    CHESS("chess", NetworkServiceTestActivity::class.java),
    NETWORK("network", NetworkServiceTestActivity::class.java),
    ;
}

sealed interface MainIntent {
    data class NavigateTo(val destination: AppDestinations) : MainIntent
}

sealed interface MainEffect {
    data class NavigateTo(val destination: AppDestinations) : MainEffect
}

data class MainState(
    val targets: List<AppDestinations> = emptyList(),
)

class MainViewModel(application: Application): AndroidViewModel(application) {
    private val _state = MutableStateFlow(MainState())
    val uiState: StateFlow<MainState> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<MainEffect>()
    val effect = _effect.asSharedFlow()

    init {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                targets = AppDestinations.values().toList()
            )
        }
    }

    fun processIntent(intent: MainIntent) {
        when (intent) {
            is MainIntent.NavigateTo -> {
                viewModelScope.launch {
                    _effect.emit(MainEffect.NavigateTo(intent.destination))
                }
            }
        }
    }

    fun handleIntentNavigateTo(activity: Activity, destination: AppDestinations) {
        val targetActivity = destination.targetClass
        val navIntent = android.content.Intent(activity, targetActivity)
        activity.startActivity(navIntent)
    }
}