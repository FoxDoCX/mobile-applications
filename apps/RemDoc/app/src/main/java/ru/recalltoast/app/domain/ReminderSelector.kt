package ru.recalltoast.app.domain

import ru.recalltoast.app.domain.model.AppSettings
import ru.recalltoast.app.domain.model.Reminder
import ru.recalltoast.app.domain.model.ReminderTrigger
import ru.recalltoast.app.domain.model.SelectionMode
import ru.recalltoast.app.domain.model.ShowStats
import ru.recalltoast.app.util.TimeUtils
import java.util.concurrent.TimeUnit
import kotlin.random.Random

data class SelectionContext(
    val nowMillis: Long,
    val dayOfWeek: Int,
    val minutesOfDay: Int,
    val dateKey: String,
    val trigger: ReminderTrigger,
    val random: Random = Random.Default
)

data class SelectionResult(
    val reminder: Reminder?,
    val updatedStats: ShowStats,
    val reason: String? = null
)

object ReminderSelector {

    fun select(settings: AppSettings, context: SelectionContext): SelectionResult {
        val stats = normalizeStats(settings.stats, context.dateKey)
        if (!settings.enabled) {
            return SelectionResult(null, stats, "disabled")
        }
        if (settings.paused || isPauseActive(settings, context.nowMillis)) {
            return SelectionResult(null, stats, "paused")
        }
        if (settings.globalCooldownSeconds > 0 &&
            context.nowMillis - stats.lastShownAt < TimeUnit.SECONDS.toMillis(settings.globalCooldownSeconds.toLong())
        ) {
            return SelectionResult(null, stats, "global_cooldown")
        }
        if (settings.maxShowsPerDay != null && stats.dailyShowCount >= settings.maxShowsPerDay) {
            return SelectionResult(null, stats, "daily_limit")
        }
        if (!isDayAllowed(settings.activeDaysOfWeek, context.dayOfWeek)) {
            return SelectionResult(null, stats, "day_blocked")
        }
        if (!isWithinTimeRange(settings.startTimeMinutes, settings.endTimeMinutes, context.minutesOfDay)) {
            return SelectionResult(null, stats, "time_blocked")
        }

        val candidates = settings.reminders
            .filter { it.enabled }
            .filter { it.text.isNotBlank() }
            .filter { reminderMatchesTrigger(it, context.trigger) }
            .filter { isDayAllowed(effectiveDays(it, settings), context.dayOfWeek) }
            .filter {
                isWithinTimeRange(
                    it.startTimeMinutes ?: settings.startTimeMinutes,
                    it.endTimeMinutes ?: settings.endTimeMinutes,
                    context.minutesOfDay
                )
            }
            .filter { reminderCooldownOk(it, stats, context.nowMillis) }
            .filter { reminderDailyOk(it, stats) }

        if (candidates.isEmpty()) {
            return SelectionResult(null, stats, "no_candidates")
        }

        val chosen = when (settings.selectionMode) {
            SelectionMode.SINGLE -> pickPrimary(settings, candidates)
            SelectionMode.ROUND_ROBIN -> pickRoundRobin(candidates, stats.lastShownReminderId)
            SelectionMode.RANDOM -> candidates.random(context.random)
            SelectionMode.RANDOM_NO_IMMEDIATE_REPEAT -> pickRandomNoRepeat(candidates, stats.lastShownReminderId, context.random)
            SelectionMode.PRIORITY -> candidates.sortedWith(
                compareByDescending<Reminder> { it.priority }.thenByDescending { it.weight }
            ).first()
        }

        return SelectionResult(chosen, stats, null)
    }

    fun markShown(stats: ShowStats, reminderId: String, nowMillis: Long, dateKey: String): ShowStats {
        val normalized = normalizeStats(stats, dateKey)
        return normalized.copy(
            lastShownReminderId = reminderId,
            lastShownAt = nowMillis,
            dailyShowCount = normalized.dailyShowCount + 1,
            dailyCountDate = dateKey
        )
    }

    fun isDayAllowed(days: Set<Int>, dayOfWeek: Int): Boolean {
        if (days.isEmpty()) return true
        return dayOfWeek in days
    }

