package ru.recalltoast.app.service

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import ru.recalltoast.app.util.Logger
import ru.recalltoast.app.util.PermissionUtils

class ServiceController(private val context: Context) {

    fun startIfAllowed(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !PermissionUtils.hasPostNotificationsPermission(context)
        ) {
            Logger.w("POST_NOTIFICATIONS missing; cannot start FGS reliably")
            return false
        }
        return try {
            val intent = Intent(context, ReminderForegroundService::class.java).apply {
                action = ReminderForegroundService.ACTION_START
            }
            ContextCompat.startForegroundService(context, intent)
            true
        } catch (t: Throwable) {
            // Includes ForegroundServiceStartNotAllowedException on Android 12+
            Logger.e("FGS start not allowed", t)
            false
        }
    }

    fun stop() {
        val intent = Intent(context, ReminderForegroundService::class.java).apply {
            action = ReminderForegroundService.ACTION_STOP
        }
        try {
            context.startService(intent)
        } catch (_: Throwable) {
            context.stopService(Intent(context, ReminderForegroundService::class.java))
        }
    }

    fun isRunning(): Boolean = ReminderForegroundService.isRunning
}
