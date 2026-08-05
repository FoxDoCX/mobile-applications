package ru.recalltoast.app.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings

data class OemHelp(
    val manufacturer: String,
    val title: String,
    val steps: List<String>,
    val intentCandidates: List<Intent> = emptyList()
)

object OemHelpProvider {

    fun current(context: Context): OemHelp {
        val manufacturer = Build.MANUFACTURER.orEmpty()
        val brand = Build.BRAND.orEmpty()
        val key = (manufacturer.ifBlank { brand }).lowercase()
        return when {
            key.contains("xiaomi") || key.contains("redmi") || key.contains("poco") -> xiaomi()
            key.contains("huawei") || key.contains("honor") -> huawei()
            key.contains("samsung") -> samsung()
            key.contains("oneplus") -> oneplus()
            key.contains("oppo") || key.contains("realme") -> oppoFamily(key)
            key.contains("vivo") -> vivo()
            else -> generic(manufacturer.ifBlank { brand.ifBlank { "Android" } })
        }.let { help ->
            help.copy(intentCandidates = help.intentCandidates.filter { canResolve(context, it) })
        }
    }

    fun openBestIntent(context: Context): Intent {
        val help = current(context)
        return help.intentCandidates.firstOrNull()
            ?: BatteryOptimizationUtils.appDetailsIntent(context)
    }

    private fun canResolve(context: Context, intent: Intent): Boolean {
        return context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null
    }

    private fun xiaomi() = OemHelp(
        manufacturer = "Xiaomi",
        title = "Xiaomi / MIUI / HyperOS",
        steps = listOf(
            "Откройте настройки приложения → Автозапуск и разрешите его.",
            "В экономии батареи выберите «Без ограничений».",
            "Отключите очистку приложения в «Недавних», если такая опция есть.",
            "Для надёжного режима оставьте уведомление сервиса видимым."
        ),
        intentCandidates = listOf(
            Intent().setComponent(
                ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
                )
            ),
            Intent("miui.intent.action.OP_AUTO_START").addCategory(Intent.CATEGORY_DEFAULT)
        )
    )

    private fun huawei() = OemHelp(
        manufacturer = "Huawei",
        title = "Huawei / Honor",
        steps = listOf(
            "Разрешите автозапуск в «Запуске приложений».",
            "Отключите агрессивное управление батареей для RemDoc.",
            "Закрепите приложение в списке недавних, если прошивка это предлагает."
        ),
        intentCandidates = listOf(
            Intent().setComponent(
                ComponentName(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
                )
            ),
            Intent().setComponent(
                ComponentName(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity"
                )
            )
        )
    )

    private fun samsung() = OemHelp(
        manufacturer = "Samsung",
        title = "Samsung / One UI",
        steps = listOf(
            "Уберите приложение из «Спящих» и «Глубоко спящих».",
            "Разрешите работу в фоне в настройках батареи приложения.",
            "Не используйте принудительную остановку — она отключает события."
        ),
        intentCandidates = listOf(
            Intent().setComponent(
                ComponentName(
                    "com.samsung.android.lool",
                    "com.samsung.android.sm.battery.ui.BatteryActivity"
                )
            ),
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        )
    )

    private fun oneplus() = OemHelp(
        manufacturer = "OnePlus",
        title = "OnePlus / OxygenOS",
        steps = listOf(
            "Включите автозапуск, если доступен.",
            "Отключите оптимизацию батареи для приложения.",
            "Разрешите показ поверх других окон при выборе карточки."
        ),
        intentCandidates = listOf(
            Intent().setComponent(
                ComponentName(
                    "com.oneplus.security",
                    "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"
                )
            )
        )
    )

    private fun oppoFamily(key: String) = OemHelp(
        manufacturer = if (key.contains("realme")) "Realme" else "Oppo",
        title = "Oppo / Realme / ColorOS",
        steps = listOf(
            "Разрешите автозапуск.",
            "Поставьте расход батареи в режим «Не ограничивать».",
            "Отключите «заморозку» фоновых приложений для RemDoc."
        ),
        intentCandidates = listOf(
            Intent().setComponent(
                ComponentName(
                    "com.coloros.safecenter",
                    "com.coloros.safecenter.permission.startup.StartupAppListActivity"
                )
            ),
            Intent().setComponent(
                ComponentName(
                    "com.oplus.safecenter",
                    "com.oplus.safecenter.permission.startup.StartupAppListActivity"
                )
            )
        )
    )

    private fun vivo() = OemHelp(
        manufacturer = "Vivo",
        title = "Vivo / Funtouch / OriginOS",
        steps = listOf(
            "Разрешите автозапуск и фоновую активность.",
            "Отключите высокоинтенсивную оптимизацию батареи.",
            "Для разблокировки используйте надёжный режим с уведомлением."
        ),
        intentCandidates = listOf(
            Intent().setComponent(
                ComponentName(
                    "com.iqoo.secure",
                    "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"
                )
            )
        )
    )

    private fun generic(name: String) = OemHelp(
        manufacturer = name,
        title = name,
        steps = listOf(
            "Откройте сведения о приложении и разрешите нужные разрешения.",
            "Отключите оптимизацию батареи, если события разблокировки пропускаются.",
            "После перезагрузки откройте приложение один раз, если автозапуск ограничен."
        ),
        intentCandidates = listOf(
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        )
    )
}
