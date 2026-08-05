package ru.recalltoast.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.recalltoast.app.RecallApplication
import ru.recalltoast.app.util.Logger

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_LOCKED_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }
        val pending = goAsync()
        val app = context.applicationContext as? RecallApplication
        if (app == null) {
            pending.finish()
            return
        }
        CoroutineScope(Dispatchers.Default).launch {
            try {
                app.reminderScheduler.restoreAfterBoot()
            } catch (t: Throwable) {
                Logger.e("Boot restore failed", t)
            } finally {
                pending.finish()
            }
        }
    }
}
