package app.nearwave.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import app.nearwave.core.Identity
import app.nearwave.ui.theme.ChannelCodeStyle
import app.nearwave.ui.theme.Spacing

private enum class Step { Name, Choose, Create, Join }

@Composable
fun OnboardingScreen(identity: Identity, onDone: () -> Unit) {
    var step by rememberSaveable {
        mutableStateOf(if (identity.displayName.isNullOrBlank()) Step.Name else Step.Choose)
    }
    var name by rememberSaveable { mutableStateOf(identity.displayName.orEmpty()) }
    val newCode = rememberSaveable { Identity.newChannelCode() }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screen, vertical = Spacing.xl),
        ) {
            if (step != Step.Name) {
                IconButton(onClick = {
                    step = if (step == Step.Choose) Step.Name else Step.Choose
                }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
            } else {
                Spacer(Modifier.height(48.dp))
            }
            Spacer(Modifier.height(Spacing.xl))

            when (step) {
                Step.Name -> NameStep(name, onName = { name = it.take(Identity.MAX_NAME_LENGTH) }) {
                    identity.displayName = name
                    step = Step.Choose
                }
                Step.Choose -> ChooseStep(onCreate = { step = Step.Create }, onJoin = { step = Step.Join })
                Step.Create -> CreateStep(newCode) {
                    identity.channelCode = newCode
                    onDone()
                }
                Step.Join -> JoinStep { code ->
                    identity.channelCode = code
                    onDone()
                }
            }
        }
    }
}

@Composable
private fun NameStep(name: String, onName: (String) -> Unit, onContinue: () -> Unit) {
    Text("Talk to the people next to you", style = MaterialTheme.typography.displaySmall)
    Spacer(Modifier.height(Spacing.m))
    Text(
        "Nearwave works like a walkie-talkie between phones close together. No mobile data or Wi-Fi network needed, so it keeps working on planes, buses and trains.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(Spacing.xxl))
    OutlinedTextField(
        value = name,
        onValueChange = onName,
        label = { Text("Your name") },
        placeholder = { Text("For example, Papa or Asha") },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { if (name.isNotBlank()) onContinue() }),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(Spacing.s))
    Text("Others see this name on their radar.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(Spacing.xl))
    PrimaryButton("Continue", enabled = name.isNotBlank(), onClick = onContinue)
}

@Composable
private fun ChooseStep(onCreate: () -> Unit, onJoin: () -> Unit) {
    Text("Set up your channel", style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(Spacing.m))
    Text(
        "Everyone who should hear each other uses the same channel. One person creates it, the others join with its code.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(Spacing.xl))
    ChoiceCard(Icons.Rounded.AddCircleOutline, "Create a new channel", "You'll get a code to share with your family", onCreate)
    Spacer(Modifier.height(Spacing.m))
    ChoiceCard(Icons.AutoMirrored.Rounded.Login, "Join with a code", "Someone has already created the channel", onJoin)
}

@Composable
private fun CreateStep(code: String, onDone: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    Text("Your channel code", style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(Spacing.m))
    Text(
        "Read this out or show it to everyone who should hear you. They type it once on their phone.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(Spacing.xl))
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(Spacing.xl), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(code, style = ChannelCodeStyle)
        }
    }
    Spacer(Modifier.height(Spacing.m))
    OutlinedButton(
        onClick = { clipboard.setText(AnnotatedString(code)) },
        modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.touch),
    ) {
        Icon(Icons.Rounded.ContentCopy, contentDescription = null)
        Spacer(Modifier.width(Spacing.s))
        Text("Copy code", style = MaterialTheme.typography.labelLarge)
    }
    Spacer(Modifier.height(Spacing.s))
    PrimaryButton("Start using this channel", onClick = onDone)
}

@Composable
private fun JoinStep(onJoin: (String) -> Unit) {
    var input by rememberSaveable { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    val code = Identity.normalizeChannelCode(input)

    Text("Enter the channel code", style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(Spacing.m))
    Text(
        "Ask the person who created the channel. It has 8 letters and numbers.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(Spacing.xl))
    OutlinedTextField(
        value = input,
        onValueChange = {
            input = it.take(12)
            showError = false
        },
        label = { Text("Channel code") },
        placeholder = { Text("ABCD-2345") },
        singleLine = true,
        isError = showError,
        supportingText = if (showError) {
            { Text("That code doesn't look right. Check it has 8 letters and numbers.") }
        } else null,
        textStyle = ChannelCodeStyle.copy(fontSize = MaterialTheme.typography.headlineSmall.fontSize),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { if (code != null) onJoin(code) else showError = true }),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(Spacing.xl))
    PrimaryButton("Join channel", enabled = input.isNotBlank()) {
        if (code != null) onJoin(code) else showError = true
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChoiceCard(icon: ImageVector, title: String, body: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        tonalElevation = 0.dp,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(Spacing.l), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(Spacing.l))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth().heightIn(min = Spacing.touch),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}
