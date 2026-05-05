package com.minicut.timer

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.minicut.timer.data.local.OnboardingPreferences
import com.minicut.timer.notifications.scheduleMiniCutNotifications
import com.minicut.timer.ui.MiniCutRoot
import com.minicut.timer.ui.onboarding.OnboardingScreen
import com.minicut.timer.ui.theme.MiniCutTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        ensureNotificationsReady()
        val adsConsentManager = (application as MiniCutApplication).adsConsentManager
        adsConsentManager.gatherConsent(this)

        setContent {
            MiniCutTheme {
                val adsConsentState by adsConsentManager.uiState.collectAsStateWithLifecycle()
                var showOnboarding by rememberSaveable {
                    mutableStateOf(!OnboardingPreferences.isCompleted(this))
                }

                if (showOnboarding) {
                    OnboardingScreen(
                        onStart = {
                            OnboardingPreferences.setCompleted(this, true)
                            showOnboarding = false
                        },
                    )
                } else {
                    MiniCutRoot(
                        canRequestAds = adsConsentState.canRequestAds,
                        isPrivacyOptionsRequired = adsConsentState.isPrivacyOptionsRequired,
                        onPrivacyOptionsClick = {
                            adsConsentManager.showPrivacyOptionsForm(this) {}
                        },
                    )
                }
            }
        }
    }

    private fun ensureNotificationsReady() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted =
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
            if (granted) {
                scheduleMiniCutNotifications(this)
            }
        } else {
            scheduleMiniCutNotifications(this)
        }
    }
}
