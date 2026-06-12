package me.ltthuc.kmp.core.common.share

import android.app.Activity
import java.lang.ref.WeakReference

/**
 * Holds a weak reference to the current foreground Activity so platform features that require an
 * Activity (e.g. Play In-App Review) can reach it from DI-constructed services. Set from the host
 * Activity's lifecycle (see MainActivity).
 */
object AndroidActivityHolder {
    private var ref: WeakReference<Activity>? = null

    var current: Activity?
        get() = ref?.get()
        set(value) {
            ref = value?.let { WeakReference(it) }
        }
}
