package com.chenjili.chess.api

/**
 * Interface for a single chess game instance
 */
interface IChessGame {
    /**
     * Get unique identifier for this game
     */
    val id: String
    
    /**
     * Import game state from FEN notation
     * @param fen FEN string representing the board position
     * @return true if import successful, false otherwise
     */
    fun importFEN(fen: String): Boolean
    
    /**
     * Export current game state to FEN notation
     * @return FEN string representing current position
     */
    fun exportFEN(): String
    
    /**
     * Import game from PGN notation
     * @param pgn PGN string containing game moves
     * @return true if import successful, false otherwise
     */
    fun importPGN(pgn: String): Boolean
    
    /**
     * Export game to PGN notation
     * @return PGN string containing game moves
     */
    fun exportPGN(): String
    
    /**
     * Set up a custom position by placing pieces
     * @param pieces Map of positions to pieces
     * @param activeColor Which color moves next
     * @param castlingRights String indicating castling availability (e.g., "KQkq")
     * @param enPassantTarget Position where en passant capture is possible, or null
     * @param halfMoveClock Number of half-moves since last capture or pawn move
     * @param fullMoveNumber Current move number
     */
    fun setupPosition(
        pieces: Map<Position, Piece>,
        activeColor: PieceColor = PieceColor.WHITE,
        castlingRights: String = "KQkq",
        enPassantTarget: Position? = null,
        halfMoveClock: Int = 0,
        fullMoveNumber: Int = 1
    )
    
    /**
     * Get piece at a specific position
     * @param position The position to query
     * @return Piece at the position, or null if empty
     */
    fun getPieceAt(position: Position): Piece?
    
    /**
     * Get all pieces on the board
     * @return Map of positions to pieces
     */
    fun getAllPieces(): Map<Position, Piece>
    
    /**
     * Make a move
     * @param from Starting position
     * @param to Ending position
     * @param promotionPiece Type of piece to promote to (for pawn promotion)
     * @return Move object if successful, null if illegal move
     */
    fun makeMove(from: Position, to: Position, promotionPiece: PieceType? = null): Move?
    
    /**
     * Get all legal moves for the current position
     * @param position If provided, only return moves for piece at this position
     * @return List of all legal moves
     */
    fun getLegalMoves(position: Position? = null): List<Move>

    /**
     * 判断指定颜色的王当前是否处于被将军状态
     * @param color 要判断哪一方是否被将军
     * @return true 表示该方正在被将军
     */
    fun isInCheck(color: PieceColor): Boolean
    
    /**
     * Get the current game state
     * @return Current state of the game
     */
    fun getGameState(): GameState
    
    /**
     * Get the color of the side to move
     * @return Color that should move next
     */
    fun getActiveColor(): PieceColor
    
    /**
     * Get move history
     * @return List of all moves made in the game
     */
    fun getMoveHistory(): List<Move>
    
    /**
     * Undo the last move and return to previous state
     * @return true if undo was successful, false if no move to undo
     */
    fun undoLastMove(): Boolean
    
    /**
     * Undo moves to reach a specific move number
     * @param moveNumber The move number to undo to (1-based). All moves after this will be undone.
     * @return true if undo was successful, false if moveNumber is invalid
     */
    fun undoToMove(moveNumber: Int): Boolean
    
    /**
     * Get the number of moves that can be undone
     * @return Number of half-moves in history
     */
    fun getUndoCount(): Int
    
    /**
     * Reset the game to initial position
     */
    fun reset()
}
