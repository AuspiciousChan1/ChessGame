// kotlin
package com.chenjili.chessgame.pages.chess.ui

import android.app.Application
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.chenjili.chessgame.R
import com.chenjili.chessgame.pages.chess.ui.theme.ChessGameTheme
import java.util.ArrayList
import kotlin.div
import kotlin.text.toInt

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

                        // Create a map to track animated positions for each piece
                        val piecePositions = remember { mutableStateMapOf<String, Animatable<Offset, *>>() }

                        state.pieces.forEach { piece ->
                            val targetX = (cellDp * piece.column) + pieceOffsetInner
                            val targetY = (cellDp * (7 - piece.row)) + pieceOffsetInner
                            val targetOffset = Offset(targetX.value, targetY.value)
                            
                            // Use piece ID as the key for animation tracking
                            val pieceKey = piece.id
                            
                            // Get or create animation for this piece position
                            val animatedOffset = piecePositions.getOrPut(pieceKey) {
                                Animatable(targetOffset, Offset.VectorConverter)
                            }

                            // Animate to the target position when it changes
                            LaunchedEffect(targetOffset) {
                                animatedOffset.animateTo(
                                    targetValue = targetOffset,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                )
                            }

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
                                val currentPosition = animatedOffset.value
                                Image(
                                    painter = painterResource(id = resId),
                                    contentDescription = "${colorName}_$typeName",
                                    modifier = Modifier
                                        .size(pieceDp)
                                        .align(Alignment.TopStart)
                                        .offset(x = with(density) { currentPosition.x.toDp() }, 
                                               y = with(density) { currentPosition.y.toDp() })
                                )
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
                                        // tap 是相对该 Box（棋盘左上角）的坐标，单位 px
                                        val x = tap.x.coerceIn(0f, boardSizePx - 0.001f)
                                        val y = tap.y.coerceIn(0f, boardSizePx - 0.001f)

                                        val colFromLeft = (x / cellSizePx).toInt().coerceIn(0, 7)
                                        val rowFromTop = (y / cellSizePx).toInt().coerceIn(0, 7)

                                        // 你的绘制：y = cell * (7 - row)，所以 row = 7 - rowFromTop
                                        var column = colFromLeft
                                        var row = 7 - rowFromTop

                                        // 若玩家视角为黑方，你对棋盘做了 rotate(180)，坐标也需要镜像
                                        if (state.playerColor == PlayerColor.Black) {
                                            column = 7 - column
                                            row = 7 - row
                                        }

                                        onIntent(ChessIntent.BoardCellClicked(column, row))
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
                    
                    // Move history area (棋谱区域)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .height(150.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFF5F5DC)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "棋谱",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            
                            val listState = rememberLazyListState()
                            
                            // Auto-scroll to the bottom when new moves are added
                            LaunchedEffect(state.moveHistory.size) {
                                if (state.moveHistory.isNotEmpty()) {
                                    listState.animateScrollToItem(state.moveHistory.size - 1)
                                }
                            }
                            
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(state.moveHistory) { move ->
                                    Text(
                                        text = move,
                                        fontSize = 14.sp,
                                        modifier = Modifier.padding(vertical = 2.dp)
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
