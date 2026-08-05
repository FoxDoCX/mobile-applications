package ru.recalltoast.app

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import ru.recalltoast.app.domain.ReminderSelector
import ru.recalltoast.app.domain.SelectionContext
import ru.recalltoast.app.domain.model.AppSettings
import ru.recalltoast.app.domain.model.Reminder
import ru.recalltoast.app.domain.model.ReminderTrigger
import ru.recalltoast.app.domain.model.SelectionMode
import ru.recalltoast.app.domain.model.ShowStats
import ru.recalltoast.app.telephony.CallEndDetector
import android.telephony.TelephonyManager
import kotlin.random.Random

class ReminderSelectorTest {

    private fun reminder(
        id: String,
        text: String = "Hello",
        days: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7),
        start: Int? = null,
        end: Int? = null,
        priority: Int = 0,
        cooldown: Int = 0,
        maxPerDay: Int? = null
    ) = Reminder(
        id = id,
        text = text,
        activeDaysOfWeek = days,
        startTimeMinutes = start,
        endTimeMinutes = end,
        priority = priority,
        cooldownMinutes = cooldown,
        maxShowsPerDay = maxPerDay,
        createdAt = 1,
        updatedAt = 1
    )

    private fun ctx(
        day: Int = 1,
        minutes: Int = 12 * 60,
        trigger: ReminderTrigger = ReminderTrigger.UNLOCK,
        now: Long = 1_000_000L,
        date: String = "2026-08-03"
    ) = SelectionContext(now, day, minutes, date, trigger, Random(1))

    @Test
    fun dayOfWeekFilter() {
        val settings = AppSettings(
            enabled = true,
            reminders = listOf(reminder("a", days = setOf(1, 2))),
            activeDaysOfWeek = setOf(1, 2, 3, 4, 5, 6, 7)
        )
        assertThat(ReminderSelector.select(settings, ctx(day = 1)).reminder).isNotNull()
        assertThat(ReminderSelector.select(settings, ctx(day = 7)).reminder).isNull()
    }

    @Test
    fun timeRangeNormalAndOvernight() {
        assertThat(ReminderSelector.isWithinTimeRange(9 * 60, 18 * 60, 12 * 60)).isTrue()
        assertThat(ReminderSelector.isWithinTimeRange(9 * 60, 18 * 60, 20 * 60)).isFalse()
        assertThat(ReminderSelector.isWithinTimeRange(22 * 60, 6 * 60, 23 * 60)).isTrue()
        assertThat(ReminderSelector.isWithinTimeRange(22 * 60, 6 * 60, 3 * 60)).isTrue()
        assertThat(ReminderSelector.isWithinTimeRange(22 * 60, 6 * 60, 12 * 60)).isFalse()
    }

    @Test
    fun globalCooldown() {
        val settings = AppSettings(
            enabled = true,
            globalCooldownSeconds = 30,
            reminders = listOf(reminder("a")),
            stats = ShowStats(lastShownAt = 1_000_000L, dailyCountDate = "2026-08-03")
        )
        assertThat(ReminderSelector.select(settings, ctx(now = 1_010_000L)).reminder).isNull()
        assertThat(ReminderSelector.select(settings, ctx(now = 1_040_000L)).reminder).isNotNull()
    }

    @Test
    fun dailyLimit() {
        val settings = AppSettings(
            enabled = true,
            maxShowsPerDay = 2,
            reminders = listOf(reminder("a")),
            stats = ShowStats(dailyShowCount = 2, dailyCountDate = "2026-08-03")
        )
        assertThat(ReminderSelector.select(settings, ctx()).reminder).isNull()
    }

    @Test
    fun roundRobin() {
        val list = listOf(reminder("a"), reminder("b"), reminder("c"))
        assertThat(ReminderSelector.pickRoundRobin(list, null).id).isEqualTo("a")
        assertThat(ReminderSelector.pickRoundRobin(list, "a").id).isEqualTo("b")
        assertThat(ReminderSelector.pickRoundRobin(list, "c").id).isEqualTo("a")
    }

    @Test
    fun randomWithoutImmediateRepeat() {
        val list = listOf(reminder("a"), reminder("b"))
        repeat(20) {
            val picked = ReminderSelector.pickRandomNoRepeat(list, "a", Random(it))
            assertThat(picked.id).isEqualTo("b")
        }
    }

    @Test
    fun priorityMode() {
        val settings = AppSettings(
            enabled = true,
            selectionMode = SelectionMode.PRIORITY,
            reminders = listOf(reminder("low", priority = 1), reminder("high", priority = 5))
        )
        assertThat(ReminderSelector.select(settings, ctx()).reminder?.id).isEqualTo("high")
    }

    @Test
    fun toastTruncation() {
        val long = "x".repeat(200)
        val truncated = ReminderSelector.truncateForSystemToast(long, 120)
        assertThat(truncated.length).isAtMost(120)
        assertThat(truncated.endsWith("…")).isTrue()
    }

    @Test
    fun emptyTextRejected() {
        val settings = AppSettings(
            enabled = true,
            reminders = listOf(reminder("a", text = "   "))
        )
        assertThat(ReminderSelector.select(settings, ctx()).reminder).isNull()
    }
}

class CallEndDetectorTest {
    @Test
    fun detectsOffhookToIdle() {
        assertThat(
            CallEndDetector.isCallEnded(
                TelephonyManager.CALL_STATE_OFFHOOK,
                TelephonyManager.CALL_STATE_IDLE
            )
        ).isTrue()
    }

    @Test
    fun detectsRingingToIdle() {
        assertThat(
            CallEndDetector.isCallEnded(
                TelephonyManager.CALL_STATE_RINGING,
                TelephonyManager.CALL_STATE_IDLE
            )
        ).isTrue()
    }

    @Test
    fun ignoresIdleToIdle() {
        assertThat(
            CallEndDetector.isCallEnded(
                TelephonyManager.CALL_STATE_IDLE,
                TelephonyManager.CALL_STATE_IDLE
            )
        ).isFalse()
    }
}

class SettingsMigrationTest {
    @Test
    fun migrateClampsUnlockDelay() {
        val raw = AppSettings(version = 0, unlockDelaySeconds = 99, globalCooldownSeconds = -1)
        var result = raw
        if (result.globalCooldownSeconds < 0) result = result.copy(globalCooldownSeconds = 5)
        if (result.unlockDelaySeconds !in 0..10) {
            result = result.copy(unlockDelaySeconds = result.unlockDelaySeconds.coerceIn(0, 10))
        }
        result = result.copy(version = AppSettings.CURRENT_VERSION)
        assertThat(result.unlockDelaySeconds).isEqualTo(10)
        assertThat(result.globalCooldownSeconds).isEqualTo(5)
        assertThat(result.version).isEqualTo(AppSettings.CURRENT_VERSION)
    }
}
