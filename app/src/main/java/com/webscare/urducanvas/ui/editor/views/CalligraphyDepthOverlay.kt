package com.webscare.urducanvas.ui.editor.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.webscare.urducanvas.R
import com.webscare.urducanvas.common.canvas.model.CanvasElement
import com.webscare.urducanvas.common.canvas.model.TextToken
import com.webscare.urducanvas.common.utils.Utils.addPressEffect
import com.webscare.urducanvas.databinding.ItemCalligraphyTokenCardBinding
import com.webscare.urducanvas.databinding.LayoutCalligraphyDepthOverlayBinding

class CalligraphyDepthOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding: LayoutCalligraphyDepthOverlayBinding =
        LayoutCalligraphyDepthOverlayBinding.inflate(LayoutInflater.from(context), this, true)

    private val tokenAdapter = TokenOverlayAdapter { token ->
        onTokenSelected?.invoke(token)
    }

    var onTokenSelected: ((TextToken) -> Unit)? = null
    var onKashidaClicked: (() -> Unit)? = null
    var onDotlessClicked: (() -> Unit)? = null
    var onCollapseClicked: (() -> Unit)? = null
    var onDoneClicked: (() -> Unit)? = null

    init {
        binding.rvOverlayTokens.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.rvOverlayTokens.adapter = tokenAdapter

        binding.btnOverlayKashida.addPressEffect {
            onKashidaClicked?.invoke()
        }

        binding.btnOverlayDotless.addPressEffect {
            onDotlessClicked?.invoke()
        }

        binding.btnOverlayCollapse.addPressEffect {
            onCollapseClicked?.invoke()
        }

        binding.btnOverlayDone.addPressEffect {
            onDoneClicked?.invoke()
        }
    }

    fun bind(element: CanvasElement) {
        val cData = element.calligraphyData
        if (cData == null || cData.tokens.isEmpty()) {
            visibility = View.GONE
            return
        }

        visibility = View.VISIBLE
        tokenAdapter.submitList(cData.tokens, cData.activeTokenId)

        val activeIdx = cData.tokens.indexOfFirst { it.id == cData.activeTokenId }
        if (activeIdx >= 0) {
            binding.rvOverlayTokens.post {
                binding.rvOverlayTokens.smoothScrollToPosition(activeIdx)
            }
        }
    }

    private class TokenOverlayAdapter(
        private val onTokenClicked: (TextToken) -> Unit
    ) : RecyclerView.Adapter<TokenOverlayAdapter.TokenViewHolder>() {

        private val tokens = mutableListOf<TextToken>()
        private var activeTokenId: String? = null

        fun submitList(newTokens: List<TextToken>, activeId: String?) {
            tokens.clear()
            tokens.addAll(newTokens)
            activeTokenId = activeId
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TokenViewHolder {
            val binding = ItemCalligraphyTokenCardBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return TokenViewHolder(binding)
        }

        override fun onBindViewHolder(holder: TokenViewHolder, position: Int) {
            holder.bind(tokens[position], tokens[position].id == activeTokenId)
        }

        override fun getItemCount(): Int = tokens.size

        inner class TokenViewHolder(private val b: ItemCalligraphyTokenCardBinding) :
            RecyclerView.ViewHolder(b.root) {

            fun bind(token: TextToken, isActive: Boolean) {
                val ctx = b.root.context
                b.tvTokenText.text = token.getFullDisplayText()

                if (isActive) {
                    b.tokenCardRoot.backgroundTintList = android.content.res.ColorStateList.valueOf(
                        ContextCompat.getColor(ctx, R.color.appColor)
                    )
                    b.tvTokenText.setTextColor(android.graphics.Color.WHITE)
                    b.tokenIndicator.visibility = View.VISIBLE
                } else {
                    b.tokenCardRoot.backgroundTintList = android.content.res.ColorStateList.valueOf(
                        ContextCompat.getColor(ctx, R.color.contrast)
                    )
                    b.tvTokenText.setTextColor(
                        ContextCompat.getColor(ctx, R.color.black)
                    )
                    b.tokenIndicator.visibility = View.GONE
                }

                b.tokenCardRoot.addPressEffect {
                    onTokenClicked(token)
                }
            }
        }
    }
}
