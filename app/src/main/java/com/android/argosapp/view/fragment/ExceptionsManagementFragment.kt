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
import com.android.argosapp.util.navigation.FragmentNavHelper
import com.android.argosapp.view.adapter.ExceptionsManagementAdapter
import com.android.argosapp.viewmodel.ExceptionsManagementViewModel

class ExceptionsManagementFragment : Fragment(R.layout.fragment_exceptions_management) {
    private var _binding: FragmentExceptionsManagementBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val viewModel: ExceptionsManagementViewModel by viewModels()
    private var exceptionsAdapter: ExceptionsManagementAdapter? = null
    private var selectedFilter = ExceptionsManagementViewModel.ExceptionFilter.ALL

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentExceptionsManagementBinding.bind(view)

        applySystemBarInsets()
        setupBackButton()
        setupExceptionsList()
        setupFilters()
        setupAddButton()
        renderSummary()
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
        renderExceptions()
    }

    private fun setupFilters() {
        binding.exceptionsFilterAll.setOnClickListener {
            selectFilter(
                binding.exceptionsFilterAll,
                ExceptionsManagementViewModel.ExceptionFilter.ALL,
                animate = true,
            )
        }
        binding.exceptionsFilterFit.setOnClickListener {
            selectFilter(
                binding.exceptionsFilterFit,
                ExceptionsManagementViewModel.ExceptionFilter.FIT,
                animate = true,
            )
        }
        binding.exceptionsFilterUnfit.setOnClickListener {
            selectFilter(
                binding.exceptionsFilterUnfit,
                ExceptionsManagementViewModel.ExceptionFilter.UNFIT,
                animate = true,
            )
        }
        binding.exceptionsFilterContainer.post {
            if (_binding != null) {
                selectFilter(
                    binding.exceptionsFilterAll,
                    ExceptionsManagementViewModel.ExceptionFilter.ALL,
                    animate = false,
                )
            }
        }
    }

    private fun selectFilter(
        target: View,
        filter: ExceptionsManagementViewModel.ExceptionFilter,
        animate: Boolean,
    ) {
        selectedFilter = filter
        val tabs = listOf(
            binding.exceptionsFilterAll,
            binding.exceptionsFilterFit,
            binding.exceptionsFilterUnfit,
        )
        tabs.forEach { tab ->
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
            val targetWidth = target.width
            if (targetWidth == 0) return@post

            if (indicator.layoutParams.width != targetWidth) {
                indicator.layoutParams = indicator.layoutParams.apply { width = targetWidth }
            }
         
            val targetTranslation = target.left.toFloat()
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

    private fun renderSummary() {
        binding.exceptionsTotalCount.text = viewModel.exceptions.size.toString()
        binding.exceptionsFitCount.text = viewModel.fitCount.toString()
        binding.exceptionsUnfitCount.text = viewModel.unfitCount.toString()
    }

    private fun renderExceptions() {
        exceptionsAdapter?.submitList(viewModel.filter(selectedFilter))
    }

    private fun applySystemBarInsets() {
        val root = _binding?.root ?: return
        val initialTopPadding = root.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val statusBarInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            view.setPadding(
                view.paddingLeft,
                initialTopPadding + statusBarInset,
                view.paddingRight,
                view.paddingBottom,
            )
            insets
        }
        ViewCompat.requestApplyInsets(root)
    }

    companion object {
        const val TAG = "exceptions_management"
    }
}
