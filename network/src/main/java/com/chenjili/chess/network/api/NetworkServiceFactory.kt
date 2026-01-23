package com.chenjili.chess.network.api

import com.chenjili.chess.network.inner.NetworkService

object NetworkServiceFactory {
    val networkService: INetworkService = NetworkService()
    
    /**
     * Create a new network service instance
     * Useful for testing or when multiple independent connections are needed
     */
    fun createNetworkService(): INetworkService {
        return NetworkService()
    }
}