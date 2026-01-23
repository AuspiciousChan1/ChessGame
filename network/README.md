# Network Module

网络模块，支持在同一局域网或热点环境下建立设备间的长连接。

## Features

- 支持把本机当成 service，接收其他设备发起的长连接请求
- 支持发起连接到其他设备的长连接
- 双向消息传递
- 连接状态监控
- 支持局域网（LAN）连接
- 支持热点（Hotspot）连接

## Architecture

```
network/
├── api/
│   ├── INetworkService.kt          # 网络服务接口
│   ├── NetworkModels.kt            # 数据模型（连接状态、消息等）
│   └── NetworkServiceFactory.kt    # 工厂类
└── inner/
    └── NetworkService.kt           # 网络服务实现
```

## Quick Start

### Server Mode (接收连接)

```kotlin
val networkService = NetworkServiceFactory.networkService

val listener = object : ConnectionStateListener {
    override fun onConnectionStateChanged(state: ConnectionState, info: ConnectionInfo?) {
        // 处理连接状态变化
    }
    
    override fun onMessageReceived(message: NetworkMessage) {
        // 处理接收到的消息
    }
    
    override fun onError(error: String, exception: Exception?) {
        // 处理错误
    }
}

// 在端口 8888 上启动服务器
networkService.startServer(8888, listener)
```

### Client Mode (发起连接)

```kotlin
val networkService = NetworkServiceFactory.networkService

val listener = object : ConnectionStateListener {
    // ... 实现监听器方法
}

// 连接到服务器
networkService.connect("192.168.1.100", 8888, listener)

// 发送消息
val message = NetworkMessage(MessageType.TEXT, "Hello!")
networkService.sendMessage(message)
```

## API Documentation

### INetworkService

主要方法：

- `startServer(port: Int, listener: ConnectionStateListener): NetworkResult<Unit>`
  - 启动服务器模式，监听指定端口
  
- `stopServer()`
  - 停止服务器并关闭所有连接
  
- `connect(address: String, port: Int, listener: ConnectionStateListener): NetworkResult<Unit>`
  - 连接到远程设备
  
- `disconnect()`
  - 断开当前连接
  
- `sendMessage(message: NetworkMessage): NetworkResult<Unit>`
  - 发送消息
  
- `getConnectionState(): ConnectionState`
  - 获取当前连接状态
  
- `isConnected(): Boolean`
  - 检查是否已连接

### Data Models

#### ConnectionState
- `IDLE`: 空闲状态
- `LISTENING`: 监听中（服务器模式）
- `CONNECTING`: 连接中
- `CONNECTED`: 已连接
- `DISCONNECTED`: 已断开
- `ERROR`: 错误状态

#### MessageType
- `TEXT`: 文本消息
- `GAME_MOVE`: 游戏移动
- `GAME_STATE`: 游戏状态
- `CONTROL`: 控制消息
- `DATA`: 通用数据

## Usage Scenarios

### 局域网连接
1. 两台设备连接到同一 WiFi 网络
2. 服务器设备启动监听（如：192.168.1.100:8888）
3. 客户端设备连接到服务器 IP 和端口

### 热点连接
1. 设备 A 开启 WiFi 热点
2. 设备 B 连接到设备 A 的热点
3. 设备 A 启动服务器（通常 IP 为 192.168.43.1）
4. 设备 B 连接到设备 A

## Permissions

在 AndroidManifest.xml 中需要以下权限：

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
```

## Testing

运行单元测试：

```bash
./gradlew :network:test
```

## Example

详细使用示例请参考 [USAGE.md](USAGE.md)

## Thread Safety

网络服务实现是线程安全的：
- 所有网络操作在独立线程中执行
- 使用 `@Volatile` 和 `AtomicBoolean` 保证状态一致性
- 回调方法可能在后台线程中调用，UI 更新需要切换到主线程

## Error Handling

服务提供了完善的错误处理：
- 所有公共方法返回 `NetworkResult<T>` 表示操作结果
- 连接错误通过 `ConnectionStateListener.onError()` 回调
- 异常情况自动转换为错误状态
