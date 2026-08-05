package ru.recalltoast.app.domain

import android.content.Context
import android.content.Intent
import android.os.Build
import ru.recalltoast.app.data.SettingsRepository
import ru.recalltoast.app.domain.model.ReliabilityMode
import ru.recalltoast.app.service.ReminderForegroundService
import ru.recalltoast.app.service.ServiceController
import ru.recalltoast.app.trigger.TriggerRegistrar
import ru.recalltoast.app.util.Logger
import ru.recalltoast.app.widget.ReminderWidgetProvider

class ReminderScheduler(
    private val context: Context,
    private val repository: SettingsRepository,
    private val serviceController: ServiceController,
    private val triggerRegistrar: TriggerRegistrar
) {
    suspend fun applyCurrentSettings() {
        val settings = repository.getSettings()
        triggerRegistrar.update(settings)

        if (!settings.enabled || settings.paused) {
            serviceController.stop()
            ReminderWidgetProvider.requestUpdate(context)
            return
        }

        when (settings.reliabilityMode) {
            ReliabilityMode.RELIABLE -> {
                val started = serviceController.startIfAllowed()
                if (!started) {
                    Logger.w("Reliable mode requested but FGS start was not allowed")
                }
            }
            ReliabilityMode.ECONOMY -> {
                serviceController.stop()
            }
        }
        ReminderWidgetProvider.requestUpdate(context)
    }

    suspend fun restoreAfterBoot() {
        val settings = repository.getSettings()
        if (!settings.autostartAfterBoot || !settings.enabled || settings.paused) {
            Logger.d("Boot restore skipped")
            return
        }
        applyCurrentSettings()
    }

    fun openAppIntent(): Intent {
        return context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent().setClassName(context, "ru.recalltoast.app.MainActivity")
    }
}
