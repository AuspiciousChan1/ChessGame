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
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.chenjili.chessgame.R
import com.chenjili.chessgame.pages.chess.ui.theme.ChessGameTheme
import java.util.ArrayList

@Composable
fun ChessScreen(
    application: Application,
    paddingDp: Dp = 8.dp,
    playerColor: PlayerColor = PlayerColor.White,
    pieces: List<ChessPieceDisplay> = remember { ArrayList() },
    onBoardLayoutChanged: (x: Dp, y: Dp, width: Dp, height: Dp) -> Unit = { _, _, _, _ -> },
    onPlayerColorChanged: (PlayerColor) -> Unit = { _ -> }
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
                    Image(
                        painter = painterResource(id = R.drawable.chess_board_default),
                        contentDescription = "Chess board",
                        modifier = Modifier
                            .size(squareSize)
                            .align(Alignment.TopStart)
                            .rotate(if (playerColor == PlayerColor.Black) 180f else 0f)
                    )
//                    // 使用自定义 View 作为棋盘背景
//                    AndroidView(
//                        modifier = Modifier
//                            .size(squareSize)
//                            .align(Alignment.TopStart),
//                        factory = { ctx ->
//                            StrokeChessBoardView(ctx).apply {
//                                setPlayerColor(playerColor)
//                                startDraw() // 可根据需要移除或控制
//                            }
//                        },
//                        update = { view ->
//                            view.setPlayerColor(playerColor)
//                        }
//                    )
                    // 计算格子与棋子尺寸
                    val cellDp = squareSize / 8f
                    val pieceDp = cellDp * 0.8f
                    val pieceOffsetInner = (cellDp - pieceDp) / 2f

                    pieces.forEach { piece ->
                        val x = (cellDp * piece.column) + pieceOffsetInner
                        val y = (cellDp * (7 - piece.row)) + pieceOffsetInner

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
