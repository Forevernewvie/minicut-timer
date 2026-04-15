package com.minicut.timer.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.minicut.timer.BuildConfig
import com.minicut.timer.R

private const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"

@Composable
fun AdMobBanner(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val adUnitId =
        if (BuildConfig.DEBUG) {
            TEST_BANNER_AD_UNIT_ID
        } else {
            stringResource(R.string.admob_banner_unit_id)
        }
    val adSize = remember(configuration.orientation, configuration.screenWidthDp) {
        AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
            context,
            configuration.screenWidthDp,
        )
    }
    val adView = remember(context) { AdView(context) }

    DisposableEffect(adView) {
        onDispose { adView.destroy() }
    }

    LaunchedEffect(adView, adUnitId, adSize) {
        adView.adUnitId = adUnitId
        adView.setAdSize(adSize)
        adView.loadAd(AdRequest.Builder().build())
    }

    AndroidView(
        modifier = modifier,
        factory = { adView },
    )
}
