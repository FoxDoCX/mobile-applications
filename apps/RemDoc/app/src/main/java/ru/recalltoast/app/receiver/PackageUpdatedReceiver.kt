package ru.recalltoast.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.recalltoast.app.RecallApplication
import ru.recalltoast.app.util.Logger

class PackageUpdatedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val pending = goAsync()
        val app = context.applicationContext as? RecallApplication
        if (app == null) {
            pending.finish()
            return
        }
        CoroutineScope(Dispatchers.Default).launch {
            try {
                app.reminderScheduler.applyCurrentSettings()
            } catch (t: Throwable) {
                Logger.e("Package update restore failed", t)
            } finally {
                pending.finish()
            }
        }
    }
}
