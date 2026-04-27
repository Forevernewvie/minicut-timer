package com.minicut.timer.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.minicut.timer.util.MiniCutDiagnostics
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MiniCutAdsConsentManager(
    private val applicationContext: Context,
) {
    private val mobileAdsInitialized = AtomicBoolean(false)
    private var consentInformation: ConsentInformation? = null
    private val mutableUiState = MutableStateFlow(AdsConsentUiState())

    val uiState: StateFlow<AdsConsentUiState> = mutableUiState.asStateFlow()

    fun gatherConsent(activity: Activity) {
        val consentInfo = UserMessagingPlatform.getConsentInformation(activity)
        consentInformation = consentInfo
        val params = ConsentRequestParameters.Builder().build()

        consentInfo.requestConsentInfoUpdate(
            activity,
            params,
            {
                syncUiState(consentInfo)
                loadAndShowConsentFormIfRequired(activity, consentInfo)
                initializeMobileAdsIfAllowed(consentInfo)
            },
            {
                syncUiState(consentInfo)
                initializeMobileAdsIfAllowed(consentInfo)
            },
        )
    }

    fun showPrivacyOptionsForm(
        activity: Activity,
        onDismissed: () -> Unit,
    ) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) {
            consentInformation?.let { consentInfo ->
                syncUiState(consentInfo)
                initializeMobileAdsIfAllowed(consentInfo)
            }
            onDismissed()
        }
    }

    private fun loadAndShowConsentFormIfRequired(
        activity: Activity,
        consentInfo: ConsentInformation,
    ) {
        UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
            syncUiState(consentInfo)
            initializeMobileAdsIfAllowed(consentInfo)
        }
    }

    private fun initializeMobileAdsIfAllowed(consentInfo: ConsentInformation) {
        if (!consentInfo.canRequestAds()) return
        if (!mobileAdsInitialized.compareAndSet(false, true)) return

        Thread {
            MiniCutDiagnostics.guard("MiniCutAdsConsentManager.initializeMobileAds") {
                MobileAds.initialize(applicationContext) {}
            }
        }.start()
    }

    private fun syncUiState(consentInfo: ConsentInformation) {
        mutableUiState.update {
            it.copy(
                canRequestAds = consentInfo.canRequestAds(),
                isPrivacyOptionsRequired =
                    consentInfo.privacyOptionsRequirementStatus ==
                        ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED,
            )
        }
    }
}

data class AdsConsentUiState(
    val canRequestAds: Boolean = false,
    val isPrivacyOptionsRequired: Boolean = false,
)
