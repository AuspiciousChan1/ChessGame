package com.chenjili.chessgame.pages.edit.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chenjili.chess.api.Piece
import com.chenjili.chess.api.PieceColor
import com.chenjili.chess.api.PieceType
import com.chenjili.chessgame.pages.chess.ui.ChessPieceDisplay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class EditType {
    NONE,
    PUT,
    MOVE,
    REMOVE
}

data class EditModeState(
    val playerColor: PieceColor = PieceColor.WHITE,
    val pieces: List<ChessPieceDisplay> = emptyList(),
    val selectedCell: Pair<Int, Int>? = null,
    val selectedPiece: Piece? = null,
    val editType: EditType = EditType.NONE,
)

sealed interface EditModeIntent {
    data class PlayerColorChanged(val newColor: PieceColor) : EditModeIntent
    data class BoardCellClicked(val column: Int, val row: Int, val playerColor: PieceColor): EditModeIntent
    data class PieceForEditClicked(val removeMode: Boolean, val piece: Piece?,): EditModeIntent
    data class ClearBoard(val unused: Unit): EditModeIntent
}

class EditModeViewModel : ViewModel() {
    private val _state = MutableStateFlow(EditModeState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val pieces = mutableListOf<ChessPieceDisplay>()
            var pieceId = 0

            // 白方底线 rank = 0
            pieces += listOf(
                ChessPieceDisplay(Piece(PieceType.ROOK, PieceColor.WHITE), 0, 0, pieceId++),
                ChessPieceDisplay(Piece(PieceType.KNIGHT, PieceColor.WHITE), 1, 0, pieceId++),
                ChessPieceDisplay(Piece(PieceType.BISHOP, PieceColor.WHITE), 2, 0, pieceId++),
                ChessPieceDisplay(Piece(PieceType.QUEEN, PieceColor.WHITE), 3, 0, pieceId++),
                ChessPieceDisplay(Piece(PieceType.KING, PieceColor.WHITE), 4, 0, pieceId++),
                ChessPieceDisplay(Piece(PieceType.BISHOP, PieceColor.WHITE), 5, 0, pieceId++),
                ChessPieceDisplay(Piece(PieceType.KNIGHT, PieceColor.WHITE), 6, 0, pieceId++),
                ChessPieceDisplay(Piece(PieceType.ROOK, PieceColor.WHITE), 7, 0, pieceId++)
            )
            // 白兵 rank = 1
            for (f in 0..7) pieces += ChessPieceDisplay(Piece(PieceType.PAWN, PieceColor.WHITE), f, 1, pieceId++)

            // 黑兵 rank = 6
            for (f in 0..7) pieces += ChessPieceDisplay(Piece(PieceType.PAWN, PieceColor.BLACK), f, 6, pieceId++)
            // 黑方底线 rank = 7
            pieces += listOf(
                ChessPieceDisplay(Piece(PieceType.ROOK, PieceColor.BLACK), 0, 7, pieceId++),
                ChessPieceDisplay(Piece(PieceType.KNIGHT, PieceColor.BLACK), 1, 7, pieceId++),
                ChessPieceDisplay(Piece(PieceType.BISHOP, PieceColor.BLACK), 2, 7, pieceId++),
                ChessPieceDisplay(Piece(PieceType.QUEEN, PieceColor.BLACK), 3, 7, pieceId++),
                ChessPieceDisplay(Piece(PieceType.KING, PieceColor.BLACK), 4, 7, pieceId++),
                ChessPieceDisplay(Piece(PieceType.BISHOP, PieceColor.BLACK), 5, 7, pieceId++),
                ChessPieceDisplay(Piece(PieceType.KNIGHT, PieceColor.BLACK), 6, 7, pieceId++),
                ChessPieceDisplay(Piece(PieceType.ROOK, PieceColor.BLACK), 7, 7, pieceId++)
            )

            // 初始化棋盘为空
            _state.value = EditModeState(
                playerColor = PieceColor.WHITE,
                pieces = pieces,
                selectedCell = null,
                selectedPiece = null,
                editType = EditType.NONE,
            )
        }
    }

    fun processIntent(intent: EditModeIntent) {
        when (intent) {
            is EditModeIntent.PlayerColorChanged -> {
                // 处理玩家颜色更改
                handlePlayerColorChanged(intent.newColor)
            }
            is EditModeIntent.BoardCellClicked -> {
                // 处理棋盘格子点击事件
                handleBoardCellClicked(intent.column, intent.row)
            }
            is EditModeIntent.PieceForEditClicked -> {
                handlePieceForEditClicked(intent.removeMode, intent.piece)
            }
            is EditModeIntent.ClearBoard -> {
                handleClearBoardClicked()
            }
        }
    }

    private fun handlePlayerColorChanged(newColor: PieceColor) {
        val currentState = _state.value
        val updatedPieces = currentState.pieces.map { piece ->
            piece.copy(
                row = 7 - piece.row,
                column = 7 - piece.column
            )
        }

        _state.value = currentState.copy(
            playerColor = newColor,
            pieces = updatedPieces,
            selectedCell = null, // Clear selection when switching sides
            selectedPiece = null,
            editType = EditType.NONE,
        )
    }

    /**
     * 处理棋盘格子点击事件
     * @param column 被点击的列 (0-7)，不受棋盘被翻转的影响
     * @param row 被点击的行 (0-7)，不受棋盘被翻转的影响
     * @param playerColor 当前玩家颜色
     */
    private fun handleBoardCellClicked(column: Int, row: Int) {
        // Check if click is within valid board range
        if (column !in 0..7 || row !in 0..7) {
            // Click outside board - clear selection
            return
        }

        val currentState = _state.value
        val clickedCell = Pair(column, row)

        // Find piece at clicked position
        val pieceAtClickedCell = currentState.pieces.find {
            it.column == column && it.row == row
        }

        when {
            // Case 1: 点击已经被选中的格子 -> 取消选中
            currentState.selectedCell == clickedCell -> {
                select(false, null, null, null)
            }

            // Case 2: 当前没有棋子被选中，而且不是remove模式 -> 选中这个格子，如果格子上有棋子，也选中这个棋子
            currentState.selectedPiece == null && currentState.editType != EditType.REMOVE -> {
                select(false, pieceAtClickedCell?.piece, clickedCell.first, clickedCell.second)
            }

            // Case 3: 当前没有格子被选中，但有棋子（编辑区的棋子）被选中或者处于remove模式 -> 在此格子放置选中的棋子或者删除格子上的棋子
            currentState.selectedCell == null && (currentState.selectedPiece != null || currentState.editType == EditType.REMOVE) -> {
                putPiece(currentState.selectedPiece, column, row)
            }

            // Case 4: 一个格子上的棋子被选中，用户点击了另一个格子 -> 移动棋子
            currentState.selectedCell != null && currentState.selectedPiece != null -> {
                putPiece(null, currentState.selectedCell.first, currentState.selectedCell.second)
                putPiece(currentState.selectedPiece, column, row, pieceAtClickedCell?.id)
            }

            // Case 5: No cell selected and clicked on empty cell -> do nothing
            else -> {
                // No action needed
            }
        }
    }

    // 处理用于编辑局面的棋子被点击事件
    private fun handlePieceForEditClicked(removeMode: Boolean, piece: Piece?) {
        select(removeMode, piece, null, null)
    }

    private fun handleClearBoardClicked() {
        val currentState = _state.value
        _state.value = currentState.copy(
            pieces = emptyList(),
            selectedCell = null,
            selectedPiece = null,
            editType = EditType.NONE,
        )
    }

    private fun select(removeMode: Boolean, piece: Piece?, cellColumn: Int?, cellRow: Int?) {
        if (removeMode) {
            // 选中删除模式
            _state.value = _state.value.copy(
                selectedCell = null,
                selectedPiece = null,
                editType = EditType.REMOVE
            )
            return
        }
        // 选中一个棋盘上的棋子，或者编辑区的棋子
        val currentState = _state.value
        val toSelectCell = if (cellColumn != null && cellRow != null) Pair(cellColumn, cellRow) else null
        val toSelectPiece = if (currentState.selectedPiece?.equals(piece) ?: false) null else piece
        val editType = when {
            toSelectPiece == null && toSelectCell == null -> EditType.NONE
            toSelectPiece != null && toSelectCell == null -> EditType.PUT
            toSelectPiece != null && toSelectCell != null -> EditType.MOVE
            else -> EditType.NONE
        }
        _state.value = currentState.copy(
            selectedCell =  toSelectCell,
            selectedPiece = toSelectPiece,
            editType = editType
        )
    }

    private fun putPiece(piece: Piece?, column: Int, row: Int, pieceId: Int? = null) {
        val currentState = _state.value
        val newPieces = ArrayList(currentState.pieces.filter {
            !(it.column == column && it.row == row)
        })
        if (piece != null) {
            val newPieceId = pieceId ?: (if (currentState.pieces.isEmpty()) 0 else currentState.pieces.maxOf { it.id } + 1)
            newPieces.add(ChessPieceDisplay(piece, column, row, newPieceId))
        }

        _state.value = currentState.copy(
            pieces = newPieces,
            selectedCell = null,
            selectedPiece =  null,
            editType = EditType.NONE,
        )
    }
}