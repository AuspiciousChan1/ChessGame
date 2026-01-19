package com.chenjili.chess.api

/**
 * Service for managing multiple chess game instances
 */
interface IChessService {
    /**
     * Create a new chess game instance
     * @return New game instance with unique ID
     */
    fun createGame(): IChessGame
    
    /**
     * Create a new chess game instance with specific ID
     * @param id Unique identifier for the game
     * @return New game instance, or null if ID already exists
     */
    fun createGame(id: String): IChessGame?
    
    /**
     * Get an existing game by ID
     * @param id Game identifier
     * @return Game instance, or null if not found
     */
    fun getGame(id: String): IChessGame?
    
    /**
     * Delete a game instance
     * @param id Game identifier
     * @return true if game was deleted, false if not found
     */
    fun deleteGame(id: String): Boolean
    
    /**
     * Get all active game IDs
     * @return List of all game IDs
     */
    fun getAllGameIds(): List<String>
    
    /**
     * Delete all games
     */
    fun clearAllGames()
}