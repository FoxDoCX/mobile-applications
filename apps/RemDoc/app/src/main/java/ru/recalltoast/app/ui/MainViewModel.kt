package ru.recalltoast.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.recalltoast.app.RecallApplication
import ru.recalltoast.app.domain.model.AppSettings
import ru.recalltoast.app.domain.model.DisplayMode
import ru.recalltoast.app.domain.model.OverlayStyle
import ru.recalltoast.app.domain.model.ReliabilityMode
import ru.recalltoast.app.domain.model.Reminder
import ru.recalltoast.app.domain.model.ReminderTrigger
import ru.recalltoast.app.domain.model.SelectionMode
import ru.recalltoast.app.domain.model.ThemeMode
import ru.recalltoast.app.util.TimeUtils
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as RecallApplication
    private val repo = app.settingsRepository
    private var textSaveJob: Job? = null
    private var draftSyncedId: String? = null

    val settings: StateFlow<AppSettings> = repo.settingsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        AppSettings()
    )

    private val _draftText = MutableStateFlow("")
    val draftText: StateFlow<String> = _draftText.asStateFlow()

    init {
        viewModelScope.launch {
            settings.collect { s ->
                val primary = s.reminders.firstOrNull { it.id == s.primaryReminderId }
                    ?: s.reminders.firstOrNull()
                val id = primary?.id
                // Sync draft from store only when reminder identity changes (not on every save).
                if (id != draftSyncedId) {
                    draftSyncedId = id
                    _draftText.value = primary?.text.orEmpty()
                }
            }
        }
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            repo.update(transform)
            app.reminderScheduler.applyCurrentSettings()
        }
    }

    fun onDraftTextChange(text: String) {
        _draftText.value = text
        textSaveJob?.cancel()
        textSaveJob = viewModelScope.launch {
            delay(350)
            persistPrimaryText(text)
        }
    }

    fun clearDraftText() {
        _draftText.value = ""
        textSaveJob?.cancel()
        viewModelScope.launch { persistPrimaryText("") }
    }

    fun flushDraftNow() {
        textSaveJob?.cancel()
        viewModelScope.launch { persistPrimaryText(_draftText.value) }
    }

    private suspend fun persistPrimaryText(text: String) {
        val settings = repo.getSettings()
        val now = System.currentTimeMillis()
        val primaryId = settings.primaryReminderId
        val current = settings.reminders.firstOrNull { it.id == primaryId }
        if (current != null) {
            if (current.text == text) return
            repo.upsertReminder(current.copy(text = text, updatedAt = now))
        } else {
            val reminder = Reminder(
                id = UUID.randomUUID().toString(),
                text = text,
                createdAt = now,
                updatedAt = now
            )
            draftSyncedId = reminder.id
            repo.upsertReminder(reminder)
            repo.update { it.copy(primaryReminderId = reminder.id) }
        }
        // Text-only change must not restart receivers/service.
    }

    fun setPrimaryText(text: String) = onDraftTextChange(text)

    fun toggleTheme() {
        val current = settings.value.themeMode
        val next = when (current) {
            ThemeMode.LIGHT -> ThemeMode.DARK
            ThemeMode.DARK -> ThemeMode.LIGHT
            ThemeMode.SYSTEM -> ThemeMode.DARK
        }
        setTheme(next)
    }

    fun showNow() = app.reminderPresenter.showNow()

    fun start() = updateSettings { it.copy(enabled = true, paused = false, pauseUntilEpochMillis = 0L) }

    fun stop() = updateSettings { it.copy(enabled = false) }

    fun pause(minutes: Int? = null, untilTomorrow: Boolean = false) {
        val until = when {
            untilTomorrow -> TimeUtils.pauseUntilTomorrowEpoch()
            minutes != null -> TimeUtils.minutesFromNow(minutes)
            else -> 0L
        }
        updateSettings { it.copy(paused = true, pauseUntilEpochMillis = until) }
    }

    fun resume() = updateSettings { it.copy(paused = false, pauseUntilEpochMillis = 0L) }

    fun completeOnboarding(mode: ReliabilityMode, display: DisplayMode) {
        updateSettings {
            it.copy(
                onboardingDone = true,
                reliabilityMode = mode,
                displayMode = display
            )
        }
    }

    fun setReliabilityMode(mode: ReliabilityMode) = updateSettings { it.copy(reliabilityMode = mode) }

    fun setDisplayMode(mode: DisplayMode) = updateSettings { it.copy(displayMode = mode) }

    fun setTheme(mode: ThemeMode) = updateSettings { it.copy(themeMode = mode) }

    fun setUnlockEnabled(enabled: Boolean) = updateSettings { it.copy(unlockEnabled = enabled) }

    fun setAfterCallEnabled(enabled: Boolean) = updateSettings { it.copy(afterCallEnabled = enabled) }

    fun setPowerEnabled(enabled: Boolean) = updateSettings {
        it.copy(powerConnectedEnabled = enabled, powerDisconnectedEnabled = enabled)
    }

    fun setScheduleEnabled(enabled: Boolean) = updateSettings { it.copy(scheduleEnabled = enabled) }

    fun setOverlayStyle(style: OverlayStyle) = updateSettings { it.copy(overlayStyle = style) }

    fun setSelectionMode(mode: SelectionMode) = updateSettings { it.copy(selectionMode = mode) }

    fun upsertReminder(reminder: Reminder) {
        viewModelScope.launch {
            repo.upsertReminder(reminder)
            app.reminderScheduler.applyCurrentSettings()
        }
    }

    fun deleteReminder(id: String) {
        viewModelScope.launch {
            repo.deleteReminder(id)
            app.reminderScheduler.applyCurrentSettings()
        }
    }

    fun addReminder() {
        val now = System.currentTimeMillis()
        upsertReminder(
            Reminder(
                id = UUID.randomUUID().toString(),
                text = "",
                createdAt = now,
                updatedAt = now,
                triggers = setOf(ReminderTrigger.UNLOCK)
            )
        )
    }

    fun exportJson(): String = run {
        // sync-ish via blocking get in coroutine caller preferred; provide suspend alternative
        ""
    }

    suspend fun exportSettingsJson(): String = repo.exportJson(repo.getSettings())

    suspend fun importSettingsJson(raw: String): Result<Unit> {
        val parsed = repo.importJson(raw)
        return parsed.mapCatching { imported ->
            repo.update { imported.copy(onboardingDone = true) }
            app.reminderScheduler.applyCurrentSettings()
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repo.clearAll()
            repo.ensureDefaultReminder()
            app.reminderScheduler.applyCurrentSettings()
        }
    }

    fun restoreOverlayDefaults() = setOverlayStyle(OverlayStyle())

    // Debug helpers
    fun debugSimulateUnlock() = app.reminderPresenter.showForTrigger(ReminderTrigger.UNLOCK, force = true)
    fun debugSimulateCallEnd() = app.reminderPresenter.showForTrigger(ReminderTrigger.AFTER_CALL, force = true)
    fun debugResetOnboarding() = updateSettings { it.copy(onboardingDone = false) }
}
