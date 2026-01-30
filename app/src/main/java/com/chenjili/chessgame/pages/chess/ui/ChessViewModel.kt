package com.chenjili.chessgame.pages.chess.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chenjili.chess.api.ChessServiceFactory
import com.chenjili.chess.api.GameState
import com.chenjili.chess.api.IChessGame
import com.chenjili.chess.api.Move
import com.chenjili.chess.api.Piece
import com.chenjili.chess.api.PieceColor
import com.chenjili.chess.api.PieceType
import com.chenjili.chess.api.Position
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.inc

data class ChessPieceDisplay (
    val piece: Piece,
    val column: Int, // 0..7 白方对应a-h；黑方对应h-a
    val row: Int,  // 0..7 白方对应1-8；黑方对应8-1
    val id: Int // Unique identifier for animation tracking
)

data class ChessMove(
    val move: Move,
    val playerColor: PieceColor,
    val notation: String // e.g., "Nb1-c3"
)

// Pending pawn promotion info
data class PendingPromotion(
    val from: Position,
    val to: Position,
    val pieceColor: PieceColor
)

// MVI: Intent - 表示用户的所有可能操作
sealed interface ChessIntent {
    data class PlayerColorChanged(val newColor: PieceColor) : ChessIntent
    data class RestartGame(val playerColor: PieceColor) : ChessIntent
    data class BoardCellClicked(val column: Int, val row: Int, val playerColor: PieceColor) : ChessIntent
    data class PromotionPieceSelected(val pieceType: PieceType) : ChessIntent
    object PromotionCancelled : ChessIntent
    object GameOverDialogDismissed : ChessIntent
    object UndoMove : ChessIntent
}

// MVI: State - 表示整个UI状态
data class ChessState(
    val pieces: List<ChessPieceDisplay> = emptyList(),
    val playerColor: PieceColor = PieceColor.WHITE,
    val selectedCell: Pair<Int, Int>? = null, // (column, row) of the selected cell
    val moveHistory: List<ChessMove> = emptyList(), // History of all moves
    val gameState: GameState = GameState.IN_PROGRESS, // 游戏状态
    val pendingPromotion: PendingPromotion? = null // Pending promotion awaiting user choice
)

class ChessViewModel(application: Application) : AndroidViewModel(application) {

