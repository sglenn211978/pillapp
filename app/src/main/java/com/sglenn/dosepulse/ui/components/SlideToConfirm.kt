package com.sglenn.dosepulse.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sglenn.dosepulse.ui.theme.StatusTaken
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SlideToConfirm(
    onConfirmed: () -> Unit,
    modifier: Modifier = Modifier,
    text: String = "Slide to Confirm Taken",
    thumbColor: Color = StatusTaken,
    trackColor: Color = Color(0xFF1E293B)
) {
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    var trackWidthPx by remember { mutableFloatStateOf(0f) }
    val thumbWidth = 64.dp
    val thumbWidthPx = with(LocalDensity.current) { thumbWidth.toPx() }

    val offsetX = remember { Animatable(0f) }
    var isConfirmed by remember { mutableStateOf(false) }

    val maxDrag = (trackWidthPx - thumbWidthPx).coerceAtLeast(0f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(trackColor)
            .onGloballyPositioned { coordinates ->
                trackWidthPx = coordinates.size.width.toFloat()
            },
        contentAlignment = Alignment.CenterStart
    ) {
        // Center Prompt Text (Fades as slider advances)
        val progress = if (maxDrag > 0f) (offsetX.value / maxDrag).coerceIn(0f, 1f) else 0f
        Text(
            text = if (isConfirmed) "Confirmed ✓" else text,
            color = Color.White.copy(alpha = (1f - progress * 0.8f).coerceIn(0.2f, 1f)),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.Center)
        )

        // Draggable Thumb Button
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .size(thumbWidth)
                .padding(4.dp)
                .clip(CircleShape)
                .background(if (isConfirmed) Color.White else thumbColor)
                .pointerInput(maxDrag, isConfirmed) {
                    if (isConfirmed) return@pointerInput

                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (offsetX.value >= maxDrag * 0.82f) {
                                // Locked confirmation threshold reached!
                                isConfirmed = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                coroutineScope.launch {
                                    offsetX.animateTo(maxDrag, tween(150))
                                    onConfirmed()
                                }
                            } else {
                                // Snap back with spring
                                coroutineScope.launch {
                                    offsetX.animateTo(0f, tween(200))
                                }
                            }
                        },
                        onDragCancel = {
                            coroutineScope.launch { offsetX.animateTo(0f, tween(200)) }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            if (!isConfirmed && maxDrag > 0f) {
                                val next = (offsetX.value + dragAmount).coerceIn(0f, maxDrag)
                                coroutineScope.launch { offsetX.snapTo(next) }
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isConfirmed) Icons.Default.Check else Icons.Default.ArrowForward,
                contentDescription = "Confirm",
                tint = if (isConfirmed) StatusTaken else Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
