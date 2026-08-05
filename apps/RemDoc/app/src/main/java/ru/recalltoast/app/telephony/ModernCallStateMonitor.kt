package ru.recalltoast.app.telephony

import android.content.Context
import android.os.Build
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import ru.recalltoast.app.util.Logger
import ru.recalltoast.app.util.PermissionUtils
import java.util.concurrent.Executor

@RequiresApi(Build.VERSION_CODES.S)
class ModernCallStateMonitor(
    context: Context,
    private val onCallEnded: () -> Unit
) : CallStateMonitor {

    private val appContext = context.applicationContext
    private val telephony = appContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    private val executor: Executor = appContext.mainExecutor
    private var lastState = TelephonyManager.CALL_STATE_IDLE
    private var registered = false
    private var ignoreFirst = true

    private val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
        override fun onCallStateChanged(state: Int) {
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

    override fun register() {
        if (registered) return
        if (!PermissionUtils.hasPhoneStatePermission(appContext)) return
        try {
            lastState = telephony.callState
            ignoreFirst = true
            telephony.registerTelephonyCallback(executor, callback)
            registered = true
        } catch (t: Throwable) {
            Logger.e("Modern call monitor register failed", t)
        }
    }

    override fun unregister() {
        if (!registered) return
        try {
            telephony.unregisterTelephonyCallback(callback)
        } catch (t: Throwable) {
            Logger.e("Modern call monitor unregister failed", t)
        }
        registered = false
    }
}
