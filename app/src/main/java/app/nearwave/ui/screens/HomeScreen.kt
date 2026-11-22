package app.nearwave.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BluetoothDisabled
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.LocationOff
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SyncProblem
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.nearwave.core.Incoming
import app.nearwave.core.LogEntry
import app.nearwave.core.Problem
import app.nearwave.core.RadioState
import app.nearwave.radio.RadioController
import app.nearwave.service.RadioService
import app.nearwave.ui.components.BigActionButton
import app.nearwave.ui.components.NoticeBanner
import app.nearwave.ui.components.Radar
import app.nearwave.ui.components.TalkButton
import app.nearwave.ui.theme.NearwaveTheme
import app.nearwave.ui.theme.Spacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(radio: RadioController, onLeftChannel: () -> Unit) {
    val state by radio.state.collectAsStateWithLifecycle()
    val identity = radio.identity
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val colors = NearwaveTheme.colors

    var showMessages by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var tapToTalk by remember { mutableStateOf(identity.tapToTalk) }
    var seenLogId by rememberSaveable { mutableLongStateOf(0L) }
    val unread = state.log.count { !it.mine && it.kind == LogEntry.Kind.Text && it.id > seenLogId }
    val code = identity.channelCode.orEmpty()

    fun say(message: String) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(message)
        }
    }

    LifecycleResumeEffect(Unit) {
        radio.setRadarVisible(true)
        onPauseOrDispose { radio.setRadarVisible(false) }
    }

    val incoming = state.incoming
    LaunchedEffect(incoming?.entry?.id) {
        if (incoming != null) {
            delay(8_000)
            radio.dismissIncoming()
        }
    }
    if (showMessages) {
        LaunchedEffect(state.log.size) { seenLogId = state.log.lastOrNull()?.id ?: 0L }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Spacing.screen),
        ) {
            TopBar(code = code, onSettings = { showSettings = true })

            when {
                !state.running -> NoticeBanner(
                    icon = Icons.Rounded.PowerSettingsNew,
                    title = "Nearwave is off",
                    body = "Others can't reach you right now.",
                    container = MaterialTheme.colorScheme.secondaryContainer,
                    content = MaterialTheme.colorScheme.onSecondaryContainer,
                    actionLabel = "Turn on",
                    onAction = { RadioService.start(context) },
                    modifier = Modifier.padding(bottom = Spacing.s),
                )
                state.problem != null -> ProblemBanner(state.problem!!, state.problemDetail, context, onRetry = radio::retryLink)
            }

            if (incoming != null) {
                IncomingBanner(
                    incoming,
                    onReply = {
                        radio.dismissIncoming()
                        showMessages = true
                    },
                    onDismiss = radio::dismissIncoming,
                )
            }

            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Radar(state.members)
            }

            StatusLine(state, code)
            Spacer(Modifier.height(Spacing.l))

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                BigActionButton(
                    icon = Icons.Rounded.Vibration,
                    label = "Buzz",
                    container = colors.buzzContainer,
                    content = colors.onBuzzContainer,
                    onClick = {
                        if (radio.buzz()) say("Buzzed everyone on your channel") else say("No one is connected yet")
                    },
                    modifier = Modifier.weight(1f),
                )
                BigActionButton(
                    icon = Icons.Rounded.ChatBubble,
                    label = "Messages",
                    container = MaterialTheme.colorScheme.secondaryContainer,
                    content = MaterialTheme.colorScheme.onSecondaryContainer,
                    onClick = { showMessages = true },
                    badge = unread,
                    modifier = Modifier.weight(1f),
                )
            }

            Box(Modifier.fillMaxWidth().padding(vertical = Spacing.s), contentAlignment = Alignment.Center) {
                TalkButton(
                    transmitting = state.transmitting,
                    available = state.connectedCount > 0,
                    tapToTalk = tapToTalk,
                    onStart = radio::startTalking,
                    onStop = radio::stopTalking,
                    onUnavailable = { say("No one is connected yet. Move closer or check they have Nearwave open.") },
                )
            }
        }
    }

    if (showMessages) {
        MessagesSheet(
            log = state.log,
            onSend = radio::sendText,
            onDismiss = { showMessages = false },
        )
    }
    if (showSettings) {
        SettingsSheet(
            radio = radio,
            running = state.running,
            tapToTalk = tapToTalk,
            onTapToTalk = {
                tapToTalk = it
                identity.tapToTalk = it
            },
            onLeftChannel = {
                showSettings = false
                onLeftChannel()
            },
            onDismiss = { showSettings = false },
        )
    }
}

