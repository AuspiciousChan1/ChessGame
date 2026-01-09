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
    var column: Int, // 0..7 白方对应a-h；黑方对应h-a
    var row: Int  // 0..7 白方对应1-8；黑方对应8-1
)

class ChessViewModel(application: Application) : AndroidViewModel(application) {
    private val _pieces = MutableStateFlow<ArrayList<ChessPieceDisplay>>(ArrayList())
    private val _color = MutableStateFlow<PlayerColor>(PlayerColor.White)
    val pieces: StateFlow<ArrayList<ChessPieceDisplay>> = _pieces.asStateFlow()
    val color: StateFlow<PlayerColor> = _color.asStateFlow()

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
            _pieces.value = ArrayList(initialPieces)
            _color.value = PlayerColor.White
        }
    }

    fun onPlayerColorChanged(newColor: PlayerColor) {
        _color.value = newColor

        for (piece in _pieces.value) {
            piece.row = 7 - piece.row
            piece.column = 7 - piece.column
        }
    }

    fun onBoardCellClicked(column: Int, row: Int) {
        // 处理点击事件，例如打印点击的格子位置
        println("Cell clicked at row: $row, column: $column")
    }
}