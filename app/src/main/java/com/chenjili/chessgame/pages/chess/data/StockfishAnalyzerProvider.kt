package com.chenjili.chessgame.pages.chess.data

import android.content.Context
import android.util.Log
import com.chenjili.chess.api.AnalysisFactory
import com.chenjili.chess.api.IPositionAnalyzer
import java.io.File

class StockfishAnalyzerProvider(
    private val context: Context,
) {
    companion object {
        private const val TAG = "StockfishAnalyzer"
        private const val NATIVE_LIBRARY_ENGINE_FILE_NAME = "libstockfish.so"
    }

    fun createAnalyzerOrNull(): IPositionAnalyzer? {
        val commandPath = resolveCommandPath() ?: return null
        return AnalysisFactory.createUciStockfishAnalyzer(
            command = listOf(commandPath)
        )
    }

    private fun resolveCommandPath(): String? {
        resolveBundledNativeBinary()?.let { bundledBinary ->
            Log.d(TAG, "Using bundled Stockfish binary from nativeLibraryDir: ${bundledBinary.absolutePath}")
            return bundledBinary.absolutePath
        }

        Log.i(TAG, "No bundled Stockfish native binary found; falling back to heuristic analyzer")
        return null
    }

    private fun resolveBundledNativeBinary(): File? {
        val nativeLibraryDir = context.applicationInfo.nativeLibraryDir ?: return null
        val nativeBinary = File(nativeLibraryDir, NATIVE_LIBRARY_ENGINE_FILE_NAME)
        return nativeBinary.takeIf { it.exists() && it.isFile }
    }
}
