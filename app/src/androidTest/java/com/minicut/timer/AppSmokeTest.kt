package com.minicut.timer

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import org.junit.Rule
import org.junit.Test

class AppSmokeTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun launchShowsCoreBranding() {
        composeTestRule.onNodeWithText("미니컷 타이머").assertIsDisplayed()
    }
}
