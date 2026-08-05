package ru.recalltoast.app.tile

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import ru.recalltoast.app.RecallApplication
import ru.recalltoast.app.domain.model.ReminderTrigger

@RequiresApi(Build.VERSION_CODES.N)
class ReminderTileService : TileService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onStartListening() {
        super.onStartListening()
        refresh()
    }

    override fun onClick() {
        super.onClick()
        val app = application as RecallApplication
        app.reminderPresenter.showForTrigger(ReminderTrigger.QUICK_TILE, force = true)
        refresh()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun refresh() {
        scope.launch(Dispatchers.IO) {
            val settings = (application as RecallApplication).settingsRepository.getSettings()
            launch(Dispatchers.Main) {
                qsTile?.apply {
                    state = if (settings.enabled && !settings.paused) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                    updateTile()
                }
            }
        }
    }
}
