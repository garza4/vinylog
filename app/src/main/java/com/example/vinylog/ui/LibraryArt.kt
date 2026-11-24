package com.example.vinylog.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
fun VinylRecord(modifier: Modifier = Modifier, holeColor: Color) {
    Canvas(modifier.discSize()) {
        val c = center
        val r = size.minDimension / 2f
        drawCircle(
            color = Color.Black.copy(alpha = 0.28f),
            radius = r,
            center = c + Offset(0f, r * 0.06f),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.00f to Color(0xFFE4572E),
                    0.30f to Color(0xFFC73E1D),
                    0.33f to Color(0xFF101010),
                    0.88f to Color(0xFF1A1A1A),
                    0.95f to Color(0xFF3A3A3A),
                    1.00f to Color(0xFF070707),
                ),
                center = c,
                radius = r,
            ),
            radius = r,
            center = c,
        )
        for (i in 1..11) {
            val groove = r * (0.38f + i * 0.05f)
            drawCircle(
                color = Color.White.copy(alpha = if (i % 2 == 0) 0.09f else 0.035f),
                radius = groove,
                center = c,
                style = Stroke(width = r * 0.012f),
            )
        }
        drawArc(
            color = Color.White.copy(alpha = 0.22f),
            startAngle = 206f,
            sweepAngle = 70f,
            useCenter = false,
            topLeft = Offset(c.x - r * 0.86f, c.y - r * 0.86f),
            size = Size(r * 1.72f, r * 1.72f),
            style = Stroke(width = r * 0.07f, cap = StrokeCap.Round),
        )
        drawCircle(Color(0xFFF2C14E), r * 0.30f, c)
        drawCircle(Color(0xFF8C2F1B), r * 0.30f, c, style = Stroke(width = r * 0.012f))
        drawCircle(Color(0xFF8C2F1B).copy(alpha = 0.55f), r * 0.18f, c, style = Stroke(width = r * 0.008f))
        drawCircle(holeColor, r * 0.045f, c)
        drawCircle(Color(0xFF2A2A2A), r * 0.045f, c, style = Stroke(width = r * 0.012f))
    }
}

@Composable
fun CompactDisc(modifier: Modifier = Modifier, holeColor: Color) {
    Canvas(modifier.discSize()) {
        val c = center
        val r = size.minDimension / 2f
        drawCircle(
            color = Color.Black.copy(alpha = 0.22f),
            radius = r,
            center = c + Offset(0f, r * 0.06f),
        )
        drawCircle(
            brush = Brush.sweepGradient(
                colors = listOf(
                    Color(0xFFE6E6E6),
                    Color(0xFF9FD4FF),
                    Color(0xFFD7B0FF),
                    Color(0xFFFFE29A),
                    Color(0xFFB6F3C9),
                    Color(0xFFF7F7F7),
                    Color(0xFFE6E6E6),
                ),
                center = c,
            ),
            radius = r,
            center = c,
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.45f), Color.Transparent),
                center = c + Offset(-r * 0.25f, -r * 0.3f),
                radius = r * 0.8f,
            ),
            radius = r,
            center = c,
        )
        for (i in 1..7) {
            drawCircle(
                color = Color.White.copy(alpha = 0.28f),
                radius = r * (0.42f + i * 0.07f),
                center = c,
                style = Stroke(width = r * 0.008f),
            )
        }
        drawCircle(Color(0xFFCFCFCF), r * 0.22f, c)
        drawCircle(holeColor, r * 0.08f, c)
        drawCircle(Color(0xFF8A8A8A), r * 0.08f, c, style = Stroke(width = r * 0.015f))
    }
}

