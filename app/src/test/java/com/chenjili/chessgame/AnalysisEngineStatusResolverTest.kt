package com.chenjili.chessgame

import com.chenjili.chessgame.pages.chess.ui.AnalysisEngineStatus
import com.chenjili.chessgame.pages.chess.ui.AnalysisEngineStatusResolver
import org.junit.Assert.assertEquals
import org.junit.Test

class AnalysisEngineStatusResolverTest {

    @Test
    fun fromAnalyzerAvailable_returnsStockfishWhenAnalyzerExists() {
        assertEquals(
            AnalysisEngineStatus.STOCKFISH,
            AnalysisEngineStatusResolver.fromAnalyzerAvailable(true)
        )
    }

    @Test
    fun fromAnalyzerAvailable_returnsFallbackWhenAnalyzerMissing() {
        assertEquals(
            AnalysisEngineStatus.HEURISTIC_FALLBACK,
            AnalysisEngineStatusResolver.fromAnalyzerAvailable(false)
        )
    }
}

