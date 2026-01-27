package com.chenjili.chessgame.pages.edit.ui

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chenjili.chess.api.Piece
import com.chenjili.chess.api.PieceColor
import com.chenjili.chess.api.PieceType
import com.chenjili.chessgame.R
import com.chenjili.chessgame.pages.edit.ui.theme.ChessGameTheme

@Composable
fun EditModeScreen(
    state: EditModeState = EditModeState(),
    onIntent: (EditModeIntent) -> Unit = { },
    paddingDp: Dp = 8.dp,
)
{
    ChessGameTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            BoxWithConstraints(modifier = Modifier.fillMaxSize()
                .padding(innerPadding)) {
                val maxW = this.maxWidth
                val maxH = this.maxHeight
                val squareSize = minOf(maxW, maxH) - paddingDp * 2f
                val density = LocalDensity.current
                val context = LocalContext.current
                val initialTopOffset = remember { paddingDp }
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
                    Box(modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .fillMaxWidth()
                        .padding(10.dp))
                    {
                        PiecesForEdit(
                            pieceColor = if(state.playerColor==PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE,
                            pieceSize = pieceSize,
                            pieceSpacing = pieceSpacing,
                            onIntent = onIntent
                        )
                    }
                    // 棋盘区
                    Box(
                        modifier = Modifier
                            .size(squareSize)
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

                                // 直接使用目标位置（不再动画）
                                val posX = targetX
                                val posY = targetY

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
                                            .offset(x = posX, y = posY)
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
                        val boardSizePx = with(density) { squareSize.toPx() }
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
                                            EditModeIntent.BoardCellClicked(
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
                    Box(modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .fillMaxWidth()
                        .padding(10.dp))
                    {
                        PiecesForEdit(
                            pieceColor = if(state.playerColor==PieceColor.WHITE) PieceColor.WHITE else PieceColor.BLACK,
                            pieceSize = pieceSize,
                            pieceSpacing = pieceSpacing,
                            onIntent = onIntent
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
                                onIntent(EditModeIntent.PlayerColorChanged(newColor))
                            }
                        ) {
                            Text(text = stringResource(id = R.string.switch_side))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PiecesForEdit(pieceColor: PieceColor, pieceSize: Dp, pieceSpacing: Dp, onIntent: (EditModeIntent) -> Unit = { }) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 顺序：兵 马 象 车 后 王 空
        val topPieces = listOf(
            Piece(PieceType.PAWN, pieceColor),
            Piece(PieceType.KNIGHT, pieceColor),
            Piece(PieceType.BISHOP, pieceColor),
            Piece(PieceType.ROOK, pieceColor),
            Piece(PieceType.QUEEN, pieceColor),
            Piece(PieceType.KING, pieceColor),
            null
        )
        topPieces.forEachIndexed { index, piece ->
            val resId = piece?.getDrawableId() ?: R.drawable.remove_piece
            if (resId != 0) {
                Image(
                    painter = painterResource(id = resId),
                    contentDescription = piece?.contentDescription() ?: "remove_piece",
                    modifier = Modifier
                        .size(pieceSize)
                        .padding(horizontal = pieceSpacing / 2)
                        .clickable {
                            onIntent(
                                EditModeIntent.PieceForEditClicked(piece==null, piece)
                            )
                        }
                )
            } else {
                // 占位（若资源缺失），用透明 Box 保持间距
                Box(modifier = Modifier.size(pieceSize))
            }
        }
    }
}

private fun Piece.getDrawableId(): Int {
    // 根据 Piece 的类型和颜色返回对应的 drawable 资源 ID
    if (this.color == PieceColor.WHITE) {
        return when (this.type) {
            PieceType.KING -> R.drawable.chess_piece_white_king
            PieceType.QUEEN -> R.drawable.chess_piece_white_queen
            PieceType.ROOK -> R.drawable.chess_piece_white_rook
            PieceType.BISHOP -> R.drawable.chess_piece_white_bishop
            PieceType.KNIGHT -> R.drawable.chess_piece_white_knight
            PieceType.PAWN -> R.drawable.chess_piece_white_pawn
        }
    } else {
        return when (this.type) {
            PieceType.KING -> R.drawable.chess_piece_black_king
            PieceType.QUEEN -> R.drawable.chess_piece_black_queen
            PieceType.ROOK -> R.drawable.chess_piece_black_rook
            PieceType.BISHOP -> R.drawable.chess_piece_black_bishop
            PieceType.KNIGHT -> R.drawable.chess_piece_black_knight
            PieceType.PAWN -> R.drawable.chess_piece_black_pawn
        }
    }
}

private fun Piece.contentDescription(): String = "${this.color}_${this.type}"