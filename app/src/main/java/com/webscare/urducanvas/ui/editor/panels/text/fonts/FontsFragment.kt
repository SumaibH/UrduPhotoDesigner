package com.webscare.urducanvas.ui.editor.panels.text.fonts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.widget.ViewPager2
import com.webscare.urducanvas.common.canvas.enums.PanelType
import com.webscare.urducanvas.data.model.FontCategory
import com.webscare.urducanvas.data.model.FontLanguages
import com.webscare.urducanvas.R
import com.webscare.urducanvas.databinding.FragmentFontsBinding
import com.webscare.urducanvas.ui.editor.panels.preview.PanelPreviewHost
import com.webscare.urducanvas.ui.editor.panels.preview.PreviewHostOwner
import com.webscare.urducanvas.ui.editor.views.RailCategoryItem
import com.webscare.urducanvas.ui.editor.views.RailSubCategoryItem
import com.webscare.urducanvas.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FontsFragment : Fragment(), PreviewHostOwner {

    private var _binding: FragmentFontsBinding? = null

    override var previewHost: PanelPreviewHost? = null
        private set

    private val binding get() = _binding!!

    private val mainViewModel: MainViewModel by activityViewModels()

    private lateinit var pagerAdapter: FontsPagerAdapter

    private var standaloneMode: Boolean = false
    private var searchScope: String = com.webscare.urducanvas.viewmodels.SearchScope.TEXT_FONT
    private var selectedLanguage: String = "All"
    private var selectedCategory: String? = null

    private var pendingCategory: String? = null
    private var pendingLanguage: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        standaloneMode = arguments?.getBoolean(ARG_STANDALONE_MODE, false) ?: false
        searchScope = arguments?.getString(ARG_SEARCH_SCOPE)
            ?: com.webscare.urducanvas.viewmodels.SearchScope.TEXT_FONT
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFontsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // The rail is how you get between shelves, so the preview covers the grid
        // beside it rather than the whole panel.
        previewHost = PanelPreviewHost(this)
        setupRailView()
        setupViewPager()
        observeLocalFontsForLanguages()
        observeFontImported()
    }

    private fun setupRailView() {
        binding.collapsibleRail.bindPanelId("text_fonts")

        binding.collapsibleRail.onCategorySelectedListener = { catItem ->
            val position = pagerAdapter.categories.indexOfFirst { it.id.toString() == catItem.id }
            if (position >= 0) {
                val lang = pagerAdapter.categories[position]
                selectedLanguage = lang.name
                selectedCategory = catItem.selectedSubItemId
                binding.viewPager.setCurrentItem(position, false)
                val catFilter = if (selectedCategory.equals("All", ignoreCase = true)) null else selectedCategory
                applyFilterToPage(position, selectedLanguage, catFilter)
            }
        }

        binding.collapsibleRail.onSubCategorySelectedListener = { parentItem, subItem ->
            val position = pagerAdapter.categories.indexOfFirst { it.id.toString() == parentItem.id }
            if (position >= 0) {
                val lang = pagerAdapter.categories[position]
                selectedLanguage = lang.name
                selectedCategory = subItem.id
                if (binding.viewPager.currentItem != position) {
                    binding.viewPager.setCurrentItem(position, false)
                }
                val catFilter = if (subItem.id.equals("All", ignoreCase = true)) null else subItem.id
                applyFilterToPage(position, selectedLanguage, catFilter)
            }
        }
    }

    private fun setupViewPager() {
        pagerAdapter = FontsPagerAdapter(this, emptyList(), standaloneMode, searchScope)
        binding.viewPager.adapter = pagerAdapter
        binding.viewPager.isUserInputEnabled = false

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                val currentCategories = pagerAdapter.categories
                val currentLang = currentCategories.getOrNull(position)
                if (currentLang != null) {
                    selectedLanguage = currentLang.name
                    binding.collapsibleRail.setSelectedCategory(currentLang.id.toString())
                }

                val lang = pendingLanguage
                val cat  = pendingCategory
                if (lang != null) {
                    applyFilterToPage(position, lang, cat)
                    pendingLanguage = null
                    pendingCategory = null
                }
            }
        })
    }

    private fun applyFilterToPage(position: Int, language: String, category: String?) {
        val itemId   = pagerAdapter.categories.getOrNull(position)?.id?.toLong() ?: return
        val tag      = "f$itemId"
        val fragment = childFragmentManager.findFragmentByTag(tag) as? FontsListFragment

        if (fragment != null) {
            fragment.applyFilter(language, category)
        } else {
            binding.viewPager.post {
                if (_binding == null) return@post
                val f = childFragmentManager.findFragmentByTag("f$itemId") as? FontsListFragment
                f?.applyFilter(language, category)
            }
        }
    }

    private var pendingSelectImported = false

    private fun observeFontImported() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.fontImportedEvent.collect {
                    selectImportedCategory()
                }
            }
        }
    }

    private fun selectImportedCategory() {
        val categories = pagerAdapter.categories
        val position = categories.indexOfFirst { it.name.equals("Imported", ignoreCase = true) }
        if (position >= 0) {
            selectedLanguage = "Imported"
            selectedCategory = null
            binding.viewPager.setCurrentItem(position, false)
            val langId = categories[position].id.toString()
            binding.collapsibleRail.setSelectedCategory(langId)
        } else {
            pendingSelectImported = true
        }
    }

    private fun observeLocalFontsForLanguages() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                kotlinx.coroutines.flow.combine(
                    mainViewModel.localFonts,
                    mainViewModel.recentFonts
                ) { fonts, _ -> fonts }
                    .collect { fonts ->
                        val languages = buildFontLanguages(fonts)
                        pagerAdapter.updateCategories(languages)

                        val railItems = languages.map { lang ->
                            val subItems = lang.categories.map { cat ->
                                RailSubCategoryItem(
                                    id = cat.name,
                                    label = cat.name
                                )
                            }
                            val isLangSelected = lang.name.equals(selectedLanguage, ignoreCase = true)
                            RailCategoryItem(
                                id = lang.id.toString(),
                                label = lang.name,
                                iconRes = null,
                                subItems = subItems,
                                isSubListExpanded = (isLangSelected && subItems.isNotEmpty()),
                                selectedSubItemId = if (isLangSelected) (selectedCategory ?: "All") else null,
                                hasSubList = subItems.isNotEmpty()
                            )
                        }
                        val selectedLang = languages.firstOrNull { it.name.equals(selectedLanguage, ignoreCase = true) }
                            ?: languages.firstOrNull { it.is_selected }
                            ?: languages.firstOrNull()
                        binding.collapsibleRail.setCategories(railItems, selectedLang?.id?.toString())

                        if (pendingSelectImported) {
                            val pos = languages.indexOfFirst { it.name.equals("Imported", ignoreCase = true) }
                            if (pos >= 0) {
                                pendingSelectImported = false
                                selectedLanguage = "Imported"
                                selectedCategory = null
                                binding.viewPager.setCurrentItem(pos, false)
                            }
                        }
                    }
            }
        }
    }

    private fun buildFontLanguages(
        fonts: List<com.webscare.urducanvas.data.model.FontEntity>
    ): List<FontLanguages> {
        val byLanguage = fonts
            .filter { it.font_language.isNotBlank() }
            .groupBy { it.font_language.trim() }

        val result = mutableListOf<FontLanguages>()

        result.add(
            FontLanguages(
                id         = 0,
                name       = "All",
                categories = emptyList(),
                is_selected = selectedLanguage == "All"
            )
        )

        val recentFonts = mainViewModel.recentFonts.value
        if (recentFonts.isNotEmpty()) {
            result.add(
                FontLanguages(
                    id          = -1,
                    name        = "Recents",
                    categories  = emptyList(),
                    is_selected = selectedLanguage == "Recents"
                )
            )
        }

        val preferredOrder  = listOf("Urdu", "English")
        val allLangs        = byLanguage.keys.filter { !it.equals("Imported", ignoreCase = true) }
        val preferred       = preferredOrder.filter { p -> allLangs.any { it.equals(p, ignoreCase = true) } }
        val rest            = allLangs.filter { l -> preferredOrder.none { it.equals(l, ignoreCase = true) } }.sorted()
        val orderedLanguages = preferred + rest

        orderedLanguages.forEachIndexed { index, langName ->
            val fontsForLang = byLanguage[langName] ?: return@forEachIndexed
            val catNames = fontsForLang
                .map { it.font_category.trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()

            val categories = if (catNames.isNotEmpty()) {
                (listOf("All") + catNames).mapIndexed { catIndex, catName ->
                    FontCategory(id = catIndex, name = catName, isSelected = catIndex == 0)
                }
            } else {
                emptyList()
            }

            result.add(
                FontLanguages(
                    id          = index + 1,
                    name        = langName,
                    categories  = categories,
                    is_selected = selectedLanguage == langName
                )
            )
        }

        if (byLanguage.keys.any { it.equals("Imported", ignoreCase = true) }) {
            result.add(
                FontLanguages(
                    id          = result.size,
                    name        = "Imported",
                    categories  = emptyList(),
                    is_selected = selectedLanguage == "Imported"
                )
            )
        }

        return result
    }

    override fun onDestroyView() {
        previewHost?.release()
        previewHost = null
        _binding?.viewPager?.adapter = null
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_STANDALONE_MODE = "standalone_mode"
        private const val ARG_SEARCH_SCOPE = "search_scope"

        /**
         * [searchScope] says whose query this font list filters on. The same fragment backs
         * both the text panel's Font tab and the table panel's, and they are two different
         * lists on screen at two different times — a term typed into one must not follow the
         * user into the other, which is what a single shared query used to do.
         */
        fun newInstance(
            standaloneMode: Boolean = false,
            searchScope: String = com.webscare.urducanvas.viewmodels.SearchScope.TEXT_FONT
        ): FontsFragment {
            return FontsFragment().also {
                it.arguments = Bundle().apply {
                    putBoolean(ARG_STANDALONE_MODE, standaloneMode)
                    putString(ARG_SEARCH_SCOPE, searchScope)
                }
            }
        }
    }
}