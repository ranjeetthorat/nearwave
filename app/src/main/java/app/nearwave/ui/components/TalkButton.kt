package app.nearwave.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.dp
import app.nearwave.ui.theme.NearwaveTheme
import app.nearwave.ui.theme.Spacing

/**
 * The one loud element on screen. Hold to talk (or tap to start and tap to stop, if the
 * person prefers). It sinks slightly when pressed and sends out a ring while transmitting.
 */
@Composable
fun TalkButton(
    transmitting: Boolean,
    available: Boolean,
    tapToTalk: Boolean,
    onStart: () -> Boolean,
    onStop: () -> Unit,
    onUnavailable: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = NearwaveTheme.colors
    val haptics = LocalHapticFeedback.current
    var pressed by remember { mutableStateOf(false) }

    val isAvailable by rememberUpdatedState(available)
    val isTransmitting by rememberUpdatedState(transmitting)
    val start by rememberUpdatedState(onStart)
    val stop by rememberUpdatedState(onStop)
    val unavailable by rememberUpdatedState(onUnavailable)

    fun begin(): Boolean {
        if (!isAvailable) {
            unavailable()
            return false
        }
        val ok = start()
        if (ok) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        return ok
    }

    fun end() {
        stop()
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    val scale by animateFloatAsState(if (pressed || transmitting) 0.94f else 1f, tween(120), label = "press")
    val fill by animateColorAsState(
        when {
            !available -> colors.talkDisabled
            transmitting -> colors.talkPressed
            else -> colors.talk
        },
        label = "fill",
    )
    val ink = if (available) colors.onTalk else colors.onTalkDisabled
    val label = when {
        transmitting && tapToTalk -> "Tap to stop"
        transmitting -> "Talking"
        !available -> "No one connected"
        tapToTalk -> "Tap to talk"
        else -> "Hold to talk"
    }

    Box(modifier.size(Spacing.talkButton + 48.dp), contentAlignment = Alignment.Center) {
        if (transmitting) TransmitRing()

        Box(
            Modifier
                .size(Spacing.talkButton)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .shadow(if (available && !transmitting) 8.dp else 0.dp, CircleShape)
                .clip(CircleShape)
                .background(fill)
                .pointerInput(tapToTalk) {
                    if (tapToTalk) {
                        detectTapGestures(onTap = { if (isTransmitting) end() else begin() })
                    } else {
                        detectTapGestures(onPress = {
                            pressed = true
                            val started = begin()
                            tryAwaitRelease()
                            pressed = false
                            if (started) end()
                        })
                    }
                }
                .clearAndSetSemantics {
                    role = Role.Button
                    contentDescription = label
                    onClick(label = if (isTransmitting) "Stop talking" else "Start talking") {
                        if (isTransmitting) end() else begin()
                        true
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.Mic, contentDescription = null, tint = ink, modifier = Modifier.size(52.dp))
                Spacer(Modifier.height(6.dp))
                Text(label, style = MaterialTheme.typography.titleMedium, color = ink)
            }
        }
    }
}

@Composable
private fun TransmitRing() {
    val color = NearwaveTheme.colors.talk
    val transition = rememberInfiniteTransition(label = "transmit")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1_100), RepeatMode.Restart),
        label = "ring",
    )
    Canvas(Modifier.fillMaxSize()) {
        val base = Spacing.talkButton.toPx() / 2
        val extra = 24.dp.toPx()
        drawCircle(
            color = color.copy(alpha = 0.55f * (1f - progress)),
            radius = base + extra * progress,
            style = Stroke(width = 6.dp.toPx()),
        )
    }
}
