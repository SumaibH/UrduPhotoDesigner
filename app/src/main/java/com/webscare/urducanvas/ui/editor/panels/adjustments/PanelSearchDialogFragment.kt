package com.webscare.urducanvas.ui.editor.panels.adjustments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.webscare.urducanvas.common.utils.Utils.addPressEffect
import com.webscare.urducanvas.databinding.DialogPanelSearchBinding
import com.webscare.urducanvas.viewmodels.MainViewModel

@dagger.hilt.android.AndroidEntryPoint
class PanelSearchDialogFragment : DialogFragment() {

    @javax.inject.Inject
    lateinit var analyticsTracker: com.webscare.urducanvas.analytics.AnalyticsTracker

    private var _binding: DialogPanelSearchBinding? = null
    private val binding get() = _binding!!

    private val mainViewModel: MainViewModel by activityViewModels()

    /** Which panel opened this. See [newInstance]. */
    private var placement: String = "panel"

    /** Keeps one search to one event, the way SearchFragment's own guard does. */
    private var lastReportedQuery: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, android.R.style.Theme_DeviceDefault_Dialog_NoActionBar_MinWidth)
        placement = arguments?.getString(ARG_PLACEMENT) ?: "panel"
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setDimAmount(0f)
            setGravity(android.view.Gravity.BOTTOM)
            setLayout(
                android.view.WindowManager.LayoutParams.MATCH_PARENT,
                android.view.WindowManager.LayoutParams.WRAP_CONTENT
            )
            setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = DialogPanelSearchBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Whatever a previous panel's list counted belongs to that panel's search, not this
        // one. Dropping it here is what makes an uncounted panel — the tables one, which has
        // no list that filters — report "unknown" instead of a stale number.
        mainViewModel.beginPanelSearch()

        val currentQuery = mainViewModel.searchQuery.value
        binding.editSearchInput.setText(currentQuery)
        binding.editSearchInput.setSelection(currentQuery.length)
        binding.btnClearSearch.isVisible = currentQuery.isNotEmpty()

        binding.editSearchInput.requestFocus()

        binding.editSearchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString().orEmpty()
                binding.btnClearSearch.isVisible = query.isNotEmpty()
                mainViewModel.setQuery(query)
            }
        })

        binding.editSearchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                reportSearch()
                dismiss()
                true
            } else false
        }

        binding.btnClearSearch.addPressEffect {
            binding.editSearchInput.setText("")
            mainViewModel.setQuery("")
        }

        binding.btnDone.addPressEffect {
            reportSearch()
            dismiss()
        }
    }

    /**
     * Reports the query the user settled on, not every keystroke.
     *
     * The panels filter live, so there is no submit to hang this off — leaving the dialog is
     * the commit, and both ways out come through here. In-editor asset search emitted nothing
     * at all before this: `search` fired only from the home screen, so the searches that
     * happen while someone is actually building a design — the ones that say which assets the
     * catalogue is missing — were the half we never saw.
     *
     * The result count comes from whichever list is filtering, because this dialog is shared
     * by three panels and sees no list of its own — and it is asked for *by query*, so a
     * count belonging to some other search cannot be read as this one's. The tables panel
     * has no list that filters, so its searches honestly report -1. Sanitised the same way
     * the home call site sanitises: a free-text box is where an email or a phone number ends
     * up, and GA4 rejects events carrying them anyway.
     */
    private fun reportSearch() {
        val term = mainViewModel.searchQuery.value.trim()
        if (term.isBlank() || term == lastReportedQuery) return
        lastReportedQuery = term
        if (PERSONAL_DATA.containsMatchIn(term)) return

        analyticsTracker.logSearch(
            term = term.take(100),
            resultCount = mainViewModel.searchResultCountFor(term),
            placement = placement
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_PLACEMENT = "arg_placement"

        /** Same shape as SearchFragment's guard — see its companion for why. */
        private val PERSONAL_DATA = Regex(
            "[\\w.+-]+@[\\w-]+\\.[\\w.]+" +   // anything email-shaped
            "|\\+?\\d[\\d\\s().-]{7,}"        // or a run of digits long enough to be a number
        )

        /**
         * [placement] names the panel that opened this, following the convention the home
         * screen's own `search` call set: a short, stable screen-or-panel name.
         */
        fun newInstance(placement: String = "panel") = PanelSearchDialogFragment().apply {
            arguments = Bundle().apply { putString(ARG_PLACEMENT, placement) }
        }
    }
}