@Composable
fun MovieCase(modifier: Modifier = Modifier) {
    Canvas(modifier.discSize()) {
        val caseH = size.minDimension * 0.92f
        val caseW = caseH * 0.70f
        val left = (size.width - caseW) / 2f
        val top = (size.height - caseH) / 2f
        val radius = CornerRadius(caseW * 0.045f, caseW * 0.045f)
        val spine = caseW * 0.12f

        drawRoundRect(
            color = Color.Black.copy(alpha = 0.28f),
            topLeft = Offset(left + caseW * 0.04f, top + caseH * 0.03f),
            size = Size(caseW, caseH),
            cornerRadius = radius,
        )
        drawRoundRect(
            color = Color(0xFF1A2330),
            topLeft = Offset(left, top),
            size = Size(caseW, caseH),
            cornerRadius = radius,
        )
        drawRoundRect(
            color = Color(0xFF101820),
            topLeft = Offset(left, top),
            size = Size(spine, caseH),
            cornerRadius = CornerRadius(caseW * 0.04f, caseW * 0.04f),
        )
        val coverLeft = left + spine + caseW * 0.03f
        val coverTop = top + caseH * 0.035f
        val coverW = caseW - spine - caseW * 0.06f
        val coverH = caseH - caseH * 0.07f
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF243B5A), Color(0xFF142033), Color(0xFF0C1016)),
                startY = coverTop,
                endY = coverTop + coverH,
            ),
            topLeft = Offset(coverLeft, coverTop),
            size = Size(coverW, coverH),
            cornerRadius = CornerRadius(caseW * 0.02f, caseW * 0.02f),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFFE7A3), Color(0xFFFFE7A3).copy(alpha = 0f)),
                center = Offset(coverLeft + coverW * 0.55f, coverTop + coverH * 0.34f),
                radius = coverW * 0.55f,
            ),
            radius = coverW * 0.55f,
            center = Offset(coverLeft + coverW * 0.55f, coverTop + coverH * 0.34f),
        )
        val stripTop = coverTop + coverH * 0.62f
        val stripH = coverH * 0.22f
        drawRect(
            color = Color(0xFF111111),
            topLeft = Offset(coverLeft, stripTop),
            size = Size(coverW, stripH),
        )
        val holeCount = 6
        val hole = stripH * 0.16f
        for (i in 0 until holeCount) {
            val y = stripTop + stripH * (i + 0.5f) / holeCount - hole / 2f
            drawRoundRect(
                color = Color(0xFFE8E0C8),
                topLeft = Offset(coverLeft + hole * 0.7f, y),
                size = Size(hole, hole),
                cornerRadius = CornerRadius(hole * 0.25f, hole * 0.25f),
            )
            drawRoundRect(
                color = Color(0xFFE8E0C8),
                topLeft = Offset(coverLeft + coverW - hole * 1.7f, y),
                size = Size(hole, hole),
                cornerRadius = CornerRadius(hole * 0.25f, hole * 0.25f),
            )
        }
        drawRect(
            color = Color.White.copy(alpha = 0.85f),
            topLeft = Offset(coverLeft + coverW * 0.28f, stripTop + stripH * 0.38f),
            size = Size(coverW * 0.44f, stripH * 0.10f),
        )
        drawRect(
            color = Color.White.copy(alpha = 0.55f),
            topLeft = Offset(coverLeft + coverW * 0.34f, stripTop + stripH * 0.56f),
            size = Size(coverW * 0.32f, stripH * 0.08f),
        )
    }
}

