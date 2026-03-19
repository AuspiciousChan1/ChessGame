package com.chenjili.chessgame.pages.chess.data

import android.content.Context
import android.system.Os
import java.io.File

class StockfishBinaryInstaller(
    private val context: Context,
) {
    data class InstalledBinary(
        val abi: String,
        val file: File,
    )

    fun installOrNull(resolved: StockfishAbiResolver.ResolvedEngineAsset?): InstalledBinary? {
        resolved ?: return null
        val targetDir = File(context.noBackupFilesDir, "engines/${resolved.abi}")
        val targetFile = File(targetDir, StockfishAbiResolver.ENGINE_FILE_NAME)
        val markerFile = File(targetDir, ".asset_path")

        return runCatching {
            targetDir.mkdirs()
            val needsCopy = !targetFile.exists() || targetFile.length() == 0L || markerFile.readTextOrNull() != resolved.assetPath
            if (needsCopy) {
                context.assets.open(resolved.assetPath).use { input ->
                    targetFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                markerFile.writeText(resolved.assetPath)
            }
            ensureExecutable(targetFile)
            InstalledBinary(abi = resolved.abi, file = targetFile)
        }.getOrNull()
    }

    private fun ensureExecutable(file: File) {
        file.setReadable(true, false)
        file.setExecutable(true, false)
        runCatching { Os.chmod(file.absolutePath, 0b111101101) }
    }

    private fun File.readTextOrNull(): String? = runCatching { readText() }.getOrNull()
}

