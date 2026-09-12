package com.webscare.urducanvas.ui.navigation.settings

import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.os.bundleOf
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.navigation.fragment.findNavController
import com.webscare.urducanvas.ui.navigation.settings.subscriptions.SubscriptionsFragment
import com.webscare.urducanvas.analytics.AnalyticsConstants.Values
import com.webscare.urducanvas.R
import com.webscare.urducanvas.common.utils.Utils.addPressEffect
import com.webscare.urducanvas.databinding.FragmentSettingsBinding
import dagger.hilt.android.AndroidEntryPoint
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.webscare.urducanvas.MainActivity
import com.webscare.urducanvas.BuildConfig
import com.webscare.urducanvas.di.AppReviewManager
import com.webscare.urducanvas.di.BillingManager
import com.webscare.urducanvas.di.BillingManager.SubscriptionStatus
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import com.webscare.urducanvas.common.utils.InsetUtils.applyStatusBarTopPadding

@AndroidEntryPoint
class SettingsFragment : androidx.fragment.app.Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var billingManager: BillingManager

    @Inject
    lateinit var adAnalyticsCoordinator: com.webscare.urducanvas.analytics.ads.AdAnalyticsCoordinator

    @Inject
    lateinit var appReviewManager: AppReviewManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Edge to edge: the window no longer reserves the status bar, so leave the margin here.
        view.applyStatusBarTopPadding()

        alignRowIcons()
        observeSubscription()
        setEvents()
        setVersionInfo()
        setupBannerAd()
    }

    /**
     * Attaches the settings banner.
     *
     * The attach is the opportunity — a banner is a slot the SDK fills whenever it likes,
     * with no "show" call to hang one off. The matching impression comes from
     * `AdConfig.onAdImpression`, wired in MyApplication, and the coordinator counts only
     * the first render per attach: AdMob refreshes the same AdView on its own schedule and
     * reports every refresh, so counting those would make the two numbers undivisible.
     */
    private fun setupBannerAd() {
        // Blank in the no-ads flavour, and a subscriber has paid not to see this.
        if (BuildConfig.AD_BANNER_SETTINGS.isBlank() || billingManager.isSubscribed.value) return
        binding.settingsBannerAd.isVisible = true
        binding.settingsBannerAd.setAdUnitId(BuildConfig.AD_BANNER_SETTINGS)
        adAnalyticsCoordinator.onAdSlotAttached(
            adUnitName = "banner_settings",
            adUnitId = BuildConfig.AD_BANNER_SETTINGS,
            adFormat = "banner",
            triggerFeature = "settings"
        )
    }

    /**
     * Pins every settings row's leading icon into one fixed-width box.
     *
     * Compound drawables are laid out at their intrinsic width, and these icons range
     * from 12dp to 18dp, so each row's label started at a different x — "Request a
     * Feature" sat visibly further left than the rows above and below it. Centring each
     * glyph in a common box lines the labels up without stretching any of the artwork.
     */
    private fun alignRowIcons() {
        val b = _binding ?: return
        val box = (ROW_ICON_BOX_DP * resources.displayMetrics.density).toInt()
        listOf(
            b.preferences, b.tutorials, b.support, b.privacy, b.rate,
            b.whatsappChannel, b.improve, b.requestFeature, b.reportBug
        ).forEach { row ->
            val start = row.compoundDrawablesRelative[0] ?: return@forEach
            val pad = ((box - start.intrinsicWidth) / 2).coerceAtLeast(0)
            val boxed = android.graphics.drawable.InsetDrawable(start, pad, 0, pad, 0)
            boxed.setBounds(0, 0, box, start.intrinsicHeight)
            row.setCompoundDrawablesRelative(
                boxed, null, row.compoundDrawablesRelative[2], null
            )
        }
    }

    override fun onResume() {
        super.onResume()
        billingManager.refreshSnapshot()
    }

    private fun setVersionInfo() {
        val versionName = try {
            requireContext().packageManager
                .getPackageInfo(requireContext().packageName, 0).versionName
        } catch (e: Exception) {
            "—"
        }
        binding.versionInfo.text = "Version $versionName"
    }

    private fun observeSubscription() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                billingManager.snapshot.collect { snap ->
                    val subscribed = snap.status != SubscriptionStatus.NOT_SUBSCRIBED
                            && snap.status != SubscriptionStatus.PENDING

                    if (subscribed) {
                        binding.subscriptionCard.visibility = View.GONE
                        binding.currentPlanCard.visibility = View.VISIBLE
                        renderCurrentPlanCard(snap)
                    } else {
                        binding.subscriptionCard.visibility = View.VISIBLE
                        binding.currentPlanCard.visibility = View.GONE
                    }
                }
            }
        }
    }

    /**
     * Fills in the compact "current plan" pill card — accent bar, icon
     * chip, and status badge all follow the actual subscription status
     * (same color mapping ManageSubscriptionFragment uses), not a
     * hardcoded "active" look. The subtitle line shows a short renewal/
     * trial/access date when one is available.
     */
    private fun renderCurrentPlanCard(snap: BillingManager.PlayBillingSnapshot) {
        val status = snap.status
        val planName = planFriendlyName(snap.productId)

        val (accentRes, tintRes) = when (status) {
            SubscriptionStatus.TRIAL    -> R.color.state_teal to R.color.state_teal_tint
            SubscriptionStatus.ACTIVE   -> R.color.state_green to R.color.state_green_tint
            SubscriptionStatus.CANCELED -> R.color.state_amber to R.color.state_amber_tint
            else                        -> R.color.state_green to R.color.state_green_tint
        }
        val accent = color(accentRes)
        val tint = color(tintRes)

        ViewCompat.setBackgroundTintList(binding.statusAccentBar, ColorStateList.valueOf(accent))
        ViewCompat.setBackgroundTintList(binding.manageCardIcon, ColorStateList.valueOf(tint))
        binding.manageCardIcon.imageTintList = ColorStateList.valueOf(accent)

        binding.manageCardTitle.text = planName

        ViewCompat.setBackgroundTintList(binding.manageCardStatusBadge, ColorStateList.valueOf(tint))
        binding.manageCardStatusBadge.setTextColor(accent)
        binding.manageCardStatusBadge.text = getString(when (status) {
            SubscriptionStatus.TRIAL    -> R.string.mng_chip_trial
            SubscriptionStatus.CANCELED -> R.string.mng_chip_canceled
            else                        -> R.string.mng_chip_active
        }).uppercase(Locale.getDefault())

        binding.manageCardSubTitle.text = statusDetailLine(status, snap.expiryTimeMillis)
    }

    /** Short one-line status/date summary — "Renews Aug 1, 2026", "Trial ends ...", etc. */
    private fun statusDetailLine(status: SubscriptionStatus, expiryMillis: Long?): String {
        val date = expiryMillis?.takeIf { it > 0L }?.let {
            SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(it))
        }
        return when (status) {
            SubscriptionStatus.ACTIVE   -> if (date != null) "Renews $date" else "Auto-renews"
            SubscriptionStatus.CANCELED -> if (date != null) "Access until $date" else "Auto-renew is off"
            SubscriptionStatus.TRIAL    -> if (date != null) "Trial ends $date" else "Free trial"
            else                        -> "Pro plan"
        }
    }

    private fun planFriendlyName(productId: String?): String = when (productId) {
        "urducanvas_monthly" -> "Monthly"
        "urducanvas_6months" -> "6 Months"
        "urducanvas_yearly"  -> "Yearly"
        else                 -> "Pro"
    }

    private fun goToSubscriptions() {
        view?.post {
            findNavController().navigate(
                R.id.subscriptionsFragment,
                bundleOf(SubscriptionsFragment.ARG_SOURCE to Values.PAYWALL_SETTINGS)
            )
        }
    }

    private fun setEvents() {
        binding.back.addPressEffect { findNavController().navigateUp() }

        // Entire upgrade card + button both navigate to subscriptions.
        binding.subscriptionCard.addPressEffect { goToSubscriptions() }
        binding.upgradeNow.addPressEffect { goToSubscriptions() }

        // Entire manage card + button both navigate to manage screen.
        binding.currentPlanCard.addPressEffect {
            view?.post { findNavController().navigate(R.id.manageSubscriptionFragment) }
        }
        binding.manage.addPressEffect {
            view?.post { findNavController().navigate(R.id.manageSubscriptionFragment) }
        }

        binding.preferences.addPressEffect {
            view?.post { findNavController().navigate(R.id.preferencesFragment) }
        }

        binding.tutorials.addPressEffect {
            view?.post { findNavController().navigate(R.id.tutorialsFragment) }
        }

        binding.support.addPressEffect {
            openEmail(
                to = "support@urducanvas.com",
                subject = "Support Request",
                body = ""
            )
        }

        binding.privacy.addPressEffect {
            openUrl("https://urducanvas.com/privacy-policy")
        }

        binding.rate.addPressEffect {
            appReviewManager.launchExplicitReview(requireActivity()) {
                openUrl("https://play.google.com/store/apps/details?id=${requireContext().packageName}")
            }
        }

        binding.whatsappChannel.addPressEffect {
            openUrl("https://whatsapp.com/channel/0029Vb79Ac14IBhIMZYyvj0Y")
        }

        binding.improve.addPressEffect {
            openEmail(
                to = "support@urducanvas.com",
                subject = "Feedback – Help Us Improve",
                body = ""
            )
        }

        binding.requestFeature.addPressEffect {
            openEmail(
                to = "support@urducanvas.com",
                subject = "Feature Request",
                body = "Hi, I'd like to request the following feature:\n\n"
            )
        }

        binding.reportBug.addPressEffect {
            openEmail(
                to = "support@urducanvas.com",
                subject = "Bug Report",
                body = "Hi, I'd like to report the following bug:\n\nDevice: ${android.os.Build.MODEL}\nAndroid: ${android.os.Build.VERSION.RELEASE}\n\nDescription:\n"
            )
        }
    }

    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            // no browser installed — silently ignore
        }
    }

    private fun openEmail(to: String, subject: String, body: String) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = "mailto:".toUri()
                putExtra(Intent.EXTRA_EMAIL, arrayOf(to))
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
            }
            startActivity(Intent.createChooser(intent, "Send Email"))
        } catch (e: Exception) {
            // no email app installed — silently ignore
        }
    }

    private fun color(res: Int) = ContextCompat.getColor(requireContext(), res)

    /**
     * Takes the banner out of the tree before this view goes.
     *
     * Measured on a Samsung S24 with LeakCanary: closing Settings left the whole
     * screen's view hierarchy reachable. The chain is
     * `JNI global → Google's ad WebView → AdView → its AdListener →
     * BannerAdHandler$loadBanner$1.$container → WebsCareBannerView → mParent →
     * this fragment's ConstraintLayout`.
     *
     * Every link above the banner belongs to the ad SDKs — the root is one of
     * Google Mobile Ads' own global references, and `WebsCareBannerView` exposes no
     * release of its own — so the banner itself outlives this fragment whatever we
     * do. `mParent` is the single link the app owns, and cutting it is the
     * difference between leaking one ad view and leaking every view on the screen
     * behind it. Removing it also fires the SDK's own `onDetachedFromWindow`
     * cleanup, which is where it calls `destroyBanner`.
     */
    override fun onDestroyView() {
        _binding?.settingsBannerAd?.let { banner ->
            (banner.parent as? ViewGroup)?.removeView(banner)
        }
        super.onDestroyView()
        _binding = null
    }

    companion object {
        /** Wide enough for the broadest row icon (18dp) with a little air either side. */
        private const val ROW_ICON_BOX_DP = 20
    }
}