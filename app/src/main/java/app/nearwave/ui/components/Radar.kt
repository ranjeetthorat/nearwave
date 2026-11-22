package app.nearwave.ui.components

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.nearwave.core.Member
import app.nearwave.core.Proximity
import app.nearwave.ui.theme.NearwaveTheme
import kotlin.math.cos
import kotlin.math.sin

private val RingFractions = listOf(0.40f, 0.70f, 1.0f)
private val MarkerReach = 36.dp

private fun fractionFor(p: Proximity): Float = when (p) {
    Proximity.Near -> 0.24f
    Proximity.Medium -> 0.55f
    Proximity.Far -> 0.85f
    Proximity.Connected -> 0.85f
    Proximity.OutOfRange -> 1.0f
}

/**
 * The radar. You are the centre; rings mean near, medium and far. Phones can measure how
 * strong a signal is but not which way it comes from, so each person keeps a fixed spot
 * around the circle and only moves inwards or outwards.
 */
@Composable
fun Radar(members: List<Member>, modifier: Modifier = Modifier) {
    val colors = NearwaveTheme.colors
    val reduceMotion = rememberReducedMotion()
    val sweep = rememberInfiniteTransition(label = "radar").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(6_000, easing = LinearEasing)),
        label = "sweep",
    )
    val ordered = remember(members) { members.sortedBy { it.id } }
    val summary = remember(members) {
        if (members.isEmpty()) "Radar: nobody found yet"
        else "Radar: " + members.joinToString("; ") { "${it.name}, ${describe(it.proximity)}" }
    }

    BoxWithConstraints(
        modifier
            .aspectRatio(1f)
            .semantics { contentDescription = summary },
        contentAlignment = Alignment.Center,
    ) {
        val radius = minOf(maxWidth, maxHeight) / 2 - MarkerReach

        Canvas(Modifier.fillMaxSize()) {
            val c = center
            val r = radius.toPx()
            drawCircle(colors.radarSurface, radius = r + 22.dp.toPx(), center = c)
            if (!reduceMotion) {
                rotate(sweep.value, c) {
                    drawCircle(
                        brush = Brush.sweepGradient(
                            0f to Color.Transparent,
                            0.78f to Color.Transparent,
                            1f to colors.radarSweep.copy(alpha = 0.20f),
                            center = c,
                        ),
                        radius = r,
                        center = c,
                    )
                }
            }
            val hairline = 1.dp.toPx()
            drawLine(colors.radarRing.copy(alpha = 0.6f), Offset(c.x - r, c.y), Offset(c.x + r, c.y), hairline)
            drawLine(colors.radarRing.copy(alpha = 0.6f), Offset(c.x, c.y - r), Offset(c.x, c.y + r), hairline)
            RingFractions.forEach { f ->
                drawCircle(colors.radarRing, radius = r * f, center = c, style = Stroke(1.5.dp.toPx()))
            }
        }

        RingLabel("Near", Modifier.align(Alignment.Center).offset(y = -(radius * RingFractions[0]) + 10.dp))
        RingLabel("Far", Modifier.align(Alignment.Center).offset(y = -(radius * RingFractions[2]) + 10.dp))

        // You, in the middle.
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(18.dp).clip(CircleShape).background(colors.connected))
            Spacer(Modifier.height(4.dp))
            Text("You", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        ordered.forEachIndexed { index, member ->
            key(member.id) {
                val fraction by animateFloatAsState(fractionFor(member.proximity), tween(700), label = "distance")
                val angle = Math.toRadians(-90.0 + 30.0 + 360.0 * index / ordered.size)
                val x = radius * (fraction * cos(angle)).toFloat()
                val y = radius * (fraction * sin(angle)).toFloat()
                MemberMarker(member, Modifier.align(Alignment.Center).offset(x, y))
            }
        }
    }
}

@Composable
private fun MemberMarker(member: Member, modifier: Modifier) {
    val away = member.proximity == Proximity.OutOfRange
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Avatar(member.name, member.id, size = 46.dp, faded = away, talking = member.talking)
        Spacer(Modifier.height(2.dp))
        Text(
            member.name,
            style = MaterialTheme.typography.labelMedium,
            color = if (away) NearwaveTheme.colors.offline else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 88.dp),
        )
    }
}

@Composable
private fun RingLabel(text: String, modifier: Modifier) {
    Text(text, modifier = modifier, style = MaterialTheme.typography.labelSmall, color = NearwaveTheme.colors.offline)
}

fun describe(p: Proximity): String = when (p) {
    Proximity.Near -> "near you"
    Proximity.Medium -> "a few rows away"
    Proximity.Far -> "far, signal weak"
    Proximity.Connected -> "connected"
    Proximity.OutOfRange -> "out of range"
}

@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}
