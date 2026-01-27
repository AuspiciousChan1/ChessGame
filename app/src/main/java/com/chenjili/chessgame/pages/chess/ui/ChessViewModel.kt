package com.chenjili.chessgame.pages.chess.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chenjili.chess.api.ChessServiceFactory
import com.chenjili.chess.api.IChessGame
import com.chenjili.chess.api.IChessService
import com.chenjili.chess.api.Move
import com.chenjili.chess.api.Piece
import com.chenjili.chess.api.PieceColor
import com.chenjili.chess.api.PieceType
import com.chenjili.chess.api.Position
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
    val move: Move,
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

    private lateinit var chessGame: IChessGame
    private val _state = MutableStateFlow(ChessState())
    val state: StateFlow<ChessState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            initPieces()
            chessGame = ChessServiceFactory.chessService.createGame()
        }
    }

    private fun initPieces() {
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

    /**
     * 生成移动的标准棋谱表示
     * @param piece 被移动的棋子
     * @param from 起始位置 (column, row)
     * @param to 目标位置 (column, row)
     * @return 棋谱字符串，例如 "Nb1-c3"
     */
    private fun getMoveNotation(piece: Piece, from: Pair<Int, Int>, to: Pair<Int, Int>): String {
        val fromNotation = positionToNotation(from.first, from.second, piece.color)
        val toNotation = positionToNotation(to.first, to.second, piece.color)
        val pieceNotation = getPieceNotation(piece.type)
        return "$pieceNotation$fromNotation-$toNotation"
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
        val pieceAtClickedCell: ChessPieceDisplay? = currentState.pieces.find {
            it.column == column && it.row == row 
        }

        val selectedPiece: ChessPieceDisplay? = currentState.selectedCell?.let { (selectedCol, selectedRow) ->
            currentState.pieces.find {
                it.column == selectedCol && it.row == selectedRow
            }
        }

        when {
            // Case 1: 点击已经被选中的格子 -> 取消选中
            currentState.selectedCell == clickedCell -> {
                cancelSelect()
            }

            // Case 2: 当前没有棋子被选中，而且新格子中有一个棋子 -> 选中这个新格子
            selectedPiece == null && pieceAtClickedCell != null  -> {
                selectCell(clickedCell.first, clickedCell.second)
            }

            // Case 3: 当前没有棋子被选中，而且新格子中也没有棋子 -> 什么都不做
            selectedPiece == null && pieceAtClickedCell != null  -> {
                // No action needed
            }

            // Case 4: 一个格子上的棋子被选中，用户点击了另一个格子 -> 移动棋子或者吃子。这里需要考虑吃过路兵、王车易位和升变操作。
            selectedPiece != null -> {

                // Create move notation
                val selectedCol = selectedPiece.column
                val selectedRow = selectedPiece.row
                val selectedPosition = if (playerColor == PieceColor.WHITE) {
                    Position(selectedCol, selectedRow)
                } else {
                    Position(7 - selectedCol, 7 - selectedRow)
                }
                val clickedPosition = if (playerColor == PieceColor.WHITE) {
                    Position(column, row)
                } else {
                    Position(7 - column, 7 - row)
                }
                val move: Move? = chessGame.makeMove(selectedPosition, clickedPosition)
                if (move == null) {
                    // Illegal move - ignore
                    // @CJL/TODO: 可以考虑给用户一些反馈，提示非法走法
                    _state.value = currentState.copy(selectedCell = null)
                    return
                }
                val moveNotation = getMoveNotation(selectedPiece.piece, selectedCol to selectedRow, column to row)

                val newMove = ChessMove(
                    move = move,
                    notation = moveNotation
                )

                // Remove piece at destination if exists (capture) and move selected piece
                val updatedPieces = currentState.pieces.let { currentPieces ->
                    // 吃子
                    val withoutCaptured = if (move.capturedPiece != null) {
                        val capturedPosition = if (move.isEnPassant) {
                            // For en passant, the captured pawn is not on the target square
                            val direction = if (selectedPiece.piece.color == PieceColor.WHITE) -1 else 1
                            Position(move.to.file, move.to.rank + direction)
                        } else {
                            move.to.copy()
                        }
                        currentPieces.filterNot { p ->
                            p.column == (if (playerColor == PieceColor.WHITE) capturedPosition.file else 7 - capturedPosition.file) &&
                            p.row == (if (playerColor == PieceColor.WHITE) capturedPosition.rank else 7 - capturedPosition.rank)
                        }
                    } else {
                        currentPieces
                    }

                    // 走子
                    withoutCaptured.map { pieceDisplay ->
                        if (pieceDisplay.column == selectedCol && pieceDisplay.row == selectedRow) {
                            // Move the selected piece to the new location
                            val peaceType = move.promotionPiece ?: pieceDisplay.piece.type
                            val updatedPiece = pieceDisplay.piece.copy(type = peaceType)
                            pieceDisplay.copy(
                                piece = updatedPiece,
                                column = column,
                                row = row
                            )
                        } else if (move.isCastling && pieceDisplay.piece.type == PieceType.ROOK) {
                            // 王车易位中车的移动
                            if (selectedPiece.piece.color == PieceColor.WHITE) {
                                // White castling
                                if (move.to.file == 6 && move.to.rank == 0) {
                                    // Kingside rook
                                    if (playerColor == PieceColor.WHITE && pieceDisplay.column == 7 && pieceDisplay.row == 0) {
                                        pieceDisplay.copy(column = 5, row = 0)
                                    } else if (playerColor == PieceColor.BLACK && pieceDisplay.column == 0 && pieceDisplay.row == 7) {
                                        pieceDisplay.copy(column = 2, row = 7)
                                    } else {
                                        pieceDisplay
                                    }
                                } else if (move.to.file == 2 && move.to.rank == 0) {
                                    // Queenside rook
                                    if (playerColor == PieceColor.WHITE && pieceDisplay.column == 0 && pieceDisplay.row == 0) {
                                        pieceDisplay.copy(column = 3, row = 0)
                                    } else if (playerColor == PieceColor.BLACK && pieceDisplay.column == 7 && pieceDisplay.row == 7) {
                                        pieceDisplay.copy(column = 4, row = 7)
                                    } else {
                                        pieceDisplay
                                    }
                                } else {
                                    pieceDisplay
                                }
                            } else {
                                // Black castling
                                if (move.to.file == 6 && move.to.rank == 7) {
                                    // Kingside rook
                                    if (playerColor == PieceColor.WHITE && pieceDisplay.column == 7 && pieceDisplay.row == 7) {
                                        pieceDisplay.copy(column = 5, row = 7)
                                    } else if (playerColor == PieceColor.BLACK && pieceDisplay.column == 0 && pieceDisplay.row == 0) {
                                        pieceDisplay.copy(column = 2, row = 0)
                                    } else {
                                        pieceDisplay
                                    }
                                } else if (move.to.file == 2 && move.to.rank == 7) {
                                    // Queenside rook
                                    if (playerColor == PieceColor.WHITE && pieceDisplay.column == 0 && pieceDisplay.row == 7) {
                                        pieceDisplay.copy(column = 3, row = 7)
                                    } else if (playerColor == PieceColor.BLACK && pieceDisplay.column == 7 && pieceDisplay.row == 0) {
                                        pieceDisplay.copy(column = 4, row = 0)
                                    } else {
                                        pieceDisplay
                                    }
                                } else {
                                    pieceDisplay
                                }
                            }
                        }
                        else {
                            pieceDisplay
                        }
                    }

                }

                _state.value = currentState.copy(
                    pieces = updatedPieces,
                    selectedCell = null,
                    moveHistory = currentState.moveHistory + newMove
                )
            }
            
            // Case 5: No cell selected and clicked on empty cell -> do nothing
            else -> {
                // No action needed
            }
        }
    }

    private fun selectCell(column: Int, row: Int) {
        _state.value = _state.value.copy(selectedCell = Pair(column, row))
    }

    private fun cancelSelect() {
        _state.value = _state.value.copy(selectedCell = null)
    }
}