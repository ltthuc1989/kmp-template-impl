package me.ltthuc.kmp.ads

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import io.github.aakira.napier.Napier

/**
 * Manages App Open Ads — fullscreen ads shown when user brings the app back to the
 * foreground after being away. Implements:
 *   - Per-process lifecycle observer (ProcessLifecycleOwner) → detect foreground
 *   - 4-hour ad expiry (Google AdMob recommended)
 *   - First-launch suppression (don't show on cold start, only background→foreground)
 *
 * Initialize in [Application.onCreate]. Lives for app process lifetime.
 */
class AppOpenAdManager(
    private val application: Application,
    private val adUnitId: String,
) : Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {

    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    private var isShowingAd = false
    private var loadTime: Long = 0
    private var currentActivity: Activity? = null
    /** Skip the very first foreground transition (cold start). */
    private var coldStart = true

    init {
        application.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        loadAd()
    }

    // ---- Lifecycle (process foreground) ----
    override fun onStart(owner: LifecycleOwner) {
        if (coldStart) {
            coldStart = false
            return
        }
        showAdIfAvailable()
    }

    // ---- ActivityLifecycleCallbacks (track current activity for show()) ----
    override fun onActivityStarted(activity: Activity) {
        if (!isShowingAd) currentActivity = activity
    }

    override fun onActivityCreated(activity: Activity, bundle: Bundle?) = Unit
    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity === activity) currentActivity = null
    }

    // ---- Ad lifecycle ----
    private fun loadAd() {
        if (isLoadingAd || isAdAvailable()) return
        isLoadingAd = true
        AppOpenAd.load(
            application,
            adUnitId,
            AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    loadTime = System.currentTimeMillis()
                    isLoadingAd = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoadingAd = false
                    Napier.w(tag = TAG) { "Load fail: ${error.message}" }
                }
            },
        )
    }

    private fun isAdAvailable(): Boolean =
        appOpenAd != null && System.currentTimeMillis() - loadTime < AD_EXPIRY_MS

    private fun showAdIfAvailable() {
        val activity = currentActivity ?: run { loadAd(); return }
        if (isShowingAd || !isAdAvailable()) {
            loadAd()
            return
        }
        appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                appOpenAd = null
                isShowingAd = false
                loadAd()
            }

            override fun onAdShowedFullScreenContent() {
                isShowingAd = true
            }

            override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                appOpenAd = null
                isShowingAd = false
                Napier.w(tag = TAG) { "Show fail: ${adError.message}" }
                loadAd()
            }
        }
        appOpenAd?.show(activity)
    }

    private companion object {
        const val TAG = "AppOpenAdManager"
        const val AD_EXPIRY_MS = 4L * 60 * 60 * 1000  // 4 hours per AdMob recommendation
    }
}
