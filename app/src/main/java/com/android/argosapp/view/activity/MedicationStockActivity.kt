package com.android.argosapp.view.activity

import android.os.Bundle
import android.view.animation.PathInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.FragmentActivity
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.argosapp.R
import com.android.argosapp.databinding.ActivityMedicationStockBinding
import com.android.argosapp.model.MedicationStockItem
import com.android.argosapp.view.adapter.MedicationStockAdapter
import com.android.argosapp.view.fragment.MedicationStockRegistrationBottomSheet
import com.android.argosapp.viewmodel.MedicationStockViewModel

class MedicationStockActivity : FragmentActivity() {
    private lateinit var binding: ActivityMedicationStockBinding
    private val viewModel: MedicationStockViewModel by viewModels()
    private val adapter = MedicationStockAdapter()
    private var items = emptyList<MedicationStockItem>()
    private var category = StockCategoryFilter.ALL
    private var loading = false
    private var refreshing = false
    private var hasMore = false
    private var failed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMedicationStockBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applySystemBarInsets()

        binding.backButton.setOnClickListener { finish() }
        binding.stockRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.stockRecyclerView.adapter = adapter
        binding.stockRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(view: RecyclerView, dx: Int, dy: Int) {
                if (dy >= 0) loadNextPageIfNeeded()
            }
        })
        binding.searchInput.doAfterTextChanged { renderItems() }
        binding.searchButton.setOnClickListener {
            binding.searchInput.clearFocus()
            (getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager)
                ?.hideSoftInputFromWindow(binding.searchInput.windowToken, 0)
            renderItems()
        }
        binding.stockSwipeRefresh.setColorSchemeResources(R.color.argos_green)
        binding.stockSwipeRefresh.setOnRefreshListener { refresh() }
        binding.filterAllButton.setOnClickListener { selectFilter(binding.filterAllButton, StockCategoryFilter.ALL, true) }
        binding.filterVaccinesButton.setOnClickListener { selectFilter(binding.filterVaccinesButton, StockCategoryFilter.VACCINE, true) }
        binding.filterMedicationsButton.setOnClickListener { selectFilter(binding.filterMedicationsButton, StockCategoryFilter.MEDICATION, true) }
        selectFilter(binding.filterAllButton, StockCategoryFilter.ALL, false)
        binding.addStockButton.setOnClickListener {
            if (supportFragmentManager.findFragmentByTag(MedicationStockRegistrationBottomSheet.TAG) == null) {
                MedicationStockRegistrationBottomSheet().show(supportFragmentManager, MedicationStockRegistrationBottomSheet.TAG)
            }
        }
        supportFragmentManager.setFragmentResultListener(
            MedicationStockRegistrationBottomSheet.REGISTERED_RESULT,
            this,
        ) { _, _ -> refresh() }
        render()
    }

    private fun render() {
        viewModel.render(
            onLoading = { loading = it; renderState() },
            onRefreshing = { refreshing = it; renderState() },
            onLoaded = { data, more ->
                items = data
                hasMore = more
                failed = false
                renderItems()
            },
            onError = { _, hadData ->
                failed = !hadData
                if (hadData) Toast.makeText(this, R.string.refresh_failed, Toast.LENGTH_SHORT).show()
                renderState()
            },
        )
    }

    private fun refresh() {
        viewModel.render(
            forceRefresh = true,
            onLoading = { loading = it; renderState() },
            onRefreshing = { refreshing = it; renderState() },
            onLoaded = { data, more -> items = data; hasMore = more; failed = false; renderItems() },
            onError = { _, hadData ->
                failed = !hadData
                if (hadData) Toast.makeText(this, R.string.refresh_failed, Toast.LENGTH_SHORT).show()
                renderState()
            },
        )
    }

    private fun loadNextPageIfNeeded() {
        val layout = binding.stockRecyclerView.layoutManager as LinearLayoutManager
        if (hasMore && !loading && !refreshing && !failed && adapter.itemCount > 0 &&
            layout.findLastVisibleItemPosition() >= adapter.itemCount - 2
        ) loadNextPage()
    }

    private fun loadNextPage() {
        if (!hasMore || loading || refreshing) return
        viewModel.loadNextPage(
            onLoading = { loading = it; renderState() },
            onLoaded = { page, more ->
                items = (items + page).distinctBy { it.id }
                hasMore = more
                failed = false
                renderItems()
            },
            onError = {
                failed = true
                Toast.makeText(this, R.string.stock_error, Toast.LENGTH_SHORT).show()
                renderState()
            },
        )
    }

    private fun selectFilter(target: TextView, selected: StockCategoryFilter, animate: Boolean) {
        category = selected
        listOf(binding.filterAllButton, binding.filterVaccinesButton, binding.filterMedicationsButton).forEach { tab ->
            tab.isSelected = tab == target
            tab.setTextColor(
                ContextCompat.getColor(
                    this,
                    if (tab == target) R.color.argos_background else R.color.argos_text_primary,
                ),
            )
        }
        binding.filterContainer.post {
            if (target.width == 0) return@post
            val indicator = binding.filterIndicator
            if (indicator.layoutParams.width != target.width) {
                indicator.layoutParams = indicator.layoutParams.apply { width = target.width }
            }
            val targetTranslation = target.left + target.width / 2f - binding.filterContainer.width / 2f
            indicator.animate().cancel()
            if (animate) {
                indicator.animate()
                    .translationX(targetTranslation)
                    .setDuration(320L)
                    .setInterpolator(PathInterpolator(0.22f, 1f, 0.36f, 1f))
                    .start()
            } else {
                indicator.translationX = targetTranslation
            }
        }
        renderItems()
    }

    private fun renderItems() {
        val query = binding.searchInput.text?.toString()?.trim().orEmpty()
        val filtered = items.filter { item ->
            val matchesQuery = query.isBlank() || listOfNotNull(item.medicineName, item.batch, item.activeIngredient)
                .any { it.contains(query, ignoreCase = true) }
            val isVaccine = item.therapeuticCategory.orEmpty().contains("vacina", ignoreCase = true)
            val matchesCategory = when (category) {
                StockCategoryFilter.ALL -> true
                StockCategoryFilter.VACCINE -> isVaccine
                StockCategoryFilter.MEDICATION -> !isVaccine
            }
            matchesQuery && matchesCategory
        }
        adapter.submitList(filtered)
        renderState(filtered.isEmpty())
        binding.stockRecyclerView.post {
            if (filtered.isEmpty() && hasMore && !loading && !refreshing && !failed) loadNextPage()
            else loadNextPageIfNeeded()
        }
    }

    private fun renderState(noFilteredItems: Boolean = false) {
        binding.loadingIndicator.isVisible = loading && items.isEmpty()
        binding.paginationIndicator.isVisible = loading && items.isNotEmpty()
        val showMessage = !loading && (failed || noFilteredItems || items.isEmpty())
        binding.stateMessage.isVisible = showMessage
        if (showMessage) {
            binding.stateMessage.setText(if (failed) R.string.stock_error else R.string.stock_empty)
            binding.stateMessage.setTextColor(
                ContextCompat.getColor(this, if (failed) R.color.argos_notice_important else R.color.argos_text_secondary),
            )
        }
        binding.stockSwipeRefresh.isRefreshing = refreshing
    }

    private fun applySystemBarInsets() {
        val root = binding.root
        val initialTopPadding = root.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val statusBarInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            view.setPadding(view.paddingLeft, initialTopPadding + statusBarInset, view.paddingRight, view.paddingBottom)
            insets
        }
        ViewCompat.requestApplyInsets(root)
    }

    private enum class StockCategoryFilter { ALL, VACCINE, MEDICATION }
}
