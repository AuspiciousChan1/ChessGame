package com.chenjili.chess.inner

import com.chenjili.chess.api.*
import java.util.UUID
import kotlin.collections.get
import kotlin.compareTo
import kotlin.div
import kotlin.inc
import kotlin.text.compareTo
import kotlin.text.get
import kotlin.to

/**
 * Represents a snapshot of the game state at a particular point in time
 * Note: This data class contains an Array field which uses referential equality.
 * Do not rely on default equals()/hashCode() for comparing snapshots.
 */
private data class GameStateSnapshot(
    val board: Array<Array<Piece?>>,
    val activeColor: PieceColor,
    val castlingRights: String,
    val enPassantTarget: Position?,
    val halfMoveClock: Int,
    val fullMoveNumber: Int
) {
    // Deep copy constructor
    fun copy(): GameStateSnapshot {
        val boardCopy = Array(8) { rank ->
            Array(8) { file ->
                board[rank][file]?.copy()
            }
        }
        return GameStateSnapshot(
            boardCopy,
            activeColor,
            castlingRights,
            enPassantTarget?.copy(),
            halfMoveClock,
            fullMoveNumber
        )
    }
}

/**
 * Implementation of a chess game
 */
class ChessGame(override val id: String = UUID.randomUUID().toString()) : IChessGame {
    
    // Board represented as 8x8 array, indexed by [rank][file]
    private var board: Array<Array<Piece?>> = Array(8) { Array(8) { null } }
    
    // Game state
    private var activeColor: PieceColor = PieceColor.WHITE
    private var castlingRights: String = "KQkq"
    private var enPassantTarget: Position? = null
    private var halfMoveClock: Int = 0
    private var fullMoveNumber: Int = 1
    private val moveHistory: MutableList<Move> = mutableListOf()
    
    // History of game states for undo functionality
    // Note: This creates a deep copy of the board for each move, which could consume memory in very long games.
    // Consider implementing a maximum history size or delta-based storage for production use.
    private val stateHistory: MutableList<GameStateSnapshot> = mutableListOf()
    
    init {
        reset()
    }
    
    /**
     * Create a snapshot of the current game state
     */
    private fun createSnapshot(): GameStateSnapshot {
        val boardCopy = Array(8) { rank ->
            Array(8) { file ->
                board[rank][file]?.copy()
            }
        }
        return GameStateSnapshot(
            boardCopy,
            activeColor,
            castlingRights,
            enPassantTarget?.copy(),
            halfMoveClock,
            fullMoveNumber
        )
    }
    
    /**
     * Restore the game state from a snapshot
     */
    private fun restoreSnapshot(snapshot: GameStateSnapshot) {
        board = Array(8) { rank ->
            Array(8) { file ->
                snapshot.board[rank][file]?.copy()
            }
        }
        activeColor = snapshot.activeColor
        castlingRights = snapshot.castlingRights
        enPassantTarget = snapshot.enPassantTarget?.copy()
        halfMoveClock = snapshot.halfMoveClock
        fullMoveNumber = snapshot.fullMoveNumber
    }
    
    override fun reset() {
        // Clear board
        board = Array(8) { Array(8) { null } }
        
        // Set up initial position
        // White pieces (rank 0 and 1)
        board[0][0] = Piece(PieceType.ROOK, PieceColor.WHITE)
        board[0][1] = Piece(PieceType.KNIGHT, PieceColor.WHITE)
        board[0][2] = Piece(PieceType.BISHOP, PieceColor.WHITE)
        board[0][3] = Piece(PieceType.QUEEN, PieceColor.WHITE)
        board[0][4] = Piece(PieceType.KING, PieceColor.WHITE)
        board[0][5] = Piece(PieceType.BISHOP, PieceColor.WHITE)
        board[0][6] = Piece(PieceType.KNIGHT, PieceColor.WHITE)
        board[0][7] = Piece(PieceType.ROOK, PieceColor.WHITE)
        for (file in 0..7) {
            board[1][file] = Piece(PieceType.PAWN, PieceColor.WHITE)
        }
        
        // Black pieces (rank 6 and 7)
        for (file in 0..7) {
            board[6][file] = Piece(PieceType.PAWN, PieceColor.BLACK)
        }
        board[7][0] = Piece(PieceType.ROOK, PieceColor.BLACK)
        board[7][1] = Piece(PieceType.KNIGHT, PieceColor.BLACK)
        board[7][2] = Piece(PieceType.BISHOP, PieceColor.BLACK)
        board[7][3] = Piece(PieceType.QUEEN, PieceColor.BLACK)
        board[7][4] = Piece(PieceType.KING, PieceColor.BLACK)
        board[7][5] = Piece(PieceType.BISHOP, PieceColor.BLACK)
        board[7][6] = Piece(PieceType.KNIGHT, PieceColor.BLACK)
        board[7][7] = Piece(PieceType.ROOK, PieceColor.BLACK)
        
        // Reset game state
        activeColor = PieceColor.WHITE
        castlingRights = "KQkq"
        enPassantTarget = null
        halfMoveClock = 0
        fullMoveNumber = 1
        moveHistory.clear()
        stateHistory.clear()
        
        // Save initial state
        stateHistory.add(createSnapshot())
    }
    
