package com.android.argosapp.view.fragment

import android.os.Bundle
import android.view.View
import android.view.animation.PathInterpolator
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.argosapp.R
import com.android.argosapp.databinding.FragmentLotsManagementBinding
import com.android.argosapp.util.navigation.FragmentNavHelper
import com.android.argosapp.view.adapter.LotsManagementAdapter
import com.android.argosapp.viewmodel.LotsManagementViewModel

class LotsManagementFragment : Fragment(R.layout.fragment_lots_management) {
    private var _binding: FragmentLotsManagementBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val viewModel: LotsManagementViewModel by viewModels()
    private var lotsAdapter: LotsManagementAdapter? = null
    private var selectedCategory: String? = null
    private var isLoadingLots = false
    private var hasLotLoadFailed = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLotsManagementBinding.bind(view)
        applySystemBarInsets()
        setupBackButton()
        setupLotsList()
        setupSearch()
        setupFilters()
        setupPullToRefresh()
        setupAddButton()
        setupLotRegistrationResult()
        renderLoadingState()
        viewModel.render(
            onLoading = ::setLoading,
            onRefreshing = ::setRefreshing,
            onLotsLoaded = { showLots() },
            onError = { showLoadError() },
        )
    }

    override fun onDestroyView() {
        _binding?.lotsRecyclerView?.adapter = null
        lotsAdapter = null
        _binding = null
        super.onDestroyView()
    }

    private fun setupBackButton() {
        binding.lotsBackButton.setOnClickListener {
            FragmentNavHelper(parentFragmentManager).popBackStack()
        }
    }

    private fun setupLotsList() {
        lotsAdapter = LotsManagementAdapter()
        binding.lotsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = lotsAdapter
        }
    }

    private fun setupSearch() {
        binding.lotsSearchInput.doAfterTextChanged { renderLots() }
        binding.lotsSearchButton.setOnClickListener { renderLots() }
    }

    private fun setupFilters() {
        binding.lotsFilterAll.setOnClickListener { selectFilter(binding.lotsFilterAll, null, true) }
        binding.lotsFilterEngorda.setOnClickListener {
            selectFilter(binding.lotsFilterEngorda, getString(R.string.lots_filter_engorda), true)
        }
        binding.lotsFilterRecria.setOnClickListener {
            selectFilter(binding.lotsFilterRecria, getString(R.string.lots_filter_recria), true)
        }
        binding.lotsFilterContainer.post {
            if (_binding != null) selectFilter(binding.lotsFilterAll, null, false)
        }
    }

    private fun selectFilter(target: View, category: String?, animate: Boolean) {
        selectedCategory = category
        listOf(binding.lotsFilterAll, binding.lotsFilterEngorda, binding.lotsFilterRecria).forEach { tab ->
            val selected = tab == target
            tab.isSelected = selected
            tab.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (selected) R.color.argos_background else R.color.argos_text_primary,
                ),
            )
        }
        binding.lotsFilterContainer.post {
            if (_binding == null || target.width == 0) return@post
            val indicator = binding.lotsFilterIndicator
            if (indicator.layoutParams.width != target.width) {
                indicator.layoutParams = indicator.layoutParams.apply { width = target.width }
            }
            val targetTranslation = target.left + target.width / 2f - binding.lotsFilterContainer.width / 2f
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
        renderLots()
    }

    private fun setupPullToRefresh() {
        binding.lotsSwipeRefresh.setColorSchemeResources(R.color.argos_green)
        binding.lotsSwipeRefresh.setOnRefreshListener {
            viewModel.refresh(
                onRefreshing = ::setRefreshing,
                onLotsLoaded = { showLots() },
                onError = { showLoadError() },
            )
        }
    }

    private fun setupAddButton() {
        binding.lotsAddButton.setOnClickListener {
            FragmentNavHelper(parentFragmentManager).showBottomSheet(
                CreateLotBottomSheetFragment(),
                CreateLotBottomSheetFragment.TAG,
            )
        }
    }

    private fun setupLotRegistrationResult() {
        parentFragmentManager.setFragmentResultListener(
            CreateLotBottomSheetFragment.REGISTERED_RESULT,
            viewLifecycleOwner,
        ) { _, _ ->
            viewModel.refresh(
                onRefreshing = ::setRefreshing,
                onLotsLoaded = { showLots() },
                onError = { showLoadError() },
            )
        }
    }

    private fun showLots() {
        hasLotLoadFailed = false
        renderLots()
    }

    private fun renderLots() {
        val currentBinding = _binding ?: return
        lotsAdapter?.submitList(
            viewModel.filterLots(
                query = currentBinding.lotsSearchInput.text.toString(),
                category = selectedCategory,
            ),
        )
        renderLoadingState()
    }

    private fun setLoading(loading: Boolean) {
        isLoadingLots = loading
        if (loading) hasLotLoadFailed = false
        renderLoadingState()
    }

    private fun setRefreshing(refreshing: Boolean) {
        _binding?.lotsSwipeRefresh?.isRefreshing = refreshing
    }

    private fun showLoadError() {
        if (_binding == null) return
        hasLotLoadFailed = true
        renderLoadingState()
    }

    private fun renderLoadingState() {
        val currentBinding = _binding ?: return
        val hasItems = (lotsAdapter?.itemCount ?: 0) > 0
        currentBinding.lotsRecyclerView.visibility = if (hasItems) View.VISIBLE else View.GONE
        currentBinding.lotsLoadingIndicator.visibility =
            if (isLoadingLots && !hasItems) View.VISIBLE else View.GONE
        val showState = !isLoadingLots && !hasItems
        currentBinding.lotsStateContainer.visibility = if (showState) View.VISIBLE else View.GONE
        if (!showState) return
        currentBinding.lotsStateMessage.setText(
            if (hasLotLoadFailed) R.string.lots_load_error else R.string.lots_empty,
        )
        currentBinding.lotsStateMessage.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (hasLotLoadFailed) R.color.argos_notice_important else R.color.argos_text_secondary,
            ),
        )
    }

    private fun applySystemBarInsets() {
        val root = _binding?.root ?: return
        val initialTopPadding = root.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val statusBarInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            view.setPadding(view.paddingLeft, initialTopPadding + statusBarInset, view.paddingRight, view.paddingBottom)
            insets
        }
        ViewCompat.requestApplyInsets(root)
    }

    companion object {
        const val TAG = "lots_management"
    }
}
