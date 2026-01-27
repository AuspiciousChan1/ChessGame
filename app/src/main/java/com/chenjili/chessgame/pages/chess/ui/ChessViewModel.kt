package com.chenjili.chessgame.pages.chess.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chenjili.chess.api.Piece
import com.chenjili.chess.api.PieceColor
import com.chenjili.chess.api.PieceType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChessPieceDisplay (
    val piece: Piece,
    val column: Int, // 0..7 白方对应a-h；黑方对应h-a
    val row: Int,  // 0..7 白方对应1-8；黑方对应8-1
    val id: Int // Unique identifier for animation tracking
)

data class ChessMove(
    val pieceType: PieceType,
    val fromColumn: Int,
    val fromRow: Int,
    val toColumn: Int,
    val toRow: Int,
    val pieceColor: PieceColor,
    val notation: String // e.g., "Nb1-c3"
)

// MVI: Intent - 表示用户的所有可能操作
sealed interface ChessIntent {
    data class PlayerColorChanged(val newColor: PieceColor) : ChessIntent
    data class BoardCellClicked(val column: Int, val row: Int, val playerColor: PieceColor) : ChessIntent
}

// MVI: State - 表示整个UI状态
data class ChessState(
    val pieces: List<ChessPieceDisplay> = emptyList(),
    val playerColor: PieceColor = PieceColor.WHITE,
    val selectedCell: Pair<Int, Int>? = null, // (column, row) of the selected cell
    val moveHistory: List<ChessMove> = emptyList() // History of all moves
)

