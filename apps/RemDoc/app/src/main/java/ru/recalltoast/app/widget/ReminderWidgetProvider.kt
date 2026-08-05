package ru.recalltoast.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import ru.recalltoast.app.MainActivity
import ru.recalltoast.app.R
import ru.recalltoast.app.RecallApplication
import ru.recalltoast.app.domain.model.ReminderTrigger

class ReminderWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val app = context.applicationContext as RecallApplication
        val settings = runBlocking { app.settingsRepository.getSettings() }
        val text = settings.reminders
            .firstOrNull { it.id == settings.primaryReminderId }
            ?.text
            ?.ifBlank { context.getString(R.string.widget_empty) }
            ?: context.getString(R.string.widget_empty)
        val preview = if (text.length > 40) text.take(39) + "…" else text
        val running = settings.enabled && !settings.paused

        appWidgetIds.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.widget_reminder).apply {
                setTextViewText(R.id.widget_text, preview)
                setTextViewText(
                    R.id.widget_status,
                    context.getString(if (running) R.string.status_active else R.string.status_paused)
                )
                setOnClickPendingIntent(R.id.widget_root, openApp(context))
                setOnClickPendingIntent(R.id.widget_show, action(context, ACTION_SHOW))
                setOnClickPendingIntent(R.id.widget_toggle, action(context, ACTION_TOGGLE))
            }
            appWidgetManager.updateAppWidget(id, views)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val app = context.applicationContext as? RecallApplication ?: return
        when (intent.action) {
            ACTION_SHOW -> app.reminderPresenter.showForTrigger(ReminderTrigger.WIDGET, force = true)
            ACTION_TOGGLE -> {
                CoroutineScope(Dispatchers.Default).launch {
                    val settings = app.settingsRepository.getSettings()
                    if (settings.enabled && !settings.paused) {
                        app.settingsRepository.setPaused(true)
                    } else {
                        app.settingsRepository.update {
                            it.copy(enabled = true, paused = false, pauseUntilEpochMillis = 0L)
                        }
                    }
                    app.reminderScheduler.applyCurrentSettings()
                    requestUpdate(context)
                }
            }
        }
    }

    private fun openApp(context: Context): PendingIntent {
        return PendingIntent.getActivity(
            context,
            10,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun action(context: Context, action: String): PendingIntent {
        return PendingIntent.getBroadcast(
            context,
            action.hashCode(),
            Intent(context, ReminderWidgetProvider::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val ACTION_SHOW = "ru.recalltoast.app.widget.SHOW"
        const val ACTION_TOGGLE = "ru.recalltoast.app.widget.TOGGLE"

        fun requestUpdate(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, ReminderWidgetProvider::class.java))
            if (ids.isEmpty()) return
            val intent = Intent(context, ReminderWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }
    }
}
