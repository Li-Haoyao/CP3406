package au.edu.jcu.assessment.utilityapp.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import au.edu.jcu.assessment.utilityapp.model.CollabCategory

fun artBackground(category: CollabCategory): Color = when (category) {
    CollabCategory.DRINK -> Color(0xFFE3EDCE)
    CollabCategory.FOOD -> Color(0xFFF8E4DC)
    CollabCategory.FASHION -> Color(0xFFE9E4F4)
    CollabCategory.RETAIL -> Color(0xFFF3EDD5)
    else -> Color(0xFFDFEAF1)
}

/** Original vector artwork; no downloaded brand logos or copyrighted game artwork. */
@Composable
fun CampaignArt(category: CollabCategory, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        scale(size.width / 160f, size.height / 160f, pivot = Offset.Zero) {
            val ink = Color(0xFF384932)
            drawCircle(Color.White.copy(alpha = .36f), 58f, Offset(83f, 79f))
            drawOval(ink.copy(alpha = .1f), Offset(27f, 134f), Size(111f, 12f))
            when (category) {
                CollabCategory.DRINK -> rotate(10f, Offset(81f, 89f)) {
                    drawLine(ink, Offset(88f, 20f), Offset(80f, 86f), 8f, StrokeCap.Round)
                    val cup = Path().apply { moveTo(39f, 58f); lineTo(124f, 58f); lineTo(112f, 135f); quadraticBezierTo(82f, 149f, 51f, 134f); close() }
                    drawPath(cup, Color(0xFFF9F5E7)); drawPath(cup, ink, style = Stroke(3f))
                    drawOval(Color(0xFFD1DEB3), Offset(36f, 47f), Size(91f, 25f))
                    drawOval(ink, Offset(36f, 47f), Size(91f, 25f), style = Stroke(3f))
                    drawRoundRect(Color(0xFF627A50), Offset(43f, 84f), Size(76f, 31f), CornerRadius(5f))
                    star(Offset(80f, 99f), 13f, Color(0xFFE7FCA7))
                    listOf(Offset(65f, 128f), Offset(79f, 132f), Offset(94f, 127f)).forEach { drawCircle(ink, 4f, it) }
                }
                CollabCategory.FOOD -> {
                    drawLine(ink, Offset(32f, 33f), Offset(124f, 82f), 5f, StrokeCap.Round)
                    drawLine(ink, Offset(46f, 23f), Offset(133f, 69f), 5f, StrokeCap.Round)
                    val bowl = Path().apply { moveTo(26f, 81f); lineTo(133f, 81f); quadraticBezierTo(128f, 136f, 80f, 137f); quadraticBezierTo(33f, 133f, 26f, 81f); close() }
                    drawPath(bowl, Color(0xFFD67957)); drawPath(bowl, ink, style = Stroke(3f))
                    drawOval(Color(0xFFF8C7A1), Offset(26f, 69f), Size(107f, 28f)); drawOval(ink, Offset(26f, 69f), Size(107f, 28f), style = Stroke(3f))
                    drawCircle(Color(0xFF719657), 9f, Offset(60f, 79f)); drawCircle(Color(0xFFF6EAC8), 8f, Offset(93f, 82f))
                    star(Offset(81f, 116f), 12f, Color(0xFFFFEDC1))
                    drawArc(Color(0xFFAA9A82), 220f, 120f, false, Offset(62f, 21f), Size(14f, 35f), style = Stroke(3f))
                }
                CollabCategory.FASHION -> {
                    val shirt = Path().apply { moveTo(54f, 36f); lineTo(36f, 45f); lineTo(14f, 72f); lineTo(36f, 91f); lineTo(47f, 78f); lineTo(43f, 139f); lineTo(118f, 139f); lineTo(114f, 78f); lineTo(126f, 91f); lineTo(146f, 72f); lineTo(125f, 45f); lineTo(106f, 36f); quadraticBezierTo(81f, 58f, 54f, 36f); close() }
                    drawPath(shirt, Color(0xFF9D8BBC)); drawPath(shirt, ink, style = Stroke(3f)); star(Offset(80f, 91f), 24f, Color(0xFFF0E8C9))
                    drawLine(ink.copy(alpha = .3f), Offset(49f, 129f), Offset(112f, 129f), 2f)
                }
                CollabCategory.RETAIL -> {
                    drawArc(ink, 180f, 180f, false, Offset(59f, 28f), Size(43f, 57f), style = Stroke(6f))
                    val bag = Path().apply { moveTo(40f, 61f); lineTo(119f, 61f); lineTo(129f, 141f); lineTo(32f, 141f); close() }
                    drawPath(bag, Color(0xFFE0B95F)); drawPath(bag, ink, style = Stroke(3f)); star(Offset(80f, 101f), 23f, Color(0xFFF9F3D8))
                }
                else -> {
                    drawRoundRect(Color(0xFFAAC4D1), Offset(23f, 56f), Size(117f, 72f), CornerRadius(28f))
                    drawRoundRect(ink, Offset(23f, 56f), Size(117f, 72f), CornerRadius(28f), style = Stroke(3f))
                    drawLine(ink, Offset(44f, 91f), Offset(67f, 91f), 7f, StrokeCap.Round)
                    drawLine(ink, Offset(55f, 80f), Offset(55f, 102f), 7f, StrokeCap.Round)
                    drawCircle(Color(0xFFD6EE87), 7f, Offset(111f, 82f)); drawCircle(Color(0xFFF2BCAA), 7f, Offset(123f, 99f))
                    val cord = Path().apply { moveTo(81f, 56f); cubicTo(60f, 24f, 106f, 38f, 88f, 17f) }
                    drawPath(cord, ink, style = Stroke(4f))
                }
            }
            star(Offset(133f, 25f), 10f, ink)
            star(Offset(22f, 112f), 6f, ink.copy(alpha = .6f))
            drawCircle(ink.copy(alpha = .3f), 3f, Offset(22f, 46f))
        }
    }
}
private fun DrawScope.star(center: Offset, radius: Float, color: Color) {
    val path = Path().apply {
        moveTo(center.x, center.y - radius)
        quadraticBezierTo(center.x + radius * .18f, center.y - radius * .18f, center.x + radius, center.y)
        quadraticBezierTo(center.x + radius * .18f, center.y + radius * .18f, center.x, center.y + radius)
        quadraticBezierTo(center.x - radius * .18f, center.y + radius * .18f, center.x - radius, center.y)
        quadraticBezierTo(center.x - radius * .18f, center.y - radius * .18f, center.x, center.y - radius)
        close()
    }
    drawPath(path, color)
}
