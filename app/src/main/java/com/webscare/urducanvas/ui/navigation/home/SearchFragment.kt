package com.webscare.urducanvas.ui.navigation.home

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Dialog
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.view.inputmethod.InputMethodManager
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.webscare.urducanvas.common.utils.Utils.addPressEffect
import com.webscare.urducanvas.common.utils.Utils.setupClearButton
import com.webscare.urducanvas.data.model.FontEntity
import com.webscare.urducanvas.data.model.orderWithUrduFirst
import com.webscare.urducanvas.data.model.TemplateEntity
import com.webscare.urducanvas.databinding.FragmentSearchBinding
import com.webscare.urducanvas.ui.navigation.files.FilesAdapter
import com.webscare.urducanvas.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import androidx.lifecycle.asFlow
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.webscare.urducanvas.R
import com.webscare.urducanvas.common.canvas.CanvasViewModel
import com.webscare.urducanvas.common.canvas.model.CanvasSize
import com.webscare.urducanvas.data.model.ProgressUi
import com.webscare.urducanvas.data.model.ExportResult
import com.webscare.urducanvas.data.model.ImageEntity
import com.webscare.urducanvas.data.model.toExportResultFinal
import com.webscare.urducanvas.databinding.DialogLoadingProgressBinding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.webscare.urducanvas.common.canvas.sealed.FontDownloadState
import com.webscare.urducanvas.common.canvas.sealed.TemplateDownloadState
import com.webscare.urducanvas.common.utils.showGlobalSuccessSnack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.webscare.urducanvas.common.utils.InsetUtils.applyStatusBarTopPadding

@AndroidEntryPoint
class SearchFragment : Fragment() {
    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private val mainViewModel: MainViewModel by activityViewModels()
    private val canvasViewModel: CanvasViewModel by activityViewModels()
    private lateinit var templatesAdapter: PopularTemplatesAdapter
    private lateinit var fontsAdapter: FontsAdapter
    private lateinit var filesAdapter: FilesAdapter
    private var loadingDialog: Dialog? = null
    private var dialogBinding: DialogLoadingProgressBinding? = null
    private var rotationAnimator: ObjectAnimator? = null

    val navOptions = NavOptions.Builder().setLaunchSingleTop(true).build()
    private var bundle: Bundle = Bundle()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Edge to edge: the window no longer reserves the status bar, so leave the margin here.
        view.applyStatusBarTopPadding()

        binding.searchBar.requestFocus()

        // Force keyboard to remain open
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        view.postDelayed({
            if (_binding == null) return@postDelayed
            imm.showSoftInput(binding.searchBar, InputMethodManager.SHOW_IMPLICIT)
        }, 150)

