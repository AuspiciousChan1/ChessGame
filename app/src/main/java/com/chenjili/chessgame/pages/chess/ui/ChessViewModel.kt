package com.chenjili.chessgame.pages.chess.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
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

enum class ImportFormat {
    FEN,
    PGN,
}

enum class ImportError {
    EMPTY_INPUT,
    INVALID_FEN,
    INVALID_PGN,
}

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
    data class ImportRequested(val format: ImportFormat, val content: String) : ChessIntent
    object ClearImportFeedback : ChessIntent
    object PromotionCancelled : ChessIntent
    object GameOverDialogDismissed : ChessIntent
    object UndoMove : ChessIntent
    // 点击投降按钮
    object SurrenderClicked : ChessIntent
    data class SurrenderConfirmed(val playerColor: PieceColor) : ChessIntent
    // 取消投降
    object SurrenderCancelled : ChessIntent
}

// MVI: State - 表示整个UI状态
data class ChessState(
    val pieces: List<ChessPieceDisplay> = emptyList(),
    val playerColor: PieceColor = PieceColor.WHITE,
    val selectedCell: Pair<Int, Int>? = null, // (column, row) of the selected cell
    val moveHistory: List<ChessMove> = emptyList(), // History of all moves
    val gameState: GameState = GameState.IN_PROGRESS, // 游戏状态
    val pendingPromotion: PendingPromotion? = null, // Pending promotion awaiting user choice
    val showSurrenderDialog: Boolean = false, // 是否展示投降确认框
    val importError: ImportError? = null,
    val importSuccessVersion: Long = 0,
    val showGameOverDialog: Boolean = false,
)

class ChessViewModel(application: Application) : AndroidViewModel(application) {

    private lateinit var chessGame: IChessGame
    private val _state = MutableStateFlow(ChessState())
    val state: StateFlow<ChessState> = _state.asStateFlow()

