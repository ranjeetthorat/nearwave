package app.nearwave.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import app.nearwave.NearwaveApp

/**
 * Keeps the radio alive while the screen is off or another app is open, so a buzz can
 * always get through. Shows a quiet "Nearwave is on" notification with a Turn off button.
 */
class RadioService : Service() {

    private val radio get() = (application as NearwaveApp).radio

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            radio.stop()
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        val type = if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE else 0
        try {
            ServiceCompat.startForeground(this, Alerts.ID_RADIO, radio.alerts.radioNotification(0), type)
        } catch (e: Exception) {
            Log.w("RadioService", "could not start in foreground", e)
            stopSelf()
            return START_NOT_STICKY
        }
        radio.start()
        return START_STICKY
    }

    override fun onDestroy() {
        radio.stop()
        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP = "app.nearwave.STOP"

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, RadioService::class.java))
        }

        fun stop(context: Context) {
            context.startService(Intent(context, RadioService::class.java).setAction(ACTION_STOP))
        }
    }
}
