package com.chenjili.chessgame.pages.chess.data

import android.content.Context
import com.chenjili.chess.api.AnalysisFactory
import com.chenjili.chess.api.IPositionAnalyzer

class StockfishAnalyzerProvider(
    private val context: Context,
    private val abiResolver: StockfishAbiResolver = StockfishAbiResolver(),
    private val installer: StockfishBinaryInstaller = StockfishBinaryInstaller(context),
) {
    fun createAnalyzerOrNull(): IPositionAnalyzer? {
        val resolved = abiResolver.resolve() ?: return null
        val installed = installer.installOrNull(resolved) ?: return null
        return AnalysisFactory.createUciStockfishAnalyzer(
            command = listOf(installed.file.absolutePath)
        )
    }
}

