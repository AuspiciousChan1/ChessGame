package com.chenjili.chessgame.pages.edit.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chenjili.chess.api.ChessServiceFactory
import com.chenjili.chess.api.Piece
import com.chenjili.chess.api.PieceColor
import com.chenjili.chess.api.PieceType
import com.chenjili.chess.api.Position
import com.chenjili.chessgame.pages.chess.ui.ChessPieceDisplay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FenExportOptions(
    val activeColor: PieceColor,
    val castlingRights: String,
    val enPassantTarget: Position?,
    val halfMoveClock: Int,
    val fullMoveNumber: Int,
)

enum class FenExportValidationError {
    INVALID_EN_PASSANT,
    INVALID_HALF_MOVE_CLOCK,
    INVALID_FULL_MOVE_NUMBER,
}

enum class FenCastlingUnavailableReason {
    KING_NOT_ON_START,
    ROOK_NOT_ON_START,
    KING_AND_ROOK_NOT_ON_START,
}

data class FenCastlingOptionAvailability(
    val enabled: Boolean = true,
    val reason: FenCastlingUnavailableReason? = null,
)

data class FenCastlingAvailability(
    val whiteKingside: FenCastlingOptionAvailability = FenCastlingOptionAvailability(),
    val whiteQueenside: FenCastlingOptionAvailability = FenCastlingOptionAvailability(),
    val blackKingside: FenCastlingOptionAvailability = FenCastlingOptionAvailability(),
    val blackQueenside: FenCastlingOptionAvailability = FenCastlingOptionAvailability(),
) {
    fun optionFor(slot: FenCastlingSlot): FenCastlingOptionAvailability = when (slot) {
        FenCastlingSlot.WHITE_KINGSIDE -> whiteKingside
        FenCastlingSlot.WHITE_QUEENSIDE -> whiteQueenside
        FenCastlingSlot.BLACK_KINGSIDE -> blackKingside
        FenCastlingSlot.BLACK_QUEENSIDE -> blackQueenside
    }
}

internal object EditModeFenExportRules {
	private fun normalizePieces(
		pieces: List<ChessPieceDisplay>,
		boardPerspective: PieceColor,
	): Map<Position, Piece> {
		return pieces.associate { pieceDisplay ->
			val position = if (boardPerspective == PieceColor.WHITE) {
				Position(pieceDisplay.column, pieceDisplay.row)
			} else {
				Position(7 - pieceDisplay.column, 7 - pieceDisplay.row)
			}
			position to pieceDisplay.piece
		}
	}

	fun buildEnPassantOptions(
		activeColor: PieceColor,
		pieces: List<ChessPieceDisplay>,
		boardPerspective: PieceColor,
	): List<String> {
		val normalizedPieces = normalizePieces(pieces, boardPerspective)
		val targets = sortedSetOf<String>()

		fun pieceAt(file: Int, rank: Int): Piece? =
			if (file in 0..7 && rank in 0..7) normalizedPieces[Position(file, rank)] else null

		when (activeColor) {
			PieceColor.WHITE -> {
				for ((position, piece) in normalizedPieces) {
					if (piece.type != PieceType.PAWN || piece.color != PieceColor.BLACK || position.rank != 4) continue
					val targetRank = 5
					val sourceRank = 6
					val targetSquareEmpty = pieceAt(position.file, targetRank) == null
					val sourceSquareEmpty = pieceAt(position.file, sourceRank) == null
					val capturerExists = listOf(position.file - 1, position.file + 1).any { adjacentFile ->
						pieceAt(adjacentFile, position.rank)?.let {
							it.type == PieceType.PAWN && it.color == PieceColor.WHITE
						} == true
					}
					if (targetSquareEmpty && sourceSquareEmpty && capturerExists) {
						targets += Position(position.file, targetRank).toAlgebraic()
					}
				}
			}
			PieceColor.BLACK -> {
				for ((position, piece) in normalizedPieces) {
					if (piece.type != PieceType.PAWN || piece.color != PieceColor.WHITE || position.rank != 3) continue
					val targetRank = 2
					val sourceRank = 1
					val targetSquareEmpty = pieceAt(position.file, targetRank) == null
					val sourceSquareEmpty = pieceAt(position.file, sourceRank) == null
					val capturerExists = listOf(position.file - 1, position.file + 1).any { adjacentFile ->
						pieceAt(adjacentFile, position.rank)?.let {
							it.type == PieceType.PAWN && it.color == PieceColor.BLACK
						} == true
					}
					if (targetSquareEmpty && sourceSquareEmpty && capturerExists) {
						targets += Position(position.file, targetRank).toAlgebraic()
					}
				}
			}
		}

		return listOf("-") + targets.toList()
	}

