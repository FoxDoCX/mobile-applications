package ru.recalltoast.app.trigger

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.recalltoast.app.RecallApplication
import ru.recalltoast.app.domain.model.AppSettings
import ru.recalltoast.app.domain.model.ReminderTrigger
import ru.recalltoast.app.receiver.UserPresentReceiver
import ru.recalltoast.app.util.Logger

/**
 * Enables/disables optional receivers based on settings. No heavy event engine.
 */
class TriggerRegistrar(private val context: Context) {

    private val appContext = context.applicationContext
    @Volatile private var latest: AppSettings = AppSettings()
    private var powerReceiver: BroadcastReceiver? = null
    private var headsetReceiver: BroadcastReceiver? = null
    private var screenReceiver: BroadcastReceiver? = null

    fun update(settings: AppSettings) {
        latest = settings
        setComponentEnabled(
            UserPresentReceiver::class.java,
            settings.enabled && !settings.paused
        )
        refreshPower(settings)
        refreshHeadset(settings)
        refreshScreen(settings)
        refreshSchedule(settings)
    }

    private fun refreshPower(settings: AppSettings) {
        unregister(powerReceiver)
        powerReceiver = null
        val need = settings.enabled && !settings.paused &&
            (settings.powerConnectedEnabled || settings.powerDisconnectedEnabled || settings.lowBatteryEnabled)
        if (!need) return

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val s = latest
                val app = appContext as RecallApplication
                when (intent?.action) {
                    Intent.ACTION_POWER_CONNECTED ->
                        if (s.powerConnectedEnabled) {
                            app.reminderPresenter.showForTrigger(ReminderTrigger.POWER_CONNECTED)
                        }
                    Intent.ACTION_POWER_DISCONNECTED ->
                        if (s.powerDisconnectedEnabled) {
                            app.reminderPresenter.showForTrigger(ReminderTrigger.POWER_DISCONNECTED)
                        }
                    Intent.ACTION_BATTERY_LOW ->
                        if (s.lowBatteryEnabled) {
                            app.reminderPresenter.showForTrigger(ReminderTrigger.LOW_BATTERY)
                        }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_BATTERY_LOW)
        }
        ContextCompat.registerReceiver(appContext, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        powerReceiver = receiver
    }

    private fun refreshHeadset(settings: AppSettings) {
        unregister(headsetReceiver)
        headsetReceiver = null
        if (!settings.enabled || settings.paused || !settings.headsetEnabled) return

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action != Intent.ACTION_HEADSET_PLUG) return
                if (intent.getIntExtra("state", 0) == 1) {
                    (appContext as RecallApplication).reminderPresenter
                        .showForTrigger(ReminderTrigger.HEADSET_CONNECTED)
                }
            }
        }
        ContextCompat.registerReceiver(
            appContext,
            receiver,
            IntentFilter(Intent.ACTION_HEADSET_PLUG),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        headsetReceiver = receiver
    }

    private fun refreshScreen(settings: AppSettings) {
        unregister(screenReceiver)
        screenReceiver = null
        if (!settings.enabled || settings.paused || !settings.screenOnEnabled) return

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action != Intent.ACTION_SCREEN_ON) return
                (appContext as RecallApplication).reminderPresenter
                    .showForTrigger(ReminderTrigger.SCREEN_ON)
            }
        }
        ContextCompat.registerReceiver(
            appContext,
            receiver,
            IntentFilter(Intent.ACTION_SCREEN_ON),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        screenReceiver = receiver
    }

    private fun refreshSchedule(settings: AppSettings) {
        val am = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = schedulePendingIntent()
        am.cancel(pi)
        if (!settings.enabled || settings.paused || !settings.scheduleEnabled) return
        val intervalMs = settings.scheduleIntervalMinutes.coerceAtLeast(15) * 60_000L
        val triggerAt = System.currentTimeMillis() + intervalMs
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } else {
            @Suppress("DEPRECATION")
            am.set(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    private fun schedulePendingIntent(): PendingIntent {
        val intent = Intent(appContext, ScheduleReceiver::class.java)
        return PendingIntent.getBroadcast(
            appContext,
            42,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun setComponentEnabled(clazz: Class<*>, enabled: Boolean) {
        val pm = appContext.packageManager
        val state = if (enabled) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        pm.setComponentEnabledSetting(
            ComponentName(appContext, clazz),
            state,
            PackageManager.DONT_KILL_APP
        )
    }

    private fun unregister(receiver: BroadcastReceiver?) {
        if (receiver == null) return
        try {
            appContext.unregisterReceiver(receiver)
        } catch (_: IllegalArgumentException) {
        }
    }
}

class ScheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val app = context.applicationContext as? RecallApplication ?: return
        app.reminderPresenter.showForTrigger(ReminderTrigger.SCHEDULE)
        CoroutineScope(Dispatchers.Default).launch {
            try {
                app.reminderScheduler.applyCurrentSettings()
            } catch (t: Throwable) {
                Logger.e("Schedule reschedule failed", t)
            }
        }
    }
}
