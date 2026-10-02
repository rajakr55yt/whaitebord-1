package com.example.util

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

object RewardedAdManager {

    private const val TAG = "RewardedAdManager"
    const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

    private var rewardedAd: RewardedAd? = null
    private var isAdLoading = false
    private var isInitialized = false

    fun initialize(context: Context) {
        if (isInitialized) return
        try {
            MobileAds.initialize(context) {
                Log.d(TAG, "MobileAds initialized")
                isInitialized = true
                loadAd(context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing MobileAds", e)
        }
    }

    fun loadAd(context: Context, onLoaded: (() -> Unit)? = null) {
        if (rewardedAd != null) {
            onLoaded?.invoke()
            return
        }
        if (isAdLoading) return

        isAdLoading = true
        val adRequest = AdRequest.Builder().build()

        RewardedAd.load(
            context,
            TEST_REWARDED_AD_UNIT_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d(TAG, "Rewarded ad loaded successfully")
                    rewardedAd = ad
                    isAdLoading = false
                    onLoaded?.invoke()
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e(TAG, "Rewarded ad failed to load: ${loadAdError.message}")
                    rewardedAd = null
                    isAdLoading = false
                }
            }
        )
    }

    fun isAdAvailable(): Boolean = rewardedAd != null

    fun showAd(
        activity: Activity,
        onRewardEarned: (amount: Int, type: String) -> Unit,
        onAdClosed: () -> Unit = {}
    ) {
        val currentAd = rewardedAd

        if (currentAd == null) {
            Toast.makeText(activity, "Loading rewarded test ad...", Toast.LENGTH_SHORT).show()
            loadAd(activity) {
                rewardedAd?.let { loadedAd ->
                    displayAd(activity, loadedAd, onRewardEarned, onAdClosed)
                } ?: run {
                    Toast.makeText(activity, "Could not load test ad right now. Please check network.", Toast.LENGTH_SHORT).show()
                    onAdClosed()
                }
            }
            return
        }

        displayAd(activity, currentAd, onRewardEarned, onAdClosed)
    }

    private fun displayAd(
        activity: Activity,
        ad: RewardedAd,
        onRewardEarned: (amount: Int, type: String) -> Unit,
        onAdClosed: () -> Unit
    ) {
        var rewardAwarded = false

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Ad dismissed")
                rewardedAd = null
                // Preload next ad
                loadAd(activity)
                onAdClosed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(TAG, "Ad failed to show: ${adError.message}")
                rewardedAd = null
                loadAd(activity)
                onAdClosed()
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Ad showed full screen content")
            }
        }

        ad.show(activity) { rewardItem: RewardItem ->
            Log.d(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
            rewardAwarded = true
            onRewardEarned(rewardItem.amount, rewardItem.type)
        }
    }
}
