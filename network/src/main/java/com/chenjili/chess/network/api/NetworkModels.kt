package com.chenjili.chess.network.api

/**
 * Represents the state of a network connection
 */
enum class ConnectionState {
    IDLE,           // Not connected
    LISTENING,      // Server is listening for connections
    CONNECTING,     // Attempting to connect to remote
    CONNECTED,      // Successfully connected
    DISCONNECTED,   // Disconnected (after being connected)
    ERROR           // Error state
}

/**
 * Information about a network connection
 */
data class ConnectionInfo(
    val address: String,      // IP address
    val port: Int,            // Port number
    val isServer: Boolean,    // True if this device is the server
    val deviceName: String = "" // Optional device name
)

/**
 * Represents a message exchanged over the network
 */
data class NetworkMessage(
    val type: MessageType,
    val payload: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Types of network messages
 */
enum class MessageType {
    TEXT,           // Plain text message
    GAME_MOVE,      // Chess game move
    GAME_STATE,     // Game state update
    CONTROL,        // Control message (handshake, ping, etc.)
    DATA            // Generic data
}

/**
 * Result of a network operation
 */
sealed class NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>()
    data class Error(val message: String, val exception: Exception? = null) : NetworkResult<Nothing>()
}

/**
 * Callback for connection state changes
 */
interface ConnectionStateListener {
    fun onConnectionStateChanged(state: ConnectionState, info: ConnectionInfo?)
    fun onMessageReceived(message: NetworkMessage)
    fun onError(error: String, exception: Exception?)
}
