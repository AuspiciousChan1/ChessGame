package com.chenjili.chessgame.pages.chess.data

import android.os.Build

class StockfishAbiResolver(
    private val supportedAbis: Array<String> = Build.SUPPORTED_ABIS,
) {
    companion object {
        const val ENGINE_ASSET_ROOT = "stockfish"
        const val ENGINE_FILE_NAME = "stockfish"

        val SUPPORTED_ABI_ASSET_PATHS: Map<String, String> = linkedMapOf(
            "arm64-v8a" to "$ENGINE_ASSET_ROOT/arm64-v8a/$ENGINE_FILE_NAME",
            "armeabi-v7a" to "$ENGINE_ASSET_ROOT/armeabi-v7a/$ENGINE_FILE_NAME",
        )
    }

    data class ResolvedEngineAsset(
        val abi: String,
        val assetPath: String,
    )

    fun resolve(): ResolvedEngineAsset? {
        val match = supportedAbis.firstNotNullOfOrNull { abi ->
            SUPPORTED_ABI_ASSET_PATHS[abi]?.let { assetPath ->
                ResolvedEngineAsset(abi = abi, assetPath = assetPath)
            }
        }
        return match
    }
}

