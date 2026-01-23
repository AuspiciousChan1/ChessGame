package com.chenjili.chessgame.testtool.testpage.networkservice.ui

import android.app.Application
import android.os.Handler
import android.os.HandlerThread
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chenjili.chess.network.api.ConnectionInfo
import com.chenjili.chess.network.api.ConnectionState
import com.chenjili.chess.network.api.ConnectionStateListener
import com.chenjili.chess.network.api.NetworkMessage
import com.chenjili.chess.network.api.NetworkServiceFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class NetworkServiceTestApiType {
    START_SERVER,
    STOP_SERVER,
    CONNECT,
    DISCONNECT,
    SEND_MESSAGE,
    GET_CONNECTION_STATE,
    GET_CONNECTION_INFO,
    IS_CONNECTED,
}

data class NetworkServiceTestItem(
    val apiType: NetworkServiceTestApiType,
    val description: String
)

data class NetworkServiceTestState(
    val items: List<NetworkServiceTestItem> = emptyList()
)

sealed interface NetworkServiceTestIntent {
    data class CallApi(val apiType: NetworkServiceTestApiType): NetworkServiceTestIntent
}

class NetworkServiceTestViewModel(application: Application): AndroidViewModel(application) {
    private val _state = MutableStateFlow(NetworkServiceTestState())
    val state = _state.asStateFlow()

    private var workerThread: HandlerThread? = null
    private var workerHandler: Handler? = null

    init {
        viewModelScope.launch {
            val itemList = listOf(
                NetworkServiceTestItem(
                    NetworkServiceTestApiType.START_SERVER,
                    "Start TCP Server"
                ),
                NetworkServiceTestItem(
                    NetworkServiceTestApiType.STOP_SERVER,
                    "Stop TCP Server"
                ),
                NetworkServiceTestItem(
                    NetworkServiceTestApiType.CONNECT,
                    "Connect to Server"
                ),
                NetworkServiceTestItem(
                    NetworkServiceTestApiType.DISCONNECT,
                    "Disconnect from Server"
                ),
                NetworkServiceTestItem(
                    NetworkServiceTestApiType.SEND_MESSAGE,
                    "Send Message to Server"
                ),
                NetworkServiceTestItem(
                    NetworkServiceTestApiType.GET_CONNECTION_STATE,
                    "Get Connection State"
                ),
                NetworkServiceTestItem(
                    NetworkServiceTestApiType.GET_CONNECTION_INFO,
                    "Get Connection Info"
                ),
                NetworkServiceTestItem(
                    NetworkServiceTestApiType.IS_CONNECTED,
                    "Is Connected"
                ),
            )
            _state.value = NetworkServiceTestState(items = itemList)
        }
    }

    private fun ensureWorkerThread() {
        if (workerThread?.isAlive != true) {
            workerThread = HandlerThread("NetworkServiceWorker").apply { start() }
            workerHandler = Handler(workerThread!!.looper)
        }
    }

    private fun stopAndReleaseWorkerThreadSafely() {
        val handler = workerHandler
        val thread = workerThread
        if (handler == null || thread == null) return

        // 在 worker 线程上执行 stop/cleanup 并退出 looper
        handler.post {
            try {
                // 这里不指定具体操作，调用处会在 post 中执行实际 stop/disconnect
            } finally {
                // 请求安全退出 looper，随后清理引用
                thread.quitSafely()
            }
        }

        // 清理引用（可以立即置空，实际 looper 会在安全退出后停止）
        workerHandler = null
        workerThread = null
    }

    fun processIntent(intent: NetworkServiceTestIntent) {
        when (intent) {
            is NetworkServiceTestIntent.CallApi -> {
                callApi(intent.apiType)
            }
        }
    }

