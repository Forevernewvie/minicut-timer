package com.minicut.timer

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AppSmokeTest {
    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Before
    fun resetLocalPrefs() {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit()
    }

    @After
    fun cleanupLocalPrefs() {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun launchShowsOnboardingWhenNotCompleted() {
        ActivityScenario.launch(MainActivity::class.java).use {
            composeTestRule.onNodeWithText("플랜 만들고 시작하기").assertIsDisplayed()
        }
    }

    @Test
    fun launchShowsDashboardWhenOnboardingCompleted() {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ONBOARDING_COMPLETED, true)
            .commit()

        ActivityScenario.launch(MainActivity::class.java).use {
            composeTestRule.onNodeWithText("오늘 스프린트 컨트롤").assertIsDisplayed()
        }
    }

    private companion object {
        const val PREFS_NAME = "minicut_prefs"
        const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    }
}
