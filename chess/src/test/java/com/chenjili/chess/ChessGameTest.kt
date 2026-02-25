package com.chenjili.chess

import com.chenjili.chess.api.*
import com.chenjili.chess.inner.ChessGame
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before

/**
 * Unit tests for ChessGame implementation
 */
class ChessGameTest {
    
    private lateinit var game: ChessGame
    
    @Before
    fun setup() {
        game = ChessGame()
    }
    
    @Test
    fun testInitialPosition() {
        // Verify white pieces on rank 1 (index 0)
        assertEquals(PieceType.ROOK, game.getPieceAt(Position(0, 0))?.type)
        assertEquals(PieceColor.WHITE, game.getPieceAt(Position(0, 0))?.color)
        assertEquals(PieceType.KING, game.getPieceAt(Position(4, 0))?.type)
        
        // Verify white pawns on rank 2 (index 1)
        assertEquals(PieceType.PAWN, game.getPieceAt(Position(0, 1))?.type)
        assertEquals(PieceColor.WHITE, game.getPieceAt(Position(0, 1))?.color)
        
        // Verify empty squares
        assertNull(game.getPieceAt(Position(4, 4)))
        
        // Verify black pieces on rank 8 (index 7)
        assertEquals(PieceType.ROOK, game.getPieceAt(Position(0, 7))?.type)
        assertEquals(PieceColor.BLACK, game.getPieceAt(Position(0, 7))?.color)
        assertEquals(PieceType.KING, game.getPieceAt(Position(4, 7))?.type)
    }
    
    @Test
    fun testFENExportInitialPosition() {
        val fen = game.exportFEN()
        assertEquals("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", fen)
    }
    
    @Test
    fun testFENImportExport() {
        val testFen = "rnbqkbnr/pp1ppppp/8/2p5/4P3/8/PPPP1PPP/RNBQKBNR w KQkq c6 0 2"
        assertTrue(game.importFEN(testFen))
        assertEquals(testFen, game.exportFEN())
    }
    
    @Test
    fun testFENImportInvalid() {
        assertFalse(game.importFEN("invalid fen"))
        assertFalse(game.importFEN(""))
    }
    
    @Test
    fun testSimpleMove() {
        // Move white pawn from e2 to e4
        val e2 = Position.fromAlgebraic("e2")!!
        val e4 = Position.fromAlgebraic("e4")!!
        
        val move = game.makeMove(e2, e4)
        assertNotNull(move)
        assertEquals(PieceType.PAWN, move?.piece?.type)
        assertEquals(e2, move?.from)
        assertEquals(e4, move?.to)
        
        // Verify piece moved
        assertNull(game.getPieceAt(e2))
        assertEquals(PieceType.PAWN, game.getPieceAt(e4)?.type)
        assertEquals(PieceColor.WHITE, game.getPieceAt(e4)?.color)
    }
    
    @Test
    fun testIllegalMove() {
        // Try to move pawn from e2 to e5 (can only move 1 or 2 squares from start)
        val e2 = Position.fromAlgebraic("e2")!!
        val e5 = Position.fromAlgebraic("e5")!!
        
        val move = game.makeMove(e2, e5)
        assertNull(move)
        
        // Verify piece didn't move
        assertEquals(PieceType.PAWN, game.getPieceAt(e2)?.type)
        assertNull(game.getPieceAt(e5))
    }
    
    @Test
    fun testCapture() {
        // Set up position where white pawn can capture black pawn
        game.importFEN("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1")
        
        // Move black pawn to d5
        val d7 = Position.fromAlgebraic("d7")!!
        val d5 = Position.fromAlgebraic("d5")!!
        game.makeMove(d7, d5)
        
        // White pawn captures black pawn
        val e4 = Position.fromAlgebraic("e4")!!
        val move = game.makeMove(e4, d5)
        
        assertNotNull(move)
        assertEquals(PieceType.PAWN, move?.capturedPiece?.type)
        assertEquals(PieceColor.BLACK, move?.capturedPiece?.color)
        assertEquals(PieceColor.WHITE, game.getPieceAt(d5)?.color)
    }
    
    @Test
    fun testEnPassant() {
        // Set up en passant scenario
        game.importFEN("rnbqkbnr/ppp1pppp/8/3pP3/8/8/PPPP1PPP/RNBQKBNR w KQkq d6 0 1")
        
        val e5 = Position.fromAlgebraic("e5")!!
        val d6 = Position.fromAlgebraic("d6")!!
        
        val move = game.makeMove(e5, d6)
        assertNotNull(move)
        assertTrue(move?.isEnPassant ?: false)
        assertEquals(PieceType.PAWN, move?.capturedPiece?.type)
        
        // Verify en passant capture removed the pawn
        assertNull(game.getPieceAt(Position.fromAlgebraic("d5")!!))
        assertEquals(PieceType.PAWN, game.getPieceAt(d6)?.type)
    }
    