    init {
        chessGame = ChessServiceFactory.chessService.createGame()
        _state.value = rebuildStateFromGame(
            baseState = ChessState(playerColor = PieceColor.WHITE),
            playerColor = PieceColor.WHITE
        )
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

    private fun buildMoveHistory(moves: List<Move>): List<ChessMove> {
        return moves.map { move ->
            ChessMove(
                move = move,
                playerColor = move.piece.color,
                notation = getMoveNotation(move, isInCheck = false)
            )
        }
    }

    private fun rebuildStateFromGame(
        baseState: ChessState = _state.value,
        playerColor: PieceColor = baseState.playerColor,
        importError: ImportError? = baseState.importError,
        importSuccessVersion: Long = baseState.importSuccessVersion,
        showGameOverDialog: Boolean? = null,
    ): ChessState {
        val gameState = chessGame.getGameState()
        val resolvedShowGameOverDialog = showGameOverDialog ?: if (baseState.showGameOverDialog) {
            true
        } else {
            !baseState.gameState.isGameOver() && gameState.isGameOver()
        }
        val baseDisplay = if (baseState.pieces.isNotEmpty()) baseState.pieces else initPieces()
        return baseState.copy(
            pieces = syncDisplayPieces(
                boardPieces = chessGame.getAllPieces(),
                currentDisplay = baseDisplay,
                playerColor = playerColor
            ),
            playerColor = playerColor,
            selectedCell = null,
            moveHistory = buildMoveHistory(chessGame.getMoveHistory()),
            gameState = gameState,
            pendingPromotion = null,
            showSurrenderDialog = false,
            importError = importError,
            importSuccessVersion = importSuccessVersion,
            showGameOverDialog = resolvedShowGameOverDialog,
        )
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
        val bridge = if (move.capturedPiece != null || move.isEnPassant) "x" else ""
        val suffixBase = when (val promotionPiece = move.promotionPiece) {
            null -> if (move.isEnPassant) " e.p." else ""
            else -> {
                val promotionStr = getPieceNotation(promotionPiece)
                "=$promotionStr"
            }
        }
        val suf = suffixBase + if (isInCheck) "+" else ""
        return "$peaceTypeNotation$fromFileStr$fromRankStr$bridge$toFileStr$toRankStr$suf"
    }

    // MVI: 处理Intent的唯一入口
    fun processIntent(intent: ChessIntent) {
        when (intent) {
            is ChessIntent.PlayerColorChanged -> handlePlayerColorChanged(intent.newColor)
            is ChessIntent.RestartGame -> handleRestartGame(intent.playerColor)
            is ChessIntent.BoardCellClicked -> handleBoardCellClicked(intent.column, intent.row)
            is ChessIntent.PromotionPieceSelected -> handlePromotionPieceSelected(intent.pieceType)
            is ChessIntent.ImportRequested -> handleImportRequested(intent.format, intent.content)
            is ChessIntent.ClearImportFeedback -> handleClearImportFeedback()
            is ChessIntent.PromotionCancelled -> handlePromotionCancelled()
            is ChessIntent.GameOverDialogDismissed -> handleGameOverDialogDismissed()
            is ChessIntent.UndoMove -> handleUndoMove()
            is ChessIntent.SurrenderClicked -> handleSurrenderClicked()
            is ChessIntent.SurrenderConfirmed -> handleSurrenderConfirmed(intent.playerColor)
            is ChessIntent.SurrenderCancelled -> handleSurrenderCancelled()
        }
    }

    private fun handlePlayerColorChanged(newColor: PieceColor) {
        val currentState = _state.value
        _state.value = rebuildStateFromGame(
            baseState = currentState,
            playerColor = newColor,
        )
    }

    private fun handleRestartGame(playerColor: PieceColor) {
        chessGame.reset()
        _state.value = rebuildStateFromGame(
            baseState = _state.value,
            playerColor = playerColor,
            importError = null,
            showGameOverDialog = false,
        )
    }

    private fun handleImportRequested(format: ImportFormat, content: String) {
        val trimmedContent = content.trim()
        if (trimmedContent.isEmpty()) {
            _state.value = _state.value.copy(importError = ImportError.EMPTY_INPUT)
            return
        }

        val importedGame = ChessServiceFactory.chessService.createGame()
        val success = when (format) {
            ImportFormat.FEN -> importedGame.importFEN(trimmedContent)
            ImportFormat.PGN -> importedGame.importPGN(trimmedContent)
        }

        if (!success) {
            ChessServiceFactory.chessService.deleteGame(importedGame.id)
            _state.value = _state.value.copy(
                importError = when (format) {
                    ImportFormat.FEN -> ImportError.INVALID_FEN
                    ImportFormat.PGN -> ImportError.INVALID_PGN
                }
            )
            return
        }

        val previousGameId = chessGame.id
        chessGame = importedGame
        ChessServiceFactory.chessService.deleteGame(previousGameId)

        val currentState = _state.value
        _state.value = rebuildStateFromGame(
            baseState = currentState,
            playerColor = currentState.playerColor,
            importError = null,
            importSuccessVersion = currentState.importSuccessVersion + 1,
            showGameOverDialog = false,
        ).let { rebuiltState ->
            if (rebuiltState.gameState.isGameOver()) {
                rebuiltState.copy(showGameOverDialog = true)
            } else {
                rebuiltState
            }
        }
    }

    private fun handleClearImportFeedback() {
        _state.value = _state.value.copy(importError = null)
    }

    /**
     * 处理棋盘格子点击事件
     * @param column 被点击的列 (0-7)，不受棋盘被翻转的影响
     * @param row 被点击的行 (0-7)，不受棋盘被翻转的影响
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
            selectedPiece == null && pieceAtClickedCell == null  -> {
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

                _state.value = rebuildStateFromGame(
                    baseState = currentState,
                    playerColor = currentState.playerColor,
                    importError = null,
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

        _state.value = rebuildStateFromGame(
            baseState = currentState,
            playerColor = currentState.playerColor,
            importError = null,
        )
    }

    private fun handlePromotionCancelled() {
        _state.value = _state.value.copy(pendingPromotion = null)
    }

    private fun handleGameOverDialogDismissed() {
        _state.value = _state.value.copy(showGameOverDialog = false)
    }

    private fun handleUndoMove() {
        if (chessGame.undoLastMove()) {
            _state.value = rebuildStateFromGame(
                baseState = _state.value,
                playerColor = _state.value.playerColor,
                importError = null,
                showGameOverDialog = false,
            )
        }
    }

    private fun handleSurrenderClicked() {
        _state.value = _state.value.copy(showSurrenderDialog = true)
    }

    private fun handleSurrenderConfirmed(playerColor: PieceColor) {
        chessGame.resign(playerColor)
        _state.value = rebuildStateFromGame(
            baseState = _state.value,
            playerColor = _state.value.playerColor,
            importError = null,
        )
    }

    private fun handleSurrenderCancelled() {
        _state.value = _state.value.copy(showSurrenderDialog = false)
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

        // 目标棋盘：Position -> display(cell) 与 piece
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

        // Step 1: 先匹配“未移动”：同格 + piece 完全相同 -> 直接复用该格子的 ID
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

        // Step 3a: 处理升变前进：目标是非兵，但当前剩余池里可能还是同色兵，应复用兵的 ID
        for ((i, t) in targets.withIndex()) {
            if (usedTargetIndex[i]) continue
            if (t.piece.type == PieceType.PAWN) continue

            val promotionForwardMatch = takeFirst {
                it.piece.color == t.piece.color && it.piece.type == PieceType.PAWN
            }
            if (promotionForwardMatch != null) {
                result.add(promotionForwardMatch.copy(piece = t.piece, column = t.cell.col, row = t.cell.row))
                usedTargetIndex[i] = true
            }
        }

        // Step 3b: 处理升变回退：目标是兵，但当前 UI 还保留着升变后的后/车/象/马
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