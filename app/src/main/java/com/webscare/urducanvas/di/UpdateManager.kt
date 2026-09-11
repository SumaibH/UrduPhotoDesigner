package com.webscare.urducanvas.di

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.testing.FakeAppUpdateManager
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.webscare.urducanvas.common.datastore.PreferenceDataStoreAPI
import com.webscare.urducanvas.common.datastore.PreferenceDataStoreKeysConstants.REMIND_LATER_TIMESTAMP
import com.webscare.urducanvas.common.utils.GlobalSnackbar
import com.webscare.urducanvas.ui.update.UpdateSheet
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

// UpdateManager.kt
@Singleton
class UpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: PreferenceDataStoreAPI
) {

    companion object {
        const val REQUEST_CODE_UPDATE = 1001
        const val DEBUG_MODE = false
    }

    private val appUpdateManager = if (DEBUG_MODE) {
        FakeAppUpdateManager(context)
    } else {
        AppUpdateManagerFactory.create(context)
    }

    fun checkForUpdate(activity: AppCompatActivity) {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main) {
                if (DEBUG_MODE) {
                    (appUpdateManager as FakeAppUpdateManager).setUpdateAvailable(10)
                    fetchAndShowUpdate(activity)
                } else {
                    fetchAndShowUpdate(activity)
                }
            }
        }
    }

    private fun fetchAndShowUpdate(activity: AppCompatActivity) {
        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { appUpdateInfo ->
                when {
                    appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                            && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) -> {
                        showUpdateSheet(activity, appUpdateInfo, isForced = false)
                    }

                    appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                            && (appUpdateInfo.clientVersionStalenessDays() ?: 0) >= 7
                            && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) -> {
                        showUpdateSheet(activity, appUpdateInfo, isForced = true)
                    }
                }
            }
            .addOnFailureListener { it.printStackTrace() }
    }

    /**
     * A forced update passes no remind-later, which is what makes the sheet refuse to
     * be dismissed — the one screen in the app the user cannot walk away from.
     */
    private fun showUpdateSheet(
        activity: AppCompatActivity,
        appUpdateInfo: AppUpdateInfo,
        isForced: Boolean
    ) {
        val manager = activity.supportFragmentManager
        // Two checks can land together on a cold start, and a saved state means the
        // activity is on its way out — showing into either would crash or double up.
        if (manager.isStateSaved) return
        if (manager.findFragmentByTag(UpdateSheet.TAG) != null) return

        UpdateSheet.newInstance(
            onUpdateNow = { startUpdate(activity, appUpdateInfo, isForced) },
            onRemindLater = if (isForced) null else ({
                CoroutineScope(Dispatchers.IO).launch {
                    dataStore.putPreference(REMIND_LATER_TIMESTAMP, System.currentTimeMillis())
                }
            })
        ).show(manager, UpdateSheet.TAG)
    }

    private fun startUpdate(
        activity: AppCompatActivity,
        appUpdateInfo: AppUpdateInfo,
        isForced: Boolean
    ) {
        val updateType = if (isForced) AppUpdateType.IMMEDIATE else AppUpdateType.FLEXIBLE
        appUpdateManager.startUpdateFlowForResult(
            appUpdateInfo,
            updateType,
            activity,
            REQUEST_CODE_UPDATE
        )
    }

    private val installStateListener = InstallStateUpdatedListener { state ->
        when (state.installStatus()) {
            InstallStatus.DOWNLOADED -> currentActivity?.runOnUiThread { showRestartSnackbar() }
            InstallStatus.FAILED,
            InstallStatus.CANCELED -> currentActivity?.runOnUiThread {
                currentActivity?.let { fetchAndShowUpdate(it) }
            }

            else -> {}
        }
    }

    // Hold a weak reference to avoid leaking the Activity
    private var currentActivity: AppCompatActivity? = null

    private fun showRestartSnackbar() {
        val activity = currentActivity ?: return
        GlobalSnackbar.showSuccess(
            activity,
            message = "Update ready! Restart to apply.",
            actionText = "Restart",
            duration = Snackbar.LENGTH_INDEFINITE,
            anchor = null,
            onAction = { appUpdateManager.completeUpdate() }
        )
    }

    fun onResume(activity: AppCompatActivity) {
        currentActivity = activity
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.installStatus() == InstallStatus.DOWNLOADED) {
                showRestartSnackbar()
            }
            if (info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                appUpdateManager.startUpdateFlowForResult(
                    info,
                    AppUpdateType.IMMEDIATE,
                    activity,
                    REQUEST_CODE_UPDATE
                )
            }
        }
    }

    fun onDestroy() {
        currentActivity = null
        appUpdateManager.unregisterListener(installStateListener)
    }

    fun registerListener(activity: AppCompatActivity) {
        currentActivity = activity
        appUpdateManager.registerListener(installStateListener)
    }
}