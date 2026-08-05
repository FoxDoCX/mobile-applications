package ru.recalltoast.app.telephony

import android.content.Context
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import ru.recalltoast.app.util.Logger
import ru.recalltoast.app.util.PermissionUtils

@Suppress("DEPRECATION")
class LegacyCallStateMonitor(
    context: Context,
    private val onCallEnded: () -> Unit
) : CallStateMonitor {

    private val appContext = context.applicationContext
    private val telephony = appContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    private var lastState = TelephonyManager.CALL_STATE_IDLE
    private var registered = false
    private var ignoreFirst = true

    private val listener = object : PhoneStateListener() {
        @Deprecated("Deprecated in Java")
        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
            // phoneNumber is intentionally unused — we never read or store numbers.
            handleState(state)
        }
    }

    override fun register() {
        if (registered) return
        if (!PermissionUtils.hasPhoneStatePermission(appContext)) return
        try {
            lastState = telephony.callState
            ignoreFirst = true
            telephony.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
            registered = true
        } catch (t: Throwable) {
            Logger.e("Legacy call monitor register failed", t)
        }
    }

    override fun unregister() {
        if (!registered) return
        try {
            telephony.listen(listener, PhoneStateListener.LISTEN_NONE)
        } catch (t: Throwable) {
            Logger.e("Legacy call monitor unregister failed", t)
        }
        registered = false
    }

    private fun handleState(state: Int) {
        if (ignoreFirst) {
            ignoreFirst = false
            lastState = state
            return
        }
        val ended = CallEndDetector.isCallEnded(previous = lastState, current = state)
        lastState = state
        if (ended) onCallEnded()
    }
}
