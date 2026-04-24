package com.minicut.timer

import android.app.Application
import com.minicut.timer.data.AppContainer
import com.minicut.timer.notifications.createMiniCutNotificationChannel
import com.minicut.timer.util.MiniCutDiagnostics
import com.google.android.gms.ads.MobileAds

class MiniCutApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        createMiniCutNotificationChannel(this)
        MiniCutDiagnostics.guard("MiniCutApplication.initializeMobileAds") {
            MobileAds.initialize(this)
        }
    }
}
