package com.android.argosapp.view.activity

import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.argosapp.R
import com.android.argosapp.databinding.ActivityMedicationStockBinding
import com.android.argosapp.model.StockCategory
import com.android.argosapp.view.adapter.MedicationStockAdapter
import com.android.argosapp.view.fragment.MedicationStockRegistrationBottomSheet
import com.android.argosapp.viewmodel.MedicationStockViewModel
import kotlinx.coroutines.launch

class MedicationStockActivity : FragmentActivity() {
    private lateinit var binding: ActivityMedicationStockBinding
    private val viewModel: MedicationStockViewModel by viewModels()
    private val stockAdapter = MedicationStockAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMedicationStockBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener { finish() }
        binding.stockRecyclerView.adapter = stockAdapter
        binding.stockRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(view: RecyclerView, dx: Int, dy: Int) {
                if (dy >= 0) loadIfAtEnd()
            }
        })
        binding.searchInput.setText(viewModel.state.value.query)
        binding.filterGroup.check(when (viewModel.state.value.category) {
            StockCategory.VACCINE -> R.id.filterVaccinesButton
            StockCategory.MEDICATION -> R.id.filterMedicationsButton
            null -> R.id.filterAllButton
        })
        binding.searchInput.doAfterTextChanged { viewModel.search(it?.toString().orEmpty()) }
        binding.searchButton.setOnClickListener { viewModel.refresh() }
        binding.filterGroup.addOnButtonCheckedListener { _, id, checked ->
            if (checked) viewModel.filter(when (id) {
                R.id.filterVaccinesButton -> StockCategory.VACCINE
                R.id.filterMedicationsButton -> StockCategory.MEDICATION
                else -> null
            })
        }
        binding.retryButton.setOnClickListener { viewModel.loadNextPage(retry = true) }
        binding.addStockButton.setOnClickListener {
            if (supportFragmentManager.findFragmentByTag(MedicationStockRegistrationBottomSheet.TAG) == null) {
                MedicationStockRegistrationBottomSheet().show(supportFragmentManager, MedicationStockRegistrationBottomSheet.TAG)
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    stockAdapter.submitList(state.items) { binding.stockRecyclerView.post { loadIfAtEnd() } }
                    binding.loadingIndicator.isVisible = state.loading && state.items.isEmpty()
                    binding.paginationIndicator.isVisible = state.loading && state.items.isNotEmpty()
                    binding.stateContainer.isVisible = !state.loading && (state.items.isEmpty() || state.error)
                    binding.retryButton.isVisible = state.error
                    binding.stateMessage.setText(when {
                        state.error -> R.string.stock_error
                        viewModel.integrationPending -> R.string.stock_integration_pending
                        else -> R.string.stock_empty
                    })
                }
            }
        }
    }

    private fun loadIfAtEnd() {
        val layout = binding.stockRecyclerView.layoutManager as LinearLayoutManager
        if (stockAdapter.itemCount > 0 && layout.findLastVisibleItemPosition() >= stockAdapter.itemCount - 1) {
            viewModel.loadNextPage()
        }
    }
}