@Composable
fun GameCartridge(
    modifier: Modifier = Modifier.discSize(),
    labelColor: Color = Color(0xFFE60012),
) {
    Canvas(modifier) {
        val cartW = size.minDimension * 0.72f
        val cartH = cartW * 1.12f
        val left = (size.width - cartW) / 2f
        val top = (size.height - cartH) / 2f
        val radius = CornerRadius(cartW * 0.08f, cartW * 0.08f)

        drawRoundRect(
            color = Color.Black.copy(alpha = 0.28f),
            topLeft = Offset(left + cartW * 0.045f, top + cartH * 0.04f),
            size = Size(cartW, cartH),
            cornerRadius = radius,
        )
        drawRoundRect(
            color = Color(0xFFD7D1C8),
            topLeft = Offset(left, top),
            size = Size(cartW, cartH),
            cornerRadius = radius,
        )
        drawRoundRect(
            color = Color(0xFFB7B0A6),
            topLeft = Offset(left + cartW * 0.045f, top + cartH * 0.04f),
            size = Size(cartW * 0.91f, cartH * 0.90f),
            cornerRadius = CornerRadius(cartW * 0.05f, cartW * 0.05f),
            style = Stroke(width = cartW * 0.018f),
        )
        val labelLeft = left + cartW * 0.12f
        val labelTop = top + cartH * 0.10f
        val labelW = cartW * 0.76f
        val labelH = cartH * 0.46f
        drawRoundRect(
            color = labelColor,
            topLeft = Offset(labelLeft, labelTop),
            size = Size(labelW, labelH),
            cornerRadius = CornerRadius(cartW * 0.03f, cartW * 0.03f),
        )
        drawRect(
            color = Color.White.copy(alpha = 0.92f),
            topLeft = Offset(labelLeft + labelW * 0.10f, labelTop + labelH * 0.32f),
            size = Size(labelW * 0.80f, labelH * 0.09f),
        )
        drawRect(
            color = Color.White.copy(alpha = 0.7f),
            topLeft = Offset(labelLeft + labelW * 0.10f, labelTop + labelH * 0.50f),
            size = Size(labelW * 0.48f, labelH * 0.07f),
        )
        val pinTop = top + cartH * 0.76f
        val pinH = cartH * 0.14f
        val pinCount = 8
        val pinLeft = left + cartW * 0.14f
        val pinSpan = cartW * 0.72f
        val slot = pinSpan / pinCount
        for (i in 0 until pinCount) {
            drawRoundRect(
                color = Color(0xFFC6A15A),
                topLeft = Offset(pinLeft + i * slot + slot * 0.22f, pinTop),
                size = Size(slot * 0.56f, pinH),
                cornerRadius = CornerRadius(slot * 0.12f, slot * 0.12f),
            )
        }
    }
}

@Composable
fun Book(modifier: Modifier = Modifier.discSize()) {
    Canvas(modifier) {
        val bookH = size.minDimension * 0.88f
        val bookW = bookH * 0.74f
        val left = (size.width - bookW) / 2f
        val top = (size.height - bookH) / 2f
        val spine = bookW * 0.16f
        val page = bookW * 0.07f
        val radius = CornerRadius(bookW * 0.045f, bookW * 0.045f)

        drawRoundRect(
            color = Color.Black.copy(alpha = 0.28f),
            topLeft = Offset(left + bookW * 0.05f, top + bookH * 0.035f),
            size = Size(bookW, bookH),
            cornerRadius = radius,
        )
        drawRoundRect(
            color = Color(0xFFF4E7D4),
            topLeft = Offset(left + spine * 0.4f, top + bookH * 0.025f),
            size = Size(bookW - spine * 0.4f, bookH * 0.95f),
            cornerRadius = CornerRadius(bookW * 0.03f, bookW * 0.03f),
        )
        val lineCount = 7
        for (i in 0 until lineCount) {
            val y = top + bookH * (0.12f + i * 0.11f)
            drawLine(
                color = Color(0xFFD9C4A8),
                start = Offset(left + bookW - page * 0.85f, y),
                end = Offset(left + bookW - page * 0.15f, y),
                strokeWidth = bookH * 0.012f,
            )
        }
        drawRoundRect(
            color = Color(0xFF8E2E2E),
            topLeft = Offset(left, top),
            size = Size(bookW - page, bookH),
            cornerRadius = radius,
        )
        drawRoundRect(
            color = Color(0xFF6E2222),
            topLeft = Offset(left, top),
            size = Size(spine, bookH),
            cornerRadius = CornerRadius(bookW * 0.04f, bookW * 0.04f),
        )
        drawRect(
            color = Color(0xFFC6A15A),
            topLeft = Offset(left + spine * 0.38f, top + bookH * 0.12f),
            size = Size(spine * 0.12f, bookH * 0.76f),
        )
        drawRect(
            color = Color(0xFFF4E4C4),
            topLeft = Offset(left + spine + bookW * 0.08f, top + bookH * 0.22f),
            size = Size(bookW * 0.46f, bookH * 0.045f),
        )
        drawRect(
            color = Color.White.copy(alpha = 0.75f),
            topLeft = Offset(left + spine + bookW * 0.08f, top + bookH * 0.32f),
            size = Size(bookW * 0.32f, bookH * 0.03f),
        )
    }
}

private fun Modifier.discSize(): Modifier =
    this.fillMaxHeight().aspectRatio(1f, matchHeightConstraintsFirst = true)
