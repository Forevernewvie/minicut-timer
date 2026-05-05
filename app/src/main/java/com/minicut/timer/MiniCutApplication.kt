package com.minicut.timer

import android.app.Application
import com.minicut.timer.ads.MiniCutAdsConsentManager
import com.minicut.timer.data.AppContainer
import com.minicut.timer.notifications.createMiniCutNotificationChannel

class MiniCutApplication : Application() {
    lateinit var container: AppContainer
        private set
    lateinit var adsConsentManager: MiniCutAdsConsentManager
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        adsConsentManager = MiniCutAdsConsentManager(this)
        createMiniCutNotificationChannel(this)
    }
}
