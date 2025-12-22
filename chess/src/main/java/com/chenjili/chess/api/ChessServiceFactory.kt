package com.chenjili.chess.api

import com.chenjili.chess.inner.ChessService

object ChessServiceFactory {
    val chessService: IChessService = ChessService()
}