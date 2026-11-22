package app.nearwave.ui.screens

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.nearwave.core.LogEntry
import app.nearwave.core.Protocol
import app.nearwave.ui.theme.NearwaveTheme
import app.nearwave.ui.theme.Spacing
import java.util.Date

/** One-tap messages for the most common things people need to say while travelling. */
private val QuickMessages = listOf(
    "Are you okay?",
    "I'm okay",
    "Come to me",
    "I need help",
    "Yes",
    "No",
    "Wait for me",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesSheet(
    log: List<LogEntry>,
    onSend: (String) -> Boolean,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var draft by rememberSaveable { mutableStateOf("") }
    var failed by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    fun send(text: String) {
        if (onSend(text)) {
            draft = ""
            failed = false
        } else {
            failed = true
        }
    }

    LaunchedEffect(log.size) {
        if (log.isNotEmpty()) listState.animateScrollToItem(log.size - 1)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .imePadding()
                .padding(horizontal = Spacing.screen),
        ) {
            Text("Messages", style = MaterialTheme.typography.headlineSmall)

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.s),
                contentPadding = PaddingValues(vertical = Spacing.m),
            ) {
                if (log.isEmpty()) {
                    item {
                        Text(
                            "No messages yet. Tap a quick message below or type your own.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(log, key = { it.id }) { LogRow(it) }
            }

            if (failed) {
                Text(
                    "Not sent. No one is connected right now.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = Spacing.s),
                )
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                items(QuickMessages) { message ->
                    SuggestionChip(
                        onClick = { send(message) },
                        label = { Text(message, style = MaterialTheme.typography.labelLarge) },
                        modifier = Modifier.height(44.dp),
                    )
                }
            }
            Spacer(Modifier.height(Spacing.s))

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it.take(Protocol.MAX_TEXT_LENGTH) },
                    placeholder = { Text("Type a message") },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    maxLines = 3,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(Spacing.s))
                FilledIconButton(
                    onClick = { send(draft) },
                    enabled = draft.isNotBlank(),
                    modifier = Modifier.size(Spacing.touch),
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send")
                }
            }
            Spacer(Modifier.height(Spacing.m))
        }
    }
}

@Composable
private fun LogRow(entry: LogEntry) {
    val context = LocalContext.current
    val time = remember(entry.at) { DateFormat.getTimeFormat(context).format(Date(entry.at)) }

    if (entry.kind == LogEntry.Kind.Buzz) {
        Text(
            if (entry.mine) "You buzzed everyone at $time" else "${entry.fromName} buzzed you at $time",
            style = MaterialTheme.typography.labelMedium,
            color = NearwaveTheme.colors.buzz,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs),
        )
        return
    }

    val mine = entry.mine
    Box(Modifier.fillMaxWidth(), contentAlignment = if (mine) Alignment.CenterEnd else Alignment.CenterStart) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp, topEnd = 18.dp,
                bottomStart = if (mine) 18.dp else 6.dp,
                bottomEnd = if (mine) 6.dp else 18.dp,
            ),
            color = if (mine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = if (mine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.widthIn(max = 300.dp),
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                if (!mine) Text(entry.fromName, style = MaterialTheme.typography.labelMedium)
                Text(entry.body, style = MaterialTheme.typography.bodyLarge)
                Text(time, style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.End))
            }
        }
    }
}
