// kotlin
package com.chenjili.chessgame.pages.chess.ui

import android.app.Application
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.chenjili.chessgame.R
import com.chenjili.chessgame.pages.chess.ui.theme.ChessGameTheme
import kotlin.times

@Composable
fun ChessScreen(
    application: Application,
    paddingDp: Dp = 8.dp,
    playerColor: PlayerColor = PlayerColor.White,
    pieces: List<ChessPiece> = remember { initialPosition() },
    onBoardLayoutChanged: (x: Dp, y: Dp, width: Dp, height: Dp) -> Unit = { _, _, _, _ -> }
) {
    ChessGameTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                val maxW = this.maxWidth
                val maxH = this.maxHeight
                val squareSize = minOf(maxW, maxH) - paddingDp * 2f
                val density = LocalDensity.current
                val context = LocalContext.current

                Box(
                    modifier = Modifier
                        .size(squareSize)
                        .align(Alignment.Center)
                        .onGloballyPositioned { coordinates ->
                            val topLeft = coordinates.positionInWindow()
                            val sizePx = coordinates.size
                            with(density) {
                                val xDp = topLeft.x.toDp()
                                val yDp = topLeft.y.toDp()
                                val widthDp = sizePx.width.toDp()
                                val heightDp = sizePx.height.toDp()
                                onBoardLayoutChanged(xDp, yDp, widthDp, heightDp)
                            }
                        }
                ) {
                    // 棋盘背景图
//                    Image(
//                        painter = painterResource(id = R.drawable.chess_board_default),
//                        contentDescription = "Chess board",
//                        modifier = Modifier
//                            .size(squareSize)
//                            .align(Alignment.TopStart)
//                            .rotate(if (playerColor == PlayerColor.Black) 180f else 0f)
//                    )
                    // 使用自定义 View 作为棋盘背景
                    AndroidView(
                        modifier = Modifier
                            .size(squareSize)
                            .align(Alignment.TopStart),
                        factory = { ctx ->
                            StrokeChessBoardView(ctx).apply {
                                setPlayerColor(playerColor)
                                startDraw() // 可根据需要移除或控制
                            }
                        },
                        update = { view ->
                            view.setPlayerColor(playerColor)
                        }
                    )

                    // 计算格子与棋子尺寸
                    val cellDp = squareSize / 8f
                    val pieceDp = cellDp * 0.9f
                    val pieceOffsetInner = (cellDp - pieceDp) / 2f

                    // 遍历 pieces 并放置：file 0..7 从左到右（白方视角），rank 0..7 从底到顶
                    pieces.forEach { piece ->
                        val (fileForPos, rankForPos) = if (playerColor == PlayerColor.Black) {
                            (7 - piece.file) to (7 - piece.rank)
                        } else {
                            piece.file to piece.rank
                        }

                        val x = (cellDp * fileForPos) + pieceOffsetInner
                        val y = (cellDp * (7 - rankForPos)) + pieceOffsetInner

                        val typeName = when (piece.type) {
                            PieceType.KING -> "king"
                            PieceType.QUEEN -> "queen"
                            PieceType.ROOK -> "rook"
                            PieceType.BISHOP -> "bishop"
                            PieceType.KNIGHT -> "knight"
                            PieceType.PAWN -> "pawn"
                        }
                        val colorName = if (piece.color == PlayerColor.White) "white" else "black"
                        val resName = "chess_piece_${colorName}_$typeName"
                        val resId = context.resources.getIdentifier(resName, "drawable", context.packageName)

                        if (resId != 0) {
                            Image(
                                painter = painterResource(id = resId),
                                contentDescription = "${colorName}_$typeName",
                                modifier = Modifier
                                    .size(pieceDp)
                                    .align(Alignment.TopStart)
                                    .offset(x = x, y = y)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun initialPosition(): List<ChessPiece> {
    val pieces = mutableListOf<ChessPiece>()
    // 白方底线 rank = 0
    pieces += listOf(
        ChessPiece(PieceType.ROOK, PlayerColor.White, 0, 0),
        ChessPiece(PieceType.KNIGHT, PlayerColor.White, 1, 0),
        ChessPiece(PieceType.BISHOP, PlayerColor.White, 2, 0),
        ChessPiece(PieceType.QUEEN, PlayerColor.White, 3, 0),
        ChessPiece(PieceType.KING, PlayerColor.White, 4, 0),
        ChessPiece(PieceType.BISHOP, PlayerColor.White, 5, 0),
        ChessPiece(PieceType.KNIGHT, PlayerColor.White, 6, 0),
        ChessPiece(PieceType.ROOK, PlayerColor.White, 7, 0)
    )
    // 白兵 rank = 1
    for (f in 0..7) pieces += ChessPiece(PieceType.PAWN, PlayerColor.White, f, 1)

    // 黑兵 rank = 6
    for (f in 0..7) pieces += ChessPiece(PieceType.PAWN, PlayerColor.Black, f, 6)
    // 黑方底线 rank = 7
    pieces += listOf(
        ChessPiece(PieceType.ROOK, PlayerColor.Black, 0, 7),
        ChessPiece(PieceType.KNIGHT, PlayerColor.Black, 1, 7),
        ChessPiece(PieceType.BISHOP, PlayerColor.Black, 2, 7),
        ChessPiece(PieceType.QUEEN, PlayerColor.Black, 3, 7),
        ChessPiece(PieceType.KING, PlayerColor.Black, 4, 7),
        ChessPiece(PieceType.BISHOP, PlayerColor.Black, 5, 7),
        ChessPiece(PieceType.KNIGHT, PlayerColor.Black, 6, 7),
        ChessPiece(PieceType.ROOK, PlayerColor.Black, 7, 7)
    )
    return pieces
}
