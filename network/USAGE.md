# Network Module Usage Examples

This document demonstrates how to use the network module for establishing long connections between devices.

## Overview

The network module supports:
- Starting a service to accept incoming connections (server mode)
- Connecting to a remote device (client mode)
- Sending and receiving messages
- Connection state monitoring

## Basic Usage

### Server Side (Device accepting connections)

```kotlin
import com.chenjili.chess.network.api.*

// Create a network service instance
val networkService = NetworkServiceFactory.networkService

// Create a connection state listener
val listener = object : ConnectionStateListener {
    override fun onConnectionStateChanged(state: ConnectionState, info: ConnectionInfo?) {
        when (state) {
            ConnectionState.LISTENING -> {
                println("Server is listening on ${info?.address}:${info?.port}")
            }
            ConnectionState.CONNECTED -> {
                println("Client connected from ${info?.address}")
            }
            ConnectionState.DISCONNECTED -> {
                println("Client disconnected")
            }
            else -> {}
        }
    }
    
    override fun onMessageReceived(message: NetworkMessage) {
        println("Received message: ${message.payload}")
        
        // Process message based on type
        when (message.type) {
            MessageType.TEXT -> handleTextMessage(message)
            MessageType.GAME_MOVE -> handleGameMove(message)
            MessageType.GAME_STATE -> handleGameState(message)
            else -> {}
        }
    }
    
    override fun onError(error: String, exception: Exception?) {
        println("Error: $error")
    }
}

// Start the server on port 8888
val result = networkService.startServer(8888, listener)
when (result) {
    is NetworkResult.Success -> println("Server started successfully")
    is NetworkResult.Error -> println("Failed to start server: ${result.message}")
}

// Send a message to connected client
if (networkService.isConnected()) {
    val message = NetworkMessage(MessageType.TEXT, "Hello from server!")
    networkService.sendMessage(message)
}

// Stop the server when done
networkService.stopServer()
```

### Client Side (Device initiating connection)

```kotlin
import com.chenjili.chess.network.api.*

// Create a network service instance
val networkService = NetworkServiceFactory.networkService

// Create a connection state listener
val listener = object : ConnectionStateListener {
    override fun onConnectionStateChanged(state: ConnectionState, info: ConnectionInfo?) {
        when (state) {
            ConnectionState.CONNECTING -> {
                println("Connecting to server...")
            }
            ConnectionState.CONNECTED -> {
                println("Connected to server at ${info?.address}:${info?.port}")
            }
            ConnectionState.DISCONNECTED -> {
                println("Disconnected from server")
            }
            else -> {}
        }
    }
    
    override fun onMessageReceived(message: NetworkMessage) {
        println("Received message: ${message.payload}")
        
        // Process message based on type
        when (message.type) {
            MessageType.TEXT -> handleTextMessage(message)
            MessageType.GAME_MOVE -> handleGameMove(message)
            MessageType.GAME_STATE -> handleGameState(message)
            else -> {}
        }
    }
    
    override fun onError(error: String, exception: Exception?) {
        println("Error: $error")
    }
}

// Connect to server at IP address 192.168.1.100 on port 8888
val result = networkService.connect("192.168.1.100", 8888, listener)
when (result) {
    is NetworkResult.Success -> println("Connection initiated")
    is NetworkResult.Error -> println("Failed to connect: ${result.message}")
}

// Send a message to server
if (networkService.isConnected()) {
    val message = NetworkMessage(MessageType.TEXT, "Hello from client!")
    networkService.sendMessage(message)
}

// Disconnect when done
networkService.disconnect()
```

## Android Integration

### Required Permissions

Add to your AndroidManifest.xml:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
```

### In Android Activity/Fragment

```kotlin
class GameActivity : AppCompatActivity() {
    private lateinit var networkService: INetworkService
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        networkService = NetworkServiceFactory.networkService
        
        // Check network permission before using
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.INTERNET
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startNetworkService()
        } else {
            // Request permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.INTERNET),
                PERMISSION_REQUEST_CODE
            )
        }
    }
    
    private fun startNetworkService() {
        val listener = object : ConnectionStateListener {
            override fun onConnectionStateChanged(state: ConnectionState, info: ConnectionInfo?) {
                runOnUiThread {
                    // Update UI based on connection state
                    updateConnectionStatus(state)
                }
            }
            
            override fun onMessageReceived(message: NetworkMessage) {
                runOnUiThread {
                    // Process message on UI thread
                    handleMessage(message)
                }
            }
            
            override fun onError(error: String, exception: Exception?) {
                runOnUiThread {
                    Toast.makeText(this@GameActivity, error, Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        // Start as server or client based on user choice
        if (isServerMode) {
            networkService.startServer(8888, listener)
        } else {
            networkService.connect(serverIpAddress, 8888, listener)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        networkService.disconnect()
        networkService.stopServer()
    }
}
```

## LAN and Hotspot Scenarios

### Same LAN
When both devices are connected to the same WiFi network:
1. Server device starts listening on a port (e.g., 8888)
2. Find the server's IP address (e.g., 192.168.1.100)
3. Client device connects to server IP and port

### Hotspot Mode
When one device creates a WiFi hotspot:
1. Device A enables WiFi hotspot (becomes 192.168.43.1 typically)
2. Device B connects to Device A's hotspot
3. Device A starts server on port 8888
4. Device B connects to 192.168.43.1:8888

## Message Types

- `MessageType.TEXT`: Plain text messages
- `MessageType.GAME_MOVE`: Chess game moves (e.g., "e2e4")
- `MessageType.GAME_STATE`: Game state updates
- `MessageType.CONTROL`: Control messages (handshake, ping, etc.)
- `MessageType.DATA`: Generic data

## Best Practices

1. Always check connection state before sending messages
2. Handle connection errors gracefully
3. Clean up resources (disconnect/stop server) when done
4. Run network operations on background threads (already handled internally)
5. Update UI on the main thread when receiving messages
6. Use appropriate message types for different data
