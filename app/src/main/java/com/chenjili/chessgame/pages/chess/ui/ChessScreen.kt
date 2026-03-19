package com.chenjili.chessgame.pages.chess.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
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
import com.chenjili.chess.api.GameState
import com.chenjili.chess.api.PieceColor
import com.chenjili.chess.api.PieceType
import com.chenjili.chessgame.R
import com.chenjili.chessgame.pages.chess.ui.theme.ChessGameTheme
import java.util.Locale

@Composable
private fun WalnutActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, colorResource(R.color.walnut_dark)),
        colors = ButtonDefaults.buttonColors(
            containerColor = colorResource(R.color.walnut_medium),
            contentColor = Color.White
        ),
        contentPadding = ButtonDefaults.ContentPadding
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ImportGameDialog(
    format: ImportFormat,
    importError: ImportError?,
    onImportRequested: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var inputText by rememberSaveable(format) { mutableStateOf("") }

    val titleResId = when (format) {
        ImportFormat.FEN -> R.string.import_fen_title
        ImportFormat.PGN -> R.string.import_pgn_title
    }
    val placeholderResId = when (format) {
        ImportFormat.FEN -> R.string.import_fen_placeholder
        ImportFormat.PGN -> R.string.import_pgn_placeholder
    }
    val errorText = when (importError) {
        ImportError.EMPTY_INPUT -> stringResource(
            if (format == ImportFormat.FEN) R.string.import_fen_error_empty else R.string.import_pgn_error_empty
        )
        ImportError.INVALID_FEN -> stringResource(R.string.import_fen_error_invalid)
        ImportError.INVALID_PGN -> stringResource(R.string.import_pgn_error_invalid)
        null -> null
    }

    Dialog(
        onDismissRequest = onDismiss,
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
                    BorderStroke(2.dp, colorResource(R.color.walnut_dark)),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(20.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.widthIn(min = 280.dp, max = 420.dp)
            ) {
                Text(
                    text = stringResource(titleResId),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(R.color.walnut_accent)
                )

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp),
                    placeholder = {
                        Text(
                            text = stringResource(placeholderResId),
                            color = Color.White.copy(alpha = 0.72f)
                        )
                    },
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 14.sp),
                    minLines = 6,
                    maxLines = 12,
                    supportingText = {
                        if (errorText != null) {
                            Text(
                                text = errorText,
                                color = colorResource(R.color.black),
                            )
                        }
                    },
                    isError = errorText != null,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)
                ) {
                    WalnutActionButton(
                        text = stringResource(R.string.cancel),
                        onClick = onDismiss,
                    )
                    WalnutActionButton(
                        text = stringResource(R.string.import_confirm),
                        onClick = { onImportRequested(inputText) },
                    )
                }
            }
        }
    }
}

