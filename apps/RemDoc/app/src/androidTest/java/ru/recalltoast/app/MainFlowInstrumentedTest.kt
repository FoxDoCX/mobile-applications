package ru.recalltoast.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainFlowInstrumentedTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun appLaunchesAndShowsContent() {
        rule.waitForIdle()
        // Onboarding title (EN) or app name on main screen.
        val matcher = androidx.compose.ui.test.hasText("RemDoc")
            .or(androidx.compose.ui.test.hasText("Quick local reminders"))
            .or(androidx.compose.ui.test.hasText("Локальные напоминания"))
        rule.onNode(matcher).assertIsDisplayed()
    }
}