    @Test
    fun testCastlingKingside() {
        // Set up position for kingside castling
        game.importFEN("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQK2R w KQkq - 0 1")
        
        val e1 = Position.fromAlgebraic("e1")!!
        val g1 = Position.fromAlgebraic("g1")!!
        
        val move = game.makeMove(e1, g1)
        assertNotNull(move)
        assertTrue(move?.isCastling ?: false)
        
        // Verify king and rook positions after castling
        assertEquals(PieceType.KING, game.getPieceAt(g1)?.type)
        assertEquals(PieceType.ROOK, game.getPieceAt(Position.fromAlgebraic("f1")!!)?.type)
        assertNull(game.getPieceAt(e1))
        assertNull(game.getPieceAt(Position.fromAlgebraic("h1")!!))
    }
    
    @Test
    fun testCastlingQueenside() {
        // Set up position for queenside castling
        game.importFEN("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/R3KBNR w KQkq - 0 1")
        
        val e1 = Position.fromAlgebraic("e1")!!
        val c1 = Position.fromAlgebraic("c1")!!
        
        val move = game.makeMove(e1, c1)
        assertNotNull(move)
        assertTrue(move?.isCastling ?: false)
        
        // Verify king and rook positions after castling
        assertEquals(PieceType.KING, game.getPieceAt(c1)?.type)
        assertEquals(PieceType.ROOK, game.getPieceAt(Position.fromAlgebraic("d1")!!)?.type)
        assertNull(game.getPieceAt(e1))
        assertNull(game.getPieceAt(Position.fromAlgebraic("a1")!!))
    }
    
    @Test
    fun testPawnPromotion() {
        // Set up position where white pawn can promote
        game.importFEN("8/P7/8/8/8/8/8/K6k w - - 0 1")
        
        val a7 = Position.fromAlgebraic("a7")!!
        val a8 = Position.fromAlgebraic("a8")!!
        
        val move = game.makeMove(a7, a8, PieceType.QUEEN)
        assertNotNull(move)
        assertEquals(PieceType.QUEEN, move?.promotionPiece)
        
        // Verify promoted piece
        assertEquals(PieceType.QUEEN, game.getPieceAt(a8)?.type)
        assertEquals(PieceColor.WHITE, game.getPieceAt(a8)?.color)
    }
    
    @Test
    fun testLegalMoves() {
        // Get legal moves for white pawn at e2
        val e2 = Position.fromAlgebraic("e2")!!
        val moves = game.getLegalMoves(e2)
        
        // Pawn at e2 can move to e3 or e4
        assertTrue(moves.size >= 2)
        assertTrue(moves.any { it.to == Position.fromAlgebraic("e3")!! })
        assertTrue(moves.any { it.to == Position.fromAlgebraic("e4")!! })
    }
    
    @Test
    fun testGetAllLegalMoves() {
        val allMoves = game.getLegalMoves()
        
        // In initial position, white has 20 legal moves
        // (16 pawn moves: 8 pawns × 2 moves each, 4 knight moves: 2 knights × 2 moves each)
        assertEquals(20, allMoves.size)
    }
    
    @Test
    fun testGameStateInProgress() {
        assertEquals(GameState.IN_PROGRESS, game.getGameState())
    }
    
    @Test
    fun testGameStateCheckmate() {
        // Fool's mate position
        game.importFEN("rnb1kbnr/pppp1ppp/8/4p3/6Pq/5P2/PPPPP2P/RNBQKBNR w KQkq - 1 3")
        assertEquals(GameState.CHECKMATE_BLACK_WINS, game.getGameState())
    }
    
    @Test
    fun testGameStateStalemate() {
        // Stalemate position
        game.importFEN("k7/8/1Q6/8/8/8/8/K7 b - - 0 1")
        assertEquals(GameState.STALEMATE, game.getGameState())
    }
    
    @Test
    fun testActiveColor() {
        assertEquals(PieceColor.WHITE, game.getActiveColor())
        
        // Make a move
        val e2 = Position.fromAlgebraic("e2")!!
        val e4 = Position.fromAlgebraic("e4")!!
        game.makeMove(e2, e4)
        
        assertEquals(PieceColor.BLACK, game.getActiveColor())
    }
    
