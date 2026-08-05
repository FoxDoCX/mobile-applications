package ru.recalltoast.app.display

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import ru.recalltoast.app.domain.ReminderSelector
import ru.recalltoast.app.domain.model.DisplayMode
import ru.recalltoast.app.util.Logger

class SystemToastPresenter(private val context: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())

    fun show(text: String, longDuration: Boolean) {
        val truncated = ReminderSelector.truncateForSystemToast(text)
        if (truncated.isBlank()) return
        mainHandler.post {
            try {
                val duration = if (longDuration) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
                Toast.makeText(context.applicationContext, truncated, duration).show()
            } catch (t: Throwable) {
                Logger.e("Toast failed", t)
            }
        }
    }
}
