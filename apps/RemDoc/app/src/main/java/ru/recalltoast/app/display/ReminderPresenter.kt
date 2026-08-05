package ru.recalltoast.app.display

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.telephony.TelephonyManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.recalltoast.app.data.SettingsRepository
import ru.recalltoast.app.domain.ReminderSelector
import ru.recalltoast.app.domain.model.DisplayMode
import ru.recalltoast.app.domain.model.ReminderTrigger
import ru.recalltoast.app.util.Logger
import ru.recalltoast.app.util.PermissionUtils
import java.util.concurrent.atomic.AtomicLong

class ReminderPresenter(
    context: Context,
    private val repository: SettingsRepository,
    private val toastPresenter: SystemToastPresenter = SystemToastPresenter(context),
    private val overlayPresenter: OverlayReminderPresenter = OverlayReminderPresenter(context)
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val lastEventAt = AtomicLong(0L)

    fun showForTrigger(trigger: ReminderTrigger, force: Boolean = false) {
        scope.launch {
            try {
                present(trigger, force)
            } catch (t: Throwable) {
                Logger.e("Present failed", t)
            }
        }
    }

    fun showNow() = showForTrigger(ReminderTrigger.MANUAL, force = true)

    private suspend fun present(trigger: ReminderTrigger, force: Boolean) {
        val settings = repository.getSettings()
        val now = System.currentTimeMillis()

        if (!force) {
            val last = lastEventAt.get()
            if (now - last < 400L) {
                Logger.d("Dedup event")
                return
            }
            lastEventAt.set(now)

            when (trigger) {
                ReminderTrigger.UNLOCK -> if (!settings.unlockEnabled) return
                ReminderTrigger.AFTER_CALL -> if (!settings.afterCallEnabled) return
                ReminderTrigger.POWER_CONNECTED -> if (!settings.powerConnectedEnabled) return
                ReminderTrigger.POWER_DISCONNECTED -> if (!settings.powerDisconnectedEnabled) return
                ReminderTrigger.LOW_BATTERY -> if (!settings.lowBatteryEnabled) return
                ReminderTrigger.HEADSET_CONNECTED -> if (!settings.headsetEnabled) return
                ReminderTrigger.SCREEN_ON -> if (!settings.screenOnEnabled) return
                ReminderTrigger.SCHEDULE -> if (!settings.scheduleEnabled) return
                else -> Unit
            }

            if (settings.respectDnd && PermissionUtils.isDndBlocking(appContext)) {
                Logger.d("Blocked by DND")
                return
            }
            if (settings.skipWhenScreenOff && !PermissionUtils.isScreenInteractive(appContext) &&
                trigger != ReminderTrigger.UNLOCK
            ) {
                Logger.d("Screen off")
                return
            }
            if (settings.skipDuringCall && isInCall()) {
                Logger.d("In call")
                return
            }
            if (trigger == ReminderTrigger.UNLOCK && PermissionUtils.isKeyguardLocked(appContext)) {
                Logger.d("Still locked")
                return
            }
        }

        val delayMs = if (!force && trigger == ReminderTrigger.UNLOCK) {
            settings.unlockDelaySeconds.coerceIn(0, 10) * 1000L
        } else {
            0L
        }

        if (delayMs > 0) {
            withContext(Dispatchers.Main) {
                // Post delayed on main, then continue selection on background via showForTrigger path.
            }
            kotlinx.coroutines.delay(delayMs)
        }

        val context = ReminderSelector.selectionContext(System.currentTimeMillis(), trigger)
        val selection = if (force) {
            val candidates = settings.reminders.filter { it.enabled && it.text.isNotBlank() }
            val reminder = candidates.firstOrNull { it.id == settings.primaryReminderId } ?: candidates.firstOrNull()
            if (reminder == null) return
            ru.recalltoast.app.domain.SelectionResult(reminder, settings.stats, null)
        } else {
            ReminderSelector.select(settings, context)
        }

        val reminder = selection.reminder ?: return
        val text = reminder.text.trim()
        if (text.isBlank()) return

        val mode = reminder.displayMode ?: settings.displayMode
        val style = settings.overlayStyle.let { base ->
            base.copy(
                position = reminder.position ?: base.position,
                durationMillis = reminder.durationMillis ?: base.durationMillis
            )
        }

        withContext(Dispatchers.Main) {
            when (mode) {
                DisplayMode.SYSTEM_TOAST -> toastPresenter.show(text, settings.toastLong)
                DisplayMode.OVERLAY_CARD -> {
                    if (PermissionUtils.canDrawOverlays(appContext)) {
                        overlayPresenter.show(text, style)
                    } else {
                        toastPresenter.show(text, settings.toastLong)
                    }
                }
            }
        }

        if (!force) {
            val dateKey = context.dateKey
            val updated = ReminderSelector.markShown(selection.updatedStats, reminder.id, context.nowMillis, dateKey)
            repository.updateStats(updated)
        }
    }

    private fun isInCall(): Boolean {
        return try {
            val tm = appContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            @Suppress("DEPRECATION")
            tm.callState != TelephonyManager.CALL_STATE_IDLE
        } catch (_: SecurityException) {
            false
        } catch (_: Throwable) {
            false
        }
    }

    fun dismissOverlay() = overlayPresenter.dismiss()
}
