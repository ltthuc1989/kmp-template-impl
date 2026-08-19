package me.ltthuc.kmp.update

import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Play in-app updates, Android only.
 *
 * This matters more here than in a typical app: the content manifest is bundled in the APK
 * (`files/content_manifest.json`), so audio published to the CDN is invisible until the user
 * is on a build whose manifest lists it. A user who never updates never sees new lessons —
 * nudging them is the only delivery mechanism content has.
 *
 * IMMEDIATE blocks the app behind Play's own full-screen flow, so it is reserved for
 * releases that say they need it. Everything else downloads silently and only asks for a
 * restart once the bytes are already on the device, which keeps a child mid-lesson
 * undisturbed.
 *
 * Debug builds sideloaded from Gradle are not Play-installed, so the info request fails and
 * every path here no-ops. That is expected, not a misconfiguration — test on an internal
 * testing track build.
 */
internal class InAppUpdateController(
    private val activity: ComponentActivity,
    // Injectable so a test can drive the whole flow with Play's own FakeAppUpdateManager,
    // which ships inside the app-update artifact. Nothing else has a reason to pass this.
    private val manager: AppUpdateManager = AppUpdateManagerFactory.create(activity),
) : DefaultLifecycleObserver {

    private val _readyToInstall = MutableStateFlow(false)

    /** True once a flexible update has finished downloading and only needs a restart. */
    val readyToInstall: StateFlow<Boolean> = _readyToInstall.asStateFlow()

    // Registered eagerly in the constructor: ComponentActivity rejects registration once the
    // activity is STARTED, and the controller is built from onCreate.
    private val launcher = activity.registerForActivityResult(StartIntentSenderForResult()) { result ->
        if (result.resultCode != android.app.Activity.RESULT_OK) {
            // Declining is a legitimate answer, including for IMMEDIATE — Play closes the app
            // in that case, so there is nothing to recover here.
            Napier.i("In-app update flow dismissed (resultCode=${result.resultCode})")
        }
    }

    private val installListener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            _readyToInstall.value = true
        }
    }

    /**
     * Whether a flexible flow has already been offered in this process. Play keeps reporting
     * UPDATE_AVAILABLE until the update is installed, and onResume fires every time the user
     * comes back from another app — without this the same dialog would reappear all day.
     */
    private var flexibleOffered = false

    init {
        manager.registerListener(installListener)
        activity.lifecycle.addObserver(this)
    }

    override fun onResume(owner: LifecycleOwner) {
        manager.appUpdateInfo
            .addOnSuccessListener(::onInfo)
            .addOnFailureListener { Napier.i("No in-app update info: ${it.message}") }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        manager.unregisterListener(installListener)
    }

    private fun onInfo(info: AppUpdateInfo) {
        when {
            // An IMMEDIATE flow the user backgrounded out of. Play requires it to be resumed,
            // otherwise the app is left in a half-updated state.
            info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS ->
                launchFlow(info, AppUpdateType.IMMEDIATE)

            info.updateAvailability() != UpdateAvailability.UPDATE_AVAILABLE -> Unit

            // Bytes already on the device — the listener misses this when the download
            // finished while the activity was gone.
            info.installStatus() == InstallStatus.DOWNLOADED -> _readyToInstall.value = true

            // Play is fetching it; asking again would stack a second flow on the first.
            info.installStatus() == InstallStatus.DOWNLOADING -> Unit

            wantsImmediate(info) && info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) ->
                launchFlow(info, AppUpdateType.IMMEDIATE)

            !flexibleOffered && info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) -> {
                flexibleOffered = true
                launchFlow(info, AppUpdateType.FLEXIBLE)
            }
        }
    }

    /**
     * Both signals come from Play, not from our code, so the aggressiveness of a release is
     * decided at publish time and can be changed without shipping anything:
     * [AppUpdateInfo.updatePriority] is set per release through the Publishing API, and
     * staleness is measured by Play from the release date.
     *
     * Convention for this app: releases carrying new curriculum content go out at priority
     * [IMMEDIATE_PRIORITY] or above, ordinary fixes below it.
     */
    private fun wantsImmediate(info: AppUpdateInfo): Boolean =
        info.updatePriority() >= IMMEDIATE_PRIORITY ||
            (info.clientVersionStalenessDays() ?: 0) >= IMMEDIATE_STALENESS_DAYS

    private fun launchFlow(info: AppUpdateInfo, @AppUpdateType type: Int) {
        runCatching {
            manager.startUpdateFlowForResult(info, launcher, AppUpdateOptions.newBuilder(type).build())
        }.onFailure {
            Napier.w("Could not start in-app update flow (type=$type)", it)
        }
    }

    /** Installs the downloaded update. Play restarts the app, so nothing after this runs. */
    fun completeUpdate() {
        _readyToInstall.value = false
        manager.completeUpdate()
    }

    private companion object {
        /** Play's priority scale is 0..5. At or above this, block the app until updated. */
        const val IMMEDIATE_PRIORITY = 4

        /** A build this far behind is blocked regardless of the release's own priority. */
        const val IMMEDIATE_STALENESS_DAYS = 30
    }
}
