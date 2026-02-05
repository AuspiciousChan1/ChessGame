package com.chenjili.chess.inner.data

import com.chenjili.chess.api.IChessGame
import com.chenjili.chess.inner.ChessGame
import java.util.Date

/**
 * 一个基于内存实现的 IChessRepository，用于快速开发和测试。
 * 注意：此实现不会将数据持久化，App关闭后数据会丢失。
 * 未来可以替换为基于 Room 数据库或文件的实现。
 */
class InMemoryChessRepository : IChessRepository {

    // 使用 Map 存储棋局，Key 是游戏ID，Value 是 PGN 字符串
    private val gameStorage = mutableMapOf<String, String>()
    private val gameMetadata = mutableMapOf<String, Pair<Date, Int>>()

    override suspend fun saveGame(game: IChessGame) {
        val pgn = game.exportPGN()
        gameStorage[game.id] = pgn
        gameMetadata[game.id] = Pair(Date(), game.getMoveHistory().size)
    }

    override suspend fun loadGame(id: String): IChessGame? {
        val pgn = gameStorage[id] ?: return null
        return try {
            val game = ChessGame(id)
            if (game.importPGN(pgn)) {
                game
            } else {
                // PGN 格式可能已损坏或无效
                null
            }
        } catch (e: Exception) {
            // 解析过程中出现异常
            null
        }
    }

    override suspend fun deleteGame(id: String): Boolean {
        gameMetadata.remove(id)
        return gameStorage.remove(id) != null
    }

    override suspend fun getAllGamesInfo(): List<GameInfo> {
        return gameStorage.keys.mapNotNull { id ->
            val pgn = gameStorage[id]
            val meta = gameMetadata[id]
            if (pgn != null && meta != null) {
                GameInfo(
                    id = id,
                    lastModified = meta.first,
                    moveCount = meta.second,
                    pgn = pgn
                )
            } else {
                null
            }
        }.sortedByDescending { it.lastModified } // 按最后修改时间降序排列
    }
}
