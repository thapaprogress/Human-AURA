package com.example.natureaura.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Play-compliant mediaPlayback Foreground Service keeping healing frequency
 * synthesis active while the screen is locked or app is backgrounded.
 */
class TherapyForegroundService : Service() {

    companion object {
        private const val CHANNEL_ID = "nature_aura_therapy_playback"
        private const val NOTIFICATION_ID = 4102
        const val ACTION_START_OR_UPDATE = "com.example.natureaura.ACTION_THERAPY_START"
        const val ACTION_TOGGLE = "com.example.natureaura.ACTION_THERAPY_TOGGLE"
        const val ACTION_STOP = "com.example.natureaura.ACTION_THERAPY_STOP"

        fun syncForegroundService(context: Context, shouldRun: Boolean) {
            runCatching {
                if (shouldRun) {
                    val intent = Intent(context, TherapyForegroundService::class.java).apply {
                        action = ACTION_START_OR_UPDATE
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(intent)
                    } else {
                        context.startService(intent)
                    }
                } else {
                    context.stopService(Intent(context, TherapyForegroundService::class.java))
                }
            }
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var stateJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannel()
        stateJob = scope.launch {
            FrequencyTherapyEngine.isPlaying.collect { playing ->
                if (!playing) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                } else {
                    updateNotification()
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                FrequencyTherapyEngine.stop()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_TOGGLE -> {
                FrequencyTherapyEngine.togglePlayPause()
                if (!FrequencyTherapyEngine.isPlaying.value) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    return START_NOT_STICKY
                }
            }
        }

        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        return START_STICKY
    }

    private fun updateNotification() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        manager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val band = FrequencyTherapyEngine.activeBand.value
        val sec = FrequencyTherapyEngine.secondaryBand.value
        val title = if (sec != null) "${band.title} + ${sec.title}" else band.title
        val subtitle = "${band.frequencyLabel} · Offline Sound Sanctuary"

        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, TherapyForegroundService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(openAppIntent)
            .addAction(0, "Stop", stopIntent)
            .build()
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Healing Frequency Playback",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Controls active offline frequency sound therapy"
            }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        stateJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }
}
