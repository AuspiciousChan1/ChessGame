package com.chenjili.chess.network.inner

import com.chenjili.chess.network.api.*
import java.io.*
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/**
 * Implementation of network service for long connections
 */
class NetworkService: INetworkService {
    
    @Volatile
    private var connectionState: ConnectionState = ConnectionState.IDLE
    
    @Volatile
    private var connectionInfo: ConnectionInfo? = null
    
    @Volatile
    private var listener: ConnectionStateListener? = null
    
    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var outputStream: DataOutputStream? = null
    private var inputStream: DataInputStream? = null
    
    private val isRunning = AtomicBoolean(false)
    
    override fun startServer(port: Int, listener: ConnectionStateListener): NetworkResult<Unit> {
        return try {
            if (connectionState != ConnectionState.IDLE) {
                return NetworkResult.Error("Service is already running or connected")
            }
            
            this.listener = listener
            
            isRunning.set(true)
            
            // Start server in a separate thread
            thread(name = "ServerThread") {
                try {
                    serverSocket = ServerSocket(port)
                    val localAddress = InetAddress.getLocalHost().hostAddress ?: "unknown"
                    
                    // Notify listener once with connection info
                    updateConnectionState(
                        ConnectionState.LISTENING,
                        ConnectionInfo(localAddress, port, true)
                    )
                    
                    // Accept one incoming connection for a paired long connection
                    // This design supports a 1:1 connection model as per requirements
                    // For multiple connections, create multiple NetworkService instances
                    val socket = serverSocket?.accept()
                    
                    if (socket != null && isRunning.get()) {
                        handleClientConnection(socket, port, true)
                    }
                } catch (e: SocketException) {
                    if (isRunning.get()) {
                        listener.onError("Server socket error: ${e.message}", e)
                        updateConnectionState(ConnectionState.ERROR, null)
                    }
                } catch (e: Exception) {
                    listener.onError("Server error: ${e.message}", e)
                    updateConnectionState(ConnectionState.ERROR, null)
                }
            }
            
            NetworkResult.Success(Unit)
        } catch (e: Exception) {
            NetworkResult.Error("Failed to start server: ${e.message}", e)
        }
    }
    
    override fun stopServer() {
        isRunning.set(false)
        
        try {
            serverSocket?.close()
            serverSocket = null
        } catch (e: Exception) {
            // Ignore
        }
        
        disconnect()
        updateConnectionState(ConnectionState.IDLE, null)
    }
    
    override fun connect(address: String, port: Int, listener: ConnectionStateListener): NetworkResult<Unit> {
        return try {
            if (connectionState != ConnectionState.IDLE) {
                return NetworkResult.Error("Service is already running or connected")
            }
            
            this.listener = listener
            updateConnectionState(ConnectionState.CONNECTING, null)
            
            isRunning.set(true)
            
            // Connect in a separate thread
            thread(name = "ClientThread") {
                try {
                    val socket = Socket(address, port)
                    handleClientConnection(socket, port, false)
                } catch (e: Exception) {
                    listener.onError("Connection error: ${e.message}", e)
                    updateConnectionState(ConnectionState.ERROR, null)
                    isRunning.set(false)
                }
            }
            
            NetworkResult.Success(Unit)
        } catch (e: Exception) {
            NetworkResult.Error("Failed to connect: ${e.message}", e)
        }
    }
    
    override fun disconnect() {
        isRunning.set(false)
        
        try {
            outputStream?.close()
            inputStream?.close()
            clientSocket?.close()
            
            outputStream = null
            inputStream = null
            clientSocket = null
            
            updateConnectionState(ConnectionState.DISCONNECTED, null)
        } catch (e: Exception) {
            listener?.onError("Error during disconnect: ${e.message}", e)
        }
    }
    
    override fun sendMessage(message: NetworkMessage): NetworkResult<Unit> {
        return try {
            if (connectionState != ConnectionState.CONNECTED) {
                return NetworkResult.Error("Not connected")
            }
            
            val output = outputStream ?: return NetworkResult.Error("Output stream not available")
            
            synchronized(output) {
                // Send message type
                output.writeInt(message.type.ordinal)
                // Send payload
                output.writeUTF(message.payload)
                // Send timestamp
                output.writeLong(message.timestamp)
                output.flush()
            }
            
            NetworkResult.Success(Unit)
        } catch (e: Exception) {
            listener?.onError("Failed to send message: ${e.message}", e)
            NetworkResult.Error("Failed to send message: ${e.message}", e)
        }
    }
    
    override fun getConnectionState(): ConnectionState {
        return connectionState
    }
    
    override fun getConnectionInfo(): ConnectionInfo? {
        return connectionInfo
    }
    
    override fun isConnected(): Boolean {
        return connectionState == ConnectionState.CONNECTED
    }
    
    // Private helper methods
    
    private fun handleClientConnection(socket: Socket, port: Int, isServer: Boolean) {
        try {
            clientSocket = socket
            outputStream = DataOutputStream(socket.getOutputStream())
            inputStream = DataInputStream(socket.getInputStream())
            
            val remoteAddress = socket.inetAddress.hostAddress ?: "unknown"
            val info = ConnectionInfo(remoteAddress, port, isServer)
            
            updateConnectionState(ConnectionState.CONNECTED, info)
            
            // Start receiving messages
            startReceivingMessages()
            
        } catch (e: Exception) {
            listener?.onError("Error setting up connection: ${e.message}", e)
            updateConnectionState(ConnectionState.ERROR, null)
        }
    }
    
    private fun startReceivingMessages() {
        thread(name = "ReceiveThread") {
            try {
                val input = inputStream ?: return@thread
                
                while (isRunning.get() && connectionState == ConnectionState.CONNECTED) {
                    try {
                        // Read message type
                        val typeOrdinal = input.readInt()
                        val type = MessageType.values().getOrNull(typeOrdinal) ?: MessageType.DATA
                        
                        // Read payload
                        val payload = input.readUTF()
                        
                        // Read timestamp
                        val timestamp = input.readLong()
                        
                        val message = NetworkMessage(type, payload, timestamp)
                        listener?.onMessageReceived(message)
                        
                    } catch (e: EOFException) {
                        // Connection closed
                        break
                    } catch (e: SocketException) {
                        if (isRunning.get()) {
                            listener?.onError("Connection lost: ${e.message}", e)
                        }
                        break
                    }
                }
            } catch (e: Exception) {
                if (isRunning.get()) {
                    listener?.onError("Error receiving messages: ${e.message}", e)
                }
            } finally {
                if (isRunning.get()) {
                    disconnect()
                }
            }
        }
    }
    
    private fun updateConnectionState(newState: ConnectionState, info: ConnectionInfo?) {
        connectionState = newState
        connectionInfo = info
        listener?.onConnectionStateChanged(newState, info)
    }
}