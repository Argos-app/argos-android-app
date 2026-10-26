package com.android.argosapp.view.fragment

import android.os.Bundle
import android.view.View
import android.view.animation.PathInterpolator
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.argosapp.R
import com.android.argosapp.databinding.FragmentExceptionsManagementBinding
import com.android.argosapp.model.AnimalExceptionRecord
import com.android.argosapp.util.navigation.FragmentNavHelper
import com.android.argosapp.view.adapter.ExceptionsManagementAdapter
import com.android.argosapp.viewmodel.ExceptionsManagementViewModel

class ExceptionsManagementFragment : Fragment(R.layout.fragment_exceptions_management) {
    private var _binding: FragmentExceptionsManagementBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val viewModel: ExceptionsManagementViewModel by viewModels()
    private var exceptionsAdapter: ExceptionsManagementAdapter? = null
    private var selectedFilter = ExceptionsManagementViewModel.ExceptionFilter.ALL
    private var isLoadingExceptions = false
    private var hasExceptionLoadFailed = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentExceptionsManagementBinding.bind(view)
        applySystemBarInsets()
        setupBackButton()
        setupExceptionsList()
        setupFilters()
        setupPullToRefresh()
        setupAddButton()
        renderLoadingState()
        viewModel.render(
            onLoading = ::setLoading,
            onRefreshing = ::setRefreshing,
            onExceptionsLoaded = ::showExceptions,
            onError = { showLoadError() },
        )
    }

    override fun onDestroyView() {
        _binding?.exceptionsRecyclerView?.adapter = null
        exceptionsAdapter = null
        _binding = null
        super.onDestroyView()
    }

    private fun setupBackButton() {
        binding.exceptionsBackButton.setOnClickListener {
            FragmentNavHelper(parentFragmentManager).popBackStack()
        }
    }

    private fun setupPullToRefresh() {
        binding.exceptionsSwipeRefresh.setColorSchemeResources(R.color.argos_green)
        binding.exceptionsSwipeRefresh.setOnRefreshListener {
            viewModel.refresh(
                onRefreshing = ::setRefreshing,
                onExceptionsLoaded = ::showExceptions,
                onError = { showLoadError() },
            )
        }
    }

    private fun setupAddButton() {
        binding.exceptionsAddButton.setOnClickListener {
            FragmentNavHelper(parentFragmentManager).showBottomSheet(
                CreateExceptionBottomSheetFragment(),
                CreateExceptionBottomSheetFragment.TAG,
            )
        }
    }

    private fun setupExceptionsList() {
        exceptionsAdapter = ExceptionsManagementAdapter()
        binding.exceptionsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = exceptionsAdapter
        }
        renderSummary()
        renderExceptions()
    }

    private fun setupFilters() {
        binding.exceptionsFilterAll.setOnClickListener {
            selectFilter(binding.exceptionsFilterAll, ExceptionsManagementViewModel.ExceptionFilter.ALL, true)
        }
        binding.exceptionsFilterFit.setOnClickListener {
            selectFilter(binding.exceptionsFilterFit, ExceptionsManagementViewModel.ExceptionFilter.FIT, true)
        }
        binding.exceptionsFilterUnfit.setOnClickListener {
            selectFilter(binding.exceptionsFilterUnfit, ExceptionsManagementViewModel.ExceptionFilter.UNFIT, true)
        }
        binding.exceptionsFilterContainer.post {
            if (_binding != null) {
                selectFilter(binding.exceptionsFilterAll, ExceptionsManagementViewModel.ExceptionFilter.ALL, false)
            }
        }
    }

    private fun selectFilter(
        target: View,
        filter: ExceptionsManagementViewModel.ExceptionFilter,
        animate: Boolean,
    ) {
        selectedFilter = filter
        listOf(binding.exceptionsFilterAll, binding.exceptionsFilterFit, binding.exceptionsFilterUnfit)
            .forEach { tab ->
                val selected = tab == target
                tab.isSelected = selected
                tab.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        if (selected) R.color.argos_background else R.color.argos_text_primary,
                    ),
                )
            }

        binding.exceptionsFilterContainer.post {
            val indicator = binding.exceptionsFilterIndicator
            if (target.width == 0) return@post
            if (indicator.layoutParams.width != target.width) {
                indicator.layoutParams = indicator.layoutParams.apply { width = target.width }
            }
            val targetCenter = target.left + target.width / 2f
            val containerCenter = binding.exceptionsFilterContainer.width / 2f
            val targetTranslation = targetCenter - containerCenter
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
        renderExceptions()
    }

    private fun setLoading(loading: Boolean) {
        isLoadingExceptions = loading
        if (loading) hasExceptionLoadFailed = false
        renderLoadingState()
    }

    private fun setRefreshing(refreshing: Boolean) {
        _binding?.exceptionsSwipeRefresh?.isRefreshing = refreshing
    }

    private fun showExceptions(exceptions: List<AnimalExceptionRecord>) {
        if (_binding == null) return
        hasExceptionLoadFailed = false
        exceptionsAdapter?.submitList(
            when (selectedFilter) {
                ExceptionsManagementViewModel.ExceptionFilter.ALL -> exceptions
                ExceptionsManagementViewModel.ExceptionFilter.FIT -> exceptions.filter { it.eligibleToReturn }
                ExceptionsManagementViewModel.ExceptionFilter.UNFIT -> exceptions.filterNot { it.eligibleToReturn }
            },
        )
        renderSummary()
        renderLoadingState()
    }

    private fun showLoadError() {
        if (_binding == null) return
        hasExceptionLoadFailed = true
        renderLoadingState()
    }

    private fun renderSummary() {
        binding.exceptionsTotalCount.text = viewModel.exceptionCount.toString()
        binding.exceptionsFitCount.text = viewModel.fitCount.toString()
        binding.exceptionsUnfitCount.text = viewModel.unfitCount.toString()
    }

    private fun renderExceptions() {
        exceptionsAdapter?.submitList(viewModel.filter(selectedFilter))
        renderLoadingState()
    }

    private fun renderLoadingState() {
        val currentBinding = _binding ?: return
        val hasItems = (exceptionsAdapter?.itemCount ?: 0) > 0
        currentBinding.exceptionsLoadingIndicator.visibility =
            if (isLoadingExceptions && !hasItems) View.VISIBLE else View.GONE
        val showMessage = !isLoadingExceptions && !hasItems
        currentBinding.exceptionsStateMessage.visibility = if (showMessage) View.VISIBLE else View.GONE
        if (!showMessage) return
        currentBinding.exceptionsStateMessage.setText(
            if (hasExceptionLoadFailed) R.string.exceptions_load_error else R.string.exceptions_empty,
        )
        currentBinding.exceptionsStateMessage.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (hasExceptionLoadFailed) R.color.argos_notice_important else R.color.argos_text_secondary,
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
        const val TAG = "exceptions_management"
    }
}