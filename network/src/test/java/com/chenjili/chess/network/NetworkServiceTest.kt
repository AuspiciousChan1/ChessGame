package com.chenjili.chess.network

import com.chenjili.chess.network.api.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Unit tests for NetworkService
 */
class NetworkServiceTest {
    
    private lateinit var serverService: INetworkService
    private lateinit var clientService: INetworkService
    
    @Before
    fun setUp() {
        serverService = NetworkServiceFactory.createNetworkService()
        clientService = NetworkServiceFactory.createNetworkService()
    }
    
    @After
    fun tearDown() {
        try {
            serverService.stopServer()
            clientService.disconnect()
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
    }
    
    @Test
    fun testConnectionStateInitial() {
        val service = NetworkServiceFactory.createNetworkService()
        assertEquals(ConnectionState.IDLE, service.getConnectionState())
        assertFalse(service.isConnected())
        assertNull(service.getConnectionInfo())
    }
    
    @Test
    fun testStartServerSuccess() {
        val latch = CountDownLatch(1)
        var receivedState: ConnectionState? = null
        
        val listener = object : ConnectionStateListener {
            override fun onConnectionStateChanged(state: ConnectionState, info: ConnectionInfo?) {
                if (state == ConnectionState.LISTENING) {
                    receivedState = state
                    latch.countDown()
                }
            }
            
            override fun onMessageReceived(message: NetworkMessage) {}
            override fun onError(error: String, exception: Exception?) {}
        }
        
        val result = serverService.startServer(8888, listener)
        assertTrue(result is NetworkResult.Success)
        
        // Wait for server to start listening
        assertTrue(latch.await(2, TimeUnit.SECONDS))
        assertEquals(ConnectionState.LISTENING, receivedState)
    }
    
    @Test
    fun testStartServerTwiceError() {
        val listener = object : ConnectionStateListener {
            override fun onConnectionStateChanged(state: ConnectionState, info: ConnectionInfo?) {}
            override fun onMessageReceived(message: NetworkMessage) {}
            override fun onError(error: String, exception: Exception?) {}
        }
        
        serverService.startServer(8889, listener)
        
        // Try to start again
        val result = serverService.startServer(8889, listener)
        assertTrue(result is NetworkResult.Error)
    }
    
    @Test
    fun testStopServer() {
        val latch = CountDownLatch(1)
        val listener = object : ConnectionStateListener {
            override fun onConnectionStateChanged(state: ConnectionState, info: ConnectionInfo?) {
                if (state == ConnectionState.LISTENING) {
                    latch.countDown()
                }
            }
            override fun onMessageReceived(message: NetworkMessage) {}
            override fun onError(error: String, exception: Exception?) {}
        }
        
        serverService.startServer(8890, listener)
        
        // Wait for server to start listening
        assertTrue(latch.await(2, TimeUnit.SECONDS))
        
        serverService.stopServer()
        assertEquals(ConnectionState.IDLE, serverService.getConnectionState())
    }
    
    @Test
    fun testNetworkMessageCreation() {
        val message = NetworkMessage(
            type = MessageType.TEXT,
            payload = "Hello, World!"
        )
        
        assertEquals(MessageType.TEXT, message.type)
        assertEquals("Hello, World!", message.payload)
        assertTrue(message.timestamp > 0)
    }
    
    @Test
    fun testConnectionInfoCreation() {
        val info = ConnectionInfo(
            address = "192.168.1.100",
            port = 8888,
            isServer = true,
            deviceName = "TestDevice"
        )
        
        assertEquals("192.168.1.100", info.address)
        assertEquals(8888, info.port)
        assertTrue(info.isServer)
        assertEquals("TestDevice", info.deviceName)
    }
    
    @Test
    fun testSendMessageWhenNotConnected() {
        val service = NetworkServiceFactory.createNetworkService()
        val message = NetworkMessage(MessageType.TEXT, "test")
        
        val result = service.sendMessage(message)
        assertTrue(result is NetworkResult.Error)
    }
    
    @Test
    fun testNetworkResultSuccess() {
        val result: NetworkResult<String> = NetworkResult.Success("data")
        assertTrue(result is NetworkResult.Success)
        assertEquals("data", (result as NetworkResult.Success).data)
    }
    
    @Test
    fun testNetworkResultError() {
        val result: NetworkResult<String> = NetworkResult.Error("error message")
        assertTrue(result is NetworkResult.Error)
        assertEquals("error message", (result as NetworkResult.Error).message)
    }
    
    @Test
    fun testConnectionStates() {
        val states = ConnectionState.values()
        assertTrue(states.contains(ConnectionState.IDLE))
        assertTrue(states.contains(ConnectionState.LISTENING))
        assertTrue(states.contains(ConnectionState.CONNECTING))
        assertTrue(states.contains(ConnectionState.CONNECTED))
        assertTrue(states.contains(ConnectionState.DISCONNECTED))
        assertTrue(states.contains(ConnectionState.ERROR))
    }
    
    @Test
    fun testMessageTypes() {
        val types = MessageType.values()
        assertTrue(types.contains(MessageType.TEXT))
        assertTrue(types.contains(MessageType.GAME_MOVE))
        assertTrue(types.contains(MessageType.GAME_STATE))
        assertTrue(types.contains(MessageType.CONTROL))
        assertTrue(types.contains(MessageType.DATA))
    }
}
