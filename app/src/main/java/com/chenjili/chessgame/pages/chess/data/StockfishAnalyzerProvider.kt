package com.chenjili.chessgame.pages.chess.data

import android.content.Context
import android.util.Log
import com.chenjili.chess.api.AnalysisFactory
import com.chenjili.chess.api.IPositionAnalyzer
import java.io.File

class StockfishAnalyzerProvider(
    private val context: Context,
    private val abiResolver: StockfishAbiResolver = StockfishAbiResolver(),
    private val installer: StockfishBinaryInstaller = StockfishBinaryInstaller(context),
) {
    companion object {
        private const val TAG = "StockfishAnalyzer"
        private const val NATIVE_LIBRARY_ENGINE_FILE_NAME = "libstockfish.so"
    }

    fun createAnalyzerOrNull(): IPositionAnalyzer? {
        val resolved = abiResolver.resolve() ?: return null
        val commandPath = resolveCommandPath(resolved) ?: return null
        return AnalysisFactory.createUciStockfishAnalyzer(
            command = listOf(commandPath)
        )
    }

    private fun resolveCommandPath(resolved: StockfishAbiResolver.ResolvedEngineAsset): String? {
        resolveBundledNativeBinary()?.let { bundledBinary ->
            Log.d(TAG, "Using bundled Stockfish binary from nativeLibraryDir: ${bundledBinary.absolutePath}")
            return bundledBinary.absolutePath
        }

        val installed = installer.installOrNull(resolved)
        if (installed != null) {
            Log.w(
                TAG,
                "Ignoring extracted Stockfish binary from app storage because this device may mount app-private storage noexec: " +
                    installed.file.absolutePath
            )
        } else {
            Log.w(TAG, "No runnable bundled Stockfish binary found for ABI ${resolved.abi}; falling back to heuristic analyzer")
        }
        return null
    }

    private fun resolveBundledNativeBinary(): File? {
        val nativeLibraryDir = context.applicationInfo.nativeLibraryDir ?: return null
        val nativeBinary = File(nativeLibraryDir, NATIVE_LIBRARY_ENGINE_FILE_NAME)
        return nativeBinary.takeIf { it.exists() }
    }
}
