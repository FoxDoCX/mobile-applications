package ru.recalltoast.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import ru.recalltoast.app.MainActivity
import ru.recalltoast.app.R
import ru.recalltoast.app.RecallApplication
import ru.recalltoast.app.domain.model.ReminderTrigger
import ru.recalltoast.app.telephony.CallStateMonitor
import ru.recalltoast.app.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ReminderForegroundService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var userPresentReceiver: BroadcastReceiver? = null
    private var callMonitor: CallStateMonitor? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        createChannel()
        startAsForeground()
        registerUserPresent()
        maybeRegisterCallMonitor()
        Logger.d("FGS created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopEverything()
                stopSelf()
                // START_NOT_STICKY: user explicitly stopped; do not recreate.
                return START_NOT_STICKY
            }
            ACTION_SHOW_NOW -> {
                app().reminderPresenter.showNow()
            }
            ACTION_PAUSE -> {
                scope.launch {
                    app().settingsRepository.setPaused(true)
                    app().reminderScheduler.applyCurrentSettings()
                }
            }
            ACTION_START, null -> {
                startAsForeground()
                maybeRegisterCallMonitor()
            }
        }
        // START_STICKY: if the process is reclaimed while reminders are enabled,
        // ask the system to recreate the service so unlock listening can resume.
        // Work is event-driven; idle cost is only the persistent notification.
        return START_STICKY
    }

    override fun onDestroy() {
        unregisterUserPresent()
        callMonitor?.unregister()
        callMonitor = null
        app().reminderPresenter.dismissOverlay()
        scope.cancel()
        isRunning = false
        Logger.d("FGS destroyed")
        super.onDestroy()
    }

    private fun stopEverything() {
        unregisterUserPresent()
        callMonitor?.unregister()
        callMonitor = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        isRunning = false
    }

    private fun startAsForeground() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun registerUserPresent() {
        if (userPresentReceiver != null) return
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action != Intent.ACTION_USER_PRESENT) return
                app().reminderPresenter.showForTrigger(ReminderTrigger.UNLOCK)
            }
        }
        val filter = IntentFilter(Intent.ACTION_USER_PRESENT)
        ContextCompat.registerReceiver(
            this,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        userPresentReceiver = receiver
    }

    private fun unregisterUserPresent() {
        userPresentReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (_: IllegalArgumentException) {
            }
        }
        userPresentReceiver = null
    }

    private fun maybeRegisterCallMonitor() {
        scope.launch(Dispatchers.IO) {
            val settings = app().settingsRepository.getSettings()
            if (settings.afterCallEnabled) {
                if (callMonitor == null) {
                    callMonitor = app().callStateMonitorFactory.create {
                        app().reminderPresenter.showForTrigger(ReminderTrigger.AFTER_CALL)
                    }.also { it.register() }
                }
            } else {
                callMonitor?.unregister()
                callMonitor = null
            }
        }
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            1,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            pendingFlags()
        )
        val showIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, ReminderForegroundService::class.java).setAction(ACTION_SHOW_NOW),
            pendingFlags()
        )
        val pauseIntent = PendingIntent.getService(
            this,
            3,
            Intent(this, ReminderForegroundService::class.java).setAction(ACTION_PAUSE),
            pendingFlags()
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(0, getString(R.string.action_show_now), showIntent)
            .addAction(0, getString(R.string.action_pause), pauseIntent)
            .addAction(0, getString(R.string.action_open), openIntent)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            description = getString(R.string.notification_channel_desc)
            setShowBadge(false)
            enableLights(false)
            enableVibration(false)
            setSound(null, null)
        }
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }

    private fun pendingFlags(): Int {
        return PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    }

    private fun app() = application as RecallApplication

    companion object {
        const val ACTION_START = "ru.recalltoast.app.action.START"
        const val ACTION_STOP = "ru.recalltoast.app.action.STOP"
        const val ACTION_SHOW_NOW = "ru.recalltoast.app.action.SHOW_NOW"
        const val ACTION_PAUSE = "ru.recalltoast.app.action.PAUSE"
        const val CHANNEL_ID = "recall_active"
        const val NOTIFICATION_ID = 1001

        @Volatile
        var isRunning: Boolean = false
            private set
    }
}
