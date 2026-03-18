package com.chenjili.chess.inner

import com.chenjili.chess.api.AnalysisRequest
import com.chenjili.chess.api.AnalysisResult
import com.chenjili.chess.api.AnalysisSource
import com.chenjili.chess.api.IPositionAnalyzer
import com.chenjili.chess.api.Move
import com.chenjili.chess.api.PieceType
import com.chenjili.chess.api.RecommendedMove
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter

/**
 * Minimal UCI bridge for Stockfish-compatible engines.
 *
 * The host app is responsible for providing a runnable command, for example an extracted
 * Stockfish binary path plus any required arguments.
 */
class UciStockfishAnalyzer(
    private val command: List<String>,
) : IPositionAnalyzer {

    override fun analyze(fen: String, legalMoves: List<Move>, request: AnalysisRequest): AnalysisResult? {
        if (command.isEmpty()) return null

        val process = ProcessBuilder(command)
            .redirectErrorStream(true)
            .start()

        return process.useUci { reader, writer ->
            writer.send("uci")
            reader.waitForLine(prefix = "uciok") ?: return@useUci null

            writer.send("isready")
            reader.waitForLine(prefix = "readyok") ?: return@useUci null

            writer.send("setoption name MultiPV value ${request.maxRecommendations.coerceAtLeast(1)}")
            writer.send("position fen $fen")
            writer.send("go depth ${request.searchDepth.coerceAtLeast(1)}")

            val infoByMultipv = linkedMapOf<Int, EngineInfo>()
            while (true) {
                val line = reader.readLine() ?: break
                if (line.startsWith("bestmove ")) break
                if (!line.startsWith("info ")) continue

                val parsed = parseInfoLine(line) ?: continue
                infoByMultipv[parsed.multipv] = parsed
            }

            if (infoByMultipv.isEmpty()) return@useUci null

            val legalMovesByUci = legalMoves.associateBy { it.toUci() }
            val recommendations = infoByMultipv.entries
                .sortedBy { it.key }
                .mapNotNull { (_, info) ->
                    val firstMove = info.pv.firstOrNull() ?: return@mapNotNull null
                    val legalMove = legalMovesByUci[firstMove] ?: return@mapNotNull null
                    RecommendedMove(
                        move = legalMove,
                        uci = firstMove,
                        scoreCp = info.scoreCp,
                        pv = info.pv,
                    )
                }

            if (recommendations.isEmpty()) return@useUci null

            AnalysisResult(
                source = AnalysisSource.STOCKFISH_UCI,
                fen = fen,
                recommendations = recommendations,
            )
        }
    }

    private data class EngineInfo(
        val multipv: Int,
        val scoreCp: Int,
        val pv: List<String>,
    )

    private fun parseInfoLine(line: String): EngineInfo? {
        val tokens = line.split(' ')
        fun indexOf(token: String): Int = tokens.indexOf(token)

        val pvIndex = indexOf("pv")
        if (pvIndex < 0 || pvIndex + 1 >= tokens.size) return null

        val multipv = indexOf("multipv")
            .takeIf { it >= 0 && it + 1 < tokens.size }
            ?.let { tokens[it + 1].toIntOrNull() }
            ?: 1

        val scoreIndex = indexOf("score")
        val scoreCp = when {
            scoreIndex >= 0 && scoreIndex + 2 < tokens.size && tokens[scoreIndex + 1] == "cp" -> {
                tokens[scoreIndex + 2].toIntOrNull()
            }
            scoreIndex >= 0 && scoreIndex + 2 < tokens.size && tokens[scoreIndex + 1] == "mate" -> {
                val mateIn = tokens[scoreIndex + 2].toIntOrNull() ?: return null
                if (mateIn > 0) 100_000 - mateIn else -100_000 - mateIn
            }
            else -> null
        } ?: return null

        return EngineInfo(
            multipv = multipv,
            scoreCp = scoreCp,
            pv = tokens.drop(pvIndex + 1).filter { it.isNotBlank() },
        )
    }

    private inline fun <T> Process.useUci(block: (BufferedReader, BufferedWriter) -> T?): T? {
        val reader = BufferedReader(InputStreamReader(inputStream))
        val writer = BufferedWriter(OutputStreamWriter(outputStream))
        return try {
            block(reader, writer)
        } finally {
            runCatching {
                writer.send("quit")
                writer.flush()
            }
            runCatching { destroy() }
        }
    }

    private fun BufferedWriter.send(command: String) {
        write(command)
        newLine()
        flush()
    }

    private fun BufferedReader.waitForLine(prefix: String): String? {
        while (true) {
            val line = readLine() ?: return null
            if (line.startsWith(prefix)) return line
        }
    }

    private fun Move.toUci(): String {
        val promotionSuffix = when (promotionPiece) {
            PieceType.QUEEN -> "q"
            PieceType.ROOK -> "r"
            PieceType.BISHOP -> "b"
            PieceType.KNIGHT -> "n"
            else -> ""
        }
        return from.toAlgebraic() + to.toAlgebraic() + promotionSuffix
    }
}

