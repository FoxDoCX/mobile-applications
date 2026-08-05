package ru.recalltoast.app.display

import android.content.Context
import ru.recalltoast.app.domain.model.DisplayMode
import ru.recalltoast.app.domain.model.OverlayStyle

class OverlayReminderPresenter(
    context: Context,
    private val windowController: OverlayWindowController = OverlayWindowController(context)
) {
    fun show(text: String, style: OverlayStyle) {
        windowController.show(text, style)
    }

    fun dismiss() = windowController.dismissImmediate()
}