class ChessViewModel(application: Application) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(ChessState())
    val state: StateFlow<ChessState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            // 初始化棋盘
            val initialPieces = mutableListOf<ChessPieceDisplay>()
            var pieceId = 0
            
            // 白方底线 rank = 0
            initialPieces += listOf(
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
            for (f in 0..7) initialPieces += ChessPieceDisplay(Piece(PieceType.PAWN, PieceColor.WHITE), f, 1, pieceId++)

            // 黑兵 rank = 6
            for (f in 0..7) initialPieces += ChessPieceDisplay(Piece(PieceType.PAWN, PieceColor.BLACK), f, 6, pieceId++)
            // 黑方底线 rank = 7
            initialPieces += listOf(
                ChessPieceDisplay(Piece(PieceType.ROOK, PieceColor.BLACK), 0, 7, pieceId++),
                ChessPieceDisplay(Piece(PieceType.KNIGHT, PieceColor.BLACK), 1, 7, pieceId++),
                ChessPieceDisplay(Piece(PieceType.BISHOP, PieceColor.BLACK), 2, 7, pieceId++),
                ChessPieceDisplay(Piece(PieceType.QUEEN, PieceColor.BLACK), 3, 7, pieceId++),
                ChessPieceDisplay(Piece(PieceType.KING, PieceColor.BLACK), 4, 7, pieceId++),
                ChessPieceDisplay(Piece(PieceType.BISHOP, PieceColor.BLACK), 5, 7, pieceId++),
                ChessPieceDisplay(Piece(PieceType.KNIGHT, PieceColor.BLACK), 6, 7, pieceId++),
                ChessPieceDisplay(Piece(PieceType.ROOK, PieceColor.BLACK), 7, 7, pieceId++)
            )
            _state.value = ChessState(
                pieces = initialPieces,
                playerColor = PieceColor.WHITE
            )
        }
    }

    // Helper function to convert column and row to chess notation
    private fun positionToNotation(column: Int, row: Int, playerColor: PieceColor): String {
        val transformedColumn = if (playerColor == PieceColor.WHITE) column else 7 - column
        val transformedRow = if (playerColor == PieceColor.WHITE) row else 7 - row
        val file = ('a' + transformedColumn).toString()
        val rank = (transformedRow + 1).toString()
        return "$file$rank"
    }

    // Helper function to get piece notation prefix
    private fun getPieceNotation(pieceType: PieceType): String {
        return when (pieceType) {
            PieceType.KING -> "K"
            PieceType.QUEEN -> "Q"
            PieceType.ROOK -> "R"
            PieceType.BISHOP -> "B"
            PieceType.KNIGHT -> "N"
            PieceType.PAWN -> ""
        }
    }

    // MVI: 处理Intent的唯一入口
    fun processIntent(intent: ChessIntent) {
        when (intent) {
            is ChessIntent.PlayerColorChanged -> handlePlayerColorChanged(intent.newColor)
            is ChessIntent.BoardCellClicked -> handleBoardCellClicked(intent.column, intent.row)
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
            selectedCell = null // Clear selection when switching sides
        )
    }

    /**
     * 处理棋盘格子点击事件
     * @param column 被点击的列 (0-7)，不受棋盘被翻转的影响
     * @param row 被点击的行 (0-7)，不受棋盘被翻转的影响
     * @param playerColor 当前玩家颜色
     */
    private fun handleBoardCellClicked(column: Int, row: Int) {
        val playerColor = _state.value.playerColor
        // Check if click is within valid board range
        if (column !in 0..7 || row !in 0..7) {
            // Click outside board - clear selection
            _state.value = _state.value.copy(selectedCell = null)
            return
        }

        val currentState = _state.value
        val clickedCell = Pair(column, row)
        
        // Find piece at clicked position
        val pieceAtClickedCell = currentState.pieces.find { 
            it.column == column && it.row == row 
        }

        when {
            // Case 1: Click on the same cell that's already selected -> deselect
            currentState.selectedCell == clickedCell -> {
                _state.value = currentState.copy(selectedCell = null)
            }
            
            // Case 2: No cell selected and clicked cell has a piece -> select it
            currentState.selectedCell == null && pieceAtClickedCell != null -> {
                _state.value = currentState.copy(selectedCell = clickedCell)
            }
            
            // Case 3: A cell is selected and user clicks another cell -> move piece
            currentState.selectedCell != null -> {
                val (selectedCol, selectedRow) = currentState.selectedCell
                val selectedPiece = currentState.pieces.find { 
                    it.column == selectedCol && it.row == selectedRow 
                }
                
                if (selectedPiece != null) {
                    // Create move notation
                    val fromNotation = positionToNotation(selectedCol, selectedRow, playerColor)
                    val toNotation = positionToNotation(column, row, playerColor)
                    val pieceNotation = getPieceNotation(selectedPiece.piece.type)
                    val moveNotation = "$pieceNotation$fromNotation-$toNotation"
                    
                    val newMove = ChessMove(
                        pieceType = selectedPiece.piece.type,
                        fromColumn = selectedCol,
                        fromRow = selectedRow,
                        toColumn = column,
                        toRow = row,
                        pieceColor = selectedPiece.piece.color,
                        notation = moveNotation
                    )
                    
                    // Remove piece at destination if exists (capture) and move selected piece
                    val updatedPieces = currentState.pieces.mapNotNull { piece ->
                        when {
                            // Skip the selected piece being moved (we'll add it back with new position)
                            piece.column == selectedCol && piece.row == selectedRow -> 
                                piece.copy(column = column, row = row) // Move selected piece
                            // Remove any piece at the destination (capture)
                            piece.column == column && piece.row == row -> null
                            // Keep all other pieces unchanged
                            else -> piece
                        }
                    }
                    
                    _state.value = currentState.copy(
                        pieces = updatedPieces,
                        selectedCell = null,
                        moveHistory = currentState.moveHistory + newMove
                    )
                } else {
                    // No piece found at selected cell (shouldn't happen), just deselect
                    _state.value = currentState.copy(selectedCell = null)
                }
            }
            
            // Case 4: No cell selected and clicked on empty cell -> do nothing
            else -> {
                // No action needed
            }
        }
    }
}