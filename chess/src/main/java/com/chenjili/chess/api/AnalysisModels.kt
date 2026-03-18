package com.chenjili.chess.api

/**
 * Configuration for requesting move analysis.
 */
data class AnalysisRequest(
    val maxRecommendations: Int = 3,
    val searchDepth: Int = 2,
)

enum class AnalysisSource {
    HEURISTIC,
    STOCKFISH_UCI,
}

data class RecommendedMove(
    val move: Move,
    val uci: String,
    val scoreCp: Int,
    val pv: List<String>,
)

data class AnalysisResult(
    val source: AnalysisSource,
    val fen: String,
    val recommendations: List<RecommendedMove>,
)

/**
 * Optional pluggable engine bridge for stronger analysis engines such as Stockfish.
 */
interface IPositionAnalyzer {
    fun analyze(fen: String, legalMoves: List<Move>, request: AnalysisRequest): AnalysisResult?
}

