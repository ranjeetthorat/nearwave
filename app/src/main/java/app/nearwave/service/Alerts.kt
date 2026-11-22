package app.nearwave.service

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.nearwave.MainActivity
import app.nearwave.R

/** Notifications and vibration. Buzzes are deliberately strong and unmistakable. */
class Alerts(private val context: Context) {

    private val manager = NotificationManagerCompat.from(context)
    private var alertId = ID_FIRST_ALERT

    init {
        manager.createNotificationChannelsCompat(
            listOf(
                NotificationChannelCompat.Builder(CHANNEL_RADIO, NotificationManagerCompat.IMPORTANCE_LOW)
                    .setName("Radio status")
                    .setDescription("Shows that Nearwave is listening for your channel")
                    .setShowBadge(false)
                    .build(),
                NotificationChannelCompat.Builder(CHANNEL_ALERTS, NotificationManagerCompat.IMPORTANCE_HIGH)
                    .setName("Buzzes and messages")
                    .setDescription("When someone on your channel buzzes you or sends a message")
                    .setVibrationEnabled(true)
                    .setVibrationPattern(BUZZ_PATTERN)
                    .build(),
            )
        )
    }

    fun radioNotification(connected: Int): Notification {
        val text = when (connected) {
            0 -> "Listening for your channel"
            1 -> "Connected to 1 person"
            else -> "Connected to $connected people"
        }
        val stop = PendingIntent.getService(
            context, 1,
            Intent(context, RadioService::class.java).setAction(RadioService.ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(context, CHANNEL_RADIO)
            .setSmallIcon(R.drawable.ic_stat_nearwave)
            .setContentTitle("Nearwave is on")
            .setContentText(text)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(openApp())
            .addAction(0, "Turn off", stop)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    fun updateRadio(connected: Int) = notify(ID_RADIO, radioNotification(connected))

    fun buzz(from: String, notify: Boolean) {
        vibrate(BUZZ_PATTERN)
        if (notify) notify(nextAlertId(), alert("$from is buzzing you", "Tap to open Nearwave"))
    }

    fun message(from: String, body: String, notify: Boolean) {
        vibrate(MESSAGE_PATTERN)
        if (notify) notify(nextAlertId(), alert(from, body))
    }

    fun tick() = vibrate(longArrayOf(0, 30))

    private fun alert(title: String, text: String): Notification =
        NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_stat_nearwave)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(openApp())
            .build()

    private fun openApp(): PendingIntent = PendingIntent.getActivity(
        context, 0,
        Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun notify(id: Int, notification: Notification) {
        val allowed = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (allowed) runCatching { manager.notify(id, notification) }
    }

    private fun nextAlertId(): Int {
        alertId = if (alertId >= ID_FIRST_ALERT + 50) ID_FIRST_ALERT else alertId + 1
        return alertId
    }

    private fun vibrate(pattern: LongArray) {
        val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }
        runCatching { vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1)) }
    }

    companion object {
        const val ID_RADIO = 1
        private const val ID_FIRST_ALERT = 100
        private const val CHANNEL_RADIO = "radio"
        private const val CHANNEL_ALERTS = "alerts"
        private val BUZZ_PATTERN = longArrayOf(0, 450, 150, 450, 150, 800)
        private val MESSAGE_PATTERN = longArrayOf(0, 200, 120, 200)
    }
}
