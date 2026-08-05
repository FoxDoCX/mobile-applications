package ru.recalltoast.app.telephony

import android.content.Context
import android.os.Build

class CallStateMonitorFactory(private val context: Context) {
    fun create(onCallEnded: () -> Unit): CallStateMonitor {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ModernCallStateMonitor(context, onCallEnded)
        } else {
            LegacyCallStateMonitor(context, onCallEnded)
        }
    }
}