    @Test
    fun testMoveHistory() {
        assertEquals(0, game.getMoveHistory().size)
        
        // Make a few moves
        game.makeMove(Position.fromAlgebraic("e2")!!, Position.fromAlgebraic("e4")!!)
        game.makeMove(Position.fromAlgebraic("e7")!!, Position.fromAlgebraic("e5")!!)
        
        assertEquals(2, game.getMoveHistory().size)
        assertEquals(PieceType.PAWN, game.getMoveHistory()[0].piece.type)
        assertEquals(PieceType.PAWN, game.getMoveHistory()[1].piece.type)
    }
    
    @Test
    fun testSetupCustomPosition() {
        val pieces = mapOf(
            Position.fromAlgebraic("e1")!! to Piece(PieceType.KING, PieceColor.WHITE),
            Position.fromAlgebraic("e8")!! to Piece(PieceType.KING, PieceColor.BLACK),
            Position.fromAlgebraic("d4")!! to Piece(PieceType.PAWN, PieceColor.WHITE)
        )
        
        game.setupPosition(pieces, PieceColor.WHITE, "-", null, 0, 1)
        
        assertEquals(PieceType.KING, game.getPieceAt(Position.fromAlgebraic("e1")!!)?.type)
        assertEquals(PieceType.PAWN, game.getPieceAt(Position.fromAlgebraic("d4")!!)?.type)
        assertNull(game.getPieceAt(Position.fromAlgebraic("a1")!!))
    }
    
    @Test
    fun testGetAllPieces() {
        val pieces = game.getAllPieces()
        
        // Initial position has 32 pieces
        assertEquals(32, pieces.size)
        
        // Verify some pieces
        assertEquals(PieceType.KING, pieces[Position.fromAlgebraic("e1")!!]?.type)
        assertEquals(PieceType.KING, pieces[Position.fromAlgebraic("e8")!!]?.type)
    }
    
    @Test
    fun testPGNExport() {
        // Make a few moves
        game.makeMove(Position.fromAlgebraic("e2")!!, Position.fromAlgebraic("e4")!!)
        game.makeMove(Position.fromAlgebraic("e7")!!, Position.fromAlgebraic("e5")!!)
        game.makeMove(Position.fromAlgebraic("g1")!!, Position.fromAlgebraic("f3")!!)
        
        val pgn = game.exportPGN()
        
        assertTrue(pgn.contains("[Event"))
        assertTrue(pgn.contains("e2"))
        assertTrue(pgn.contains("*")) // Game in progress
    }
    
    @Test
    fun testPositionToFromAlgebraic() {
        val e4 = Position.fromAlgebraic("e4")!!
        assertEquals(4, e4.file) // e = 4
        assertEquals(3, e4.rank) // 4 = 3 (0-indexed)
        assertEquals("e4", e4.toAlgebraic())
        
        val a1 = Position.fromAlgebraic("a1")!!
        assertEquals(0, a1.file)
        assertEquals(0, a1.rank)
        assertEquals("a1", a1.toAlgebraic())
        
        val h8 = Position.fromAlgebraic("h8")!!
        assertEquals(7, h8.file)
        assertEquals(7, h8.rank)
        assertEquals("h8", h8.toAlgebraic())
    }
    
    @Test
    fun testInvalidPosition() {
        assertNull(Position.fromAlgebraic("i9"))
        assertNull(Position.fromAlgebraic("a0"))
        assertNull(Position.fromAlgebraic("z5"))
        assertNull(Position.fromAlgebraic(""))
        assertNull(Position.fromAlgebraic("abc"))
    }
    
    @Test
    fun testKnightMoves() {
        // Knight at b1 should have 2 possible moves initially
        val b1 = Position.fromAlgebraic("b1")!!
        val moves = game.getLegalMoves(b1)
        
        assertEquals(2, moves.size)
        assertTrue(moves.any { it.to == Position.fromAlgebraic("a3")!! })
        assertTrue(moves.any { it.to == Position.fromAlgebraic("c3")!! })
    }
    
    @Test
    fun testReset() {
        // Make some moves
        game.makeMove(Position.fromAlgebraic("e2")!!, Position.fromAlgebraic("e4")!!)
        game.makeMove(Position.fromAlgebraic("e7")!!, Position.fromAlgebraic("e5")!!)
        
        // Reset
        game.reset()
        
        // Verify back to initial position
        assertEquals("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", game.exportFEN())
        assertEquals(0, game.getMoveHistory().size)
        assertEquals(PieceColor.WHITE, game.getActiveColor())
    }
    
