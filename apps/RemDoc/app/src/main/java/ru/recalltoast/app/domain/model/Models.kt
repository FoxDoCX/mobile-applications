package ru.recalltoast.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class ReminderTrigger {
    UNLOCK,
    AFTER_CALL,
    POWER_CONNECTED,
    POWER_DISCONNECTED,
    LOW_BATTERY,
    HEADSET_CONNECTED,
    SCREEN_ON,
    APP_OPEN,
    QUICK_TILE,
    NOTIFICATION_ACTION,
    WIDGET,
    SCHEDULE,
    BOOT_RESTORE,
    MANUAL
}

@Serializable
enum class DisplayMode {
    SYSTEM_TOAST,
    OVERLAY_CARD
}

@Serializable
enum class ReminderPosition {
    TOP,
    CENTER,
    BOTTOM
}

@Serializable
enum class ReliabilityMode {
    RELIABLE,
    ECONOMY
}

@Serializable
enum class SelectionMode {
    SINGLE,
    ROUND_ROBIN,
    RANDOM,
    RANDOM_NO_IMMEDIATE_REPEAT,
    PRIORITY
}

@Serializable
enum class OverlayWidthMode {
    WRAP_CONTENT,
    ALMOST_FULL
}

@Serializable
enum class OverlayAnimation {
    FADE,
    SLIDE,
    NONE
}

@Serializable
enum class TextAlignMode {
    START,
    CENTER,
    END
}

@Serializable
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

@Serializable
enum class PauseUntil {
    NONE,
    MINUTES_15,
    HOUR_1,
    UNTIL_TOMORROW
}

@Serializable
data class Reminder(
    val id: String,
    val text: String,
    val enabled: Boolean = true,
    val triggers: Set<ReminderTrigger> = setOf(ReminderTrigger.UNLOCK),
    val displayMode: DisplayMode? = null,
    val durationMillis: Long? = null,
    val position: ReminderPosition? = null,
    val priority: Int = 0,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val activeDaysOfWeek: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7),
    val startTimeMinutes: Int? = null,
    val endTimeMinutes: Int? = null,
    val cooldownMinutes: Int = 0,
    val maxShowsPerDay: Int? = null,
    val weight: Int = 1
)

@Serializable
data class OverlayStyle(
    val position: ReminderPosition = ReminderPosition.TOP,
    val verticalOffsetDp: Int = 24,
    val horizontalOffsetDp: Int = 16,
    val widthMode: OverlayWidthMode = OverlayWidthMode.ALMOST_FULL,
    val durationMillis: Long = 3000L,
    val textSizeSp: Float = 16f,
    val bold: Boolean = false,
    val textAlign: TextAlignMode = TextAlignMode.START,
    val backgroundColor: Long = 0xFF141B33,
    val textColor: Long = 0xFFFFFFFF,
    val alpha: Float = 0.96f,
    val cornerRadiusDp: Float = 20f,
    val elevationDp: Float = 6f,
    val showIcon: Boolean = true,
    val animation: OverlayAnimation = OverlayAnimation.FADE,
    val maxLines: Int = 4,
    val showCloseButton: Boolean = true,
    val showProgress: Boolean = true
)

@Serializable
data class ShowStats(
    val lastShownReminderId: String? = null,
    val lastShownAt: Long = 0L,
    val dailyShowCount: Int = 0,
    val dailyCountDate: String = ""
)

@Serializable
data class AppSettings(
    val version: Int = CURRENT_VERSION,
    val onboardingDone: Boolean = false,
    val enabled: Boolean = false,
    val paused: Boolean = false,
    val pauseUntilEpochMillis: Long = 0L,
    val reliabilityMode: ReliabilityMode = ReliabilityMode.ECONOMY,
    val displayMode: DisplayMode = DisplayMode.SYSTEM_TOAST,
    val toastLong: Boolean = true,
    val unlockEnabled: Boolean = true,
    val afterCallEnabled: Boolean = false,
    val powerConnectedEnabled: Boolean = false,
    val powerDisconnectedEnabled: Boolean = false,
    val lowBatteryEnabled: Boolean = false,
    val headsetEnabled: Boolean = false,
    val screenOnEnabled: Boolean = false,
    val appOpenEnabled: Boolean = false,
    val scheduleEnabled: Boolean = false,
    val scheduleIntervalMinutes: Int = 60,
    val unlockDelaySeconds: Int = 0,
    val globalCooldownSeconds: Int = 5,
    val respectDnd: Boolean = true,
    val skipDuringCall: Boolean = true,
    val skipWhenScreenOff: Boolean = true,
    val autostartAfterBoot: Boolean = true,
    val selectionMode: SelectionMode = SelectionMode.SINGLE,
    val activeDaysOfWeek: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7),
    val startTimeMinutes: Int? = null,
    val endTimeMinutes: Int? = null,
    val maxShowsPerDay: Int? = null,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val overlayStyle: OverlayStyle = OverlayStyle(),
    val reminders: List<Reminder> = emptyList(),
    val primaryReminderId: String? = null,
    val stats: ShowStats = ShowStats()
) {
    companion object {
        const val CURRENT_VERSION = 1
        const val EXPORT_FORMAT_VERSION = 1
    }
}

@Serializable
data class SettingsExport(
    val formatVersion: Int = AppSettings.EXPORT_FORMAT_VERSION,
    val exportedAt: Long,
    val settings: AppSettings
)