// 升变提示框
@Composable
fun PromotionDialog(
    pieceColor: PieceColor,
    onPieceSelected: (PieceType) -> Unit,
    onDismiss: () -> Unit
) {
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
                        PieceType.QUEEN to R.string.queen,
                        PieceType.ROOK to R.string.rook,
                        PieceType.BISHOP to R.string.bishop,
                        PieceType.KNIGHT to R.string.knight,
                    )

                    val gridGap = 12.dp
                    BoxWithConstraints(
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        val buttonWidth = ((maxWidth - gridGap) / 2f).coerceAtLeast(96.dp)

                        Column(
                            verticalArrangement = Arrangement.spacedBy(gridGap),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            choices.chunked(2).forEach { rowItems ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(gridGap),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    rowItems.forEach { (pieceType, nameSourceId) ->
                                        PromotionPieceButton(
                                            pieceType = pieceType,
                                            pieceColor = pieceColor,
                                            name = stringResource(nameSourceId),
                                            width = buttonWidth,
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
}

// 升变选项按钮
@Composable
fun PromotionPieceButton(
    pieceType: PieceType,
    pieceColor: PieceColor,
    name: String,
    width: Dp,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val isPressed = remember { mutableStateOf(false) }
    val density = LocalDensity.current

    val scale by animateFloatAsState(
        targetValue = if (isPressed.value) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "buttonScale"
    )

    val wPx = with(density) { width.toPx() }.coerceAtLeast(1f)

    fun lerpPxToDp(minPx: Float, maxPx: Float, t: Float): Dp {
        val v = minPx + (maxPx - minPx) * t.coerceIn(0f, 1f)
        return with(density) { v.toDp() }
    }

    val t = ((wPx - with(density) { 96.dp.toPx() }) /
            (with(density) { 200.dp.toPx() } - with(density) { 96.dp.toPx() }))
        .coerceIn(0f, 1f)

    val cornerRadius = lerpPxToDp(minPx = 10f, maxPx = 16f, t = t)
    val borderWidth = lerpPxToDp(minPx = 1.5f, maxPx = 2.5f, t = t)
    val paddingH = lerpPxToDp(minPx = 8f, maxPx = 14f, t = t)
    val paddingV = lerpPxToDp(minPx = 8f, maxPx = 14f, t = t)

    val iconSize = with(density) {
        (wPx * (0.55f + 0.07f * t)).toDp().coerceIn(44.dp, 84.dp)
    }

    val labelSp = with(density) { (10f + 4f * t) }
    val topGap = lerpPxToDp(minPx = 4f, maxPx = 8f, t = t)

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
            .width(width)
            .aspectRatio(1f)
            .scale(scale)
            .clickable {
                isPressed.value = true
                onClick()
            }
            .background(
                color = colorResource(R.color.walnut_light),
                shape = RoundedCornerShape(cornerRadius)
            )
            .border(
                BorderStroke(borderWidth, colorResource(R.color.walnut_grain)),
                shape = RoundedCornerShape(cornerRadius)
            )
            .padding(horizontal = paddingH, vertical = paddingV)
    ) {
        if (resId != 0) {
            Image(
                painter = painterResource(id = resId),
                contentDescription = "${colorName} ${typeName}",
                modifier = Modifier.size(iconSize).padding(10.dp)
            )
        }

        Text(
            text = name,
            fontSize = labelSp.sp,
            color = Color.White,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = topGap)
        )
    }
}

// 对局结束提示框
@Composable
fun GameOverDialog(text: String, onDismiss: () -> Unit) {
    val showDialog = remember { mutableStateOf(true) }

    AnimatedVisibility(
        visible = showDialog.value,
        enter = fadeIn(animationSpec = tween(250)) + scaleIn(initialScale = 0.9f),
        exit = fadeOut(animationSpec = tween(200)) + scaleOut(targetScale = 0.9f)
    ) {
        Dialog(
            onDismissRequest = {
                showDialog.value = false
                onDismiss()
            },
            properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
        ) {
            Box(
                modifier = Modifier
                    .shadow(14.dp, RoundedCornerShape(14.dp))
                    .background(color = colorResource(R.color.walnut_medium), shape = RoundedCornerShape(14.dp))
                    .border(BorderStroke(2.dp, colorResource(R.color.walnut_dark)), shape = RoundedCornerShape(14.dp))
                    .padding(20.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.game_over),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorResource(R.color.walnut_accent)
                    )

                    Text(
                        text = text,
                        fontSize = 16.sp,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )

                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                showDialog.value = false
                                onDismiss()
                            }
                        ) {
                            Text(text = stringResource(R.string.ok))
                        }
                    }
                }
            }
        }
    }
}

