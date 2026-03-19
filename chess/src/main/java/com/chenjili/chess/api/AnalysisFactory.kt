package com.chenjili.chess.api

import com.chenjili.chess.inner.UciStockfishAnalyzer

object AnalysisFactory {
    fun createUciStockfishAnalyzer(command: List<String>): IPositionAnalyzer {
        return UciStockfishAnalyzer(command)
    }
}

