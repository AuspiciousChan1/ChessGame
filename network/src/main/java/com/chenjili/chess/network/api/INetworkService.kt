package com.chenjili.chess.network.api

import com.chenjili.chess.base.api.model.IService

/**
 * Network service interface for establishing and managing long connections
 * between devices in the same LAN or via hotspot
 */
interface INetworkService: IService {
    
    /**
     * Start the service to listen for incoming connections (server mode)
     * @param port Port to listen on
     * @param listener Callback for connection events
     * @return Result indicating success or failure
     */
    fun startServer(port: Int, listener: ConnectionStateListener): NetworkResult<Unit>
    
    /**
     * Stop the server and close all connections
     */
    fun stopServer()
    
    /**
     * Connect to a remote device (client mode)
     * @param address IP address of the remote device
     * @param port Port to connect to
     * @param listener Callback for connection events
     * @return Result indicating success or failure
     */
    fun connect(address: String, port: Int, listener: ConnectionStateListener): NetworkResult<Unit>
    
    /**
     * Disconnect from the current connection
     */
    fun disconnect()
    
    /**
     * Send a message over the connection
     * @param message Message to send
     * @return Result indicating success or failure
     */
    fun sendMessage(message: NetworkMessage): NetworkResult<Unit>
    
    /**
     * Get the current connection state
     * @return Current connection state
     */
    fun getConnectionState(): ConnectionState
    
    /**
     * Get current connection information
     * @return Connection info if connected, null otherwise
     */
    fun getConnectionInfo(): ConnectionInfo?
    
    /**
     * Check if currently connected
     * @return true if connected, false otherwise
     */
    fun isConnected(): Boolean
}