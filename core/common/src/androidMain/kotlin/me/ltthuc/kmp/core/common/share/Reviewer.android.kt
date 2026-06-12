package me.ltthuc.kmp.core.common.share

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.android.play.core.review.ReviewManagerFactory

actual class Reviewer(private val context: Context) {
    actual fun requestReview() {
        val activity = AndroidActivityHolder.current
        if (activity == null) {
            openStore()
            return
        }
        val manager = ReviewManagerFactory.create(activity)
        manager.requestReviewFlow().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                manager.launchReviewFlow(activity, task.result)
                    .addOnFailureListener { openStore() }
            } else {
                openStore()
            }
        }
    }

    private fun openStore() {
        val launcher = AndroidActivityHolder.current ?: context
        val market = Intent(Intent.ACTION_VIEW, Uri.parse(StoreLinks.MARKET_URI))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { launcher.startActivity(market) }.onFailure {
            val web = Intent(Intent.ACTION_VIEW, Uri.parse(StoreLinks.PLAY_STORE_URL))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { launcher.startActivity(web) }
        }
    }
}