    private lateinit var chessGame: IChessGame
    private val _state = MutableStateFlow(ChessState())
    val state: StateFlow<ChessState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            chessGame = ChessServiceFactory.chessService.createGame()
            _state.value = ChessState(
                pieces = initPieces(),
                playerColor = PieceColor.WHITE
            )
        }
    }

    private fun initPieces(): List<ChessPieceDisplay> {
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
        return initialPieces
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
     * @param move 棋子移动信息
     * @param isInCheck 移动后对方是否被将军
     * @return 棋谱字符串，例如 "Nb1-c3"
     */
    private fun getMoveNotation(move: Move, isInCheck: Boolean): String {
        if (move.isCastling) {
            return if (move.to.file > move.from.file) "O-O" else "O-O-O"
        }
        val fromFileStr = ('a' + move.from.file).toString()
        val fromRankStr = (move.from.rank + 1).toString()
        val toFileStr = ('a' + move.to.file).toString()
        val toRankStr = (move.to.rank + 1).toString()
        val peaceTypeNotation = getPieceNotation(move.piece.type)
        val bridge = if (move.capturedPiece != null || move.isEnPassant) "x" else "-"
        val suf = if (move.promotionPiece != null) {
            val promotionStr = getPieceNotation(move.promotionPiece!!)
            "=$promotionStr"
        } else if (move.isEnPassant) {
            " e.p."
        }
        else {
            ""
        } + if (isInCheck) "+" else ""
        return "$peaceTypeNotation$fromFileStr$fromRankStr$bridge$toFileStr$toRankStr$suf"
    }

    // MVI: 处理Intent的唯一入口
    fun processIntent(intent: ChessIntent) {
        when (intent) {
            is ChessIntent.PlayerColorChanged -> handlePlayerColorChanged(intent.newColor)
            is ChessIntent.RestartGame -> handleRestartGame(intent.playerColor)
            is ChessIntent.BoardCellClicked -> handleBoardCellClicked(intent.column, intent.row)
            is ChessIntent.PromotionPieceSelected -> handlePromotionPieceSelected(intent.pieceType)
            is ChessIntent.PromotionCancelled -> handlePromotionCancelled()
            is ChessIntent.GameOverDialogDismissed -> handleGameOverDialogDismissed()
            is ChessIntent.UndoMove -> handleUndoMove()
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

    private fun handleRestartGame(playerColor: PieceColor) {
        chessGame.reset()
        _state.value = ChessState(
            pieces = initPieces(),
            playerColor = playerColor,
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
                
                // Check if this is a pawn promotion move
                val isPawnPromotion = selectedPiece.piece.type == PieceType.PAWN &&
                    ((selectedPiece.piece.color == PieceColor.WHITE && clickedPosition.rank == 7) ||
                     (selectedPiece.piece.color == PieceColor.BLACK && clickedPosition.rank == 0))
                
                // First check if move is legal
                val legalMoves = chessGame.getLegalMoves(selectedPosition)
                val isLegalMove = legalMoves.any { it.to == clickedPosition }
                
                if (!isLegalMove) {
                    // Illegal move - ignore
                    _state.value = currentState.copy(selectedCell = null)
                    return
                }
                
                // If this is a pawn promotion, show the dialog instead of making the move
                if (isPawnPromotion) {
                    _state.value = currentState.copy(
                        selectedCell = null,
                        pendingPromotion = PendingPromotion(
                            from = selectedPosition,
                            to = clickedPosition,
                            pieceColor = selectedPiece.piece.color
                        )
                    )
                    return
                }
                
                // Make the move (not a promotion)
                val move: Move? = chessGame.makeMove(selectedPosition, clickedPosition)
                if (move == null) {
                    // This shouldn't happen since we checked legality above
                    _state.value = currentState.copy(selectedCell = null)
                    return
                }
                val isInCheck = chessGame.isInCheck(
                    if (move.piece.color == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE
                )
                val moveNotation = getMoveNotation(move, isInCheck)

                val newMove = ChessMove(
                    move = move,
                    playerColor = playerColor,
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

                val gameState: GameState = chessGame.getGameState()
                _state.value = currentState.copy(
                    pieces = updatedPieces,
                    selectedCell = null,
                    moveHistory = currentState.moveHistory + newMove,
                    gameState = gameState,
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
    
    private fun handlePromotionPieceSelected(pieceType: PieceType) {
        val currentState = _state.value
        val pendingPromotion = currentState.pendingPromotion ?: return
        
        // Make the move with the selected promotion piece
        val move: Move? = chessGame.makeMove(
            pendingPromotion.from,
            pendingPromotion.to,
            pieceType
        )
        
        if (move == null) {
            // This shouldn't happen but handle gracefully
            _state.value = currentState.copy(pendingPromotion = null)
            return
        }
        
        val isInCheck = chessGame.isInCheck(
            if (move.piece.color == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE
        )
        val moveNotation = getMoveNotation(move, isInCheck)
        
        val newMove = ChessMove(
            move = move,
            playerColor = currentState.playerColor,
            notation = moveNotation
        )
        
        // Update the board state
        val playerColor = currentState.playerColor
        val fromCol = if (playerColor == PieceColor.WHITE) pendingPromotion.from.file else 7 - pendingPromotion.from.file
        val fromRow = if (playerColor == PieceColor.WHITE) pendingPromotion.from.rank else 7 - pendingPromotion.from.rank
        val toCol = if (playerColor == PieceColor.WHITE) pendingPromotion.to.file else 7 - pendingPromotion.to.file
        val toRow = if (playerColor == PieceColor.WHITE) pendingPromotion.to.rank else 7 - pendingPromotion.to.rank
        
        val updatedPieces = currentState.pieces.let { currentPieces ->
            // Remove captured piece if exists
            val withoutCaptured = if (move.capturedPiece != null) {
                currentPieces.filterNot { p ->
                    p.column == toCol && p.row == toRow
                }
            } else {
                currentPieces
            }
            
            // Move and promote the pawn
            withoutCaptured.map { pieceDisplay ->
                if (pieceDisplay.column == fromCol && pieceDisplay.row == fromRow) {
                    // Promote the pawn to selected piece
                    val updatedPiece = pieceDisplay.piece.copy(type = pieceType)
                    pieceDisplay.copy(
                        piece = updatedPiece,
                        column = toCol,
                        row = toRow
                    )
                } else {
                    pieceDisplay
                }
            }
        }
        
        _state.value = currentState.copy(
            pieces = updatedPieces,
            selectedCell = null,
            pendingPromotion = null,
            moveHistory = currentState.moveHistory + newMove
        )
    }
    
    private fun handlePromotionCancelled() {
        _state.value = _state.value.copy(pendingPromotion = null)
    }

    private fun handleGameOverDialogDismissed() {

    }

    private fun handleUndoMove() {
        if (chessGame.undoLastMove()) {
            val currentState = _state.value

            // 使用优化后的同步函数复用 ID
            val updatedPieces = syncDisplayPieces(
                chessGame.getAllPieces(),
                currentState.pieces,
                currentState.playerColor
            )

            // 重建移动历史
            val updatedMoveHistory = mutableListOf<ChessMove>()
            val gameMoveHistory = chessGame.getMoveHistory()
            for (move in gameMoveHistory) {
                val isInCheck = false
                val moveNotation = getMoveNotation(move, isInCheck)
                updatedMoveHistory.add(
                    ChessMove(
                        move = move,
                        playerColor = currentState.playerColor,
                        notation = moveNotation
                    )
                )
            }

            val gameState = chessGame.getGameState()

            _state.value = currentState.copy(
                pieces = updatedPieces,
                selectedCell = null,
                moveHistory = updatedMoveHistory,
                gameState = gameState,
                pendingPromotion = null
            )
        }
    }

    /**
     * 同步逻辑棋盘与 UI 显示项目，尽量复用 ID 以维持动画连贯性
     */
    private fun syncDisplayPieces(
        boardPieces: Map<Position, Piece>,
        currentDisplay: List<ChessPieceDisplay>,
        playerColor: PieceColor
    ): List<ChessPieceDisplay> {

        fun toDisplayCol(pos: Position): Int = if (playerColor == PieceColor.WHITE) pos.file else 7 - pos.file
        fun toDisplayRow(pos: Position): Int = if (playerColor == PieceColor.WHITE) pos.rank else 7 - pos.rank

        // 目标棋盘：Position \-\> display(cell) 与 piece
        data class Cell(val col: Int, val row: Int)
        data class Target(val cell: Cell, val piece: Piece)

        val targets: List<Target> = boardPieces.entries.map { (pos, piece) ->
            Target(Cell(toDisplayCol(pos), toDisplayRow(pos)), piece)
        }

        // 当前 UI：按格子索引，方便找“未移动”
        val currentByCell: Map<Cell, ChessPieceDisplay> =
            currentDisplay.associateBy { Cell(it.column, it.row) }

        // 可复用池：用于后续“移动/升变”匹配，按 id 去重移除
        val remainingCurrent = currentDisplay.toMutableList()
        fun takeById(id: Int): ChessPieceDisplay? {
            val idx = remainingCurrent.indexOfFirst { it.id == id }
            return if (idx >= 0) remainingCurrent.removeAt(idx) else null
        }
        fun takeFirst(predicate: (ChessPieceDisplay) -> Boolean): ChessPieceDisplay? {
            val idx = remainingCurrent.indexOfFirst(predicate)
            return if (idx >= 0) remainingCurrent.removeAt(idx) else null
        }

        var maxId = currentDisplay.maxOfOrNull { it.id } ?: -1

        // 结果按目标棋盘构建，保证每个 target 都有一个 display
        val result = ArrayList<ChessPieceDisplay>(targets.size)

        // 记录哪些 target 已经被生成
        val usedTargetIndex = BooleanArray(targets.size)

        // Step 1: 先匹配“未移动”：同格 \+ piece 完全相同 \-\> 直接复用该格子的 ID
        for ((i, t) in targets.withIndex()) {
            val cur = currentByCell[t.cell]
            if (cur != null && cur.piece == t.piece) {
                // 从 remainingCurrent 中移除这枚棋子，避免后续再次被当作“移动来源”
                takeById(cur.id)
                result.add(cur.copy(piece = t.piece, column = t.cell.col, row = t.cell.row))
                usedTargetIndex[i] = true
            }
        }

        // Step 2: 再匹配“移动”：剩余 target 中，找剩余 current 里同 color/type 的棋子复用 ID
        for ((i, t) in targets.withIndex()) {
            if (usedTargetIndex[i]) continue

            val movedMatch = takeFirst { it.piece.color == t.piece.color && it.piece.type == t.piece.type }
            if (movedMatch != null) {
                result.add(movedMatch.copy(piece = t.piece, column = t.cell.col, row = t.cell.row))
                usedTargetIndex[i] = true
            }
        }

        // Step 3: 处理升变：若目标是兵，但 Step2 没找到同兵（常见于撤销升变：当前 UI 还保留着升变后的后/车/象/马）
        // \- 规则：同 color 的“非兵”可以被视作这枚兵的前身，复用其 ID（从 remainingCurrent 里取）
        for ((i, t) in targets.withIndex()) {
            if (usedTargetIndex[i]) continue
            if (t.piece.type != PieceType.PAWN) continue

            val promoRevertMatch = takeFirst {
                it.piece.color == t.piece.color && it.piece.type != PieceType.PAWN
            }
            if (promoRevertMatch != null) {
                result.add(promoRevertMatch.copy(piece = t.piece, column = t.cell.col, row = t.cell.row))
                usedTargetIndex[i] = true
            }
        }

        // Step 4: 兜底：新出现的棋子（例如撤销吃子后回来的），分配新 ID
        for ((i, t) in targets.withIndex()) {
            if (usedTargetIndex[i]) continue
            result.add(ChessPieceDisplay(piece = t.piece, column = t.cell.col, row = t.cell.row, id = ++maxId))
            usedTargetIndex[i] = true
        }

        return result
    }
}