package com.chenjili.chess.inner

import com.chenjili.chess.api.IChessGame
import com.chenjili.chess.api.IChessService
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Implementation of chess service that manages multiple game instances
 */
class ChessService : IChessService {
    
    private val games = ConcurrentHashMap<String, IChessGame>()
    
    override fun createGame(): IChessGame {
        val id = UUID.randomUUID().toString()
        val game = ChessGame(id)
        games[id] = game
        return game
    }
    
    override fun createGame(id: String): IChessGame? {
        if (games.containsKey(id)) {
            return null
        }
        val game = ChessGame(id)
        games[id] = game
        return game
    }
    
    override fun getGame(id: String): IChessGame? {
        return games[id]
    }
    
    override fun deleteGame(id: String): Boolean {
        return games.remove(id) != null
    }
    
    override fun getAllGameIds(): List<String> {
        return games.keys.toList()
    }
    
    override fun clearAllGames() {
        games.clear()
    }
}