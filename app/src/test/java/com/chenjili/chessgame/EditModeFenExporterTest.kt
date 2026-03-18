package com.chenjili.chessgame

import com.chenjili.chess.api.Piece
import com.chenjili.chess.api.PieceColor
import com.chenjili.chess.api.PieceType
import com.chenjili.chess.api.Position
import com.chenjili.chessgame.pages.chess.ui.ChessPieceDisplay
import com.chenjili.chessgame.pages.edit.ui.EditModeFenExporter
import com.chenjili.chessgame.pages.edit.ui.EditModeFenExportRules
import com.chenjili.chessgame.pages.edit.ui.FenCastlingUnavailableReason
import com.chenjili.chessgame.pages.edit.ui.FenExportOptions
import org.junit.Assert.assertEquals
import org.junit.Test

class EditModeFenExporterTest {

    @Test
    fun exportFen_whiteView_initialPositionProducesCanonicalFen() {
        val pieces = createInitialPieces(playerColor = PieceColor.WHITE)

        val fen = EditModeFenExporter.exportFen(
            pieces = pieces,
            playerColor = PieceColor.WHITE,
        )

        assertEquals("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w - - 0 1", fen)
    }

    @Test
    fun exportFen_blackView_flippedDisplayStillProducesSameBoard() {
        val pieces = createInitialPieces(playerColor = PieceColor.BLACK)

        val fen = EditModeFenExporter.exportFen(
            pieces = pieces,
            playerColor = PieceColor.BLACK,
        )

        assertEquals("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR b - - 0 1", fen)
    }

    @Test
    fun exportFen_withExplicitOptionsIncludesCastlingEnPassantAndCounters() {
        val pieces = createInitialPieces(playerColor = PieceColor.WHITE)

        val fen = EditModeFenExporter.exportFen(
            pieces = pieces,
            boardPerspective = PieceColor.WHITE,
            options = FenExportOptions(
                activeColor = PieceColor.BLACK,
                castlingRights = "Kq",
                enPassantTarget = Position.fromAlgebraic("e3"),
                halfMoveClock = 7,
                fullMoveNumber = 12,
            )
        )

        assertEquals("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR b Kq e3 7 12", fen)
    }

    @Test
    fun exportFen_blackPerspectiveKeepsOptionsWhileNormalizingBoard() {
        val pieces = createInitialPieces(playerColor = PieceColor.BLACK)

        val fen = EditModeFenExporter.exportFen(
            pieces = pieces,
            boardPerspective = PieceColor.BLACK,
            options = FenExportOptions(
                activeColor = PieceColor.WHITE,
                castlingRights = "KQkq",
                enPassantTarget = Position.fromAlgebraic("d6"),
                halfMoveClock = 3,
                fullMoveNumber = 18,
            )
        )

        assertEquals("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq d6 3 18", fen)
    }

    @Test
    fun buildEnPassantOptions_whiteToMoveDetectsG6FromF5AndG5() {
        val pieces = listOf(
            ChessPieceDisplay(Piece(PieceType.PAWN, PieceColor.WHITE), 5, 4, 0),
            ChessPieceDisplay(Piece(PieceType.PAWN, PieceColor.BLACK), 6, 4, 1),
        )

        val options = EditModeFenExportRules.buildEnPassantOptions(
            activeColor = PieceColor.WHITE,
            pieces = pieces,
            boardPerspective = PieceColor.WHITE,
        )

        assertEquals(listOf("-", "g6"), options)
    }

    @Test
    fun buildEnPassantOptions_blackToMoveDetectsE3FromD4AndE4() {
        val pieces = listOf(
            ChessPieceDisplay(Piece(PieceType.PAWN, PieceColor.BLACK), 3, 3, 0),
            ChessPieceDisplay(Piece(PieceType.PAWN, PieceColor.WHITE), 4, 3, 1),
        )

        val options = EditModeFenExportRules.buildEnPassantOptions(
            activeColor = PieceColor.BLACK,
            pieces = pieces,
            boardPerspective = PieceColor.WHITE,
        )

        assertEquals(listOf("-", "e3"), options)
    }

    @Test
    fun buildEnPassantOptions_returnsOnlyDashWhenNoValidTargetExists() {
        val pieces = listOf(
            ChessPieceDisplay(Piece(PieceType.PAWN, PieceColor.WHITE), 5, 4, 0),
            ChessPieceDisplay(Piece(PieceType.PAWN, PieceColor.BLACK), 6, 4, 1),
            ChessPieceDisplay(Piece(PieceType.KNIGHT, PieceColor.BLACK), 6, 6, 2),
        )

        val options = EditModeFenExportRules.buildEnPassantOptions(
            activeColor = PieceColor.WHITE,
            pieces = pieces,
            boardPerspective = PieceColor.WHITE,
        )

        assertEquals(listOf("-"), options)
    }

