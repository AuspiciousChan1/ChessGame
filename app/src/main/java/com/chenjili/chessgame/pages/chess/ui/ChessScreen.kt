// kotlin
package com.chenjili.chessgame.pages.chess.ui

import android.app.Application
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chenjili.chessgame.R
import com.chenjili.chessgame.pages.chess.ui.theme.ChessGameTheme
import java.util.ArrayList

// Constants for UI
private val SelectedCellOverlayColor = Color(0x8000FF00) // Semi-transparent light green

@Composable
fun ChessScreen(
    application: Application,
    paddingDp: Dp = 8.dp,
    state: ChessState = ChessState(),
    onBoardLayoutChanged: (x: Dp, y: Dp, width: Dp, height: Dp) -> Unit = { _, _, _, _ -> },
    onIntent: (ChessIntent) -> Unit = { }
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

                Column(modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingDp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(squareSize)
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
                                .rotate(if (state.playerColor == PlayerColor.Black) 180f else 0f)
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

                        state.pieces.sortedBy { it.id }.forEach { piece ->
                            key(piece.id) {
                                val targetX = (cellDp * piece.column) + pieceOffsetInner
                                val targetY = (cellDp * (7 - piece.row)) + pieceOffsetInner

                                // Animate the position with spring animation
                                val animatedX by animateDpAsState(
                                    targetValue = targetX,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    ),
                                    label = "pieceX_${piece.id}"
                                )

                                val animatedY by animateDpAsState(
                                    targetValue = targetY,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    ),
                                    label = "pieceY_${piece.id}"
                                )

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
                                            .offset(x = animatedX, y = animatedY)
                                    )
                                }
                            }
                        }

                        // Render semi-transparent light green overlay on selected cell
                        state.selectedCell?.let { (selectedColumn, selectedRow) ->
                            val overlayX = cellDp * selectedColumn
                            val overlayY = cellDp * (7 - selectedRow)
                            
                            Box(
                                modifier = Modifier
                                    .size(cellDp)
                                    .align(Alignment.TopStart)
                                    .offset(x = overlayX, y = overlayY)
                                    .background(SelectedCellOverlayColor)
                            )
                        }

                        // 2) 透明点击层（放在最上面，覆盖整个棋盘）
                        val boardSizePx = with(LocalDensity.current) { squareSize.toPx() }
                        val cellSizePx = boardSizePx / 8f

                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .matchParentSize()
                                .pointerInput(state.playerColor, boardSizePx) {
                                    detectTapGestures { tap: Offset ->
                                        val x = tap.x.coerceIn(0f, boardSizePx - 0.001f)
                                        val y = tap.y.coerceIn(0f, boardSizePx - 0.001f)

                                        val colFromLeft = (x / cellSizePx).toInt().coerceIn(0, 7)
                                        val rowFromTop = (y / cellSizePx).toInt().coerceIn(0, 7)

                                        // 你的绘制：y = cell * (7 - row)，所以 row = 7 - rowFromTop
                                        var column = colFromLeft
                                        var row = 7 - rowFromTop

                                        onIntent(
                                            ChessIntent.BoardCellClicked(
                                                column,
                                                row,
                                                state.playerColor
                                            )
                                        )
                                    }
                                }
                        )
                    }
                    Row(
                        modifier = Modifier
                            .size(squareSize, 48.dp)
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                val newColor =
                                    if (state.playerColor == PlayerColor.White) PlayerColor.Black else PlayerColor.White
                                onIntent(ChessIntent.PlayerColorChanged(newColor))
                            }
                        ) {
                            Text(text = stringResource(id = R.string.switch_side))
                        }
                    }
                    
                    // Chess move history section
                    if (state.moveHistory.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .size(squareSize, 200.dp)
                                .padding(top = 8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.game_record),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )

                            // 预处理：把 moveHistory 的 notation 按两步一组转换为 (moveNumber, white, black)
                            val notationEmptyMove = stringResource(R.string.notation_empty_move)
                            val movePairs = remember(state.moveHistory) {
                                val moves = state.moveHistory
                                val triples = ArrayList<Triple<Int, String, String>>()
                                var moveNum = 1
                                var whiteCache: String = ""
                                for (move in moves) {
                                    val notation = move.notation
                                    val pieceColor = move.pieceColor
                                    when(pieceColor) {
                                        PlayerColor.White -> {
                                            if (whiteCache.isNotEmpty()) {
                                                // 上一步白棋未配对，补全黑棋为 "--"
                                                triples.add(Triple(moveNum, whiteCache, notationEmptyMove))
                                                ++moveNum
                                            }
                                            // 记录当前的白棋走子
                                            whiteCache = notation
                                        }
                                        PlayerColor.Black -> {
                                            val whiteNotation = if (whiteCache.isNotEmpty()) whiteCache else notationEmptyMove
                                            triples.add(Triple(moveNum, whiteNotation, notation))
                                            whiteCache = ""
                                            ++moveNum
                                        }
                                    }
                                }
                                if (whiteCache.isNotEmpty()) {
                                    // 最后一步是白棋，补全黑棋为 "--"
                                    triples.add(Triple(moveNum, whiteCache, notationEmptyMove))
                                }
                                triples
                            }

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .background(Color(0xFFF5F5F5))
                                    .padding(8.dp),
                                state = rememberLazyListState()
                            ) {
                                items(movePairs) { (num, whiteMove, blackMove) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // 序号列
                                        Text(
                                            text = "$num.",
                                            modifier = Modifier
                                                .weight(0.15f)
                                                .padding(start = 4.dp),
                                        )

                                        // 白棋列（中间）
                                        Text(
                                            text = whiteMove,
                                            modifier = Modifier
                                                .weight(0.425f)
                                                .padding(start = 8.dp),
                                        )

                                        // 黑棋列（右）
                                        Text(
                                            text = blackMove,
                                            modifier = Modifier
                                                .weight(0.425f)
                                                .padding(start = 8.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
