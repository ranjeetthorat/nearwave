package app.nearwave.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.nearwave.ui.theme.NearwaveTheme
import app.nearwave.ui.theme.Palette

/** A person: initials on a colour that stays the same for them on every phone. */
@Composable
fun Avatar(
    name: String,
    id: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    faded: Boolean = false,
    talking: Boolean = false,
) {
    val fill = avatarColor(id)
    val ring = if (talking) {
        Modifier.border(3.dp, NearwaveTheme.colors.talk, CircleShape).padding(5.dp)
    } else Modifier
    Box(
        modifier
            .size(size)
            .then(ring)
            .clip(CircleShape)
            .background(if (faded) fill.copy(alpha = 0.35f) else fill),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials(name),
            color = Color.White.copy(alpha = if (faded) 0.8f else 1f),
            fontWeight = FontWeight.SemiBold,
            fontSize = (size.value * 0.36f).sp,
        )
    }
}

fun avatarColor(id: String): Color =
    Palette.Avatars[(id.hashCode() and 0x7fffffff) % Palette.Avatars.size]

fun initials(name: String): String {
    val words = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when {
        words.isEmpty() -> "?"
        words.size == 1 -> words[0].take(2).uppercase()
        else -> (words[0].take(1) + words[1].take(1)).uppercase()
    }
}