    private fun callApi(apiType: NetworkServiceTestApiType) {
        viewModelScope.launch {
            when (apiType) {
                NetworkServiceTestApiType.START_SERVER -> {
                    callApiStartServer()
                }
                NetworkServiceTestApiType.STOP_SERVER -> {
                    callApiStopServer()
                }
                NetworkServiceTestApiType.CONNECT -> {
                    callApiConnect()
                }
                NetworkServiceTestApiType.DISCONNECT -> {
                    callApiDisconnect()
                }
                NetworkServiceTestApiType.SEND_MESSAGE -> {
                    callApiSendMessage()
                }
                NetworkServiceTestApiType.GET_CONNECTION_STATE -> {
                    callApiGetConnectionState()
                }
                NetworkServiceTestApiType.GET_CONNECTION_INFO -> {
                    callApiGetConnectionInfo()
                }
                NetworkServiceTestApiType.IS_CONNECTED -> {
                    callApiIsConnected()
                }
            }
        }
    }
    private fun callApiStartServer() {
        ensureWorkerThread()
        workerHandler?.post {
            val result = NetworkServiceFactory.networkService.startServer(8080, object : ConnectionStateListener {
                override fun onConnectionStateChanged(state: ConnectionState, info: ConnectionInfo?) {
                    println("onConnectionStateChanged: $state, info: $info")
                }

                override fun onMessageReceived(message: NetworkMessage) {
                    println("onMessageReceived: ${message.payload}")
                }

                override fun onError(error: String, exception: Exception?) {
                    println("onError: $error")
                }
            })

            if (result is com.chenjili.chess.network.api.NetworkResult.Success) {
                println("Start server success: ${result.data}")
            } else if (result is com.chenjili.chess.network.api.NetworkResult.Error) {
                println("Start server failed: ${result.message} ${result.exception}")
            }
        }
    }

    private fun callApiStopServer() {
        // 在 worker 线程上先停止服务，再退出线程
        if (workerHandler != null && workerThread != null) {
            workerHandler?.post {
                try {
                    NetworkServiceFactory.networkService.stopServer()
                    println("Stop server called")
                } finally {
                    // 退出当前 worker looper
                    workerThread?.quitSafely()
                }
            }
            // 清理引用，looper 会在安全退出后结束
            workerHandler = null
            workerThread = null
        } else {
            // 如果线程不存在，直接调用（防御性）
            NetworkServiceFactory.networkService.stopServer()
            println("Stop server called (no worker thread)")
        }
    }

    private fun callApiConnect() {
        ensureWorkerThread()
        workerHandler?.post {
            val address = "10.17.195.193"
            val result = NetworkServiceFactory.networkService.connect(address, 8080, object : ConnectionStateListener {
                override fun onConnectionStateChanged(state: ConnectionState, info: ConnectionInfo?) {
                    println("onConnectionStateChanged: $state, info: $info")
                }

                override fun onMessageReceived(message: NetworkMessage) {
                    println("onMessageReceived: ${message.payload}")
                }

                override fun onError(error: String, exception: Exception?) {
                    println("onError: $error")
                }
            })

            if (result is com.chenjili.chess.network.api.NetworkResult.Success) {
                println("Connect success: ${result.data}")
            } else if (result is com.chenjili.chess.network.api.NetworkResult.Error) {
                println("Connect failed: ${result.message} ${result.exception}")
            }
        }
    }

    private fun callApiDisconnect() {
        // 在 worker 线程上断开并退出线程
        if (workerHandler != null && workerThread != null) {
            workerHandler?.post {
                try {
                    NetworkServiceFactory.networkService.disconnect()
                    println("Disconnect called")
                } finally {
                    workerThread?.quitSafely()
                }
            }
            workerHandler = null
            workerThread = null
        } else {
            NetworkServiceFactory.networkService.disconnect()
            println("Disconnect called (no worker thread)")
        }
    }

    private fun callApiSendMessage() {
        ensureWorkerThread()
        workerHandler?.post {
            val message = NetworkMessage(
                type = com.chenjili.chess.network.api.MessageType.TEXT,
                payload = "Hello from client"
            )
            NetworkServiceFactory.networkService.sendMessage(message)
        }
    }

    private fun callApiGetConnectionState() {
        println("callApiGetConnectionState")
    }

    private fun callApiGetConnectionInfo() {
        println("callApiGetConnectionInfo")
    }

    private fun callApiIsConnected() {
        println("callApiIsConnected")
    }

    override fun onCleared() {
        super.onCleared()
        // 确保 ViewModel 销毁时清理线程
        workerThread?.quitSafely()
        workerHandler = null
        workerThread = null
    }
}