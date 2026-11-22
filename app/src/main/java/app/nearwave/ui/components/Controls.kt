package app.nearwave.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.nearwave.ui.theme.Spacing

/** Large, labelled secondary actions (Buzz, Messages). Always icon plus words. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BigActionButton(
    icon: ImageVector,
    label: String,
    container: Color,
    content: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: Int = 0,
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = container,
        contentColor = content,
        modifier = modifier.heightIn(min = 64.dp),
    ) {
        Row(
            Modifier.padding(horizontal = Spacing.l, vertical = Spacing.m),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(10.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
            if (badge > 0) {
                Spacer(Modifier.width(Spacing.s))
                Surface(shape = CircleShape, color = content, contentColor = container) {
                    Box(Modifier.heightIn(min = 22.dp).padding(horizontal = 7.dp), contentAlignment = Alignment.Center) {
                        Text(if (badge > 9) "9+" else badge.toString(), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

/** A message across the top of the screen: an incoming buzz, a text, or a problem to fix. */
@Composable
fun NoticeBanner(
    icon: ImageVector,
    title: String,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
    body: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
) {
    Surface(shape = MaterialTheme.shapes.large, color = container, contentColor = content, modifier = modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = Spacing.l, top = Spacing.m, bottom = Spacing.m, end = Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(Spacing.m))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                if (body != null) Text(body, style = MaterialTheme.typography.bodyMedium)
            }
            if (actionLabel != null && onAction != null) {
                TextButton(onClick = onAction) { Text(actionLabel, color = content, style = MaterialTheme.typography.labelLarge) }
            }
            if (onDismiss != null) {
                IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, contentDescription = "Dismiss") }
            }
        }
    }
}
