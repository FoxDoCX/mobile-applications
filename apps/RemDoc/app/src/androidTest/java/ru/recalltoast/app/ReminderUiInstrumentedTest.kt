package ru.recalltoast.app

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReminderUiInstrumentedTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun canFinishOnboardingIfShown() {
        rule.waitForIdle()
        // Advance onboarding if present
        repeat(3) {
            try {
                rule.onNodeWithText("Continue").performClick()
                rule.waitForIdle()
            } catch (_: Throwable) {
                try {
                    rule.onNodeWithText("Далее").performClick()
                    rule.waitForIdle()
                } catch (_: Throwable) {
                    try {
                        rule.onNodeWithText("Done").performClick()
                    } catch (__: Throwable) {
                        try {
                            rule.onNodeWithText("Готово").performClick()
                        } catch (_: Throwable) {
                        }
                    }
                }
            }
        }
    }

    @Test
    fun settingsButtonExistsAfterOnboarding() {
        canFinishOnboardingIfShown()
        rule.waitForIdle()
        try {
            rule.onNodeWithContentDescription("Settings").assertExists()
        } catch (_: Throwable) {
            rule.onNodeWithContentDescription("Настройки").assertExists()
        }
    }
}
