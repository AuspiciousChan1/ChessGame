package com.chenjili.chessgame.pages.chess.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class PlayerColor {
    White,
    Black
}

enum class PieceType{
    KING, QUEEN, ROOK, BISHOP, KNIGHT, PAWN
}

data class ChessPiece(
    val type: PieceType,
    val color: PlayerColor,
    val file: Int, // 0..7 对应 a..h（白方视角从左到右）
    val rank: Int  // 0..7 对应 1..8（0 为白方底线）
)


data class PieceStatus (
    var color: Int,
    var x: Int,
    var y: Int,
    var type: PieceType
)

class ChessViewModel(application: Application) : AndroidViewModel(application) {
    private val _pieces = MutableStateFlow<ArrayList<PieceStatus>>(ArrayList())
    val pieces: StateFlow<ArrayList<PieceStatus>> = _pieces.asStateFlow()
}