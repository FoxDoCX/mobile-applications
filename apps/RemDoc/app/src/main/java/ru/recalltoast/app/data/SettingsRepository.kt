package ru.recalltoast.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.recalltoast.app.domain.model.AppSettings
import ru.recalltoast.app.domain.model.Reminder
import ru.recalltoast.app.domain.model.SettingsExport
import ru.recalltoast.app.domain.model.ShowStats
import ru.recalltoast.app.util.Logger
import java.util.UUID

class SettingsRepository(context: Context) {

    private val appContext = context.applicationContext
    private val dataStore = appContext.settingsDataStore

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    val settingsFlow: Flow<AppSettings> = dataStore.data
        .map { prefs -> decode(prefs[SettingsKeys.SETTINGS_JSON]) }
        .distinctUntilChanged()

    suspend fun getSettings(): AppSettings = settingsFlow.first()

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        dataStore.edit { prefs ->
            val current = decode(prefs[SettingsKeys.SETTINGS_JSON])
            val updated = transform(current).copy(version = AppSettings.CURRENT_VERSION)
            prefs[SettingsKeys.SETTINGS_JSON] = json.encodeToString(updated)
            prefs[SettingsKeys.SCHEMA_VERSION] = AppSettings.CURRENT_VERSION
        }
    }

    suspend fun setEnabled(enabled: Boolean) = update { it.copy(enabled = enabled, paused = if (enabled) false else it.paused) }

    suspend fun setPaused(paused: Boolean, untilEpochMillis: Long = 0L) = update {
        it.copy(paused = paused, pauseUntilEpochMillis = untilEpochMillis, enabled = if (paused) it.enabled else it.enabled)
    }

    suspend fun upsertReminder(reminder: Reminder) = update { settings ->
        val list = settings.reminders.toMutableList()
        val index = list.indexOfFirst { it.id == reminder.id }
        if (index >= 0) list[index] = reminder else list.add(reminder)
        val primary = settings.primaryReminderId ?: reminder.id
        settings.copy(reminders = list, primaryReminderId = primary)
    }

    suspend fun deleteReminder(id: String) = update { settings ->
        val list = settings.reminders.filterNot { it.id == id }
        settings.copy(
            reminders = list,
            primaryReminderId = if (settings.primaryReminderId == id) list.firstOrNull()?.id else settings.primaryReminderId
        )
    }

    suspend fun updateStats(stats: ShowStats) = update { it.copy(stats = stats) }

    suspend fun ensureDefaultReminder() = update { settings ->
        if (settings.reminders.isNotEmpty()) settings
        else {
            val now = System.currentTimeMillis()
            val reminder = Reminder(
                id = UUID.randomUUID().toString(),
                text = "",
                createdAt = now,
                updatedAt = now
            )
            settings.copy(reminders = listOf(reminder), primaryReminderId = reminder.id)
        }
    }

    suspend fun clearAll() {
        dataStore.edit { it.clear() }
    }

    fun exportJson(settings: AppSettings): String {
        val export = SettingsExport(
            exportedAt = System.currentTimeMillis(),
            settings = settings.copy(stats = ShowStats())
        )
        return json.encodeToString(export)
    }

    fun importJson(raw: String): Result<AppSettings> = runCatching {
        val export = json.decodeFromString<SettingsExport>(raw)
        require(export.formatVersion in 1..AppSettings.EXPORT_FORMAT_VERSION) {
            "Unsupported export format: ${export.formatVersion}"
        }
        migrate(export.settings)
    }

    private fun decode(raw: String?): AppSettings {
        if (raw.isNullOrBlank()) return AppSettings()
        return runCatching {
            migrate(json.decodeFromString<AppSettings>(raw))
        }.getOrElse {
            Logger.e("Failed to decode settings", it)
            AppSettings()
        }
    }

    /**
     * Settings migration entry point. Keep additive and idempotent.
     */
    fun migrate(settings: AppSettings): AppSettings {
        var result = settings
        if (result.version < 1) {
            result = result.copy(version = 1)
        }
        if (result.reminders.isEmpty() && result.primaryReminderId != null) {
            result = result.copy(primaryReminderId = null)
        }
        if (result.globalCooldownSeconds < 0) {
            result = result.copy(globalCooldownSeconds = 5)
        }
        if (result.unlockDelaySeconds !in 0..10) {
            result = result.copy(unlockDelaySeconds = result.unlockDelaySeconds.coerceIn(0, 10))
        }
        return result.copy(version = AppSettings.CURRENT_VERSION)
    }
}
