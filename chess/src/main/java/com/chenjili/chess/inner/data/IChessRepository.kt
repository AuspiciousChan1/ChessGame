package com.chenjili.chess.inner.data

import com.chenjili.chess.api.IChessGame
import java.util.Date

/**
 * 游戏信息概览，用于列表展示
 * @param id 游戏唯一ID
 * @param lastModified 最后修改日期
 * @param moveCount 总步数
 * @param pgn 完整的PGN格式棋谱
 */
data class GameInfo(
    val id: String,
    val lastModified: Date,
    val moveCount: Int,
    val pgn: String
)

/**
 * 国际象棋棋局存储仓库接口
 */
interface IChessRepository {
    /**
     * 保存一个棋局（新增或更新）
     * @param game 要保存的棋局
     */
    suspend fun saveGame(game: IChessGame)

    /**
     * 根据ID加载一个完整的棋局
     * @param id 游戏唯一ID
     * @return 如果找到，则返回 IChessGame 实例；否则返回 null
     */
    suspend fun loadGame(id: String): IChessGame?

    /**
     * 删除一个棋局
     * @param id 要删除的游戏的唯一ID
     * @return 如果删除成功，则返回 true
     */
    suspend fun deleteGame(id: String): Boolean

    /**
     * 获取所有已保存游戏的信息概览列表
     * @return 游戏信息列表
     */
    suspend fun getAllGamesInfo(): List<GameInfo>
}