    override fun getPieceAt(position: Position): Piece? {
        if (!position.isValid()) return null
        return board[position.rank][position.file]
    }
    
    override fun getAllPieces(): Map<Position, Piece> {
        val pieces = mutableMapOf<Position, Piece>()
        for (rank in 0..7) {
            for (file in 0..7) {
                val piece = board[rank][file]
                if (piece != null) {
                    pieces[Position(file, rank)] = piece
                }
            }
        }
        return pieces
    }
    
    override fun setupPosition(
        pieces: Map<Position, Piece>,
        activeColor: PieceColor,
        castlingRights: String,
        enPassantTarget: Position?,
        halfMoveClock: Int,
        fullMoveNumber: Int
    ) {
        // Clear board
        board = Array(8) { Array(8) { null } }
        
        // Place pieces
        pieces.forEach { (position, piece) ->
            if (position.isValid()) {
                board[position.rank][position.file] = piece
            }
        }
        
        // Set game state
        this.activeColor = activeColor
        this.castlingRights = castlingRights
        this.enPassantTarget = enPassantTarget
        this.halfMoveClock = halfMoveClock
        this.fullMoveNumber = fullMoveNumber
        moveHistory.clear()
        stateHistory.clear()
        
        // Save initial state
        stateHistory.add(createSnapshot())
    }
    
    override fun importFEN(fen: String): Boolean {
        try {
            val parts = fen.trim().split(" ")
            if (parts.size < 4) return false
            
            // Parse piece placement
            val ranks = parts[0].split("/")
            if (ranks.size != 8) return false
            
            board = Array(8) { Array(8) { null } }
            
            for (rankIndex in ranks.indices) {
                val rank = 7 - rankIndex // FEN starts from rank 8
                var file = 0
                
                for (char in ranks[rankIndex]) {
                    if (char.isDigit()) {
                        file += char.digitToInt()
                    } else {
                        val color = if (char.isUpperCase()) PieceColor.WHITE else PieceColor.BLACK
                        val type = when (char.uppercaseChar()) {
                            'K' -> PieceType.KING
                            'Q' -> PieceType.QUEEN
                            'R' -> PieceType.ROOK
                            'B' -> PieceType.BISHOP
                            'N' -> PieceType.KNIGHT
                            'P' -> PieceType.PAWN
                            else -> return false
                        }
                        if (file >= 8) return false
                        board[rank][file] = Piece(type, color)
                        file++
                    }
                }
            }
            
            // Parse active color
            activeColor = if (parts[1] == "w") PieceColor.WHITE else PieceColor.BLACK
            
            // Parse castling rights
            castlingRights = parts[2]
            
            // Parse en passant target
            enPassantTarget = if (parts[3] == "-") null else Position.fromAlgebraic(parts[3])
            
            // Parse half-move clock and full move number
            halfMoveClock = if (parts.size > 4) parts[4].toIntOrNull() ?: 0 else 0
            fullMoveNumber = if (parts.size > 5) parts[5].toIntOrNull() ?: 1 else 1
            
            moveHistory.clear()
            stateHistory.clear()
            
            // Save initial state
            stateHistory.add(createSnapshot())
            return true
        } catch (e: Exception) {
            return false
        }
    }
    