    @Test
    fun calculateCastlingAvailability_disablesSideWhenKingOrRookMissingFromStartSquare() {
        val piecesWithoutWhiteKingsideRook = createInitialPieces(playerColor = PieceColor.WHITE)
            .filterNot { it.piece.color == PieceColor.WHITE && it.piece.type == PieceType.ROOK && it.column == 7 && it.row == 0 }
        val piecesWithoutBlackKing = createInitialPieces(playerColor = PieceColor.WHITE)
            .filterNot { it.piece.color == PieceColor.BLACK && it.piece.type == PieceType.KING && it.column == 4 && it.row == 7 }

        val whiteRookMissing = EditModeFenExportRules.calculateCastlingAvailability(
            pieces = piecesWithoutWhiteKingsideRook,
            boardPerspective = PieceColor.WHITE,
        )
        val blackKingMissing = EditModeFenExportRules.calculateCastlingAvailability(
            pieces = piecesWithoutBlackKing,
            boardPerspective = PieceColor.WHITE,
        )

        assertEquals(false, whiteRookMissing.whiteKingside.enabled)
        assertEquals(FenCastlingUnavailableReason.ROOK_NOT_ON_START, whiteRookMissing.whiteKingside.reason)
        assertEquals(true, whiteRookMissing.whiteQueenside.enabled)
        assertEquals(false, blackKingMissing.blackKingside.enabled)
        assertEquals(FenCastlingUnavailableReason.KING_NOT_ON_START, blackKingMissing.blackKingside.reason)
        assertEquals(false, blackKingMissing.blackQueenside.enabled)
        assertEquals(FenCastlingUnavailableReason.KING_NOT_ON_START, blackKingMissing.blackQueenside.reason)
    }

    @Test
    fun calculateCastlingAvailability_reportsBothKingAndRookMissing() {
        val piecesWithoutWhiteKingAndQueensideRook = createInitialPieces(playerColor = PieceColor.WHITE)
            .filterNot {
                (it.piece.color == PieceColor.WHITE && it.piece.type == PieceType.KING && it.column == 4 && it.row == 0) ||
                    (it.piece.color == PieceColor.WHITE && it.piece.type == PieceType.ROOK && it.column == 0 && it.row == 0)
            }

        val availability = EditModeFenExportRules.calculateCastlingAvailability(
            pieces = piecesWithoutWhiteKingAndQueensideRook,
            boardPerspective = PieceColor.WHITE,
        )

        assertEquals(false, availability.whiteQueenside.enabled)
        assertEquals(FenCastlingUnavailableReason.KING_AND_ROOK_NOT_ON_START, availability.whiteQueenside.reason)
    }

    private fun createInitialPieces(playerColor: PieceColor): List<ChessPieceDisplay> {
        val pieces = mutableListOf<ChessPieceDisplay>()
        var id = 0

        fun displayColumn(column: Int): Int = if (playerColor == PieceColor.WHITE) column else 7 - column
        fun displayRow(row: Int): Int = if (playerColor == PieceColor.WHITE) row else 7 - row
        fun addPiece(type: PieceType, color: PieceColor, column: Int, row: Int) {
            pieces += ChessPieceDisplay(
                piece = Piece(type, color),
                column = displayColumn(column),
                row = displayRow(row),
                id = id++,
            )
        }

        addPiece(PieceType.ROOK, PieceColor.WHITE, 0, 0)
        addPiece(PieceType.KNIGHT, PieceColor.WHITE, 1, 0)
        addPiece(PieceType.BISHOP, PieceColor.WHITE, 2, 0)
        addPiece(PieceType.QUEEN, PieceColor.WHITE, 3, 0)
        addPiece(PieceType.KING, PieceColor.WHITE, 4, 0)
        addPiece(PieceType.BISHOP, PieceColor.WHITE, 5, 0)
        addPiece(PieceType.KNIGHT, PieceColor.WHITE, 6, 0)
        addPiece(PieceType.ROOK, PieceColor.WHITE, 7, 0)
        for (column in 0..7) {
            addPiece(PieceType.PAWN, PieceColor.WHITE, column, 1)
        }

        for (column in 0..7) {
            addPiece(PieceType.PAWN, PieceColor.BLACK, column, 6)
        }
        addPiece(PieceType.ROOK, PieceColor.BLACK, 0, 7)
        addPiece(PieceType.KNIGHT, PieceColor.BLACK, 1, 7)
        addPiece(PieceType.BISHOP, PieceColor.BLACK, 2, 7)
        addPiece(PieceType.QUEEN, PieceColor.BLACK, 3, 7)
        addPiece(PieceType.KING, PieceColor.BLACK, 4, 7)
        addPiece(PieceType.BISHOP, PieceColor.BLACK, 5, 7)
        addPiece(PieceType.KNIGHT, PieceColor.BLACK, 6, 7)
        addPiece(PieceType.ROOK, PieceColor.BLACK, 7, 7)

        return pieces
    }
}
