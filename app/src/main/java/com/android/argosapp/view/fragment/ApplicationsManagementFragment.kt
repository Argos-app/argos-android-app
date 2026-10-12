package com.android.argosapp.view.fragment

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.animation.PathInterpolator
import android.widget.EditText
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.argosapp.R
import com.android.argosapp.databinding.FragmentApplicationsManagementBinding
import com.android.argosapp.model.Application
import com.android.argosapp.util.validation.ApplicationFormValidator
import com.android.argosapp.view.adapter.ApplicationsManagementFragmentAdapter
import com.android.argosapp.util.navigation.FragmentNavHelper
import com.android.argosapp.viewmodel.ApplicationsManagementViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ApplicationsManagementFragment : Fragment(R.layout.fragment_applications_management) {
    private var _binding: FragmentApplicationsManagementBinding? = null
    private val viewModel: ApplicationsManagementViewModel by viewModels()
    private val applicationFormValidator = ApplicationFormValidator()
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
        setupSearch()
        setupPullToRefresh()
        setupAddApplicationButton()
        renderLoadingState()

        viewModel.render(
            onFilterLoading = ::setLoading,
            onRefreshLoading = ::setRefreshing,
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
        applicationsAdapter = ApplicationsManagementFragmentAdapter()
        binding.applicationsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = applicationsAdapter
        }
    }

    private fun setupBackButton() {
        val binding = _binding ?: return
        binding.applicationsBackButton.setOnClickListener {
            FragmentNavHelper(parentFragmentManager).popBackStack()
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
            selectFilter(binding.applicationsFilterAll, status = null, animate = true)
        }
        binding.applicationsFilterPending.setOnClickListener {
            selectFilter(
                binding.applicationsFilterPending,
                status = "Pendente",
                animate = true,
            )
        }
        binding.applicationsFilterCompleted.setOnClickListener {
            selectFilter(
                binding.applicationsFilterCompleted,
                status = "Concluida",
                animate = true,
            )
        }
        binding.applicationsFilterContainer.post {
            if (_binding != null) {
                selectFilter(binding.applicationsFilterAll, status = null, animate = false)
            }
        }
    }

    private fun setupSearch() {
        val binding = _binding ?: return
        binding.applicationsSearchInput.doAfterTextChanged { text ->
            viewModel.filterApplications(
                text = text?.toString(),
                status = selectedStatus,
                onLoading = ::setLoading,
                onApplicationsLoaded = ::showApplications,
            )
        }
    }

    private fun setupPullToRefresh() {
        val binding = _binding ?: return
        binding.applicationsSwipeRefresh.setColorSchemeResources(R.color.argos_green)
        binding.applicationsSwipeRefresh.setOnRefreshListener {
            viewModel.refresh(
                onRefreshLoading = ::setRefreshing,
                onApplicationsLoaded = ::showApplications,
                onError = { showLoadError() },
            )
        }
    }

    private fun setupAddApplicationButton() {
        val binding = _binding ?: return
        binding.applicationsAddButton.setOnClickListener {
            val content = layoutInflater.inflate(
                R.layout.dialog_application_type_selector,
                null,
                false,
            )
            val popupWidth = minOf(
                (240 * resources.displayMetrics.density).toInt(),
                resources.displayMetrics.widthPixels -
                    (32 * resources.displayMetrics.density).toInt(),
            )
            val popup = PopupWindow(
                content,
                popupWidth,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true,
            ).apply {
                setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
                elevation = 12 * resources.displayMetrics.density
                isOutsideTouchable = true
            }
            content.findViewById<View>(R.id.applicationTypeAnimal).setOnClickListener {
                popup.dismiss()
                showAnimalApplicationSheet()
            }
            content.findViewById<View>(R.id.applicationTypeLot).setOnClickListener {
                popup.dismiss()
                FragmentNavHelper(parentFragmentManager).showBottomSheet(
                    RegisterLotApplicationBottomSheetFragment(),
                    RegisterLotApplicationBottomSheetFragment.TAG,
                )
            }
            content.measure(
                View.MeasureSpec.makeMeasureSpec(popupWidth, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            )
            popup.showAsDropDown(
                binding.applicationsAddButton,
                0,
                -content.measuredHeight - binding.applicationsAddButton.height,
                android.view.Gravity.END,
            )
        }
    }

    private fun showAnimalApplicationSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val content = layoutInflater.inflate(
            R.layout.bottom_sheet_register_application,
            null,
            false,
        )
        setupApplicationDatePicker(content)

        val doseField = content.findViewById<EditText>(R.id.applicationDoseField)
        val earTagField = content.findViewById<EditText>(R.id.applicationEarTagField)
        setDigitsOnlyFilter(doseField)
        setDigitsOnlyFilter(earTagField)
        doseField.doAfterTextChanged { doseField.error = null }
        earTagField.doAfterTextChanged { earTagField.error = null }

        content.findViewById<View>(R.id.applicationRegisterButton).setOnClickListener {
            validateAnimalApplication(doseField, earTagField)
        }
        content.findViewById<View>(R.id.applicationLaterButton).setOnClickListener {
            dialog.dismiss()
        }
        dialog.setContentView(content)
        dialog.show()
    }

    private fun setupApplicationDatePicker(content: View) {
        val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val dateValue = content.findViewById<TextView>(R.id.applicationDateValue)
        var selectedDate = LocalDate.now()
        dateValue.text = selectedDate.format(dateFormatter)
        content.findViewById<View>(R.id.applicationDateField).setOnClickListener {
            DatePickerDialog(
                requireContext(),
                R.style.Theme_Argosapp_DatePickerDialog,
                { _, year, month, day ->
                    selectedDate = LocalDate.of(year, month + 1, day)
                    dateValue.text = selectedDate.format(dateFormatter)
                },
                selectedDate.year,
                selectedDate.monthValue - 1,
                selectedDate.dayOfMonth,
            ).show()
        }
    }

    private fun setDigitsOnlyFilter(field: EditText) {
        field.filters = arrayOf(android.text.InputFilter { source, _, _, _, _, _ ->
            val digitsOnly = source.filter { it in '0'..'9' }
            if (digitsOnly.length == source.length) null else digitsOnly
        })
    }

    private fun validateAnimalApplication(doseField: EditText, earTagField: EditText) {
        val validDose = applicationFormValidator.isValidDose(doseField.text.toString())
        val validEarTag = applicationFormValidator.isValidEarTag(earTagField.text.toString())

        if (!validDose) {
            doseField.error = getString(R.string.applications_register_dose_invalid)
        }
        if (!validEarTag) {
            earTagField.error = getString(R.string.applications_register_ear_tag_invalid)
        }
    }

    private var selectedStatus: String? = null

    private fun selectFilter(target: View, status: String?, animate: Boolean) {
        val binding = _binding ?: return
        selectedStatus = status
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

        viewModel.filterApplications(
            text = binding.applicationsSearchInput.text?.toString(),
            status = status,
            onLoading = ::setLoading,
            onApplicationsLoaded = ::showApplications,
        )
    }

    private fun setLoading(isLoading: Boolean) {
        isLoadingApplications = isLoading
        if (isLoading) {
            hasApplicationLoadFailed = false
        }
        renderLoadingState()
    }

    private fun setRefreshing(isRefreshing: Boolean) {
        _binding?.applicationsSwipeRefresh?.isRefreshing = isRefreshing
    }

    private fun showApplications(applications: List<Application>) {
        if (_binding == null) return
        hasApplicationLoadFailed = false
        applicationsAdapter?.submitList(applications)
        renderLoadingState()
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
