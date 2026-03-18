package com.chenjili.chessgame.pages.edit.ui

import android.widget.Toast
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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chenjili.chess.api.Piece
import com.chenjili.chess.api.PieceColor
import com.chenjili.chess.api.PieceType
import com.chenjili.chessgame.R
import com.chenjili.chessgame.pages.edit.ui.theme.ChessGameTheme

@Composable
private fun FenExportDialog(
    draft: FenExportDraft,
    onIntent: (EditModeIntent) -> Unit,
) {
    val validationTextRes = when (draft.validationError) {
        FenExportValidationError.INVALID_EN_PASSANT -> R.string.fen_export_error_invalid_en_passant
        FenExportValidationError.INVALID_HALF_MOVE_CLOCK -> R.string.fen_export_error_invalid_halfmove
        FenExportValidationError.INVALID_FULL_MOVE_NUMBER -> R.string.fen_export_error_invalid_fullmove
        null -> null
    }

    AlertDialog(
        onDismissRequest = { onIntent(EditModeIntent.DismissFenExportDialog) },
        title = {
            Text(text = stringResource(R.string.fen_export_dialog_title))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = stringResource(R.string.fen_export_active_color))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            onIntent(EditModeIntent.FenExportActiveColorChanged(PieceColor.WHITE))
                        }
                    ) {
                        RadioButton(
                            selected = draft.activeColor == PieceColor.WHITE,
                            onClick = { onIntent(EditModeIntent.FenExportActiveColorChanged(PieceColor.WHITE)) }
                        )
                        Text(text = stringResource(R.string.white_side))
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            onIntent(EditModeIntent.FenExportActiveColorChanged(PieceColor.BLACK))
                        }
                    ) {
                        RadioButton(
                            selected = draft.activeColor == PieceColor.BLACK,
                            onClick = { onIntent(EditModeIntent.FenExportActiveColorChanged(PieceColor.BLACK)) }
                        )
                        Text(text = stringResource(R.string.black_side))
                    }
                }

                Text(text = stringResource(R.string.fen_export_castling_rights))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = draft.whiteKingsideCastling,
                            onCheckedChange = { onIntent(EditModeIntent.FenExportCastlingChanged(FenCastlingSlot.WHITE_KINGSIDE, it)) }
                        )
                        Text(text = stringResource(R.string.fen_export_castling_white_kingside))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = draft.whiteQueensideCastling,
                            onCheckedChange = { onIntent(EditModeIntent.FenExportCastlingChanged(FenCastlingSlot.WHITE_QUEENSIDE, it)) }
                        )
                        Text(text = stringResource(R.string.fen_export_castling_white_queenside))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = draft.blackKingsideCastling,
                            onCheckedChange = { onIntent(EditModeIntent.FenExportCastlingChanged(FenCastlingSlot.BLACK_KINGSIDE, it)) }
                        )
                        Text(text = stringResource(R.string.fen_export_castling_black_kingside))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = draft.blackQueensideCastling,
                            onCheckedChange = { onIntent(EditModeIntent.FenExportCastlingChanged(FenCastlingSlot.BLACK_QUEENSIDE, it)) }
                        )
                        Text(text = stringResource(R.string.fen_export_castling_black_queenside))
                    }
                }

                OutlinedTextField(
                    value = draft.enPassantTargetInput,
                    onValueChange = { onIntent(EditModeIntent.FenExportEnPassantChanged(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.fen_export_en_passant)) },
                    placeholder = { Text(stringResource(R.string.fen_export_en_passant_hint)) },
                    isError = draft.validationError == FenExportValidationError.INVALID_EN_PASSANT,
                )
                OutlinedTextField(
                    value = draft.halfMoveClockInput,
                    onValueChange = { onIntent(EditModeIntent.FenExportHalfMoveChanged(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.fen_export_halfmove)) },
                    isError = draft.validationError == FenExportValidationError.INVALID_HALF_MOVE_CLOCK,
                )
                OutlinedTextField(
                    value = draft.fullMoveNumberInput,
                    onValueChange = { onIntent(EditModeIntent.FenExportFullMoveChanged(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.fen_export_fullmove)) },
                    isError = draft.validationError == FenExportValidationError.INVALID_FULL_MOVE_NUMBER,
                )

                validationTextRes?.let { textRes ->
                    Text(
                        text = stringResource(textRes),
                        color = Color(0xFFB00020)
                    )
                }

                Text(text = stringResource(R.string.fen_export_preview))
                SelectionContainer {
                    Text(
                        text = draft.previewFen.ifBlank { stringResource(R.string.fen_export_preview_invalid_placeholder) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.12f))
                            .padding(10.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onIntent(EditModeIntent.ConfirmFenExport) },
                enabled = draft.canConfirm,
            ) {
                Text(text = stringResource(R.string.copy_fen))
            }
        },
        dismissButton = {
            TextButton(onClick = { onIntent(EditModeIntent.DismissFenExportDialog) }) {
                Text(text = stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun EditModeScreen(
    state: EditModeState = EditModeState(),
    onIntent: (EditModeIntent) -> Unit = { },
    paddingDp: Dp = 8.dp,
)
{
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    LaunchedEffect(state.pendingFenToCopy) {
        state.pendingFenToCopy?.let { fen ->
            clipboardManager.setText(AnnotatedString(fen))
            Toast.makeText(
                context,
                context.getString(R.string.fen_copied_to_clipboard),
                Toast.LENGTH_SHORT
            ).show()
            onIntent(EditModeIntent.ExportFenHandled)
        }
    }

    ChessGameTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            BoxWithConstraints(modifier = Modifier.fillMaxSize()
                .padding(innerPadding)) {
                val maxW = this.maxWidth
                val maxH = this.maxHeight
                val squareSize = minOf(maxW, maxH) - paddingDp * 2f
                val density = LocalDensity.current
                val initialTopOffset = remember { paddingDp }
                val pieceSize = squareSize / 10f
                val pieceSpacing = 8.dp

                Image(
                    painter = painterResource(id = R.drawable.bg_scholar_style),
                    contentDescription = null,
                    modifier = Modifier
                        .matchParentSize()
                        .align(Alignment.Center),
                    contentScale = ContentScale.Crop
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingDp)
                        .padding(top = initialTopOffset),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    Box(modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .fillMaxWidth()
                        .padding(10.dp))
                    {
                        val selectedId: Int = if (state.selectedPiece != null && state.selectedCell == null) {
                            state.selectedPiece.getDrawableId()
                        } else if (state.selectedPiece == null && state.selectedCell == null && state.editType == EditType.REMOVE) {
                            R.drawable.remove_piece
                        }else {
                            0
                        }
                        PiecesForEdit(
                            pieceColor = if(state.playerColor==PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE,
                            pieceSize = pieceSize,
                            pieceSpacing = pieceSpacing,
                            selectedId = selectedId,
                            onIntent = onIntent
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(squareSize)
                    )
                    {
                        Image(
                            painter = painterResource(id = R.drawable.chess_board_default),
                            contentDescription = "Chess board",
                            modifier = Modifier
                                .size(squareSize)
                                .align(Alignment.TopStart)
                                .rotate(if (state.playerColor == PieceColor.BLACK) 180f else 0f)
                        )
                        val cellDp = squareSize / 8f
                        val pieceDp = cellDp * 0.8f
                        val pieceOffsetInner = (cellDp - pieceDp) / 2f

                        state.pieces.sortedBy { it.id }.forEach { pieceDisplay ->
                            key(pieceDisplay.id) {
                                val targetX = (cellDp * pieceDisplay.column) + pieceOffsetInner
                                val targetY = (cellDp * (7 - pieceDisplay.row)) + pieceOffsetInner
                                val posX = targetX
                                val posY = targetY

                                val resId = pieceDisplay.piece.getDrawableId()
                                Image(
                                    painter = painterResource(id = resId),
                                    contentDescription = pieceDisplay.piece.contentDescription(),
                                    modifier = Modifier
                                        .size(pieceDp)
                                        .align(Alignment.TopStart)
                                        .offset(x = posX, y = posY)
                                )
                            }
                        }

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
                    Box(modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .fillMaxWidth()
                        .padding(10.dp))
                    {
                        val selectedId: Int = if (state.selectedPiece != null && state.selectedCell == null) {
                            state.selectedPiece.getDrawableId()
                        } else if (state.selectedPiece == null && state.selectedCell == null && state.editType == EditType.REMOVE) {
                            R.drawable.remove_piece
                        }else {
                            0
                        }
                        PiecesForEdit(
                            pieceColor = if(state.playerColor==PieceColor.WHITE) PieceColor.WHITE else PieceColor.BLACK,
                            pieceSize = pieceSize,
                            pieceSpacing = pieceSpacing,
                            selectedId = selectedId,
                            onIntent = onIntent
                        )
                    }
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
                        Button(
                            onClick = {
                                onIntent(EditModeIntent.ClearBoard)
                            }
                        ) {
                            Text(text = stringResource(id = R.string.clear_board))
                        }
                        Button(
                            onClick = {
                                onIntent(EditModeIntent.ExportFenClicked)
                            }
                        ) {
                            Text(text = stringResource(id = R.string.copy_fen))
                        }
                    }
                }

                state.fenExportDraft?.let { draft ->
                    FenExportDialog(
                        draft = draft,
                        onIntent = onIntent,
                    )
                }
            }
        }
    }
}

@Composable
fun PiecesForEdit(pieceColor: PieceColor, pieceSize: Dp, pieceSpacing: Dp, selectedId: Int, onIntent: (EditModeIntent) -> Unit = { }) {
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
        topPieces.forEach { piece ->
            val resId = piece?.getDrawableId() ?: R.drawable.remove_piece
            if (resId != 0) {
                Image(
                    painter = painterResource(id = resId),
                    contentDescription = piece?.contentDescription() ?: "remove_piece",
                    modifier = Modifier
                        .size(pieceSize)
                        .padding(horizontal = pieceSpacing / 2)
                        .background(if (resId == selectedId) colorResource(R.color.chess_piece_selected_cell_overlay_color) else Color( 0x00000000))
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