	fun calculateCastlingAvailability(
		pieces: List<ChessPieceDisplay>,
		boardPerspective: PieceColor,
	): FenCastlingAvailability {
		val normalizedPieces = normalizePieces(pieces, boardPerspective)

		fun hasPiece(position: String, type: PieceType, color: PieceColor): Boolean {
			val parsedPosition = Position.fromAlgebraic(position) ?: return false
			val piece = normalizedPieces[parsedPosition] ?: return false
			return piece.type == type && piece.color == color
		}

		val whiteKingAtStart = hasPiece("e1", PieceType.KING, PieceColor.WHITE)
		val blackKingAtStart = hasPiece("e8", PieceType.KING, PieceColor.BLACK)
		val whiteKingsideRookAtStart = hasPiece("h1", PieceType.ROOK, PieceColor.WHITE)
		val whiteQueensideRookAtStart = hasPiece("a1", PieceType.ROOK, PieceColor.WHITE)
		val blackKingsideRookAtStart = hasPiece("h8", PieceType.ROOK, PieceColor.BLACK)
		val blackQueensideRookAtStart = hasPiece("a8", PieceType.ROOK, PieceColor.BLACK)

		fun buildOption(kingAtStart: Boolean, rookAtStart: Boolean): FenCastlingOptionAvailability {
			val reason = when {
				kingAtStart && rookAtStart -> null
				!kingAtStart && !rookAtStart -> FenCastlingUnavailableReason.KING_AND_ROOK_NOT_ON_START
				!kingAtStart -> FenCastlingUnavailableReason.KING_NOT_ON_START
				else -> FenCastlingUnavailableReason.ROOK_NOT_ON_START
			}
			return FenCastlingOptionAvailability(
				enabled = kingAtStart && rookAtStart,
				reason = reason,
			)
		}

		return FenCastlingAvailability(
			whiteKingside = buildOption(whiteKingAtStart, whiteKingsideRookAtStart),
			whiteQueenside = buildOption(whiteKingAtStart, whiteQueensideRookAtStart),
			blackKingside = buildOption(blackKingAtStart, blackKingsideRookAtStart),
			blackQueenside = buildOption(blackKingAtStart, blackQueensideRookAtStart),
		)
	}
}

internal object EditModeFenExporter {
    fun exportFen(pieces: List<ChessPieceDisplay>, playerColor: PieceColor): String {
        return exportFen(
            pieces = pieces,
            boardPerspective = playerColor,
            options = FenExportOptions(
                activeColor = playerColor,
                castlingRights = "-",
                enPassantTarget = null,
                halfMoveClock = 0,
                fullMoveNumber = 1,
            )
        )
    }

    fun exportFen(
        pieces: List<ChessPieceDisplay>,
        boardPerspective: PieceColor,
        options: FenExportOptions,
    ): String {
        val normalizedPieces = pieces.associate { pieceDisplay ->
            val position = if (boardPerspective == PieceColor.WHITE) {
                Position(pieceDisplay.column, pieceDisplay.row)
            } else {
                Position(7 - pieceDisplay.column, 7 - pieceDisplay.row)
            }
            position to pieceDisplay.piece
        }

        val game = ChessServiceFactory.chessService.createGame()
        return try {
            game.setupPosition(
                pieces = normalizedPieces,
                activeColor = options.activeColor,
                castlingRights = options.castlingRights,
                enPassantTarget = options.enPassantTarget,
                halfMoveClock = options.halfMoveClock,
                fullMoveNumber = options.fullMoveNumber,
            )
            game.exportFEN()
        } finally {
            ChessServiceFactory.chessService.deleteGame(game.id)
        }
    }
}

enum class EditType {
    NONE,
    PUT,
    MOVE,
    REMOVE,
}

enum class FenCastlingSlot {
    WHITE_KINGSIDE,
    WHITE_QUEENSIDE,
    BLACK_KINGSIDE,
    BLACK_QUEENSIDE,
}

data class FenExportDraft(
    val activeColor: PieceColor = PieceColor.WHITE,
    val whiteKingsideCastling: Boolean = false,
    val whiteQueensideCastling: Boolean = false,
    val blackKingsideCastling: Boolean = false,
    val blackQueensideCastling: Boolean = false,
    val castlingAvailability: FenCastlingAvailability = FenCastlingAvailability(),
    val enPassantTargetInput: String = "-",
    val enPassantTargetOptions: List<String> = listOf("-"),
    val halfMoveClockInput: String = "0",
    val fullMoveNumberInput: String = "1",
    val previewFen: String = "",
    val validationError: FenExportValidationError? = null,
    val canConfirm: Boolean = true,
)

data class EditModeState(
    val playerColor: PieceColor = PieceColor.WHITE,
    val pieces: List<ChessPieceDisplay> = emptyList(),
    val selectedCell: Pair<Int, Int>? = null,
    val selectedPiece: Piece? = null,
    val editType: EditType = EditType.NONE,
    val pendingFenToCopy: String? = null,
    val fenExportDraft: FenExportDraft? = null,
)

