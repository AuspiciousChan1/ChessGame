package com.chenjili.chess

import com.chenjili.chess.api.*
import com.chenjili.chess.inner.ChessService
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before

/**
 * Unit tests for ChessService implementation
 */
class ChessServiceTest {
    
    private lateinit var service: ChessService
    
    @Before
    fun setup() {
        service = ChessService()
    }
    
    @Test
    fun testCreateGame() {
        val game = service.createGame()
        assertNotNull(game)
        assertNotNull(game.id)
    }
    
    @Test
    fun testCreateGameWithId() {
        val customId = "test-game-123"
        val game = service.createGame(customId)
        
        assertNotNull(game)
        assertEquals(customId, game?.id)
    }
    
    @Test
    fun testCreateGameWithDuplicateId() {
        val customId = "test-game-123"
        val game1 = service.createGame(customId)
        val game2 = service.createGame(customId)
        
        assertNotNull(game1)
        assertNull(game2) // Should return null for duplicate ID
    }
    
    @Test
    fun testGetGame() {
        val game = service.createGame()
        val retrievedGame = service.getGame(game.id)
        
        assertNotNull(retrievedGame)
        assertEquals(game.id, retrievedGame?.id)
    }
    
    @Test
    fun testGetNonexistentGame() {
        val game = service.getGame("nonexistent-id")
        assertNull(game)
    }
    
    @Test
    fun testDeleteGame() {
        val game = service.createGame()
        val gameId = game.id
        
        assertTrue(service.deleteGame(gameId))
        assertNull(service.getGame(gameId))
    }
    
    @Test
    fun testDeleteNonexistentGame() {
        assertFalse(service.deleteGame("nonexistent-id"))
    }
    
    @Test
    fun testGetAllGameIds() {
        assertEquals(0, service.getAllGameIds().size)
        
        val game1 = service.createGame()
        val game2 = service.createGame()
        val game3 = service.createGame()
        
        val gameIds = service.getAllGameIds()
        assertEquals(3, gameIds.size)
        assertTrue(gameIds.contains(game1.id))
        assertTrue(gameIds.contains(game2.id))
        assertTrue(gameIds.contains(game3.id))
    }
    
    @Test
    fun testClearAllGames() {
        service.createGame()
        service.createGame()
        service.createGame()
        
        assertEquals(3, service.getAllGameIds().size)
        
        service.clearAllGames()
        assertEquals(0, service.getAllGameIds().size)
    }
    
    @Test
    fun testMultipleGamesIndependence() {
        val game1 = service.createGame()
        val game2 = service.createGame()
        
        // Make a move in game1
        val e2 = Position.fromAlgebraic("e2")!!
        val e4 = Position.fromAlgebraic("e4")!!
        game1.makeMove(e2, e4)
        
        // Verify game1 changed but game2 didn't
        assertNull(game1.getPieceAt(e2))
        assertNotNull(game1.getPieceAt(e4))
        
        assertNotNull(game2.getPieceAt(e2))
        assertNull(game2.getPieceAt(e4))
    }
    
    @Test
    fun testGamePersistence() {
        val customId = "persistent-game"
        val game = service.createGame(customId)
        
        // Make a move
        val e2 = Position.fromAlgebraic("e2")!!
        val e4 = Position.fromAlgebraic("e4")!!
        game?.makeMove(e2, e4)
        
        // Retrieve the same game
        val retrievedGame = service.getGame(customId)
        
        // Verify the move persisted
        assertNotNull(retrievedGame)
        assertNull(retrievedGame?.getPieceAt(e2))
        assertNotNull(retrievedGame?.getPieceAt(e4))
        assertEquals(1, retrievedGame?.getMoveHistory()?.size)
    }
    
    @Test
    fun testConcurrentGameCreation() {
        // Test that multiple games can be created concurrently
        val games = (1..10).map { service.createGame() }
        
        assertEquals(10, games.size)
        assertEquals(10, service.getAllGameIds().size)
        
        // Verify all games have unique IDs
        val uniqueIds = games.map { it.id }.toSet()
        assertEquals(10, uniqueIds.size)
    }
    
    @Test
    fun testServiceFactoryIntegration() {
        // Test that the factory returns a working service
        val factoryService = ChessServiceFactory.chessService
        assertNotNull(factoryService)
        
        val game = factoryService.createGame()
        assertNotNull(game)
        
        val retrievedGame = factoryService.getGame(game.id)
        assertNotNull(retrievedGame)
        assertEquals(game.id, retrievedGame?.id)
    }
}