    @Test
    fun testUndoLastMove() {
        // Make a move
        val e2 = Position.fromAlgebraic("e2")!!
        val e4 = Position.fromAlgebraic("e4")!!
        game.makeMove(e2, e4)
        
        // Verify move was made
        assertNull(game.getPieceAt(e2))
        assertEquals(PieceType.PAWN, game.getPieceAt(e4)?.type)
        assertEquals(1, game.getMoveHistory().size)
        assertEquals(PieceColor.BLACK, game.getActiveColor())
        
        // Undo the move
        assertTrue(game.undoLastMove())
        
        // Verify position restored
        assertEquals(PieceType.PAWN, game.getPieceAt(e2)?.type)
        assertNull(game.getPieceAt(e4))
        assertEquals(0, game.getMoveHistory().size)
        assertEquals(PieceColor.WHITE, game.getActiveColor())
    }
    
    @Test
    fun testUndoMultipleMoves() {
        // Make several moves
        game.makeMove(Position.fromAlgebraic("e2")!!, Position.fromAlgebraic("e4")!!)
        game.makeMove(Position.fromAlgebraic("e7")!!, Position.fromAlgebraic("e5")!!)
        game.makeMove(Position.fromAlgebraic("g1")!!, Position.fromAlgebraic("f3")!!)
        
        assertEquals(3, game.getMoveHistory().size)
        
        // Undo last move
        assertTrue(game.undoLastMove())
        assertEquals(2, game.getMoveHistory().size)
        assertNull(game.getPieceAt(Position.fromAlgebraic("f3")!!))
        assertEquals(PieceType.KNIGHT, game.getPieceAt(Position.fromAlgebraic("g1")!!)?.type)
        
        // Undo another move
        assertTrue(game.undoLastMove())
        assertEquals(1, game.getMoveHistory().size)
        assertEquals(PieceType.PAWN, game.getPieceAt(Position.fromAlgebraic("e7")!!)?.type)
        
        // Undo one more
        assertTrue(game.undoLastMove())
        assertEquals(0, game.getMoveHistory().size)
        assertEquals("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", game.exportFEN())
    }
    
    @Test
    fun testUndoWhenNoMoves() {
        // Try to undo when no moves have been made
        assertFalse(game.undoLastMove())
        assertEquals(0, game.getMoveHistory().size)
    }
    
    @Test
    fun testUndoCapture() {
        // Set up and make a capture
        game.importFEN("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1")
        val d7 = Position.fromAlgebraic("d7")!!
        val d5 = Position.fromAlgebraic("d5")!!
        game.makeMove(d7, d5)
        
        val e4 = Position.fromAlgebraic("e4")!!
        game.makeMove(e4, d5)
        
        // Verify capture
        assertEquals(PieceColor.WHITE, game.getPieceAt(d5)?.color)
        assertNull(game.getPieceAt(e4))
        
        // Undo the capture
        assertTrue(game.undoLastMove())
        
        // Verify both pieces restored
        assertEquals(PieceColor.WHITE, game.getPieceAt(e4)?.color)
        assertEquals(PieceColor.BLACK, game.getPieceAt(d5)?.color)
    }
    
    @Test
    fun testUndoCastling() {
        // Set up for kingside castling
        game.importFEN("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQK2R w KQkq - 0 1")
        
        val e1 = Position.fromAlgebraic("e1")!!
        val g1 = Position.fromAlgebraic("g1")!!
        game.makeMove(e1, g1)
        
        // Verify castling occurred
        assertEquals(PieceType.KING, game.getPieceAt(g1)?.type)
        assertEquals(PieceType.ROOK, game.getPieceAt(Position.fromAlgebraic("f1")!!)?.type)
        
        // Undo castling
        assertTrue(game.undoLastMove())
        
        // Verify pieces restored
        assertEquals(PieceType.KING, game.getPieceAt(e1)?.type)
        assertEquals(PieceType.ROOK, game.getPieceAt(Position.fromAlgebraic("h1")!!)?.type)
        assertNull(game.getPieceAt(g1))
        assertNull(game.getPieceAt(Position.fromAlgebraic("f1")!!))
    }
    