// 投降对话框
@Composable
fun SurrenderDialog(
    onSurrenderConfirmed: (PieceColor) -> Unit,
    onDismiss: () -> Unit
) {
    val showDialog = remember { mutableStateOf(true) }

    AnimatedVisibility(
        visible = showDialog.value,
        enter = fadeIn(animationSpec = tween(250)) + scaleIn(initialScale = 0.9f),
        exit = fadeOut(animationSpec = tween(200)) + scaleOut(targetScale = 0.9f)
    ) {
        Dialog(
            onDismissRequest = {
                showDialog.value = false
                onDismiss()
            },
            properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
        ) {
            Box(
                modifier = Modifier
                    .shadow(14.dp, RoundedCornerShape(14.dp))
                    .background(color = colorResource(R.color.walnut_medium), shape = RoundedCornerShape(14.dp))
                    .border(BorderStroke(2.dp, colorResource(R.color.walnut_dark)), shape = RoundedCornerShape(14.dp))
                    .padding(20.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.surrender),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorResource(R.color.walnut_accent)
                    )

                    Text(
                        text = stringResource(R.string.choose_surrendering_side),
                        fontSize = 16.sp,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        WalnutActionButton(
                            text = stringResource(R.string.white_surrenders),
                            onClick = {
                                showDialog.value = false
                                onSurrenderConfirmed(PieceColor.WHITE)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        WalnutActionButton(
                            text = stringResource(R.string.black_surrenders),
                            onClick = {
                                showDialog.value = false
                                onSurrenderConfirmed(PieceColor.BLACK)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChessScreen(
    paddingDp: Dp = 8.dp,
    state: ChessState = ChessState(),
    onBoardLayoutChanged: (x: Dp, y: Dp, width: Dp, height: Dp) -> Unit = { _, _, _, _ -> },
    onIntent: (ChessIntent) -> Unit = { }
) {
    var importDialogFormat by rememberSaveable { mutableStateOf<ImportFormat?>(null) }
    val contentScrollState = rememberScrollState()
    val analysisBringIntoViewRequester = remember { BringIntoViewRequester() }

    LaunchedEffect(state.importSuccessVersion) {
        if (importDialogFormat != null && state.importSuccessVersion > 0) {
            importDialogFormat = null
            onIntent(ChessIntent.ClearImportFeedback)
        }
    }

    LaunchedEffect(state.isAnalyzing, state.analysisFen, state.positionFen, state.analysisRecommendations.size) {
        val hasVisibleAnalysis = state.analysisRecommendations.isNotEmpty()
        val analysisMatchesCurrentPosition = state.analysisFen != null && state.analysisFen == state.positionFen
        if (state.isAnalyzing || (hasVisibleAnalysis && analysisMatchesCurrentPosition)) {
            analysisBringIntoViewRequester.bringIntoView()
        }
    }

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
                        .verticalScroll(contentScrollState)
                        .padding(paddingDp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    // 棋盘贴上方
                    Box(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .size(squareSize)
                            .onGloballyPositioned { coordinates ->
                                val topLeft = coordinates.positionInWindow()
                                val sizePx = coordinates.size
                                with(density) {
                                    onBoardLayoutChanged(
                                        topLeft.x.toDp(),
                                        topLeft.y.toDp(),
                                        sizePx.width.toDp(),
                                        sizePx.height.toDp()
                                    )
                                }
                            }
                    ) {
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
                                        val column = colFromLeft
                                        val row = 7 - rowFromTop
                                        onIntent(ChessIntent.BoardCellClicked(column, row, state.playerColor))
                                    }
                                }
                        )
                    }

                    AnalysisEngineStatusChip(
                        status = state.analysisEngineStatus,
                        modifier = Modifier
                            .padding(top = 10.dp)
                            .widthIn(max = squareSize)
                            .fillMaxWidth()
                    )

                    AnalysisPanel(
                        analysisEnabled = state.analysisEnabled,
                        isAnalyzing = state.isAnalyzing,
                        recommendations = state.analysisRecommendations,
                        analysisMatchesCurrentPosition = state.analysisFen != null && state.analysisFen == state.positionFen,
                        onAnalysisEnabledChanged = { enabled ->
                            onIntent(ChessIntent.ToggleAnalysis(enabled))
                        },
                        modifier = Modifier
                            .padding(top = 10.dp)
                            .bringIntoViewRequester(analysisBringIntoViewRequester)
                            .widthIn(max = squareSize)
                            .fillMaxWidth()
                    )

                    // 胡桃木风格的横向滚动工具条，预留空间
                    Box(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .widthIn(max = squareSize)
                            .fillMaxWidth()
                            .shadow(10.dp, RoundedCornerShape(14.dp))
                            .background(colorResource(R.color.walnut_light), RoundedCornerShape(14.dp))
                            .border(BorderStroke(1.dp, colorResource(R.color.walnut_grain)), RoundedCornerShape(14.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            WalnutActionButton(
                                text = stringResource(id = R.string.undo_move),
                                onClick = { onIntent(ChessIntent.UndoMove) }
                            )
                            WalnutActionButton(
                                text = stringResource(id = R.string.switch_side),
                                onClick = {
                                    val newColor =
                                        if (state.playerColor == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE
                                    onIntent(ChessIntent.PlayerColorChanged(newColor))
                                }
                            )
                            WalnutActionButton(
                                text = stringResource(id = R.string.restart_game),
                                onClick = { onIntent(ChessIntent.RestartGame(state.playerColor)) }
                            )
                            WalnutActionButton(
                                text = stringResource(id = R.string.import_fen),
                                onClick = {
                                    importDialogFormat = ImportFormat.FEN
                                    onIntent(ChessIntent.ClearImportFeedback)
                                }
                            )
                            WalnutActionButton(
                                text = stringResource(id = R.string.import_pgn),
                                onClick = {
                                    importDialogFormat = ImportFormat.PGN
                                    onIntent(ChessIntent.ClearImportFeedback)
                                }
                            )
//                            WalnutActionButton(
//                                text = stringResource(id = R.string.surrender),
//                                onClick = { onIntent(ChessIntent.SurrenderClicked) }
//                            )
                        }
                    }

                    // 棋谱区变窄：比棋盘窄的居中卡片
                    val notationEmptyMove = stringResource(R.string.notation_empty_move)
                    val movePairs = remember(state.moveHistory) {
                        val moves = state.moveHistory
                        val triples = ArrayList<Triple<Int, String, String>>()
                        var moveNum = 1
                        var whiteCache = ""
                        for (move in moves) {
                            val notation = move.notation
                            val pieceColor = move.move.piece.color
                            when (pieceColor) {
                                PieceColor.WHITE -> {
                                    if (whiteCache.isNotEmpty()) {
                                        triples.add(Triple(moveNum, whiteCache, notationEmptyMove))
                                        ++moveNum
                                    }
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
                            triples.add(Triple(moveNum, whiteCache, notationEmptyMove))
                        }
                        triples
                    }

                    val lazyListState = rememberLazyListState()
                    LaunchedEffect(movePairs.size) {
                        if (movePairs.isNotEmpty()) {
                            lazyListState.animateScrollToItem(movePairs.lastIndex)
                        }
                    }

                    Column(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .widthIn(max = squareSize * 0.78f)
                            .fillMaxWidth()
                            .shadow(10.dp, RoundedCornerShape(14.dp))
                            .background(colorResource(R.color.walnut_medium), RoundedCornerShape(14.dp))
                            .border(BorderStroke(1.dp, colorResource(R.color.walnut_dark)), RoundedCornerShape(14.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.game_record),
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 8.dp),
                            fontWeight = FontWeight.SemiBold
                        )

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .background(colorResource(R.color.walnut_light), RoundedCornerShape(10.dp))
                                .border(BorderStroke(1.dp, colorResource(R.color.walnut_grain)), RoundedCornerShape(10.dp))
                                .padding(8.dp),
                            state = lazyListState
                        ) {
                            items(movePairs) { (num, whiteMove, blackMove) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "$num.",
                                        modifier = Modifier.weight(0.16f),
                                        color = Color.White
                                    )
                                    Text(
                                        text = whiteMove,
                                        modifier = Modifier.weight(0.42f),
                                        color = Color.White
                                    )
                                    Text(
                                        text = blackMove,
                                        modifier = Modifier.weight(0.42f),
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                importDialogFormat?.let { format ->
                    ImportGameDialog(
                        format = format,
                        importError = state.importError,
                        onImportRequested = { content ->
                            onIntent(ChessIntent.ImportRequested(format, content))
                        },
                        onDismiss = {
                            importDialogFormat = null
                            onIntent(ChessIntent.ClearImportFeedback)
                        }
                    )
                }

                state.pendingPromotion?.let { pendingPromotion ->
                    PromotionDialog(
                        pieceColor = pendingPromotion.pieceColor,
                        onPieceSelected = { pieceType ->
                            onIntent(ChessIntent.PromotionPieceSelected(pieceType))
                        },
                        onDismiss = { onIntent(ChessIntent.PromotionCancelled) }
                    )
                }

                if (state.showGameOverDialog && state.gameState.isGameOver()) {
                    val gameOverTipId = when (state.gameState) {
                        GameState.IN_PROGRESS -> R.string.game_state_desc_in_progress
                        GameState.CHECKMATE_WHITE_WINS -> R.string.game_state_desc_checkmate_white_wins
                        GameState.CHECKMATE_BLACK_WINS -> R.string.game_state_desc_checkmate_black_wins
                        GameState.STALEMATE -> R.string.game_state_desc_stalemate
                        GameState.DRAW_BY_INSUFFICIENT_MATERIAL -> R.string.game_state_desc_draw_by_insufficient_material
                        GameState.DRAW_BY_FIFTY_MOVE_RULE -> R.string.game_state_desc_draw_by_fifty_move_rule
                        GameState.DRAW_BY_THREEFOLD_REPETITION -> R.string.game_state_desc_draw_by_threefold_repetition
                    }
                    GameOverDialog(
                        text = stringResource(gameOverTipId),
                        onDismiss = { onIntent(ChessIntent.GameOverDialogDismissed) }
                    )
                }

                if (state.showSurrenderDialog) {
                    SurrenderDialog(
                        onSurrenderConfirmed = { color ->
                            onIntent(ChessIntent.SurrenderConfirmed(color))
                        },
                        onDismiss = { onIntent(ChessIntent.SurrenderCancelled) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AnalysisPanel(
    analysisEnabled: Boolean,
    isAnalyzing: Boolean,
    recommendations: List<AnalysisRecommendationDisplay>,
    analysisMatchesCurrentPosition: Boolean,
    onAnalysisEnabledChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bestMove = recommendations.firstOrNull()
    val shouldShowRecommendations = recommendations.isNotEmpty() && (analysisMatchesCurrentPosition || isAnalyzing)
    Box(
        modifier = modifier
            .shadow(10.dp, RoundedCornerShape(14.dp))
            .background(colorResource(R.color.walnut_medium), RoundedCornerShape(14.dp))
            .border(
                BorderStroke(
                    1.5.dp,
                    if (analysisEnabled) colorResource(R.color.walnut_accent) else colorResource(R.color.walnut_dark)
                ),
                RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(R.string.analysis_auto_title),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        text = stringResource(
                            if (analysisEnabled) {
                                R.string.analysis_auto_subtitle_enabled
                            } else {
                                R.string.analysis_auto_subtitle_disabled
                            }
                        ),
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.78f),
                    )
                }

                Switch(
                    checked = analysisEnabled,
                    onCheckedChange = onAnalysisEnabledChanged,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = colorResource(R.color.walnut_accent),
                        checkedTrackColor = colorResource(R.color.walnut_light),
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = colorResource(R.color.walnut_dark),
                    )
                )
            }

            if (analysisEnabled) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(colorResource(R.color.walnut_grain))
                )

                when {
                    isAnalyzing && bestMove == null -> {
                        Text(
                            text = stringResource(R.string.analysis_running),
                            color = Color.White,
                            fontSize = 13.sp,
                        )
                    }

                    bestMove == null -> {
                        Text(
                            text = stringResource(R.string.analysis_waiting_for_move),
                            color = Color.White,
                            fontSize = 13.sp,
                        )
                    }

                    else -> {
                        AnalysisSummaryBanner(
                            bestMove = bestMove,
                            isAnalyzing = isAnalyzing,
                            analysisMatchesCurrentPosition = analysisMatchesCurrentPosition,
                        )

                        if (shouldShowRecommendations) {
                            Text(
                                text = stringResource(R.string.analysis_recommendations),
                                color = colorResource(R.color.walnut_accent),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                            )
                            recommendations.forEachIndexed { index, recommendation ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${index + 1}.",
                                        color = Color.White,
                                        modifier = Modifier.width(24.dp),
                                        fontSize = 13.sp,
                                    )
                                    Text(
                                        text = recommendation.notation,
                                        color = Color.White,
                                        modifier = Modifier.weight(1f),
                                        fontSize = 13.sp,
                                    )
                                    Text(
                                        text = formatAnalysisScore(recommendation.scoreCp),
                                        color = colorResource(R.color.walnut_accent),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
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
private fun AnalysisSummaryBanner(
    bestMove: AnalysisRecommendationDisplay,
    isAnalyzing: Boolean,
    analysisMatchesCurrentPosition: Boolean,
) {
    val statusTextRes = when {
        isAnalyzing && analysisMatchesCurrentPosition -> R.string.analysis_status_refreshing
        isAnalyzing -> R.string.analysis_status_updating_with_previous
        analysisMatchesCurrentPosition -> R.string.analysis_status_ready
        else -> R.string.analysis_status_previous_position
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorResource(R.color.walnut_light), RoundedCornerShape(12.dp))
            .border(BorderStroke(1.dp, colorResource(R.color.walnut_grain)), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(colorResource(R.color.walnut_accent), CircleShape)
                )
                Text(
                    text = stringResource(statusTextRes),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
            }

            Text(
                text = stringResource(R.string.analysis_best_move),
                color = colorResource(R.color.walnut_accent),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = bestMove.notation,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = formatAnalysisScore(bestMove.scoreCp),
                    color = colorResource(R.color.walnut_accent),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

private fun formatAnalysisScore(scoreCp: Int): String {
    return String.format(Locale.US, "%+.2f", scoreCp / 100f)
}

@Composable
private fun AnalysisEngineStatusChip(
    status: AnalysisEngineStatus,
    modifier: Modifier = Modifier,
) {
    val textRes = when (status) {
        AnalysisEngineStatus.STOCKFISH -> R.string.analysis_engine_status_stockfish
        AnalysisEngineStatus.HEURISTIC_FALLBACK -> R.string.analysis_engine_status_heuristic
    }
    val backgroundColor = when (status) {
        AnalysisEngineStatus.STOCKFISH -> colorResource(R.color.walnut_light)
        AnalysisEngineStatus.HEURISTIC_FALLBACK -> colorResource(R.color.walnut_medium)
    }

    Box(
        modifier = modifier
            .shadow(6.dp, RoundedCornerShape(10.dp))
            .background(backgroundColor, RoundedCornerShape(10.dp))
            .border(BorderStroke(1.dp, colorResource(R.color.walnut_grain)), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = stringResource(textRes),
            fontSize = 13.sp,
            color = Color.White,
            fontWeight = FontWeight.Medium,
        )
    }
}
