package app.nearwave.ui.screens

import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardCapitalization
import app.nearwave.core.Identity
import app.nearwave.radio.RadioController
import app.nearwave.service.RadioService
import app.nearwave.ui.theme.ChannelCodeStyle
import app.nearwave.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    radio: RadioController,
    running: Boolean,
    tapToTalk: Boolean,
    onTapToTalk: (Boolean) -> Unit,
    onLeftChannel: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val identity = radio.identity
    val savedName = identity.displayName.orEmpty()
    var name by remember { mutableStateOf(savedName) }
    var confirmLeave by remember { mutableStateOf(false) }
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "?"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screen)
                .padding(bottom = Spacing.xl),
        ) {
            Text("Settings", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(Spacing.l))

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Nearwave on", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "When off, nobody can reach you.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = running,
                    onCheckedChange = { on -> if (on) RadioService.start(context) else RadioService.stop(context) },
                )
            }

            Divider()
            SectionTitle("Your name")
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(Identity.MAX_NAME_LENGTH) },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(),
            )
            if (name.isNotBlank() && name.trim() != savedName) {
                TextButton(onClick = {
                    identity.displayName = name
                    if (running) radio.restart()
                }) { Text("Save name", style = MaterialTheme.typography.labelLarge) }
            }

            Divider()
            SectionTitle("Talk button")
            Choice("Hold to talk, release to stop", selected = !tapToTalk) { onTapToTalk(false) }
            Choice("Tap to start, tap again to stop", selected = tapToTalk) { onTapToTalk(true) }

            Divider()
            SectionTitle("Channel")
            Text(identity.channelCode.orEmpty(), style = ChannelCodeStyle.copy(fontSize = MaterialTheme.typography.headlineSmall.fontSize))
            Spacer(Modifier.height(Spacing.s))
            Row {
                OutlinedButton(onClick = { clipboard.setText(AnnotatedString(identity.channelCode.orEmpty())) }) {
                    Text("Copy code")
                }
                Spacer(Modifier.width(Spacing.s))
                TextButton(onClick = { confirmLeave = true }) {
                    Text("Leave channel", color = MaterialTheme.colorScheme.error)
                }
            }

            Divider()
            SectionTitle("Running in the background")
            Text(
                "Some phones close apps to save battery, which stops buzzes from arriving. Find Nearwave in this list and choose Don't optimise or Unrestricted.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Spacing.s))
            OutlinedButton(onClick = { open(context, Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS) }) {
                Text("Open battery settings")
            }

            Divider()
            Text(
                "Nearwave $version. Open source.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            title = { Text("Leave this channel?") },
            text = { Text("You'll stop hearing everyone on it. To come back, you need the code again.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmLeave = false
                    RadioService.stop(context)
                    identity.leaveChannel()
                    onLeftChannel()
                }) { Text("Leave", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmLeave = false }) { Text("Stay") }
            },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = Spacing.s))
}

@Composable
private fun Divider() {
    HorizontalDivider(Modifier.padding(vertical = Spacing.l), color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun Choice(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = Spacing.touch)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(Spacing.m))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}