@Composable
private fun TopBar(code: String, onSettings: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = Spacing.s, bottom = Spacing.m), verticalAlignment = Alignment.CenterVertically) {
        Text("Nearwave", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ) {
            Text(
                "Channel $code",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = Spacing.m, vertical = 6.dp),
            )
        }
        IconButton(onClick = onSettings) {
            Icon(Icons.Rounded.Settings, contentDescription = "Settings")
        }
    }
}

@Composable
private fun StatusLine(state: RadioState, code: String) {
    val colors = NearwaveTheme.colors
    val talker = state.someoneTalking
    val (dot, text) = when {
        !state.running -> colors.offline to "Off"
        state.transmitting -> colors.talk to "You're talking. Everyone connected can hear you."
        talker != null -> colors.talk to "${talker.name} is talking"
        state.connectedCount == 1 -> colors.connected to "Connected to 1 person"
        state.connectedCount > 1 -> colors.connected to "Connected to ${state.connectedCount} people"
        state.members.isEmpty() -> colors.offline to "Looking for others. They join with code $code."
        else -> colors.offline to "Looking for your channel"
    }
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(color = dot)) { append("\u25CF  ") }
            append(text)
        },
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ProblemBanner(problem: Problem, detail: String?, context: Context, onRetry: () -> Unit) {
    val (icon, title, body) = when (problem) {
        Problem.BluetoothOff -> Triple(
            Icons.Rounded.BluetoothDisabled, "Bluetooth is off",
            "Turn it on to find people nearby. It can stay on in flight mode.",
        )
        Problem.LocationOff -> Triple(
            Icons.Rounded.LocationOff, "Turn on Location",
            "Your phone needs it switched on to find others nearby. Nearwave doesn't use GPS.",
        )
        Problem.LinkFailed -> Triple(
            Icons.Rounded.SyncProblem, "Couldn't start searching",
            "Retrying automatically." + (detail?.let { "\n$it" } ?: ""),
        )
    }
    NoticeBanner(
        icon = icon,
        title = title,
        body = body,
        container = MaterialTheme.colorScheme.errorContainer,
        content = MaterialTheme.colorScheme.onErrorContainer,
        actionLabel = if (problem == Problem.LinkFailed) "Try again" else "Settings",
        onAction = {
            when (problem) {
                Problem.BluetoothOff -> open(context, Settings.ACTION_BLUETOOTH_SETTINGS)
                Problem.LocationOff -> open(context, Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                Problem.LinkFailed -> onRetry()
            }
        },
        modifier = Modifier.padding(bottom = Spacing.s),
    )
}

@Composable
private fun IncomingBanner(incoming: Incoming, onReply: () -> Unit, onDismiss: () -> Unit) {
    val entry = incoming.entry
    val colors = NearwaveTheme.colors
    if (entry.kind == LogEntry.Kind.Buzz) {
        NoticeBanner(
            icon = Icons.Rounded.Vibration,
            title = "${entry.fromName} is buzzing you",
            container = colors.buzz,
            content = colors.onBuzz,
            actionLabel = "Reply",
            onAction = onReply,
            onDismiss = onDismiss,
            modifier = Modifier.padding(bottom = Spacing.s),
        )
    } else {
        NoticeBanner(
            icon = Icons.Rounded.ChatBubble,
            title = entry.fromName,
            body = entry.body,
            container = MaterialTheme.colorScheme.primaryContainer,
            content = MaterialTheme.colorScheme.onPrimaryContainer,
            actionLabel = "Reply",
            onAction = onReply,
            onDismiss = onDismiss,
            modifier = Modifier.padding(bottom = Spacing.s),
        )
    }
}

internal fun open(context: Context, action: String) {
    runCatching { context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

