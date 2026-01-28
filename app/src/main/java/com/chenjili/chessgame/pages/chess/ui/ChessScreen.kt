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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import com.chenjili.chess.api.PieceColor
import com.chenjili.chess.api.PieceType
import com.chenjili.chessgame.R
import com.chenjili.chessgame.pages.chess.ui.theme.ChessGameTheme
import java.util.ArrayList


@Composable
fun PromotionDialog(
    pieceColor: PieceColor,
    onPieceSelected: (PieceType) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val showDialog = remember { mutableStateOf(true) }

    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer"
    )

    AnimatedVisibility(
        visible = showDialog.value,
        enter = fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.8f),
        exit = fadeOut(animationSpec = tween(200)) + scaleOut(targetScale = 0.8f)
    ) {
        Dialog(
            onDismissRequest = {
                showDialog.value = false
                onDismiss()
            },
            properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)
        ) {
            Box(
                modifier = Modifier
                    .shadow(16.dp, RoundedCornerShape(16.dp))
                    .background(
                        color = colorResource(R.color.walnut_medium),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .border(
                        BorderStroke(3.dp, colorResource(R.color.walnut_dark)),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "${stringResource(R.string.promotion)}🎉",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorResource(R.color.walnut_accent).copy(alpha = shimmerAlpha),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = stringResource(R.string.choose_promotion_piece),
                        fontSize = 16.sp,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    val choices = listOf(
                        PieceType.QUEEN to stringResource(R.string.queen),
                        PieceType.ROOK to stringResource(R.string.rook),
                        PieceType.BISHOP to stringResource(R.string.bishop),
                        PieceType.KNIGHT to stringResource(R.string.knight),
                    )

                    // 2 * 2 网格布局，避免超出弹窗
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        choices.chunked(2).forEach { rowItems ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                rowItems.forEach { (pieceType, name) ->
                                    PromotionPieceButton(
                                        pieceType = pieceType,
                                        pieceColor = pieceColor,
                                        name = name,
                                        onClick = {
                                            showDialog.value = false
                                            onPieceSelected(pieceType)
                                        }
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

@Composable
fun PromotionPieceButton(
    pieceType: PieceType,
    pieceColor: PieceColor,
    name: String,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val isPressed = remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed.value) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "buttonScale"
    )

    val typeName = when (pieceType) {
        PieceType.QUEEN -> "queen"
        PieceType.ROOK -> "rook"
        PieceType.BISHOP -> "bishop"
        PieceType.KNIGHT -> "knight"
        else -> "queen"
    }
    val colorName = if (pieceColor == PieceColor.WHITE) "white" else "black"
    val resName = "chess_piece_${colorName}_$typeName"
    val resId = context.resources.getIdentifier(resName, "drawable", context.packageName)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(120.dp) // 固定按钮宽度，2 列更稳
            .scale(scale)
            .clickable {
                isPressed.value = true
                onClick()
            }
            .background(
                color = colorResource(R.color.walnut_light),
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                BorderStroke(2.dp, colorResource(R.color.walnut_grain)),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 10.dp, vertical = 10.dp) // 收紧内边距，避免弹窗溢出
    ) {
        if (resId != 0) {
            Image(
                painter = painterResource(id = resId),
                contentDescription = "${colorName} ${typeName}",
                modifier = Modifier.size(48.dp) // 略缩小图标
            )
        }
        Text(
            text = name,
            fontSize = 12.sp,
            color = Color.White,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

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
                )
                {
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
                                    val pieceColor = move.move.piece.color
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
                
                // Show promotion dialog when there's a pending promotion
                state.pendingPromotion?.let { pendingPromotion ->
                    PromotionDialog(
                        pieceColor = pendingPromotion.pieceColor,
                        onPieceSelected = { pieceType ->
                            onIntent(ChessIntent.PromotionPieceSelected(pieceType))
                        },
                        onDismiss = {
                            onIntent(ChessIntent.PromotionCancelled)
                        }
                    )
                }
            }
        }
    }
}
