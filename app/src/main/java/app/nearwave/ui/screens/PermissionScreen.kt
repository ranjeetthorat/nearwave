package app.nearwave.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.nearwave.ui.Permissions
import app.nearwave.ui.theme.Spacing

@Composable
fun PermissionScreen(onGranted: () -> Unit) {
    val context = LocalContext.current
    var askedOnce by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        askedOnce = true
        if (Permissions.allRequiredGranted(context)) onGranted()
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screen, vertical = Spacing.xl),
        ) {
            Spacer(Modifier.height(48.dp))
            Text("Allow Nearwave to reach nearby phones", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(Spacing.xl))
            Reason(Icons.Rounded.Bluetooth, "Nearby devices", "Finds and connects to phones on your channel using Bluetooth and Wi-Fi.")
            Reason(Icons.Rounded.Mic, "Microphone", "Used only while you press the talk button.")
            Reason(Icons.Rounded.NotificationsActive, "Notifications", "So a buzz reaches you when the screen is off.")
            Spacer(Modifier.height(Spacing.s))
            Text(
                "Android counts Bluetooth scanning as location access, so it may ask about location. Nearwave never uses GPS and never shares where you are.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Spacing.xxl))
            if (askedOnce && !Permissions.allRequiredGranted(context)) {
                Text(
                    "Some permissions are still off. Open settings, tap Permissions and allow them.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.height(Spacing.m))
                PrimaryButton("Open settings") {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
                Spacer(Modifier.height(Spacing.s))
            }
            PrimaryButton(if (askedOnce) "Try again" else "Allow access") {
                launcher.launch((Permissions.required + Permissions.optional).toTypedArray())
            }
        }
    }
}

@Composable
private fun Reason(icon: ImageVector, title: String, body: String) {
    Row(Modifier.padding(vertical = Spacing.m)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(Spacing.l))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
