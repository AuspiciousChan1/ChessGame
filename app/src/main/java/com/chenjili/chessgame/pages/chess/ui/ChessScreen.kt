package com.chenjili.chessgame.pages.chess.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chenjili.chess.api.PieceColor
import com.chenjili.chess.api.PieceType
import com.chenjili.chessgame.R
import com.chenjili.chessgame.pages.chess.ui.theme.ChessGameTheme
import java.util.ArrayList

@Composable
fun ChessScreen(
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
                val initialTopOffset = remember { (maxH - squareSize) / 2f }
                val pieceSize = squareSize / 10f
                val pieceSpacing = 8.dp

                // 背景图
                Image(
                    painter = painterResource(id = R.drawable.bg_scholar_style),
                    contentDescription = null,
                    modifier = Modifier
                        .matchParentSize()      // 占满 BoxWithConstraints 的可用区域，作为背景
                        .align(Alignment.Center),
                    contentScale = ContentScale.Crop // 根据需要改为 Fit / FillBounds 等
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingDp)
                        .padding(top = initialTopOffset), // 固定顶部偏移，防止后续内容变化导致移动
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top // 改为从顶部开始布局
                ) {
                    // 编辑区：棋子预览行
                    Row(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 顺序：兵 马 象 车 后 王
                        val topPieces = listOf(
                            Pair("pawn", "pawn"),
                            Pair("knight", "knight"),
                            Pair("bishop", "bishop"),
                            Pair("rook", "rook"),
                            Pair("queen", "queen"),
                            Pair("king", "king")
                        )
                        topPieces.forEachIndexed { _, (_, typeName) ->
                            // 资源名与棋子渲染逻辑与棋盘内一致
                            val resName = if(state.playerColor==PieceColor.WHITE) {
                                "chess_piece_black_$typeName"
                            } else {
                                "chess_piece_white_$typeName"
                            }
                            val resId = context.resources.getIdentifier(resName, "drawable", context.packageName)
                            if (resId != 0) {
                                Image(
                                    painter = painterResource(id = resId),
                                    contentDescription = if(state.playerColor== PieceColor.WHITE)"black_$typeName" else "white_$typeName",
                                    modifier = Modifier
                                        .size(pieceSize)
                                        .padding(horizontal = pieceSpacing / 2)
                                        .clickable {
                                            // 空的点击事件（按要求）
                                        }
                                )
                            } else {
                                // 占位（若资源缺失），用透明 Box 保持间距
                                Box(modifier = Modifier.size(pieceSize))
                            }
                        }
                    }
                    // 棋盘区
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
                    )
                    {
                        // 棋盘背景图
                        Image(
                            painter = painterResource(id = R.drawable.chess_board_default),
                            contentDescription = "Chess board",
                            modifier = Modifier
                                .size(squareSize)
                                .align(Alignment.TopStart)
                                .rotate(if (state.playerColor == PieceColor.BLACK) 180f else 0f)
                        )
                        // 计算格子与棋子尺寸
                        val cellDp = squareSize / 8f
                        val pieceDp = cellDp * 0.8f
                        val pieceOffsetInner = (cellDp - pieceDp) / 2f

                        state.pieces.sortedBy { it.id }.forEach { pieceDisplay ->
                            key(pieceDisplay.id) {
                                val targetX = (cellDp * pieceDisplay.column) + pieceOffsetInner
                                val targetY = (cellDp * (7 - pieceDisplay.row)) + pieceOffsetInner

                                // Animate the position with spring animation
                                val animatedX by animateDpAsState(
                                    targetValue = targetX,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    ),
                                    label = "pieceX_${pieceDisplay.id}"
                                )

                                val animatedY by animateDpAsState(
                                    targetValue = targetY,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    ),
                                    label = "pieceY_${pieceDisplay.id}"
                                )

                                val typeName = when (pieceDisplay.piece.type) {
                                    PieceType.KING -> "king"
                                    PieceType.QUEEN -> "queen"
                                    PieceType.ROOK -> "rook"
                                    PieceType.BISHOP -> "bishop"
                                    PieceType.KNIGHT -> "knight"
                                    PieceType.PAWN -> "pawn"
                                }
                                val colorName = if (pieceDisplay.piece.color == PieceColor.WHITE) "white" else "black"
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
                                    .background(colorResource(R.color.chess_piece_selected_cell_overlay_color))
                            )
                        }

                        // 2) 透明点击层（放在最上面，覆盖整个棋盘）
                        val boardSizePx = with(LocalDensity.current) { squareSize.toPx() }
                        val cellSizePx = boardSizePx / 8f

                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .pointerInput(state.playerColor, boardSizePx) {
                                    detectTapGestures { tap: Offset ->
                                        val x = tap.x.coerceIn(0f, boardSizePx - 0.001f)
                                        val y = tap.y.coerceIn(0f, boardSizePx - 0.001f)

                                        val colFromLeft = (x / cellSizePx).toInt().coerceIn(0, 7)
                                        val rowFromTop = (y / cellSizePx).toInt().coerceIn(0, 7)

                                        // 你的绘制：y = cell * (7 - row)，所以 row = 7 - rowFromTop
                                        val column = colFromLeft
                                        val row = 7 - rowFromTop

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
                    // 编辑区：棋子预览行
                    Row(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    )
                    {
                        // 顺序：兵 马 象 车 后 王
                        val bottomPieces = listOf(
                            Pair("pawn", "pawn"),
                            Pair("knight", "knight"),
                            Pair("bishop", "bishop"),
                            Pair("rook", "rook"),
                            Pair("queen", "queen"),
                            Pair("king", "king")
                        )
                        bottomPieces.forEachIndexed { _, (_, typeName) ->
                            val resName = if(state.playerColor==PieceColor.WHITE) {
                                "chess_piece_white_$typeName"
                            } else {
                                "chess_piece_black_$typeName"
                            }
                            val resId = context.resources.getIdentifier(resName, "drawable", context.packageName)
                            if (resId != 0) {
                                Image(
                                    painter = painterResource(id = resId),
                                    contentDescription = if(state.playerColor==PieceColor.WHITE)"white_$typeName" else "black_$typeName",
                                    modifier = Modifier
                                        .size(pieceSize)
                                        .padding(horizontal = pieceSpacing / 2)
                                        .clickable {
                                            // 空的点击事件（按要求）
                                        }
                                )
                            } else {
                                Box(modifier = Modifier.size(pieceSize))
                            }
                        }
                    }
                    // 功能区：如切换阵营
                    Row(
                        modifier = Modifier
                            .size(squareSize, 48.dp)
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    )
                    {
                        Button(
                            onClick = {
                                val newColor =
                                    if (state.playerColor == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE
                                onIntent(ChessIntent.PlayerColorChanged(newColor))
                            }
                        ) {
                            Text(text = stringResource(id = R.string.switch_side))
                        }
                    }

                    // 棋谱区
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
                                        PieceColor.WHITE -> {
                                            if (whiteCache.isNotEmpty()) {
                                                // 上一步白棋未配对，补全黑棋为 "--"
                                                triples.add(Triple(moveNum, whiteCache, notationEmptyMove))
                                                ++moveNum
                                            }
                                            // 记录当前的白棋走子
                                            whiteCache = notation
                                        }
                                        PieceColor.BLACK -> {
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
