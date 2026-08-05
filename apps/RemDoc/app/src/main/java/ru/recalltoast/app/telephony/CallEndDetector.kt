package ru.recalltoast.app.telephony

import android.telephony.TelephonyManager

object CallEndDetector {
    /**
     * Detect real call completion: RINGING/OFFHOOK -> IDLE.
     */
    fun isCallEnded(previous: Int, current: Int): Boolean {
        if (current != TelephonyManager.CALL_STATE_IDLE) return false
        return previous == TelephonyManager.CALL_STATE_OFFHOOK ||
            previous == TelephonyManager.CALL_STATE_RINGING
    }
}
