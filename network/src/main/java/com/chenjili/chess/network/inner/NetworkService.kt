package com.chenjili.chess.network.inner

import com.chenjili.chess.network.api.ConnectionInfo
import com.chenjili.chess.network.api.ConnectionState
import com.chenjili.chess.network.api.ConnectionStateListener
import com.chenjili.chess.network.api.INetworkService
import com.chenjili.chess.network.api.MessageType
import com.chenjili.chess.network.api.NetworkMessage
import com.chenjili.chess.network.api.NetworkResult
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
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
                    val localAddress = getLocalIpv4ByInterfaces() ?: InetAddress.getLocalHost().hostAddress ?: "unknown"

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
                        handleClientConnection(socket, true)
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

    fun getLocalIpv4ByInterfaces(): String? {
        try {
            val candidates = mutableListOf<InetAddress>()
            val nis = NetworkInterface.getNetworkInterfaces()
            while (nis.hasMoreElements()) {
                val ni = nis.nextElement()
                if (!ni.isUp || ni.isLoopback || ni.isVirtual) continue
                val addrs = ni.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (addr.isLoopbackAddress) continue
                    val host = addr.hostAddress ?: continue
                    if (host.contains(":")) continue // skip IPv6
                    candidates.add(addr)
                }
            }
            // 优先返回 site\-local（192.168/10/172.16-31）
            val siteLocal = candidates.firstOrNull { it.isSiteLocalAddress }
            return siteLocal?.hostAddress ?: candidates.firstOrNull()?.hostAddress
        } catch (e: Exception) {
            return null
        }
    }

    fun getLocalIpv4BySocket(remoteHost: String = "8.8.8.8", remotePort: Int = 53): String? {
        return try {
            DatagramSocket().use { socket ->
                // connect 不会实际发送数据，但会绑定到用于到达 remote 的本地地址
                socket.connect(InetSocketAddress(remoteHost, remotePort))
                val addr = socket.localAddress
                if (addr is Inet4Address) addr.hostAddress else null
            }
        } catch (e: Exception) {
            null
        }
    }

    override fun stopServer() {
        isRunning.set(false)
        try { serverSocket?.close() } catch (_: Exception) {}
        serverSocket = null

        // 若处于 CONNECTED，disconnect 会回 DISCONNECTED；否则直接回 IDLE
        safeCloseClient()
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
                    handleClientConnection(socket, false)
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
        safeCloseClient()
        updateConnectionState(ConnectionState.DISCONNECTED, null)
    }

    private fun safeCloseClient() {
        try { inputStream?.close() } catch (_: Exception) {}
        try { outputStream?.close() } catch (_: Exception) {}
        try { clientSocket?.close() } catch (_: Exception) {}
        inputStream = null
        outputStream = null
        clientSocket = null
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

    private fun handleClientConnection(socket: Socket, isServer: Boolean) {
        try {
            clientSocket = socket
            outputStream = DataOutputStream(BufferedOutputStream(socket.getOutputStream()))
            inputStream = DataInputStream(BufferedInputStream(socket.getInputStream()))

            val remoteIp = socket.inetAddress?.hostAddress ?: "unknown"
            val remotePort = socket.port
            val localIp = socket.localAddress?.hostAddress ?: "unknown"
            val localPort = socket.localPort

            // 若 ConnectionInfo 只能放一个 address/port，至少明确含义：
            // - 服务端：address/port 记录对端（remote），因为这是“已连接对象”
            // - LISTENING 时再单独上报本机可达地址
            val info = ConnectionInfo(remoteIp, remotePort, isServer)

            updateConnectionState(ConnectionState.CONNECTED, info)

            startReceivingMessages()
        } catch (e: Exception) {
            listener?.onError("Error setting up connection: ${e.message}", e)
            safeCloseClient()
            updateConnectionState(ConnectionState.ERROR, null)
            isRunning.set(false)
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