    override fun exportFEN(): String {
        val sb = StringBuilder()
        
        // Piece placement
        for (rank in 7 downTo 0) {
            var emptyCount = 0
            for (file in 0..7) {
                val piece = board[rank][file]
                if (piece == null) {
                    emptyCount++
                } else {
                    if (emptyCount > 0) {
                        sb.append(emptyCount)
                        emptyCount = 0
                    }
                    val char = when (piece.type) {
                        PieceType.KING -> 'K'
                        PieceType.QUEEN -> 'Q'
                        PieceType.ROOK -> 'R'
                        PieceType.BISHOP -> 'B'
                        PieceType.KNIGHT -> 'N'
                        PieceType.PAWN -> 'P'
                    }
                    sb.append(if (piece.color == PieceColor.WHITE) char else char.lowercaseChar())
                }
            }
            if (emptyCount > 0) sb.append(emptyCount)
            if (rank > 0) sb.append('/')
        }
        
        // Active color
        sb.append(if (activeColor == PieceColor.WHITE) " w " else " b ")
        
        // Castling rights
        sb.append(castlingRights)
        sb.append(' ')
        
        // En passant target
        sb.append(enPassantTarget?.toAlgebraic() ?: "-")
        sb.append(' ')
        
        // Half-move clock and full move number
        sb.append(halfMoveClock)
        sb.append(' ')
        sb.append(fullMoveNumber)
        
        return sb.toString()
    }
    
    override fun importPGN(pgn: String): Boolean {
        // Simplified PGN import - just parse moves
        try {
            reset()
            
            // Remove comments and extract moves
            var cleanPgn = pgn.replace(Regex("\\{[^}]*\\}"), "")
                .replace(Regex("\\([^)]*\\)"), "")
                .replace(Regex("\\[[^\\]]*\\]"), "")
            
            // Extract move text (skip move numbers)
            val movePattern = Regex("""([KQRBN]?[a-h]?[1-8]?x?[a-h][1-8](=[QRBN])?|O-O(-O)?)""")
            val moves = movePattern.findAll(cleanPgn).map { it.value }.toList()
            
            for (moveStr in moves) {
                // Parse and make move
                val move = parseMoveFromAlgebraic(moveStr)
                if (move == null) {
                    reset()
                    return false
                }
                makeMove(move.from, move.to, move.promotionPiece)
            }
            
            return true
        } catch (e: Exception) {
            reset()
            return false
        }
    }
    
    override fun exportPGN(): String {
        val sb = StringBuilder()
        
        // Add basic headers
        sb.append("[Event \"Chess Game\"]\n")
        sb.append("[Site \"ChessGame App\"]\n")
        sb.append("[Date \"????.??.??\"]\n")
        sb.append("[Round \"-\"]\n")
        sb.append("[White \"Player\"]\n")
        sb.append("[Black \"Player\"]\n")
        
        val result = when (getGameState()) {
            GameState.CHECKMATE_WHITE_WINS -> "1-0"
            GameState.CHECKMATE_BLACK_WINS -> "0-1"
            GameState.STALEMATE, GameState.DRAW_BY_INSUFFICIENT_MATERIAL,
            GameState.DRAW_BY_FIFTY_MOVE_RULE, GameState.DRAW_BY_THREEFOLD_REPETITION -> "1/2-1/2"
            else -> "*"
        }
        sb.append("[Result \"$result\"]\n\n")
        
        // Add moves
        var moveNum = 1
        for (i in moveHistory.indices) {
            if (i % 2 == 0) {
                sb.append("$moveNum. ")
            }
            sb.append(moveHistory[i].toAlgebraic())
            sb.append(" ")
            if (i % 2 == 1) {
                moveNum++
            }
        }
        sb.append(result)
        
        return sb.toString()
    }
    
