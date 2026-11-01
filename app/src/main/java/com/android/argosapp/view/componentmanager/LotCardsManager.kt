package com.android.argosapp.view.componentmanager

import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.PathInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.doAfterTextChanged
import com.android.argosapp.R
import com.android.argosapp.model.Lot
import com.google.android.material.card.MaterialCardView

class LotCardsManager(
    private val root: View,
    private val onSearchChanged: () -> Unit,
    private val onNavigate: (Int) -> Unit,
    private val onSearch: () -> Unit,
    private val onSuggestionSelected: (Lot) -> Unit,
) {
    private val heroCard = root.findViewById<MaterialCardView>(R.id.lotsHeroCard)
    private val nextCard = root.findViewById<MaterialCardView>(R.id.lotsNextCard)
    private val nextCardColor = nextCard.cardBackgroundColor.defaultColor
    private val incomingCard = root.findViewById<View>(R.id.lotsIncomingCard)
    private val title = root.findViewById<TextView>(R.id.lotsTitle)
    private val nextTitle = root.findViewById<TextView>(R.id.lotsNextTitle)
    private val searchInput = root.findViewById<EditText>(R.id.lotsSearchInput)
    private val nextSearchText = root.findViewById<TextView>(R.id.lotsNextSearchText)
    private val searchButton = root.findViewById<View>(R.id.lotsSearchButton)
    private val previous = root.findViewById<View>(R.id.lotsPrevious)
    private val next = root.findViewById<View>(R.id.lotsNext)
    private val emptyHeader = root.findViewById<View>(R.id.lotsEmptyHeader)
    private val populatedHeader = root.findViewById<View>(R.id.lotsPopulatedHeader)
    private var animating = false
    private var colorAnimator: ValueAnimator? = null
    private var disposed = false
    private var applyingSuggestion = false
    private var suggestions = emptyList<Lot>()
    private val suggestionList = ListView(root.context).apply {
        divider = ColorDrawable(root.context.getColor(R.color.argos_border))
        dividerHeight = dp(1)
        setPadding(0, dp(8), 0, dp(8))
        clipToPadding = false
    }
    private val suggestionAdapter = object : ArrayAdapter<String>(root.context, android.R.layout.simple_list_item_1) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
            (super.getView(position, convertView, parent) as TextView).apply {
                typeface = ResourcesCompat.getFont(context, R.font.dm_sans)
                textSize = 14f
                setTextColor(context.getColor(R.color.argos_text_primary))
                setPadding(dp(16), dp(12), dp(16), dp(12))
                minHeight = dp(48)
            }
    }
    private val suggestionPopup = PopupWindow(
        suggestionList,
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT,
        false,
    ).apply {
        setBackgroundDrawable(GradientDrawable().apply {
            setColor(Color.WHITE)
            cornerRadius = dp(16).toFloat()
        })
        elevation = dp(8).toFloat()
        isOutsideTouchable = true
        setTouchInterceptor { _, event ->
            if (event.action == MotionEvent.ACTION_OUTSIDE) {
                searchInput.clearFocus()
                root.context.getSystemService(InputMethodManager::class.java)
                    .hideSoftInputFromWindow(searchInput.windowToken, 0)
            }
            false
        }
        inputMethodMode = PopupWindow.INPUT_METHOD_NEEDED
    }

    val query: String get() = searchInput.text.toString()

    private val searchWatcher = searchInput.doAfterTextChanged {
        syncSearchPreview()
        if (applyingSuggestion) return@doAfterTextChanged
        cancelAnimation()
        onSearchChanged()
    }

    init {
        suggestionList.adapter = suggestionAdapter
        suggestionList.setOnItemClickListener { _, _, position, _ ->
            suggestions.getOrNull(position)?.let(::selectSuggestion)
        }
        searchInput.textCursorDrawable = GradientDrawable().apply {
            setColor(android.graphics.Color.WHITE)
            setSize(dp(2), dp(20))
        }
        searchInput.setOnFocusChangeListener { _, hasFocus ->
            searchInput.hint = if (hasFocus) null else root.context.getString(R.string.lots_search_hint)
            syncSearchPreview()
            if (hasFocus) updateSuggestionPopup() else suggestionPopup.dismiss()
        }
        searchInput.setOnClickListener { updateSuggestionPopup() }
        searchButton.setOnClickListener {
            suggestionPopup.dismiss()
            onSearch()
        }
        previous.setOnClickListener { onNavigate(-1) }
        next.setOnClickListener { onNavigate(1) }
    }

    fun render(hasLots: Boolean, selectedName: String?) {
        title.text = selectedName.orEmpty()
        emptyHeader.visibility = if (hasLots) View.GONE else View.VISIBLE
        populatedHeader.visibility = if (hasLots) View.VISIBLE else View.GONE
        searchButton.visibility = if (hasLots) View.VISIBLE else View.GONE
        previous.visibility = if (selectedName != null) View.VISIBLE else View.GONE
        next.visibility = if (selectedName != null) View.VISIBLE else View.GONE
    }

    fun showSuggestions(items: List<Lot>) {
        if (suggestions != items) {
            suggestionAdapter.clear()
            suggestionAdapter.addAll(items.map { it.lotName })
        }
        suggestions = items
        updateSuggestionPopup()
    }

    private fun syncSearchPreview() {
        nextSearchText.text = query.ifEmpty {
            if (searchInput.hasFocus()) "" else root.context.getString(R.string.lots_search_hint)
        }
    }

    private fun updateSuggestionPopup() {
        if (disposed || !searchInput.isAttachedToWindow || !searchInput.hasFocus() ||
            suggestions.isEmpty() || searchInput.width == 0
        ) {
            suggestionPopup.dismiss()
            return
        }
        suggestionPopup.width = searchInput.width + dp(36)
        suggestionPopup.height = minOf(dp(288), dp(suggestions.size * 48 + 16), root.resources.displayMetrics.heightPixels / 2)
        if (suggestionPopup.isShowing) suggestionPopup.update(suggestionPopup.width, suggestionPopup.height)
        else suggestionPopup.showAsDropDown(searchInput, 0, dp(4))
    }

    private fun selectSuggestion(lot: Lot) {
        suggestionPopup.dismiss()
        applyingSuggestion = true
        searchInput.setText(lot.lotName)
        searchInput.setSelection(searchInput.length())
        applyingSuggestion = false
        searchInput.clearFocus()
        root.context.getSystemService(InputMethodManager::class.java)
            .hideSoftInputFromWindow(searchInput.windowToken, 0)
        cancelAnimation()
        onSearchChanged()
        onSuggestionSelected(lot)
    }

    private fun dp(value: Int): Int = (value * root.resources.displayMetrics.density + 0.5f).toInt()

    fun animateTo(name: String, onComplete: () -> Unit) {
        if (animating) return
        animating = true
        nextTitle.text = name
        incomingCard.translationX = -root.rootView.width.toFloat()
        val rise = 9f * root.resources.displayMetrics.density
        colorAnimator = ValueAnimator.ofArgb(nextCardColor, heroCard.cardBackgroundColor.defaultColor).apply {
            duration = 360L
            interpolator = PathInterpolator(0.22f, 1f, 0.36f, 1f)
            addUpdateListener { nextCard.setCardBackgroundColor(it.animatedValue as Int) }
            start()
        }
        incomingCard.animate().translationX(0f).setDuration(360L)
            .setInterpolator(PathInterpolator(0.22f, 1f, 0.36f, 1f)).start()
        nextCard.animate().translationX(rise).setDuration(360L)
            .setInterpolator(PathInterpolator(0.22f, 1f, 0.36f, 1f)).start()
        heroCard.animate().translationX(root.rootView.width.toFloat()).setDuration(360L).withEndAction {
            if (disposed) return@withEndAction
            heroCard.translationX = 0f
            nextCard.translationX = 0f
            colorAnimator?.cancel()
            colorAnimator = null
            nextCard.setCardBackgroundColor(nextCardColor)
            animating = false
            onComplete()
        }.start()
    }

    fun dispose() {
        disposed = true
        suggestionPopup.dismiss()
        cancelAnimation()
        searchInput.removeTextChangedListener(searchWatcher)
        searchInput.onFocusChangeListener = null
        searchInput.setOnClickListener(null)
        searchButton.setOnClickListener(null)
        previous.setOnClickListener(null)
        next.setOnClickListener(null)
    }

    private fun cancelAnimation() {
        colorAnimator?.cancel()
        colorAnimator = null
        heroCard.animate().cancel()
        nextCard.animate().cancel()
        incomingCard.animate().cancel()
        heroCard.translationX = 0f
        nextCard.translationX = 0f
        incomingCard.translationX = 0f
        nextCard.setCardBackgroundColor(nextCardColor)
        animating = false
    }
}
