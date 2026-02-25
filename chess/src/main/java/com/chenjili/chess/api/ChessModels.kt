package com.chenjili.chess.api

/**
 * Represents a chess piece type
 */
enum class PieceType {
    KING, QUEEN, ROOK, BISHOP, KNIGHT, PAWN
}

/**
 * Represents a piece color
 */
enum class PieceColor {
    WHITE, BLACK
}

/**
 * Represents a chess piece
 */
data class Piece(
    val type: PieceType,
    val color: PieceColor
)

/**
 * Represents a position on the chess board
 * File: a-h (0-7)
 * Rank: 1-8 (0-7)
 */
data class Position(
    val file: Int, // 0-7 (a-h)
    val rank: Int  // 0-7 (1-8)
) {
    companion object {
        /**
         * Parse position from algebraic notation (e.g., "e4", "a1")
         */
        fun fromAlgebraic(notation: String): Position? {
            if (notation.length != 2) return null
            val file = notation[0].lowercaseChar() - 'a'
            val rank = notation[1] - '1'
            return if (file in 0..7 && rank in 0..7) Position(file, rank) else null
        }
    }
    
    /**
     * Convert position to algebraic notation
     */
    fun toAlgebraic(): String {
        return "${('a' + file)}${rank + 1}"
    }
    
    fun isValid(): Boolean = file in 0..7 && rank in 0..7
}

/**
 * Represents a chess move
 */
data class Move(
    val from: Position,
    val to: Position,
    val piece: Piece,
    val capturedPiece: Piece? = null,
    val isEnPassant: Boolean = false,
    val isCastling: Boolean = false,
    val promotionPiece: PieceType? = null
) {
    /**
     * Convert move to algebraic notation
     */
    fun toAlgebraic(): String {
        val pieceNotation = when (piece.type) {
            PieceType.KING -> "K"
            PieceType.QUEEN -> "Q"
            PieceType.ROOK -> "R"
            PieceType.BISHOP -> "B"
            PieceType.KNIGHT -> "N"
            PieceType.PAWN -> ""
        }
        
        val capture = if (capturedPiece != null || isEnPassant) "x" else ""
        val promotion = if (promotionPiece != null) {
            "=" + when (promotionPiece) {
                PieceType.QUEEN -> "Q"
                PieceType.ROOK -> "R"
                PieceType.BISHOP -> "B"
                PieceType.KNIGHT -> "N"
                else -> ""
            }
        } else ""
        
        return when {
            isCastling && to.file > from.file -> "O-O" // Kingside
            isCastling -> "O-O-O" // Queenside
            else -> "$pieceNotation${from.toAlgebraic()}$capture${to.toAlgebraic()}$promotion"
        }
    }
}

/**
 * Represents the current state of a chess game
 */
enum class GameState {
    IN_PROGRESS,
    CHECKMATE_WHITE_WINS,
    CHECKMATE_BLACK_WINS,
    STALEMATE,
    DRAW_BY_INSUFFICIENT_MATERIAL,
    DRAW_BY_FIFTY_MOVE_RULE,
    DRAW_BY_THREEFOLD_REPETITION,
    ;
    fun isGameOver(): Boolean {
        return this != IN_PROGRESS
    }
}