        setupAdapters()
        setupPopularKeywords()
        setupSearchBar()
        observeSearchResults()
    }


    private fun setupAdapters() {
        // Templates
        templatesAdapter = PopularTemplatesAdapter(onClick = { template, isDownloaded ->
            if (template.is_downloading) return@PopularTemplatesAdapter
            if (isDownloaded) {
                if (template.file_path.isNullOrEmpty()) {
                    templatesAdapter.updateProgress(
                        template.id,
                        ProgressUi(
                            progress = 0, isDownloading = true, isDownloaded = false
                        )
                    )
                    mainViewModel.downloadTemplate(template)
                    return@PopularTemplatesAdapter
                } else {
                    canvasViewModel.setProjectSourceName(template.category ?: template.subcategory)
                    val exportResult = template.toExportResultFinal().copy(fileName = canvasViewModel.buildProjectFileName())
                    canvasViewModel.loadTemplateFromJsonFile(exportResult, requireContext()) { success ->
                        if (success && isAdded) {
                            findNavController().navigate(R.id.editorFragment, bundle, navOptions)
                        }
                    }
                }
            } else {
                templatesAdapter.updateProgress(
                    template.id, ProgressUi(
                        progress = 0, isDownloading = true, isDownloaded = false
                    )
                )
                mainViewModel.downloadTemplate(template)
            }
        })

        binding.popularTemplateRV.apply {
            adapter = templatesAdapter
            layoutManager =
                com.webscare.urducanvas.common.views.SafeLinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        }

        // Fonts
        fontsAdapter = FontsAdapter(onFontClick = { font, isDownloaded ->
            if (!isDownloaded) {
                mainViewModel.downloadFont(font)
            } else {
                canvasViewModel.setCanvasSize(
                    CanvasSize(
                        id = 0,"", 2000f, 2000f
                    )
                )
                canvasViewModel.addTextWithFont(
                    requireActivity().getString(R.string.dummyText), font, requireActivity()
                )

                view?.post { findNavController().navigate(R.id.editorFragment, null, navOptions) }
            }
        }, onDownload = {
            mainViewModel.downloadFont(it)
        })

        binding.fontsRV.apply {
            adapter = fontsAdapter
            layoutManager =
                com.webscare.urducanvas.common.views.SafeLinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        }

        filesAdapter = FilesAdapter(
            emptyList(),
            isGrid = false,
            onItemClick = { openItem(it) },
            onItemLongClick = {},
            onOptionsClick = { _, _ -> },
            onRename = { _, _ -> },
            onSelectionChanged = {})
        binding.filesRV.apply {
            adapter = filesAdapter
            layoutManager = com.webscare.urducanvas.common.views.SafeLinearLayoutManager(requireContext())
        }
    }

    private fun openItem(item: Any) {
        when (item) {
            is ExportResult -> {
                canvasViewModel.loadTemplateFromJsonFile(item, requireContext()) { success ->
                    if (success && isAdded) {
                        findNavController().navigate(R.id.editorFragment, bundle, navOptions)
                    }
                }
            }

            is FontEntity -> {
                canvasViewModel.setCanvasSize(
                    CanvasSize(
                        id = 0,
                        "",
                        2000f,
                        2000f
                    )
                )
                canvasViewModel.addTextWithFont(
                    requireActivity().getString(R.string.dummyText),
                    item,
                    requireActivity()
                )
                view?.post {
                    findNavController().navigate(R.id.editorFragment, null, navOptions)
                }
            }

            is ImageEntity -> {
                val bitmap = BitmapFactory.decodeFile(item.bitmapData)

                bitmap?.let {
                    val widthVal = it.width.toFloat()
                    val heightVal = it.height.toFloat()

                    val canvasSize =
                        CanvasSize(
                            id = 0,
                            "From Image",
                            widthVal,
                            heightVal
                        )

                    canvasViewModel.clearCanvas()
                    canvasViewModel.setCanvasSize(canvasSize)
                    canvasViewModel.setCanvasBackgroundImage(it, requireActivity())
                    view?.post {
                        findNavController().navigate(R.id.editorFragment, null, navOptions)
                    }
                }
            }
        }
    }

    private val popularKeywords = listOf(
        "شاعری",
        "اقوال زریں",
        "اسلامک",
        "پوسٹر",
        "بزنس کارڈ",
        "یوٹیوب تھمب نیل",
        "عید مبارک",
        "جمعتہ المبارک",
        "شادی کارڈ",
        "Poetry",
        "Islamic",
        "Thumbnail",
        "Poster"
    )

    private fun setupPopularKeywords() {
        binding.keywordsChipGroup.removeAllViews()
        popularKeywords.forEach { keyword ->
            val chip = (layoutInflater.inflate(R.layout.chip_filter_item, binding.keywordsChipGroup, false) as? Chip)
                ?: Chip(requireContext())
            chip.text = keyword
            chip.isCheckable = true
            chip.isChecked = binding.searchBar.text?.toString()?.trim().equals(keyword, ignoreCase = true)
            chip.setOnClickListener {
                val current = binding.searchBar.text?.toString()?.trim().orEmpty()
                if (current.equals(keyword, ignoreCase = true)) {
                    binding.searchBar.text?.clear()
                    chip.isChecked = false
                } else {
                    binding.searchBar.setText(keyword)
                    binding.searchBar.setSelection(keyword.length)
                    chip.isChecked = true
                }
            }
            binding.keywordsChipGroup.addView(chip)
        }
    }

    private fun updateKeywordChipsSelection(currentQuery: String) {
        val trimmed = currentQuery.trim()
        for (i in 0 until binding.keywordsChipGroup.childCount) {
            val chip = binding.keywordsChipGroup.getChildAt(i) as? Chip ?: continue
            chip.isChecked = chip.text.toString().equals(trimmed, ignoreCase = true)
        }
    }

    private fun setupSearchBar() {
        binding.back.addPressEffect {
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(binding.searchBar.windowToken, 0)
            findNavController().navigateUp()
        }

        binding.searchBar.apply {
            maxLines = 1
            isSingleLine = true
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH or android.view.inputmethod.EditorInfo.IME_ACTION_DONE
            setOnEditorActionListener { v, actionId, _ ->
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH ||
                    actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                    val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.windowToken, 0)
                    v.clearFocus()
                    true
                } else false
            }
            setupClearButton {
                mainViewModel.setQuery("")
                updateKeywordChipsSelection("")
            }
        }

        binding.searchBar.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val q = s?.toString().orEmpty()
                mainViewModel.setQuery(q)
                updateKeywordChipsSelection(q)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun TemplateEntity.matchesSearch(q: String): Boolean {
        if (template_name.lowercase().contains(q)) return true
        if (category?.lowercase()?.contains(q) == true) return true
        if (subcategory?.lowercase()?.contains(q) == true) return true
        if (tags.any { it.lowercase().contains(q) }) return true
        return false
    }

    private fun FontEntity.matchesSearch(q: String): Boolean {
        if (font_name.lowercase().contains(q)) return true
        if (font_category.lowercase().contains(q)) return true
        if (font_language.lowercase().contains(q)) return true
        return false
    }

    private fun observeSearchResults() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    mainViewModel.localTemplates,
                    mainViewModel.localFonts,
                    mainViewModel.localImages,
                    mainViewModel.exportResults.asFlow(),
                    mainViewModel.queryDebounced.debounce(250).distinctUntilChanged()
                ) { templates, fonts, images, exports, query ->
                    val q = query.trim().lowercase()

                    if (q.isEmpty()) {
                        val suggestedTemplates = templates.filter { it.is_popular }.ifEmpty { templates }.take(10)
                        val suggestedFonts = fonts.filter { !it.font_category.equals("Imported", true) }
                            .orderWithUrduFirst()
                            .take(10)

                        SearchResults(
                            query = query,
                            templates = suggestedTemplates,
                            fonts = suggestedFonts,
                            files = emptyList()
                        )
                    } else {
                        val filteredTemplates = templates.filter { it.matchesSearch(q) }

                        val filteredFonts = fonts.filter { f ->
                            !f.font_category.equals("Imported", true) && f.matchesSearch(q)
                        }.orderWithUrduFirst()

                        val filteredFiles = exports.filter { e ->
                            e.fileName.lowercase().contains(q)
                        }

                        val filteredImages = images.filter { i ->
                            i.file_name.lowercase().contains(q)
                        }

                        SearchResults(
                            query = query,
                            templates = filteredTemplates,
                            fonts = filteredFonts,
                            files = filteredFiles + filteredImages
                        )
                    }
                }.collectLatest { result ->
                    updateUI(result)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.fontDownloadStates.collect { downloadState ->
                    downloadState.values.forEach { state ->
                        when (state) {
                            is FontDownloadState.Progress -> {
                                fontsAdapter.updateProgress(
                                    state.fontEntity.id,
                                    ProgressUi(
                                        progress = state.progress,
                                        isDownloading = true,
                                        isDownloaded = false
                                    )
                                )
                            }
                            is FontDownloadState.SuccessWithTypeface -> {
                                val font = state.fontEntity
                                fontsAdapter.updateProgress(
                                    font.id,
                                    ProgressUi(100, isDownloading = false, isDownloaded = true)
                                )
                                mainViewModel.clearFontDownloadState(font.id.toString())
                                showGlobalSuccessSnack("Font downloaded") {
                                    canvasViewModel.setCanvasSize(
                                        CanvasSize(id = 0, "", 2000f, 2000f)
                                    )
                                    canvasViewModel.addTextWithFont(
                                        requireActivity().getString(R.string.dummyText),
                                        font,
                                        requireActivity()
                                    )
                                    if (isAdded && findNavController().currentDestination?.id != R.id.editorFragment) {
                                        view?.post {
                                            findNavController().navigate(R.id.editorFragment, null, navOptions)
                                        }
                                    }
                                }
                            }
                            is FontDownloadState.Error -> {
                                fontsAdapter.updateProgress(
                                    state.fontEntity.id,
                                    ProgressUi(0, isDownloading = false, isDownloaded = false)
                                )
                                mainViewModel.clearFontDownloadState(state.fontEntity.id.toString())
                            }
                            else -> {}
                        }
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.templateDownloadStates.collect { downloadState ->
                    downloadState.values.forEach { state ->
                        when (state) {
                            is TemplateDownloadState.Progress -> {
                                templatesAdapter.updateProgress(
                                    state.template.id,
                                    ProgressUi(state.progress, isDownloading = true, isDownloaded = false)
                                )
                            }
                            is TemplateDownloadState.SuccessWithTemplate -> {
                                val t = state.template
                                templatesAdapter.updateProgress(
                                    t.id,
                                    ProgressUi(100, isDownloading = false, isDownloaded = true)
                                )
                                mainViewModel.clearTemplateDownloadState()
                                showGlobalSuccessSnack("Template ready") {
                                    canvasViewModel.setProjectSourceName(t.category ?: t.subcategory)
                                    val exportResult = t.toExportResultFinal().copy(fileName = canvasViewModel.buildProjectFileName())
                                    canvasViewModel.loadTemplateFromJsonFile(exportResult, requireContext()) { success ->
                                        if (success && isAdded && findNavController().currentDestination?.id != R.id.editorFragment) {
                                            findNavController().navigate(R.id.editorFragment, bundle, navOptions)
                                        }
                                    }
                                }
                            }
                            is TemplateDownloadState.Error -> {
                                mainViewModel.clearTemplateDownloadState()
                            }
                            else -> {}
                        }
                    }
                }
            }
        }

        canvasViewModel.isLoadingTemplate.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading == true) {
                showLoadingDialog()
            } else if (isLoading == false) {
                dismissLoadingDialog()
            }
        }

        canvasViewModel.loadingStage.observe(viewLifecycleOwner) { (message, percent) ->
            dialogBinding?.apply {
                progressBar.progress = percent
                subtitle.text = "$message... $percent%"
                tvProgressPercent.text = "$percent% complete"
            }
        }
    }

    private fun showLoadingDialog() {
        if (loadingDialog?.isShowing == true) return
        dialogBinding = DialogLoadingProgressBinding.inflate(LayoutInflater.from(requireActivity()))

        loadingDialog = Dialog(requireContext()).apply {
            setContentView(dialogBinding!!.root)
            setCancelable(true)
            setOnCancelListener { dialog ->
                canvasViewModel.clearLoading()
            }
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            val params = window?.attributes
            params?.width = (resources.displayMetrics.widthPixels * 0.8).toInt() // 80% width
            params?.height = ViewGroup.LayoutParams.WRAP_CONTENT
            window?.attributes = params
            window?.setGravity(Gravity.CENTER)
            show()
        }

        dialogBinding?.title?.text = "Loading Project"

        startIconRotation()
    }

    private fun startIconRotation() {
        dialogBinding?.view4?.let { icon ->
            rotationAnimator = ObjectAnimator.ofFloat(icon, View.ROTATION, 0f, 360f).apply {
                duration = 1000L
                repeatCount = ValueAnimator.INFINITE
                interpolator = LinearInterpolator()
                start()
            }
        }
    }

    private fun stopIconRotation() {
        rotationAnimator?.cancel()
        rotationAnimator = null
    }

    private fun dismissLoadingDialog() {
        stopIconRotation()
        loadingDialog?.dismiss()
        loadingDialog = null
        dialogBinding = null
    }

    private fun updateUI(result: SearchResults) {
        val isBlankQuery = result.query.isBlank()

        // Templates
        templatesAdapter.submitList(result.templates)
        binding.popularTemplate.isVisible = result.templates.isNotEmpty()
        binding.popularTemplateRV.isVisible = result.templates.isNotEmpty()

        // Fonts
        fontsAdapter.submitList(result.fonts)
        binding.popularFonts.isVisible = result.fonts.isNotEmpty()
        binding.fontsRV.isVisible = result.fonts.isNotEmpty()

        // Files
        filesAdapter.updateList(result.files)
        binding.assets.isVisible = result.files.isNotEmpty()
        binding.filesRV.isVisible = result.files.isNotEmpty()

        // Empty state
        val noResults = !isBlankQuery &&
            result.templates.isEmpty() && result.fonts.isEmpty() && result.files.isEmpty()
        binding.noEmojis.isVisible = noResults
        if (noResults) {
            binding.noImagesText.text = getString(R.string.no_search_results_for, result.query.trim())
        }
    }

    data class SearchResults(
        val query: String = "",
        val templates: List<TemplateEntity>,
        val fonts: List<FontEntity>,
        val files: List<Any>
    )

    override fun onDestroyView() {
        _binding?.popularTemplateRV?.adapter = null
        _binding?.fontsRV?.adapter = null
        _binding?.filesRV?.adapter = null
        super.onDestroyView()
        _binding = null
    }
}