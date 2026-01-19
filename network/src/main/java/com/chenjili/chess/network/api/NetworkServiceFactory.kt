package com.chenjili.chess.network.api

import com.chenjili.chess.network.inner.NetworkService

object NetworkServiceFactory {
    val networkService: INetworkService = NetworkService()
}