    @Test
    fun testUndoEnPassant() {
        // Set up en passant scenario
        game.importFEN("rnbqkbnr/ppp1pppp/8/3pP3/8/8/PPPP1PPP/RNBQKBNR w KQkq d6 0 1")
        
        val e5 = Position.fromAlgebraic("e5")!!
        val d6 = Position.fromAlgebraic("d6")!!
        val d5 = Position.fromAlgebraic("d5")!!
        
        game.makeMove(e5, d6)
        
        // Verify en passant occurred
        assertEquals(PieceType.PAWN, game.getPieceAt(d6)?.type)
        assertNull(game.getPieceAt(d5))
        
        // Undo en passant
        assertTrue(game.undoLastMove())
        
        // Verify pieces restored
        assertEquals(PieceType.PAWN, game.getPieceAt(e5)?.type)
        assertEquals(PieceColor.WHITE, game.getPieceAt(e5)?.color)
        assertEquals(PieceType.PAWN, game.getPieceAt(d5)?.type)
        assertEquals(PieceColor.BLACK, game.getPieceAt(d5)?.color)
        assertNull(game.getPieceAt(d6))
    }
    
    @Test
    fun testUndoPromotion() {
        // Set up pawn promotion
        game.importFEN("8/P7/8/8/8/8/8/K6k w - - 0 1")
        
        val a7 = Position.fromAlgebraic("a7")!!
        val a8 = Position.fromAlgebraic("a8")!!
        
        game.makeMove(a7, a8, PieceType.QUEEN)
        
        // Verify promotion
        assertEquals(PieceType.QUEEN, game.getPieceAt(a8)?.type)
        
        // Undo promotion
        assertTrue(game.undoLastMove())
        
        // Verify pawn restored
        assertEquals(PieceType.PAWN, game.getPieceAt(a7)?.type)
        assertNull(game.getPieceAt(a8))
    }
    
    @Test
    fun testUndoToMove() {
        // Make several moves
        game.makeMove(Position.fromAlgebraic("e2")!!, Position.fromAlgebraic("e4")!!)
        game.makeMove(Position.fromAlgebraic("e7")!!, Position.fromAlgebraic("e5")!!)
        game.makeMove(Position.fromAlgebraic("g1")!!, Position.fromAlgebraic("f3")!!)
        game.makeMove(Position.fromAlgebraic("b8")!!, Position.fromAlgebraic("c6")!!)
        
        assertEquals(4, game.getMoveHistory().size)
        
        // Undo to move 2
        assertTrue(game.undoToMove(2))
        assertEquals(2, game.getMoveHistory().size)
        
        // Verify position after 2 moves
        assertEquals(PieceType.PAWN, game.getPieceAt(Position.fromAlgebraic("e4")!!)?.type)
        assertEquals(PieceType.PAWN, game.getPieceAt(Position.fromAlgebraic("e5")!!)?.type)
        assertNull(game.getPieceAt(Position.fromAlgebraic("f3")!!))
        assertNull(game.getPieceAt(Position.fromAlgebraic("c6")!!))
    }
    
    @Test
    fun testUndoToInitialPosition() {
        // Make some moves
        game.makeMove(Position.fromAlgebraic("e2")!!, Position.fromAlgebraic("e4")!!)
        game.makeMove(Position.fromAlgebraic("e7")!!, Position.fromAlgebraic("e5")!!)
        
        // Undo to initial position
        assertTrue(game.undoToMove(0))
        assertEquals(0, game.getMoveHistory().size)
        assertEquals("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", game.exportFEN())
    }
    
    @Test
    fun testUndoToInvalidMove() {
        // Make 2 moves
        game.makeMove(Position.fromAlgebraic("e2")!!, Position.fromAlgebraic("e4")!!)
        game.makeMove(Position.fromAlgebraic("e7")!!, Position.fromAlgebraic("e5")!!)
        
        // Try to undo to future move
        assertFalse(game.undoToMove(5))
        assertEquals(2, game.getMoveHistory().size)
        
        // Try to undo to negative move
        assertFalse(game.undoToMove(-1))
        assertEquals(2, game.getMoveHistory().size)
    }
    
    @Test
    fun testGetUndoCount() {
        assertEquals(0, game.getUndoCount())
        
        game.makeMove(Position.fromAlgebraic("e2")!!, Position.fromAlgebraic("e4")!!)
        assertEquals(1, game.getUndoCount())
        
        game.makeMove(Position.fromAlgebraic("e7")!!, Position.fromAlgebraic("e5")!!)
        assertEquals(2, game.getUndoCount())
        
        game.undoLastMove()
        assertEquals(1, game.getUndoCount())
        
        game.undoLastMove()
        assertEquals(0, game.getUndoCount())
    }
}
