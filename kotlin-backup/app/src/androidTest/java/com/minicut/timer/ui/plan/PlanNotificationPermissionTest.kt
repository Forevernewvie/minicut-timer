package com.minicut.timer.ui.plan

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performClick
import androidx.core.content.ContextCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.minicut.timer.MainActivity
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class PlanNotificationPermissionTest {
    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private var notificationPermissionInitiallyGranted: Boolean = false

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Before
    fun prepareState() {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        notificationPermissionInitiallyGranted = isNotificationPermissionGranted()
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .putBoolean(KEY_ONBOARDING_COMPLETED, true)
            .commit()
        revokeNotificationPermission()
    }

    @After
    fun cleanupState() {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit()
        restoreNotificationPermission()
    }

    @Test
    fun planShowsNotificationPermissionPromptWhenPermissionDenied() {
        ActivityScenario.launch(MainActivity::class.java).use {
            composeTestRule.onNodeWithText("플랜").performClick()
            composeTestRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("알림 권한 확인"))
            composeTestRule.onNodeWithText("알림 권한 확인").assertIsDisplayed()
            composeTestRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("알림 권한 허용"))
            composeTestRule.onNodeWithText("알림 권한 허용").assertIsDisplayed()
        }
    }

    private fun revokeNotificationPermission() {
        val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
        uiAutomation.adoptShellPermissionIdentity()
        try {
            uiAutomation.revokeRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        } finally {
            uiAutomation.dropShellPermissionIdentity()
        }

        check(
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED,
        ) {
            "POST_NOTIFICATIONS permission must be revoked before this test runs."
        }
    }

    private fun restoreNotificationPermission() {
        val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
        uiAutomation.adoptShellPermissionIdentity()
        try {
            if (notificationPermissionInitiallyGranted) {
                uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
            } else {
                uiAutomation.revokeRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
            }
        } finally {
            uiAutomation.dropShellPermissionIdentity()
        }
    }

    private fun isNotificationPermissionGranted(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED

    private companion object {
        const val PREFS_NAME = "minicut_prefs"
        const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    }
}
