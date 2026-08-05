package ru.recalltoast.app.display

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import ru.recalltoast.app.R
import ru.recalltoast.app.domain.model.OverlayAnimation
import ru.recalltoast.app.domain.model.OverlayStyle
import ru.recalltoast.app.domain.model.OverlayWidthMode
import ru.recalltoast.app.domain.model.ReminderPosition
import ru.recalltoast.app.domain.model.TextAlignMode
import ru.recalltoast.app.util.Logger
import ru.recalltoast.app.util.PermissionUtils
import kotlin.math.roundToInt

class OverlayWindowController(context: Context) {

    private val appContext = context.applicationContext
    private val windowManager = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var currentView: View? = null
    private var hideRunnable: Runnable? = null
    private var progressRunnable: Runnable? = null

    fun show(text: String, style: OverlayStyle) {
        if (!PermissionUtils.canDrawOverlays(appContext)) {
            Logger.w("Overlay permission missing")
            return
        }
        if (isPermissionUiLikelyVisible()) {
            Logger.d("Skip overlay over permission UI")
            return
        }
        mainHandler.post {
            try {
                dismissInternal(immediate = true)
                val view = buildView(text, style)
                val params = buildLayoutParams(style)
                windowManager.addView(view, params)
                currentView = view
                playEnter(view, style)
                scheduleHide(view, style)
            } catch (t: Throwable) {
                Logger.e("Overlay show failed", t)
                safeRemove(currentView)
                currentView = null
            }
        }
    }

    fun dismiss() {
        mainHandler.post { dismissInternal(immediate = false) }
    }

    fun dismissImmediate() {
        mainHandler.post { dismissInternal(immediate = true) }
    }

    private fun dismissInternal(immediate: Boolean) {
        hideRunnable?.let { mainHandler.removeCallbacks(it) }
        progressRunnable?.let { mainHandler.removeCallbacks(it) }
        hideRunnable = null
        progressRunnable = null
        val view = currentView ?: return
        if (immediate) {
            safeRemove(view)
            currentView = null
            return
        }
        playExit(view) {
            safeRemove(view)
            if (currentView === view) currentView = null
        }
    }