    override fun makeMove(from: Position, to: Position, promotionPiece: PieceType?): Move? {
        if (!from.isValid() || !to.isValid()) return null
        
        val piece = getPieceAt(from) ?: return null
        if (piece.color != activeColor) return null
        
        // Check if move is legal
        val legalMoves = getLegalMoves(from)
        
        val matchingMove = legalMoves.find { it.from == from && it.to == to } ?: return null
        // Handle promotion
        val finalPromotionPiece = if (piece.type == PieceType.PAWN &&
            (to.rank == 7 || to.rank == 0)) {
            promotionPiece ?: PieceType.QUEEN
        } else null
        
        val isEnPassant = matchingMove.isEnPassant
        val isCastling = matchingMove.isCastling

        // 记录被捕获的棋子
        val capturedPiece: Piece? = if (isEnPassant) {
            getPieceAt(Position(to.file, from.rank))
        } else {
            getPieceAt(to)
        }

        // 执行移动逻辑
        board[from.rank][from.file] = null
        
        if (isCastling) {
            // Move king
            board[to.rank][to.file] = piece
            
            // Move rook accordingly
            if (to.file > from.file) {
                // King-side castling: rook from file 7 to file 5
                val rook = board[from.rank][7]
                board[from.rank][7] = null
                board[from.rank][5] = rook
            } else {
                // Queen-side castling: rook from file 0 to file 3
                val rook = board[from.rank][0]
                board[from.rank][0] = null
                board[from.rank][3] = rook
            }
        } else if (isEnPassant) {
            // Place moving pawn to destination
            board[to.rank][to.file] = piece
            // Remove the captured pawn which sits at (to.file, from.rank)
            board[from.rank][to.file] = null
        } else {
            // Normal move or capture or promotion
            if (finalPromotionPiece != null) {
                board[to.rank][to.file] = Piece(finalPromotionPiece, piece.color)
            } else {
                board[to.rank][to.file] = piece
            }
        }

        // 更新游戏状态变量
        updateCastlingRights(piece, from)
        
        // Update en passant target
        enPassantTarget = if (piece.type == PieceType.PAWN &&
            kotlin.math.abs(to.rank - from.rank) == 2) {
            Position(from.file, (from.rank + to.rank) / 2)
        } else null
        
        // Update half-move clock
        halfMoveClock = if (piece.type == PieceType.PAWN || capturedPiece != null) 0
        else halfMoveClock + 1
        
        // Update full move number
        if (activeColor == PieceColor.BLACK) {
            fullMoveNumber++
        }
        
        // Switch active color
        activeColor = if (activeColor == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE
        
        // Record move

        // 在所有变量更新完成后，记录当前状态快照
        stateHistory.add(createSnapshot())

        val move = Move(from, to, piece, capturedPiece, isEnPassant, isCastling, finalPromotionPiece)
        moveHistory.add(move)
        
        return move
    }
    
    private fun updateCastlingRights(piece: Piece, from: Position) {
        when (piece.type) {
            PieceType.KING -> {
                if (piece.color == PieceColor.WHITE) {
                    castlingRights = castlingRights.replace("K", "").replace("Q", "")
                } else {
                    castlingRights = castlingRights.replace("k", "").replace("q", "")
                }
            }
            PieceType.ROOK -> {
                when {
                    from == Position(0, 0) -> castlingRights = castlingRights.replace("Q", "")
                    from == Position(7, 0) -> castlingRights = castlingRights.replace("K", "")
                    from == Position(0, 7) -> castlingRights = castlingRights.replace("q", "")
                    from == Position(7, 7) -> castlingRights = castlingRights.replace("k", "")
                }
            }
            else -> {}
        }
        if (castlingRights.isEmpty()) castlingRights = "-"
    }
    
    override fun getLegalMoves(position: Position?): List<Move> {
        val moves = mutableListOf<Move>()
        
        if (position != null) {
            val piece = getPieceAt(position)
            if (piece != null && piece.color == activeColor) {
                generateMovesForPiece(position, piece, moves)
            }
        } else {
            // Generate all legal moves for active color
            for (rank in 0..7) {
                for (file in 0..7) {
                    val pos = Position(file, rank)
                    val piece = getPieceAt(pos)
                    if (piece != null && piece.color == activeColor) {
                        generateMovesForPiece(pos, piece, moves)
                    }
                }
            }
        }
        
        // Filter out moves that leave king in check
        return moves.filter { move ->
            !leavesKingInCheck(move)
        }
    }

    override fun isInCheck(color: PieceColor): Boolean {
        // 1) 找到指定颜色的王
        var kingPos: Position? = null
        for (rank in 0..7) {
            for (file in 0..7) {
                val piece = board[rank][file]
                if (piece != null && piece.type == PieceType.KING && piece.color == color) {
                    kingPos = Position(file, rank)
                    break
                }
            }
            if (kingPos != null) break
        }

        // 2) 王不存在则视为异常局面：认为在将军中（与 leavesKingInCheck 的处理一致）
        if (kingPos == null) return true

        // 3) 判断该王格是否被对方攻击
        val opponent = if (color == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE
        return isSquareAttacked(kingPos, opponent)
    }
    
    private fun generateMovesForPiece(from: Position, piece: Piece, moves: MutableList<Move>) {
        when (piece.type) {
            PieceType.PAWN -> generatePawnMoves(from, piece, moves)
            PieceType.KNIGHT -> generateKnightMoves(from, piece, moves)
            PieceType.BISHOP -> generateBishopMoves(from, piece, moves)
            PieceType.ROOK -> generateRookMoves(from, piece, moves)
            PieceType.QUEEN -> generateQueenMoves(from, piece, moves)
            PieceType.KING -> generateKingMoves(from, piece, moves)
        }
    }
    
    private fun generatePawnMoves(from: Position, piece: Piece, moves: MutableList<Move>) {
        val direction = if (piece.color == PieceColor.WHITE) 1 else -1
        val startRank = if (piece.color == PieceColor.WHITE) 1 else 6
        val promotionRank = if (piece.color == PieceColor.WHITE) 7 else 0
        
        // Forward move
        val forward = Position(from.file, from.rank + direction)
        if (forward.isValid() && getPieceAt(forward) == null) {
            if (forward.rank == promotionRank) {
                // Promotion
                for (promoPiece in listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT)) {
                    moves.add(Move(from, forward, piece, null, false, false, promoPiece))
                }
            } else {
                moves.add(Move(from, forward, piece))
            }
            
            // Double forward from start
            if (from.rank == startRank) {
                val doubleForward = Position(from.file, from.rank + 2 * direction)
                if (doubleForward.isValid() && getPieceAt(doubleForward) == null) {
                    moves.add(Move(from, doubleForward, piece))
                }
            }
        }
        
        // Captures
        for (fileOffset in listOf(-1, 1)) {
            val capturePos = Position(from.file + fileOffset, from.rank + direction)
            if (capturePos.isValid()) {
                val targetPiece = getPieceAt(capturePos)
                if (targetPiece != null && targetPiece.color != piece.color) {
                    if (capturePos.rank == promotionRank) {
                        // Promotion with capture
                        for (promoPiece in listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT)) {
                            moves.add(Move(from, capturePos, piece, targetPiece, false, false, promoPiece))
                        }
                    } else {
                        moves.add(Move(from, capturePos, piece, targetPiece))
                    }
                } else if (enPassantTarget == capturePos) {
                    // En passant
                    val capturedPawn = Piece(PieceType.PAWN, if (piece.color == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE)
                    moves.add(Move(from, capturePos, piece, capturedPawn, true, false))
                }
            }
        }
    }
    
    private fun generateKnightMoves(from: Position, piece: Piece, moves: MutableList<Move>) {
        val offsets = listOf(
            Pair(-2, -1), Pair(-2, 1), Pair(-1, -2), Pair(-1, 2),
            Pair(1, -2), Pair(1, 2), Pair(2, -1), Pair(2, 1)
        )
        
        for ((fileOffset, rankOffset) in offsets) {
            val to = Position(from.file + fileOffset, from.rank + rankOffset)
            if (to.isValid()) {
                val targetPiece = getPieceAt(to)
                if (targetPiece == null || targetPiece.color != piece.color) {
                    moves.add(Move(from, to, piece, targetPiece))
                }
            }
        }
    }
    
    private fun generateBishopMoves(from: Position, piece: Piece, moves: MutableList<Move>) {
        val directions = listOf(Pair(-1, -1), Pair(-1, 1), Pair(1, -1), Pair(1, 1))
        generateSlidingMoves(from, piece, directions, moves)
    }
    
    private fun generateRookMoves(from: Position, piece: Piece, moves: MutableList<Move>) {
        val directions = listOf(Pair(-1, 0), Pair(1, 0), Pair(0, -1), Pair(0, 1))
        generateSlidingMoves(from, piece, directions, moves)
    }
    
    private fun generateQueenMoves(from: Position, piece: Piece, moves: MutableList<Move>) {
        val directions = listOf(
            Pair(-1, -1), Pair(-1, 1), Pair(1, -1), Pair(1, 1),
            Pair(-1, 0), Pair(1, 0), Pair(0, -1), Pair(0, 1)
        )
        generateSlidingMoves(from, piece, directions, moves)
    }
    
    private fun generateSlidingMoves(
        from: Position, 
        piece: Piece, 
        directions: List<Pair<Int, Int>>, 
        moves: MutableList<Move>
    ) {
        for ((fileDir, rankDir) in directions) {
            var file = from.file + fileDir
            var rank = from.rank + rankDir
            
            while (file in 0..7 && rank in 0..7) {
                val to = Position(file, rank)
                val targetPiece = getPieceAt(to)
                
                if (targetPiece == null) {
                    moves.add(Move(from, to, piece))
                } else {
                    if (targetPiece.color != piece.color) {
                        moves.add(Move(from, to, piece, targetPiece))
                    }
                    break
                }
                
                file += fileDir
                rank += rankDir
            }
        }
    }
    
    private fun generateKingMoves(from: Position, piece: Piece, moves: MutableList<Move>) {
        // Normal king moves
        val offsets = listOf(
            Pair(-1, -1), Pair(-1, 0), Pair(-1, 1),
            Pair(0, -1), Pair(0, 1),
            Pair(1, -1), Pair(1, 0), Pair(1, 1)
        )
        
        for ((fileOffset, rankOffset) in offsets) {
            val to = Position(from.file + fileOffset, from.rank + rankOffset)
            if (to.isValid()) {
                val targetPiece = getPieceAt(to)
                if (targetPiece == null || targetPiece.color != piece.color) {
                    moves.add(Move(from, to, piece, targetPiece))
                }
            }
        }
        
        // Castling
        if (!isSquareAttacked(from, if (piece.color == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE)) {
            // Kingside castling
            if ((piece.color == PieceColor.WHITE && castlingRights.contains('K')) ||
                (piece.color == PieceColor.BLACK && castlingRights.contains('k'))) {
                if (getPieceAt(Position(5, from.rank)) == null &&
                    getPieceAt(Position(6, from.rank)) == null &&
                    !isSquareAttacked(Position(5, from.rank), if (piece.color == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE) &&
                    !isSquareAttacked(Position(6, from.rank), if (piece.color == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE)) {
                    moves.add(Move(from, Position(6, from.rank), piece, null, false, true))
                }
            }
            
            // Queenside castling
            if ((piece.color == PieceColor.WHITE && castlingRights.contains('Q')) ||
                (piece.color == PieceColor.BLACK && castlingRights.contains('q'))) {
                if (getPieceAt(Position(3, from.rank)) == null &&
                    getPieceAt(Position(2, from.rank)) == null &&
                    getPieceAt(Position(1, from.rank)) == null &&
                    !isSquareAttacked(Position(3, from.rank), if (piece.color == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE) &&
                    !isSquareAttacked(Position(2, from.rank), if (piece.color == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE)) {
                    moves.add(Move(from, Position(2, from.rank), piece, null, false, true))
                }
            }
        }
    }
    
    private fun isSquareAttacked(position: Position, byColor: PieceColor): Boolean {
        // Check if position is attacked by any piece of byColor
        for (rank in 0..7) {
            for (file in 0..7) {
                val pos = Position(file, rank)
                val piece = getPieceAt(pos)
                if (piece != null && piece.color == byColor) {
                    val pseudoLegalMoves = mutableListOf<Move>()
                    when (piece.type) {
                        PieceType.PAWN -> {
                            val direction = if (piece.color == PieceColor.WHITE) 1 else -1
                            for (fileOffset in listOf(-1, 1)) {
                                val attackPos = Position(pos.file + fileOffset, pos.rank + direction)
                                if (attackPos == position) return true
                            }
                        }
                        PieceType.KNIGHT -> {
                            generateKnightMoves(pos, piece, pseudoLegalMoves)
                            if (pseudoLegalMoves.any { it.to == position }) return true
                        }
                        PieceType.BISHOP -> {
                            generateBishopMoves(pos, piece, pseudoLegalMoves)
                            if (pseudoLegalMoves.any { it.to == position }) return true
                        }
                        PieceType.ROOK -> {
                            generateRookMoves(pos, piece, pseudoLegalMoves)
                            if (pseudoLegalMoves.any { it.to == position }) return true
                        }
                        PieceType.QUEEN -> {
                            generateQueenMoves(pos, piece, pseudoLegalMoves)
                            if (pseudoLegalMoves.any { it.to == position }) return true
                        }
                        PieceType.KING -> {
                            val offsets = listOf(
                                Pair(-1, -1), Pair(-1, 0), Pair(-1, 1),
                                Pair(0, -1), Pair(0, 1),
                                Pair(1, -1), Pair(1, 0), Pair(1, 1)
                            )
                            for ((fileOffset, rankOffset) in offsets) {
                                val attackPos = Position(pos.file + fileOffset, pos.rank + rankOffset)
                                if (attackPos == position) return true
                            }
                        }
                    }
                }
            }
        }
        return false
    }
    
    private fun leavesKingInCheck(move: Move): Boolean {
        // Make the move temporarily
        val originalFromPiece = board[move.from.rank][move.from.file]
        val originalToPiece = board[move.to.rank][move.to.file]
        val originalEnPassant = enPassantTarget
        
        board[move.from.rank][move.from.file] = null
        board[move.to.rank][move.to.file] = move.piece
        
        // Handle en passant
        var capturedPawnPos: Position? = null
        if (move.isEnPassant) {
            capturedPawnPos = Position(move.to.file, move.from.rank)
            board[capturedPawnPos.rank][capturedPawnPos.file] = null
        }
        
        // Find king position
        var kingPos: Position? = null
        for (rank in 0..7) {
            for (file in 0..7) {
                val piece = board[rank][file]
                if (piece != null && piece.type == PieceType.KING && piece.color == move.piece.color) {
                    kingPos = Position(file, rank)
                    break
                }
            }
            if (kingPos != null) break
        }
        
        val inCheck = if (kingPos != null) {
            isSquareAttacked(kingPos, if (move.piece.color == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE)
        } else {
            true // King not found, treat as check
        }
        
        // Undo the move
        board[move.from.rank][move.from.file] = originalFromPiece
        board[move.to.rank][move.to.file] = originalToPiece
        if (capturedPawnPos != null) {
            board[capturedPawnPos.rank][capturedPawnPos.file] = Piece(PieceType.PAWN, if (move.piece.color == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE)
        }
        enPassantTarget = originalEnPassant
        
        return inCheck
    }
    
    override fun getGameState(): GameState {
        val hasLegalMoves = getLegalMoves().isNotEmpty()
        
        if (!hasLegalMoves) {
            // Find king position
            var kingPos: Position? = null
            for (rank in 0..7) {
                for (file in 0..7) {
                    val piece = board[rank][file]
                    if (piece != null && piece.type == PieceType.KING && piece.color == activeColor) {
                        kingPos = Position(file, rank)
                        break
                    }
                }
                if (kingPos != null) break
            }
            
            if (kingPos != null) {
                val inCheck = isSquareAttacked(kingPos, if (activeColor == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE)
                return if (inCheck) {
                    if (activeColor == PieceColor.WHITE) GameState.CHECKMATE_BLACK_WINS
                    else GameState.CHECKMATE_WHITE_WINS
                } else {
                    GameState.STALEMATE
                }
            }
        }
        
        // Check for fifty-move rule
        // The rule allows a draw if 50 full moves (100 half-moves/plies) pass
        // without a pawn move or capture
        if (halfMoveClock >= 100) {
            return GameState.DRAW_BY_FIFTY_MOVE_RULE
        }
        
        // Check for insufficient material
        if (isInsufficientMaterial()) {
            return GameState.DRAW_BY_INSUFFICIENT_MATERIAL
        }
        
        return GameState.IN_PROGRESS
    }
    
    private fun isInsufficientMaterial(): Boolean {
        val pieces = mutableListOf<Piece>()
        for (rank in 0..7) {
            for (file in 0..7) {
                val piece = board[rank][file]
                if (piece != null) {
                    pieces.add(piece)
                }
            }
        }
        
        // King vs King
        if (pieces.size == 2) return true
        
        // King and minor piece vs King
        if (pieces.size == 3) {
            val nonKings = pieces.filter { it.type != PieceType.KING }
            if (nonKings.size == 1) {
                val piece = nonKings[0]
                if (piece.type == PieceType.BISHOP || piece.type == PieceType.KNIGHT) {
                    return true
                }
            }
        }
        
        return false
    }
    
    override fun getActiveColor(): PieceColor = activeColor
    
    override fun getMoveHistory(): List<Move> = moveHistory.toList()
    
    /**
     * Parse algebraic chess notation to a Move object
     * Note: This is a simplified parser that handles common PGN formats.
     * It may not handle all edge cases of algebraic notation, particularly
     * complex disambiguation scenarios or non-standard notation variants.
     * For production use, consider using a more robust PGN parsing library.
     */
    private fun parseMoveFromAlgebraic(moveStr: String): Move? {
        // Handle castling
        if (moveStr == "O-O" || moveStr == "O-O-O") {
            val kingRank = if (activeColor == PieceColor.WHITE) 0 else 7
            val kingFrom = Position(4, kingRank)
            val kingTo = if (moveStr == "O-O") {
                Position(6, kingRank)
            } else {
                Position(2, kingRank)
            }
            
            // Check if this is a legal move
            val legalMoves = getLegalMoves(kingFrom)
            return legalMoves.find { it.to == kingTo }
        }
        
        // For simplicity, we'll try to find the move by checking all legal moves
        // and matching the destination square and piece type
        val cleanMove = moveStr.replace(Regex("[+#!?]"), "") // Remove check/mate indicators
        
        // Extract destination square (always last 2 characters for normal moves)
        val destMatch = Regex("[a-h][1-8]").findAll(cleanMove).lastOrNull()
        val destSquare = destMatch?.value?.let { Position.fromAlgebraic(it) } ?: return null
        
        // Determine piece type from first character
        val pieceType = when {
            cleanMove.startsWith('K') -> PieceType.KING
            cleanMove.startsWith('Q') -> PieceType.QUEEN
            cleanMove.startsWith('R') -> PieceType.ROOK
            cleanMove.startsWith('B') -> PieceType.BISHOP
            cleanMove.startsWith('N') -> PieceType.KNIGHT
            else -> PieceType.PAWN
        }
        
        // Check for promotion
        val promotionPiece = if (cleanMove.contains('=')) {
            when {
                cleanMove.contains("=Q") -> PieceType.QUEEN
                cleanMove.contains("=R") -> PieceType.ROOK
                cleanMove.contains("=B") -> PieceType.BISHOP
                cleanMove.contains("=N") -> PieceType.KNIGHT
                else -> null
            }
        } else null
        
        // Find all legal moves that match this description
        val allLegalMoves = getLegalMoves()
        val matchingMoves = allLegalMoves.filter { move ->
            move.to == destSquare && 
            move.piece.type == pieceType &&
            move.piece.color == activeColor
        }
        
        // If there's only one matching move, use it
        if (matchingMoves.size == 1) {
            return matchingMoves[0]
        }
        
        // If there are multiple matching moves, we need to disambiguate
        // Extract disambiguation info (file or rank)
        if (matchingMoves.size > 1) {
            val disambiguationFile = Regex("[a-h]").find(cleanMove.drop(1))?.value?.let { it[0] - 'a' }
            val disambiguationRank = Regex("[1-8]").find(cleanMove.drop(1).dropLast(2))?.value?.let { it[0] - '1' }
            
            return matchingMoves.find { move ->
                (disambiguationFile == null || move.from.file == disambiguationFile) &&
                (disambiguationRank == null || move.from.rank == disambiguationRank)
            }
        }
        
        return null
    }
    
    override fun undoLastMove(): Boolean {
        // moveHistory.size 与 stateHistory.size - 1 保持同步
        if (moveHistory.isEmpty() || stateHistory.size < 2) {
            return false
        }

        // 1. 移除当前最新的快照
        stateHistory.removeAt(stateHistory.size - 1)

        // 2. 恢复到移除后的最后一个快照（即上一步之后的状态或初始状态）
        restoreSnapshot(stateHistory.last())

        // 3. 移除移动记录
        moveHistory.removeAt(moveHistory.size - 1)
        
        return true
    }
    
    override fun undoToMove(moveNumber: Int): Boolean {
        if (moveNumber < 0 || moveNumber > moveHistory.size) {
            return false
        }

        // 目标快照库大小：初始状态(1) + 保留的移动步数(moveNumber)
        val targetSize = moveNumber + 1

        while (stateHistory.size > targetSize) {
            stateHistory.removeAt(stateHistory.size - 1)
        }

        restoreSnapshot(stateHistory.last())

        while (moveHistory.size > moveNumber) {
            moveHistory.removeAt(moveHistory.size - 1)
        }
        
        return true
    }
    
    override fun getUndoCount(): Int {
        return moveHistory.size
    }
}
