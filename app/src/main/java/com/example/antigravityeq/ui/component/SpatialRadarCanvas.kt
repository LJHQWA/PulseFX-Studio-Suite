package com.example.antigravityeq.ui.component

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

@Composable
fun SpatialRadarCanvas(
    angle: Int,
    separation: Int,
    direction: Int,
    onAngleChange: (Int) -> Unit,
    onSeparationChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val outlineColor = MaterialTheme.colorScheme.outlineVariant
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
    val view = LocalView.current

    // Node state: Vocal/Kick (center), Guitars (left/right wings), Synths (surround), Cymbals (perimeter)
    var dragAngle by remember(angle) { mutableStateOf(angle.toFloat()) }
    var dragSeparation by remember(separation) { mutableStateOf(separation.toFloat()) }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(240.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val center = size.width / 2f
                            val dx = change.position.x - center
                            val dy = change.position.y - center
                            val dist = sqrt(dx * dx + dy * dy)
                            val maxRadius = center * 0.9f

                            // Radius -> Separation (0% to 100%)
                            val newSep = ((dist / maxRadius) * 100f).coerceIn(10f, 100f).toInt()
                            dragSeparation = newSep.toFloat()
                            onSeparationChange(newSep)

                            // Angle calculation from top vertical axis (-90 to +90 degrees)
                            var rawAngleDeg = (atan2(abs(dx), -dy) * (180f / Math.PI.toFloat())).coerceIn(30f, 180f).toInt()
                            dragAngle = rawAngleDeg.toFloat()
                            onAngleChange(rawAngleDeg)

                            if (rawAngleDeg % 30 == 0) {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val maxRadius = (size.width / 2f) * 0.88f

                // 1. Concentric Acoustic Azimuth Rings
                val rings = listOf(0.33f, 0.66f, 1.0f)
                rings.forEachIndexed { idx, frac ->
                    val r = maxRadius * frac
                    drawCircle(
                        color = outlineColor.copy(alpha = 0.4f),
                        radius = r,
                        center = center,
                        style = Stroke(
                            width = 1.dp.toPx(),
                            pathEffect = if (idx == 2) PathEffect.dashPathEffect(floatArrayOf(8f, 8f)) else null
                        )
                    )
                }

                // 2. Horizon Center Crosshair (180° Ear-to-Ear Plane)
                drawLine(
                    color = outlineColor.copy(alpha = 0.6f),
                    start = Offset(center.x - maxRadius, center.y),
                    end = Offset(center.x + maxRadius, center.y),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = primaryColor.copy(alpha = 0.5f),
                    start = Offset(center.x, center.y - maxRadius),
                    end = center,
                    strokeWidth = 1.5.dp.toPx()
                )

                // 3. Center Listener Avatar (Headphone Anchor)
                drawCircle(
                    color = surfaceColor,
                    radius = 16.dp.toPx(),
                    center = center
                )
                drawCircle(
                    color = primaryColor,
                    radius = 16.dp.toPx(),
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
                drawCircle(
                    color = primaryColor,
                    radius = 5.dp.toPx(),
                    center = center
                )

                // 4. Draggable Soundstage Azimuth Vectors & Instrument Nodes
                val currentRad = maxRadius * (dragSeparation / 100f)
                val halfSpreadRad = (dragAngle / 2f) * (Math.PI.toFloat() / 180f)

                // Left Wing Vector (Guitars/Brass)
                val leftX = center.x - currentRad * sin(halfSpreadRad)
                val leftY = center.y - currentRad * cos(halfSpreadRad)
                val leftOffset = Offset(leftX, leftY)

                // Right Wing Vector (Synths/Keys)
                val rightX = center.x + currentRad * sin(halfSpreadRad)
                val rightY = center.y - currentRad * cos(halfSpreadRad)
                val rightOffset = Offset(rightX, rightY)

                // Front-Center Node (Vocal & Kick Anchor)
                val frontY = center.y - (maxRadius * 0.45f)
                val frontOffset = Offset(center.x, frontY)

                // Extreme 180° Perimeter Nodes (Cymbals & Air Sheen)
                val outerRad = maxRadius * 0.95f
                val outerLeftOffset = Offset(center.x - outerRad, center.y - 12.dp.toPx())
                val outerRightOffset = Offset(center.x + outerRad, center.y - 12.dp.toPx())

                // Radial Connector Rays
                drawLine(
                    color = primaryColor.copy(alpha = 0.35f),
                    start = center,
                    end = leftOffset,
                    strokeWidth = 1.5.dp.toPx()
                )
                drawLine(
                    color = primaryColor.copy(alpha = 0.35f),
                    start = center,
                    end = rightOffset,
                    strokeWidth = 1.5.dp.toPx()
                )

                // Draw Glowing Instrument Nodes
                // 🟢 Front Center Vocal
                drawCircle(color = primaryColor.copy(alpha = 0.3f), radius = 12.dp.toPx(), center = frontOffset)
                drawCircle(color = primaryColor, radius = 6.dp.toPx(), center = frontOffset)

                // 🔵 Lateral Wings
                drawCircle(color = secondaryColor.copy(alpha = 0.3f), radius = 13.dp.toPx(), center = leftOffset)
                drawCircle(color = secondaryColor, radius = 7.dp.toPx(), center = leftOffset)

                drawCircle(color = secondaryColor.copy(alpha = 0.3f), radius = 13.dp.toPx(), center = rightOffset)
                drawCircle(color = secondaryColor, radius = 7.dp.toPx(), center = rightOffset)

                // 🟣 180° Outer Cymbals & Air
                drawCircle(color = tertiaryColor.copy(alpha = 0.3f), radius = 10.dp.toPx(), center = outerLeftOffset)
                drawCircle(color = tertiaryColor, radius = 5.dp.toPx(), center = outerLeftOffset)

                drawCircle(color = tertiaryColor.copy(alpha = 0.3f), radius = 10.dp.toPx(), center = outerRightOffset)
                drawCircle(color = tertiaryColor, radius = 5.dp.toPx(), center = outerRightOffset)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "🟢 Center: Vocals/Kick",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = primaryColor
            )
            Text(
                text = "🔵 Wings: Instruments",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = secondaryColor
            )
            Text(
                text = "🟣 180°: Air/Cymbals",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = tertiaryColor
            )
        }
    }
}
