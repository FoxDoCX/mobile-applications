package ru.recalltoast.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import ru.recalltoast.app.domain.model.ReminderTrigger
import ru.recalltoast.app.ui.AboutScreen
import ru.recalltoast.app.ui.DebugScreen
import ru.recalltoast.app.ui.DiagnosticsScreen
import ru.recalltoast.app.ui.MainScreen
import ru.recalltoast.app.ui.MainViewModel
import ru.recalltoast.app.ui.OnboardingScreen
import ru.recalltoast.app.ui.PermissionScreen
import ru.recalltoast.app.ui.PrivacyScreen
import ru.recalltoast.app.ui.ReminderEditorScreen
import ru.recalltoast.app.ui.SettingsScreen
import ru.recalltoast.app.ui.theme.RecallToastTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val vm: MainViewModel = viewModel()
            val settings by vm.settings.collectAsStateWithLifecycle()
            RecallToastTheme(themeMode = settings.themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    RecallNav(vm)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val app = application as RecallApplication
        lifecycleScope.launch {
            val settings = app.settingsRepository.getSettings()
            if (settings.enabled && !settings.paused && settings.appOpenEnabled) {
                app.reminderPresenter.showForTrigger(ReminderTrigger.APP_OPEN)
            }
        }
    }
}

@Composable
private fun RecallNav(vm: MainViewModel) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val start = if (settings.onboardingDone) "main" else "onboarding"

    NavHost(navController = nav, startDestination = start) {
        composable("onboarding") {
            OnboardingScreen { mode, display ->
                vm.completeOnboarding(mode, display)
                nav.navigate("main") {
                    popUpTo("onboarding") { inclusive = true }
                }
            }
        }
        composable("main") {
            MainScreen(
                viewModel = vm,
                onOpenSettings = { nav.navigate("settings") },
                onOpenEditor = { id -> nav.navigate("editor/$id") },
                onOpenPermissions = { nav.navigate("permissions") },
                onOpenDebug = { nav.navigate("debug") }
            )
        }
        composable("settings") {
            SettingsScreen(
                viewModel = vm,
                onBack = { nav.popBackStack() },
                onOpenPrivacy = { nav.navigate("privacy") },
                onOpenAbout = { nav.navigate("about") },
                onOpenDiagnostics = { nav.navigate("diagnostics") }
            )
        }
        composable(
            route = "editor/{id}",
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { entry ->
            ReminderEditorScreen(
                reminderId = entry.arguments?.getString("id").orEmpty(),
                viewModel = vm,
                onBack = { nav.popBackStack() }
            )
        }
        composable("permissions") { PermissionScreen(onBack = { nav.popBackStack() }) }
        composable("privacy") { PrivacyScreen(onBack = { nav.popBackStack() }) }
        composable("about") { AboutScreen(onBack = { nav.popBackStack() }) }
        composable("diagnostics") { DiagnosticsScreen(onBack = { nav.popBackStack() }) }
        composable("debug") { DebugScreen(viewModel = vm, onBack = { nav.popBackStack() }) }
    }
}
