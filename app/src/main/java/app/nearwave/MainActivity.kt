package app.nearwave

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import app.nearwave.radio.RadioController
import app.nearwave.service.RadioService
import app.nearwave.ui.Permissions
import app.nearwave.ui.screens.HomeScreen
import app.nearwave.ui.screens.OnboardingScreen
import app.nearwave.ui.screens.PermissionScreen
import app.nearwave.ui.theme.NearwaveTheme

class MainActivity : ComponentActivity() {

    private val radio: RadioController get() = (application as NearwaveApp).radio

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NearwaveTheme {
                NearwaveRoot(radio)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        radio.appVisible = true
    }

    override fun onStop() {
        radio.appVisible = false
        super.onStop()
    }
}

@Composable
private fun NearwaveRoot(radio: RadioController) {
    val context = LocalContext.current
    var setUp by remember { mutableStateOf(radio.identity.isSetUp) }
    var permitted by remember { mutableStateOf(Permissions.allRequiredGranted(context)) }

    // Re-check when returning from the system settings screen.
    LifecycleResumeEffect(Unit) {
        permitted = Permissions.allRequiredGranted(context)
        onPauseOrDispose { }
    }

    when {
        !setUp -> OnboardingScreen(radio.identity, onDone = { setUp = true })
        !permitted -> PermissionScreen(onGranted = { permitted = true })
        else -> {
            LaunchedEffect(Unit) { RadioService.start(context) }
            HomeScreen(radio, onLeftChannel = { setUp = false })
        }
    }
}
