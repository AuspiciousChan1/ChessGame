package com.chenjili.chessgame.pages.chess.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class PlayerColor {
    White,
    Black
}

enum class PieceType{
    KING, QUEEN, ROOK, BISHOP, KNIGHT, PAWN
}

data class ChessPieceDisplay(
    val type: PieceType,
    val color: PlayerColor,
    val column: Int, // 0..7 白方对应a-h；黑方对应h-a
    val row: Int  // 0..7 白方对应1-8；黑方对应8-1
)

// MVI: Intent - 表示用户的所有可能操作
sealed interface ChessIntent {
    data class PlayerColorChanged(val newColor: PlayerColor) : ChessIntent
    data class BoardCellClicked(val column: Int, val row: Int) : ChessIntent
}

// MVI: State - 表示整个UI状态
data class ChessState(
    val pieces: List<ChessPieceDisplay> = emptyList(),
    val playerColor: PlayerColor = PlayerColor.White
)

class ChessViewModel(application: Application) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(ChessState())
    val state: StateFlow<ChessState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            // 初始化棋盘
            val initialPieces = mutableListOf<ChessPieceDisplay>()
            // 白方底线 rank = 0
            initialPieces += listOf(
                ChessPieceDisplay(PieceType.ROOK, PlayerColor.White, 0, 0),
                ChessPieceDisplay(PieceType.KNIGHT, PlayerColor.White, 1, 0),
                ChessPieceDisplay(PieceType.BISHOP, PlayerColor.White, 2, 0),
                ChessPieceDisplay(PieceType.QUEEN, PlayerColor.White, 3, 0),
                ChessPieceDisplay(PieceType.KING, PlayerColor.White, 4, 0),
                ChessPieceDisplay(PieceType.BISHOP, PlayerColor.White, 5, 0),
                ChessPieceDisplay(PieceType.KNIGHT, PlayerColor.White, 6, 0),
                ChessPieceDisplay(PieceType.ROOK, PlayerColor.White, 7, 0)
            )
            // 白兵 rank = 1
            for (f in 0..7) initialPieces += ChessPieceDisplay(PieceType.PAWN, PlayerColor.White, f, 1)

            // 黑兵 rank = 6
            for (f in 0..7) initialPieces += ChessPieceDisplay(PieceType.PAWN, PlayerColor.Black, f, 6)
            // 黑方底线 rank = 7
            initialPieces += listOf(
                ChessPieceDisplay(PieceType.ROOK, PlayerColor.Black, 0, 7),
                ChessPieceDisplay(PieceType.KNIGHT, PlayerColor.Black, 1, 7),
                ChessPieceDisplay(PieceType.BISHOP, PlayerColor.Black, 2, 7),
                ChessPieceDisplay(PieceType.QUEEN, PlayerColor.Black, 3, 7),
                ChessPieceDisplay(PieceType.KING, PlayerColor.Black, 4, 7),
                ChessPieceDisplay(PieceType.BISHOP, PlayerColor.Black, 5, 7),
                ChessPieceDisplay(PieceType.KNIGHT, PlayerColor.Black, 6, 7),
                ChessPieceDisplay(PieceType.ROOK, PlayerColor.Black, 7, 7)
            )
            _state.value = ChessState(
                pieces = initialPieces,
                playerColor = PlayerColor.White
            )
        }
    }

    // MVI: 处理Intent的唯一入口
    fun processIntent(intent: ChessIntent) {
        when (intent) {
            is ChessIntent.PlayerColorChanged -> handlePlayerColorChanged(intent.newColor)
            is ChessIntent.BoardCellClicked -> handleBoardCellClicked(intent.column, intent.row)
        }
    }

    private fun handlePlayerColorChanged(newColor: PlayerColor) {
        val updatedPieces = _state.value.pieces.map { piece ->
            piece.copy(
                row = 7 - piece.row,
                column = 7 - piece.column
            )
        }
        
        _state.value = _state.value.copy(
            playerColor = newColor,
            pieces = updatedPieces
        )
    }

    private fun handleBoardCellClicked(column: Int, row: Int) {
        // 处理点击事件，例如打印点击的格子位置
        println("Cell clicked at row: $row, column: $column")
    }
}