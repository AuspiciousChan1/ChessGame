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
    val id: String, // Unique identifier for the piece
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
    val playerColor: PlayerColor = PlayerColor.White,
    val selectedCell: Pair<Int, Int>? = null, // (column, row) of the selected cell
    val moveHistory: List<String> = emptyList() // List of move notations
)

class ChessViewModel(application: Application) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(ChessState())
    val state: StateFlow<ChessState> = _state.asStateFlow()

    // Helper function to convert column/row to chess notation (e.g., 0,0 -> "a1", 1,2 -> "b3")
    private fun toChessNotation(column: Int, row: Int): String {
        val files = "abcdefgh"
        return "${files[column]}${row + 1}"
    }

    // Helper function to get piece type name in Chinese
    private fun getPieceTypeName(type: PieceType): String {
        return when (type) {
            PieceType.KING -> "王"
            PieceType.QUEEN -> "后"
            PieceType.ROOK -> "车"
            PieceType.BISHOP -> "象"
            PieceType.KNIGHT -> "马"
            PieceType.PAWN -> "兵"
        }
    }

    init {
        viewModelScope.launch {
            // 初始化棋盘 - 按照需求：从start起，分别是兵、马、象、后、王
            val initialPieces = mutableListOf<ChessPieceDisplay>()
            // 白方底线 rank = 0: 兵、马、象、后、王
            initialPieces += listOf(
                ChessPieceDisplay("w_pawn_0", PieceType.PAWN, PlayerColor.White, 0, 0),
                ChessPieceDisplay("w_knight_0", PieceType.KNIGHT, PlayerColor.White, 1, 0),
                ChessPieceDisplay("w_bishop_0", PieceType.BISHOP, PlayerColor.White, 2, 0),
                ChessPieceDisplay("w_queen_0", PieceType.QUEEN, PlayerColor.White, 3, 0),
                ChessPieceDisplay("w_king_0", PieceType.KING, PlayerColor.White, 4, 0)
            )

            // 黑方底线 rank = 7: 兵、马、象、后、王
            initialPieces += listOf(
                ChessPieceDisplay("b_pawn_0", PieceType.PAWN, PlayerColor.Black, 0, 7),
                ChessPieceDisplay("b_knight_0", PieceType.KNIGHT, PlayerColor.Black, 1, 7),
                ChessPieceDisplay("b_bishop_0", PieceType.BISHOP, PlayerColor.Black, 2, 7),
                ChessPieceDisplay("b_queen_0", PieceType.QUEEN, PlayerColor.Black, 3, 7),
                ChessPieceDisplay("b_king_0", PieceType.KING, PlayerColor.Black, 4, 7)
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

    private fun handleBoardCellClicked(column: Int, row: Int) {
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
                    val fromNotation = toChessNotation(selectedCol, selectedRow)
                    val toNotation = toChessNotation(column, row)
                    val pieceTypeName = getPieceTypeName(selectedPiece.type)
                    val moveNotation = "$pieceTypeName$fromNotation-$toNotation"
                    
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
                        moveHistory = currentState.moveHistory + moveNotation
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