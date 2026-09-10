package com.webscare.urducanvas.ui.splash

import androidx.core.splashscreen.SplashScreenViewProvider

/**
 * Carries Android's own splash view from the activity, which is handed it, to the
 * fragment, which animates it away.
 *
 * Android 12+ paints the launcher icon on the window background before the process is
 * even up, and `setOnExitAnimationListener` hands that view over once the activity has
 * drawn its first frame. The fragment is created before that frame, so normally it has
 * already registered here and the view goes straight through. Either order is handled;
 * a view nobody claims is dismissed on a short timer so the app can never sit behind it.
 */
class SplashHandoff {

    private var pending: SplashScreenViewProvider? = null
    private var consumer: ((SplashScreenViewProvider) -> Unit)? = null

    /** The activity received the system splash view. */
    fun offer(provider: SplashScreenViewProvider) {
        val claim = consumer
        if (claim != null) {
            consumer = null
            claim(provider)
            return
        }
        pending = provider
        provider.view.postDelayed({
            if (pending === provider) {
                pending = null
                provider.remove()
            }
        }, UNCLAIMED_TIMEOUT_MS)
    }

    /** The fragment wants the view: now if it is already here, otherwise when it arrives. */
    fun consume(claim: (SplashScreenViewProvider) -> Unit) {
        val provider = pending
        if (provider != null) {
            pending = null
            claim(provider)
        } else {
            consumer = claim
        }
    }

    /** Forgets a waiting claim and dismisses any view still unclaimed. */
    fun clear() {
        consumer = null
        pending?.remove()
        pending = null
    }

    private companion object {
        const val UNCLAIMED_TIMEOUT_MS = 500L
    }
}
