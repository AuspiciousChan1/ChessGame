package com.chenjili.chessgame.pages.menu.ui.theme

import android.app.Activity
import android.app.Application
import androidx.core.view.ContentInfoCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chenjili.chessgame.R
import com.chenjili.chessgame.pages.chess.ui.ChessActivity
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch

enum class ItemRouteType {
    EDIT_MODE,
    SINGLE_PLAYER,
    MULTI_PLAYER,
    SETTINGS,
    ABOUT
}

data class Item(val titleId: Int, val itemRouteType: ItemRouteType)

data class MenuState(val menuItems: List<Item> = emptyList())

sealed interface MenuIntent {
    data class OnMenuItemClicked(val item: Item) : MenuIntent
}

sealed interface MenuEffect {
    data class NavigateTo(val target: Class<out Activity>, val flags: Int = 0) : MenuEffect
}

class MenuViewModel(application: Application): AndroidViewModel(application) {
    private val _state = MutableStateFlow(MenuState())
    val state = _state.asStateFlow()

    private val _effect = MutableSharedFlow<MenuEffect>()
    val effect = _effect.asSharedFlow()

    init {
        viewModelScope.launch {
            _state.value = MenuState(
                menuItems = arrayListOf(
                    Item(R.string.activity_menu_item_edit_mode, ItemRouteType.EDIT_MODE),
                    Item(R.string.activity_menu_item_single, ItemRouteType.SINGLE_PLAYER),
                    Item(R.string.activity_menu_item_two_player, ItemRouteType.MULTI_PLAYER),
                    Item(R.string.activity_menu_item_settings, ItemRouteType.SETTINGS),
                    Item(R.string.activity_menu_item_about, ItemRouteType.ABOUT)
                )
            )
        }
    }

    fun processIntent(intent: MenuIntent) {
        when (intent) {
            is MenuIntent.OnMenuItemClicked -> {
                // 处理菜单项点击事件
                navigateTo(intent.item)
            }
        }
    }

    private fun navigateTo(item: Item) {
        // 导航逻辑
        when (item.itemRouteType) {
            ItemRouteType.EDIT_MODE -> {
                // 导航到编辑模式
                viewModelScope.launch {
                    _effect.emit(
                        MenuEffect.NavigateTo(
                            target = ChessActivity::class.java
                        )
                    )
                }
            }
            ItemRouteType.SINGLE_PLAYER -> {
                // 导航到单人游戏
            }
            ItemRouteType.MULTI_PLAYER -> {
                // 导航到多人游戏
            }
            ItemRouteType.SETTINGS -> {
                // 导航到设置
            }
            ItemRouteType.ABOUT -> {
                // 导航到关于页面
            }
        }
    }
}