    fun isWithinTimeRange(startMinutes: Int?, endMinutes: Int?, nowMinutes: Int): Boolean {
        if (startMinutes == null || endMinutes == null) return true
        if (startMinutes == endMinutes) return true
        return if (startMinutes < endMinutes) {
            nowMinutes in startMinutes until endMinutes
        } else {
            // Overnight range, e.g. 22:00–06:00
            nowMinutes >= startMinutes || nowMinutes < endMinutes
        }
    }

    fun truncateForSystemToast(text: String, maxChars: Int = 120): String {
        val normalized = text.trim().replace(Regex("\\s+"), " ")
        if (normalized.length <= maxChars) return normalized
        return normalized.take(maxChars - 1).trimEnd() + "…"
    }

    fun isPauseActive(settings: AppSettings, nowMillis: Long): Boolean {
        if (!settings.paused) return false
        if (settings.pauseUntilEpochMillis <= 0L) return true
        return nowMillis < settings.pauseUntilEpochMillis
    }

    private fun normalizeStats(stats: ShowStats, dateKey: String): ShowStats {
        return if (stats.dailyCountDate == dateKey) stats
        else stats.copy(dailyShowCount = 0, dailyCountDate = dateKey)
    }

    private fun reminderMatchesTrigger(reminder: Reminder, trigger: ReminderTrigger): Boolean {
        if (trigger == ReminderTrigger.MANUAL || trigger == ReminderTrigger.NOTIFICATION_ACTION ||
            trigger == ReminderTrigger.QUICK_TILE || trigger == ReminderTrigger.WIDGET ||
            trigger == ReminderTrigger.APP_OPEN
        ) {
            return true
        }
        return reminder.triggers.isEmpty() || trigger in reminder.triggers
    }

    private fun effectiveDays(reminder: Reminder, settings: AppSettings): Set<Int> {
        return if (reminder.activeDaysOfWeek.isNotEmpty()) reminder.activeDaysOfWeek else settings.activeDaysOfWeek
    }

    private fun reminderCooldownOk(reminder: Reminder, stats: ShowStats, nowMillis: Long): Boolean {
        if (reminder.cooldownMinutes <= 0) return true
        if (stats.lastShownReminderId != reminder.id) return true
        return nowMillis - stats.lastShownAt >= TimeUnit.MINUTES.toMillis(reminder.cooldownMinutes.toLong())
    }

    private fun reminderDailyOk(reminder: Reminder, stats: ShowStats): Boolean {
        val max = reminder.maxShowsPerDay ?: return true
        // Lightweight per-reminder daily limit: if last shown was this reminder, use global daily count as soft bound.
        if (stats.lastShownReminderId == reminder.id) {
            return stats.dailyShowCount < max
        }
        return true
    }

    private fun pickPrimary(settings: AppSettings, candidates: List<Reminder>): Reminder {
        val primaryId = settings.primaryReminderId
        return candidates.firstOrNull { it.id == primaryId } ?: candidates.first()
    }

    fun pickRoundRobin(candidates: List<Reminder>, lastId: String?): Reminder {
        if (lastId == null) return candidates.first()
        val index = candidates.indexOfFirst { it.id == lastId }
        if (index < 0) return candidates.first()
        return candidates[(index + 1) % candidates.size]
    }

    fun pickRandomNoRepeat(candidates: List<Reminder>, lastId: String?, random: Random): Reminder {
        if (candidates.size == 1) return candidates.first()
        val filtered = if (lastId == null) candidates else candidates.filterNot { it.id == lastId }
        val pool = if (filtered.isEmpty()) candidates else filtered
        return pool.random(random)
    }

    fun selectionContext(nowMillis: Long = System.currentTimeMillis(), trigger: ReminderTrigger): SelectionContext {
        return SelectionContext(
            nowMillis = nowMillis,
            dayOfWeek = TimeUtils.dayOfWeekMondayBased(nowMillis),
            minutesOfDay = TimeUtils.minutesOfDay(nowMillis),
            dateKey = TimeUtils.dateKey(nowMillis),
            trigger = trigger
        )
    }
}
