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
import com.android.argosapp.databinding.FragmentApplicationsManagementBinding
import com.android.argosapp.view.activity.HomeActivity
import com.android.argosapp.model.Application
import com.android.argosapp.view.adapter.ApplicationsManagementFragmentAdapter
import com.android.argosapp.viewmodel.ApplicationsManagementViewModel

class ApplicationsManagementFragment : Fragment(R.layout.fragment_applications_management) {
    private var _binding: FragmentApplicationsManagementBinding? = null
    private val viewModel: ApplicationsManagementViewModel by viewModels()
    private var applicationsAdapter: ApplicationsManagementFragmentAdapter? = null
    private var isLoadingApplications = false
    private var hasApplicationLoadFailed = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentApplicationsManagementBinding.bind(view)
        applySystemBarInsets()
        setupBackButton()

        setupApplicationsList()
        setupFilterNavigation()
        renderLoadingState()

        viewModel.render(
            onLoading = ::setLoading,
            onApplicationsLoaded = ::showApplications,
            onError = { showLoadError() },
        )
    }

    override fun onDestroyView() {
        _binding?.applicationsRecyclerView?.adapter = null
        applicationsAdapter = null
        _binding = null
        super.onDestroyView()
    }

    private fun setupApplicationsList() {
        val binding = _binding ?: return
        applicationsAdapter = ApplicationsManagementFragmentAdapter(
            onApplicationClick = ::openApplicationDetails,
        )
        binding.applicationsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = applicationsAdapter
        }
    }

    private fun setupBackButton() {
        val binding = _binding ?: return
        binding.applicationsBackButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
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

    private fun setupFilterNavigation() {
        val binding = _binding ?: return
        binding.applicationsFilterAll.setOnClickListener {
            selectFilter(binding.applicationsFilterAll, animate = true)
        }
        binding.applicationsFilterPending.setOnClickListener {
            selectFilter(binding.applicationsFilterPending, animate = true)
        }
        binding.applicationsFilterCompleted.setOnClickListener {
            selectFilter(binding.applicationsFilterCompleted, animate = true)
        }
        binding.applicationsFilterContainer.post {
            if (_binding != null) {
                selectFilter(binding.applicationsFilterAll, animate = false)
            }
        }
    }

    private fun selectFilter(target: View, animate: Boolean) {
        val binding = _binding ?: return
        val tabs = listOf(
            binding.applicationsFilterAll,
            binding.applicationsFilterPending,
            binding.applicationsFilterCompleted,
        )
        tabs.forEach { tab ->
            tab.isSelected = tab == target
            tab.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (tab == target) R.color.argos_background else R.color.argos_text_primary,
                ),
            )
        }

        binding.applicationsFilterContainer.post {
            val indicator = binding.applicationsFilterIndicator
            val targetWidth = target.width
            if (targetWidth == 0) return@post

            if (indicator.layoutParams.width != targetWidth) {
                indicator.layoutParams = indicator.layoutParams.apply { width = targetWidth }
            }

            val targetCenter = target.left + target.width / 2f
            val containerCenter = binding.applicationsFilterContainer.width / 2f
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
    }

    private fun setLoading(isLoading: Boolean) {
        isLoadingApplications = isLoading
        if (isLoading) {
            hasApplicationLoadFailed = false
        }
        renderLoadingState()
    }

    private fun showApplications(applications: List<Application>) {
        if (_binding == null) return
        hasApplicationLoadFailed = false
        applicationsAdapter?.submitList(applications)
        renderLoadingState()
    }

    private fun openApplicationDetails(application: Application) {
        (activity as? HomeActivity)?.showApplicationManagementDetails(application)
    }

    private fun showLoadError() {
        if (_binding == null) return
        hasApplicationLoadFailed = true
        renderLoadingState()
    }

    private fun renderLoadingState() {
        val binding = _binding ?: return
        binding.applicationsLoadingIndicator.visibility =
            if (isLoadingApplications) View.VISIBLE else View.GONE

        val showMessage = !isLoadingApplications &&
            (hasApplicationLoadFailed || applicationsAdapter?.itemCount == 0)
        binding.applicationsStateMessage.visibility = if (showMessage) View.VISIBLE else View.GONE
        if (!showMessage) return

        binding.applicationsStateMessage.setText(
            if (hasApplicationLoadFailed) {
                R.string.applications_load_error
            } else {
                R.string.applications_empty
            },
        )
        val messageColor = if (hasApplicationLoadFailed) {
            R.color.argos_notice_important
        } else {
            R.color.argos_text_secondary
        }
        binding.applicationsStateMessage.setTextColor(
            ContextCompat.getColor(requireContext(), messageColor),
        )
    }

    companion object {
        const val TAG = "applications_management"
    }
}
