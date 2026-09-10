package com.webscare.urducanvas.ui.navigation.tutorials

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.webscare.urducanvas.R
import com.webscare.urducanvas.analytics.AnalyticsTracker
import com.webscare.urducanvas.common.sealed.Response
import com.webscare.urducanvas.common.utils.Constants
import com.webscare.urducanvas.common.utils.InsetUtils.applyStatusBarTopPadding
import com.webscare.urducanvas.common.utils.Utils.addPressEffect
import com.webscare.urducanvas.data.model.TutorialVideo
import com.webscare.urducanvas.databinding.FragmentTutorialsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The tutorial videos published on the UrduCanvas YouTube channel.
 *
 * Tapping a video hands it to the YouTube app, falling back to the browser when
 * YouTube is not installed.
 */
@AndroidEntryPoint
class TutorialsFragment : Fragment() {

    private var _binding: FragmentTutorialsBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var analyticsTracker: AnalyticsTracker

    private val viewModel: TutorialsViewModel by viewModels()
    private var adapter: TutorialsAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTutorialsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Edge to edge: the window draws under the status bar, so the header has to
        // reserve it itself.
        view.applyStatusBarTopPadding()

        adapter = TutorialsAdapter { video, position -> openVideo(video, position) }
        binding.tutorialsRV.layoutManager = LinearLayoutManager(requireContext())
        binding.tutorialsRV.adapter = adapter

        binding.back.addPressEffect { findNavController().navigateUp() }
        binding.openChannel.addPressEffect { openUrl(Constants.YOUTUBE_CHANNEL_URL) }
        binding.retry.addPressEffect { viewModel.load(forceRefresh = true) }
        binding.swipeRefresh.setOnRefreshListener { viewModel.load(forceRefresh = true) }

        observeState()
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { response ->
                    when (response) {
                        is Response.Loading -> showSkeletons()
                        is Response.Processing -> response.data?.let { showVideos(it) }
                        is Response.Success -> showVideos(response.data.orEmpty())
                        is Response.Error -> showEmpty(
                            getString(R.string.tutorials_error_title),
                            getString(R.string.tutorials_error_message)
                        )
                    }
                }
            }
        }
    }

    // ── States ────────────────────────────────────────────────────────────────

    /**
     * Only shown on a cold start. Once anything has been cached the list is populated
     * straight away and a refresh runs behind it, so the skeletons are not flashed at a
     * user who already has content on screen.
     */
    private fun showSkeletons() {
        if (_binding == null) return
        if (!adapter?.currentList.isNullOrEmpty()) return
        binding.skeletonContainer.isVisible = true
        binding.emptyState.isVisible = false
        binding.swipeRefresh.isVisible = false
    }

    private fun showVideos(videos: List<TutorialVideo>) {
        if (_binding == null) return
        binding.swipeRefresh.isRefreshing = false
        binding.skeletonContainer.isVisible = false

        if (videos.isEmpty()) {
            showEmpty(
                getString(R.string.tutorials_empty_title),
                getString(R.string.tutorials_empty_message)
            )
            return
        }

        binding.emptyState.isVisible = false
        binding.swipeRefresh.isVisible = true
        adapter?.submitList(videos)
    }

    private fun showEmpty(title: String, message: String) {
        if (_binding == null) return
        binding.swipeRefresh.isRefreshing = false
        binding.skeletonContainer.isVisible = false
        // A failed refresh must not wipe a list that is already on screen.
        if (!adapter?.currentList.isNullOrEmpty()) {
            binding.swipeRefresh.isVisible = true
            return
        }
        binding.swipeRefresh.isVisible = false
        binding.emptyState.isVisible = true
        binding.emptyTitle.text = title
        binding.emptyMessage.text = message
    }

    // ── Playback ──────────────────────────────────────────────────────────────

    private fun openVideo(video: TutorialVideo, position: Int) {
        analyticsTracker.logTutorialOpened(video.id, video.title, position)
        // vnd.youtube: opens the installed app directly on the video. When YouTube is
        // not installed the intent throws, and the watch page handles it in a browser.
        val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:${video.id}"))
        try {
            startActivity(appIntent)
        } catch (e: ActivityNotFoundException) {
            openUrl(video.watchUrl)
        }
    }

    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: ActivityNotFoundException) {
            // No browser at all — nothing sensible left to do beyond not crashing.
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding?.tutorialsRV?.adapter = null
        adapter = null
        _binding = null
    }
}
