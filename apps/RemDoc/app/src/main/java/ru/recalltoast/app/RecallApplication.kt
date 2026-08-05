package ru.recalltoast.app

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import ru.recalltoast.app.data.SettingsRepository
import ru.recalltoast.app.display.ReminderPresenter
import ru.recalltoast.app.domain.ReminderScheduler
import ru.recalltoast.app.service.ServiceController
import ru.recalltoast.app.telephony.CallStateMonitorFactory
import ru.recalltoast.app.trigger.TriggerRegistrar

class RecallApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var settingsRepository: SettingsRepository
        private set
    lateinit var reminderPresenter: ReminderPresenter
        private set
    lateinit var serviceController: ServiceController
        private set
    lateinit var reminderScheduler: ReminderScheduler
        private set
    lateinit var callStateMonitorFactory: CallStateMonitorFactory
        private set
    lateinit var triggerRegistrar: TriggerRegistrar
        private set

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(this)
        reminderPresenter = ReminderPresenter(this, settingsRepository)
        serviceController = ServiceController(this)
        callStateMonitorFactory = CallStateMonitorFactory(this)
        triggerRegistrar = TriggerRegistrar(this)
        reminderScheduler = ReminderScheduler(
            this,
            settingsRepository,
            serviceController,
            triggerRegistrar
        )
        appScope.launch {
            settingsRepository.ensureDefaultReminder()
            reminderScheduler.applyCurrentSettings()
        }
    }
}
