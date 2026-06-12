package me.ltthuc.kmp.core.common.share

import platform.StoreKit.SKStoreReviewController

actual class Reviewer {
    actual fun requestReview() {
        SKStoreReviewController.requestReview()
    }
}