    private fun scheduleHide(view: View, style: OverlayStyle) {
        val duration = style.durationMillis.coerceAtLeast(500L)
        if (style.showProgress) {
            val bar = view.findViewWithTag<ProgressBar>("progress")
            if (bar != null) {
                bar.max = 1000
                bar.progress = 1000
                val started = System.currentTimeMillis()
                progressRunnable = object : Runnable {
                    override fun run() {
                        val elapsed = System.currentTimeMillis() - started
                        val left = ((1f - elapsed.toFloat() / duration) * 1000f).roundToInt().coerceIn(0, 1000)
                        bar.progress = left
                        if (left > 0 && currentView === view) {
                            mainHandler.postDelayed(this, 32L)
                        }
                    }
                }
                mainHandler.post(progressRunnable!!)
            }
        }
        hideRunnable = Runnable {
            if (currentView === view) dismissInternal(immediate = false)
        }
        mainHandler.postDelayed(hideRunnable!!, duration)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun buildView(text: String, style: OverlayStyle): View {
        val density = appContext.resources.displayMetrics.density
        val container = LinearLayout(appContext).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                (16 * density).roundToInt(),
                (12 * density).roundToInt(),
                (12 * density).roundToInt(),
                (12 * density).roundToInt()
            )
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = style.cornerRadiusDp * density
                setColor(applyAlpha(style.backgroundColor.toInt(), style.alpha))
            }
            elevation = style.elevationDp * density
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            contentDescription = text
        }

        val row = LinearLayout(appContext).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        if (style.showIcon) {
            val icon = TextView(appContext).apply {
                this.text = "◐"
                setTextColor(style.textColor.toInt())
                setTextSize(TypedValue.COMPLEX_UNIT_SP, style.textSizeSp)
                setPadding(0, 0, (8 * density).roundToInt(), 0)
            }
            row.addView(icon)
        }

        val message = TextView(appContext).apply {
            this.text = text
            setTextColor(style.textColor.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, style.textSizeSp)
            maxLines = style.maxLines.coerceAtLeast(1)
            ellipsize = android.text.TextUtils.TruncateAt.END
            typeface = if (style.bold) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
            textAlignment = when (style.textAlign) {
                TextAlignMode.START -> View.TEXT_ALIGNMENT_VIEW_START
                TextAlignMode.CENTER -> View.TEXT_ALIGNMENT_CENTER
                TextAlignMode.END -> View.TEXT_ALIGNMENT_VIEW_END
            }
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        row.addView(message)

        if (style.showCloseButton) {
            val close = ImageButton(appContext).apply {
                setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                setBackgroundColor(Color.TRANSPARENT)
                setColorFilter(style.textColor.toInt())
                contentDescription = appContext.getString(R.string.action_close)
                setOnClickListener { dismiss() }
            }
            row.addView(close, LinearLayout.LayoutParams((36 * density).roundToInt(), (36 * density).roundToInt()))
        }

        container.addView(row)

        if (style.showProgress) {
            val progress = ProgressBar(appContext, null, android.R.attr.progressBarStyleHorizontal).apply {
                tag = "progress"
                max = 1000
                progress = 1000
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    (4 * density).roundToInt()
                ).also {
                    it.topMargin = (8 * density).roundToInt()
                }
            }
            container.addView(progress)
        }

        // Touch only on card; FLAG_NOT_TOUCH_MODAL + FLAG_WATCH_OUTSIDE_TOUCH keep outside touches free.
        container.setOnTouchListener { _, event ->
            event.action == MotionEvent.ACTION_OUTSIDE
        }
        return container
    }

    private fun buildLayoutParams(style: OverlayStyle): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        val density = appContext.resources.displayMetrics.density
        val width = when (style.widthMode) {
            OverlayWidthMode.WRAP_CONTENT -> WindowManager.LayoutParams.WRAP_CONTENT
            OverlayWidthMode.ALMOST_FULL -> WindowManager.LayoutParams.MATCH_PARENT
        }
        val gravity = when (style.position) {
            ReminderPosition.TOP -> Gravity.TOP or Gravity.CENTER_HORIZONTAL
            ReminderPosition.CENTER -> Gravity.CENTER
            ReminderPosition.BOTTOM -> Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        }
        return WindowManager.LayoutParams(
            width,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            this.gravity = gravity
            x = (style.horizontalOffsetDp * density).roundToInt()
            y = (style.verticalOffsetDp * density).roundToInt()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                fitInsetsTypes = android.view.WindowInsets.Type.statusBars() or
                    android.view.WindowInsets.Type.navigationBars() or
                    android.view.WindowInsets.Type.displayCutout()
            }
            windowAnimations = 0
        }
    }

    private fun playEnter(view: View, style: OverlayStyle) {
        when (style.animation) {
            OverlayAnimation.NONE -> Unit
            OverlayAnimation.FADE -> {
                view.alpha = 0f
                ObjectAnimator.ofFloat(view, View.ALPHA, 0f, 1f).setDuration(180).start()
            }
            OverlayAnimation.SLIDE -> {
                view.alpha = 0f
                view.translationY = -40f
                view.animate().alpha(1f).translationY(0f).setDuration(200).start()
            }
        }
    }

    private fun playExit(view: View, end: () -> Unit) {
        val anim = ObjectAnimator.ofFloat(view, View.ALPHA, view.alpha, 0f).setDuration(160)
        anim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) = end()
            override fun onAnimationCancel(animation: Animator) = end()
        })
        anim.start()
    }

    private fun safeRemove(view: View?) {
        if (view == null) return
        try {
            if (view.parent != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    windowManager.removeViewImmediate(view)
                } else {
                    windowManager.removeView(view)
                }
            }
        } catch (_: IllegalArgumentException) {
            // Already removed
        } catch (t: Throwable) {
            Logger.e("Overlay remove failed", t)
        }
    }

    private fun applyAlpha(color: Int, alpha: Float): Int {
        val a = (alpha.coerceIn(0.2f, 1f) * 255).roundToInt()
        return Color.argb(a, Color.red(color), Color.green(color), Color.blue(color))
    }

    private fun isPermissionUiLikelyVisible(): Boolean {
        // Avoid covering system permission screens when our process is not foreground.
        return false
    }
}