sealed interface EditModeIntent {
    data class PlayerColorChanged(val newColor: PieceColor) : EditModeIntent
    data class BoardCellClicked(val column: Int, val row: Int, val playerColor: PieceColor): EditModeIntent
    data class PieceForEditClicked(val removeMode: Boolean, val piece: Piece?,): EditModeIntent
    object ClearBoard: EditModeIntent
    object ExportFenClicked : EditModeIntent
    object ExportFenHandled : EditModeIntent
    object DismissFenExportDialog : EditModeIntent
    object ConfirmFenExport : EditModeIntent
    data class FenExportActiveColorChanged(val color: PieceColor) : EditModeIntent
    data class FenExportCastlingChanged(val slot: FenCastlingSlot, val enabled: Boolean) : EditModeIntent
    data class FenExportEnPassantChanged(val value: String) : EditModeIntent
    data class FenExportHalfMoveChanged(val value: String) : EditModeIntent
    data class FenExportFullMoveChanged(val value: String) : EditModeIntent
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
            is EditModeIntent.PlayerColorChanged -> handlePlayerColorChanged(intent.newColor)
            is EditModeIntent.BoardCellClicked -> handleBoardCellClicked(intent.column, intent.row)
            is EditModeIntent.PieceForEditClicked -> handlePieceForEditClicked(intent.removeMode, intent.piece)
            is EditModeIntent.ClearBoard -> handleClearBoardClicked()
            is EditModeIntent.ExportFenClicked -> handleExportFenClicked()
            is EditModeIntent.ExportFenHandled -> handleExportFenHandled()
            is EditModeIntent.DismissFenExportDialog -> handleDismissFenExportDialog()
            is EditModeIntent.ConfirmFenExport -> handleConfirmFenExport()
            is EditModeIntent.FenExportActiveColorChanged -> handleFenExportDraftChanged { copy(activeColor = intent.color) }
            is EditModeIntent.FenExportCastlingChanged -> handleFenExportCastlingChanged(intent.slot, intent.enabled)
            is EditModeIntent.FenExportEnPassantChanged -> handleFenExportDraftChanged {
                copy(enPassantTargetInput = intent.value)
            }
            is EditModeIntent.FenExportHalfMoveChanged -> handleFenExportDraftChanged {
                copy(halfMoveClockInput = intent.value)
            }
            is EditModeIntent.FenExportFullMoveChanged -> handleFenExportDraftChanged {
                copy(fullMoveNumberInput = intent.value)
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
            selectedCell = null,
            selectedPiece = null,
            editType = EditType.NONE,
            pendingFenToCopy = null,
            fenExportDraft = null,
        )
    }

    private fun handleBoardCellClicked(column: Int, row: Int) {
        if (column !in 0..7 || row !in 0..7) {
            return
        }

        val currentState = _state.value
        val clickedCell = Pair(column, row)
        val pieceAtClickedCell = currentState.pieces.find {
            it.column == column && it.row == row
        }

        when {
            currentState.selectedCell == clickedCell -> {
                select(false, null, null, null)
            }
            currentState.selectedPiece == null && currentState.editType != EditType.REMOVE -> {
                select(false, pieceAtClickedCell?.piece, clickedCell.first, clickedCell.second)
            }
            currentState.selectedCell == null && (currentState.selectedPiece != null || currentState.editType == EditType.REMOVE) -> {
                putPiece(currentState.selectedPiece, column, row)
            }
            currentState.selectedCell != null && currentState.selectedPiece != null -> {
                putPiece(null, currentState.selectedCell.first, currentState.selectedCell.second)
                putPiece(currentState.selectedPiece, column, row, pieceAtClickedCell?.id)
            }
            else -> Unit
        }
    }

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
            pendingFenToCopy = null,
            fenExportDraft = null,
        )
    }

    private fun handleExportFenClicked() {
        val currentState = _state.value
        val initialDraft = currentState.fenExportDraft ?: FenExportDraft(activeColor = currentState.playerColor)
        _state.value = currentState.copy(
            fenExportDraft = recomputeFenExportDraft(currentState, initialDraft)
        )
    }

    private fun handleExportFenHandled() {
        _state.value = _state.value.copy(pendingFenToCopy = null)
    }

    private fun handleDismissFenExportDialog() {
        _state.value = _state.value.copy(fenExportDraft = null)
    }

    private fun handleConfirmFenExport() {
        val currentState = _state.value
        val draft = currentState.fenExportDraft ?: return
        if (!draft.canConfirm || draft.previewFen.isBlank()) return
        _state.value = currentState.copy(
            pendingFenToCopy = draft.previewFen,
            fenExportDraft = null,
        )
    }

    private fun handleFenExportCastlingChanged(slot: FenCastlingSlot, enabled: Boolean) {
        handleFenExportDraftChanged {
            when (slot) {
                FenCastlingSlot.WHITE_KINGSIDE -> copy(whiteKingsideCastling = enabled)
                FenCastlingSlot.WHITE_QUEENSIDE -> copy(whiteQueensideCastling = enabled)
                FenCastlingSlot.BLACK_KINGSIDE -> copy(blackKingsideCastling = enabled)
                FenCastlingSlot.BLACK_QUEENSIDE -> copy(blackQueensideCastling = enabled)
            }
        }
    }

    private fun handleFenExportDraftChanged(transform: FenExportDraft.() -> FenExportDraft) {
        val currentState = _state.value
        val currentDraft = currentState.fenExportDraft ?: return
        _state.value = currentState.copy(
            fenExportDraft = recomputeFenExportDraft(currentState, currentDraft.transform())
        )
    }

    private fun recomputeFenExportDraft(
        state: EditModeState,
        draft: FenExportDraft,
    ): FenExportDraft {
        val castlingAvailability = EditModeFenExportRules.calculateCastlingAvailability(
            pieces = state.pieces,
            boardPerspective = state.playerColor,
        )
        val enPassantTargetOptions = EditModeFenExportRules.buildEnPassantOptions(
			activeColor = draft.activeColor,
			pieces = state.pieces,
			boardPerspective = state.playerColor,
		)
        val normalizedEnPassantTarget = draft.enPassantTargetInput.ifBlank { "-" }
            .trim()
            .lowercase()
            .let { if (it in enPassantTargetOptions) it else "-" }

        val sanitizedDraft = draft.copy(
            whiteKingsideCastling = draft.whiteKingsideCastling && castlingAvailability.whiteKingside.enabled,
            whiteQueensideCastling = draft.whiteQueensideCastling && castlingAvailability.whiteQueenside.enabled,
            blackKingsideCastling = draft.blackKingsideCastling && castlingAvailability.blackKingside.enabled,
            blackQueensideCastling = draft.blackQueensideCastling && castlingAvailability.blackQueenside.enabled,
            castlingAvailability = castlingAvailability,
            enPassantTargetInput = normalizedEnPassantTarget,
            enPassantTargetOptions = enPassantTargetOptions,
        )

        val castlingRights = buildString {
            if (sanitizedDraft.whiteKingsideCastling) append('K')
            if (sanitizedDraft.whiteQueensideCastling) append('Q')
            if (sanitizedDraft.blackKingsideCastling) append('k')
            if (sanitizedDraft.blackQueensideCastling) append('q')
        }.ifEmpty { "-" }

        val enPassantTarget = when (normalizedEnPassantTarget) {
            "-" -> null
            else -> Position.fromAlgebraic(normalizedEnPassantTarget)
                ?: return sanitizedDraft.copy(
                    previewFen = "",
                    validationError = FenExportValidationError.INVALID_EN_PASSANT,
                    canConfirm = false,
                )
        }

        val halfMoveClock = sanitizedDraft.halfMoveClockInput.toIntOrNull()?.takeIf { it >= 0 }
            ?: return sanitizedDraft.copy(
                previewFen = "",
                validationError = FenExportValidationError.INVALID_HALF_MOVE_CLOCK,
                canConfirm = false,
            )

        val fullMoveNumber = sanitizedDraft.fullMoveNumberInput.toIntOrNull()?.takeIf { it >= 1 }
            ?: return sanitizedDraft.copy(
                previewFen = "",
                validationError = FenExportValidationError.INVALID_FULL_MOVE_NUMBER,
                canConfirm = false,
            )

        val fen = EditModeFenExporter.exportFen(
            pieces = state.pieces,
            boardPerspective = state.playerColor,
            options = FenExportOptions(
                activeColor = sanitizedDraft.activeColor,
                castlingRights = castlingRights,
                enPassantTarget = enPassantTarget,
                halfMoveClock = halfMoveClock,
                fullMoveNumber = fullMoveNumber,
            )
        )

        return sanitizedDraft.copy(
            previewFen = fen,
            validationError = null,
            canConfirm = true,
        )
    }

    private fun select(removeMode: Boolean, piece: Piece?, cellColumn: Int?, cellRow: Int?) {
        if (removeMode) {
            _state.value = _state.value.copy(
                selectedCell = null,
                selectedPiece = null,
                editType = EditType.REMOVE,
                pendingFenToCopy = null,
                fenExportDraft = null,
            )
            return
        }
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
            selectedCell = toSelectCell,
            selectedPiece = toSelectPiece,
            editType = editType,
            pendingFenToCopy = null,
            fenExportDraft = null,
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
            selectedPiece = null,
            editType = EditType.NONE,
            pendingFenToCopy = null,
            fenExportDraft = null,
        )
    }
}