package com.android.argosapp.view.fragment

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.animation.PathInterpolator
import android.widget.EditText
import android.widget.ArrayAdapter
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.argosapp.R
import com.android.argosapp.data.dto.RegisterLotApplicationDto
import com.android.argosapp.databinding.FragmentApplicationsManagementBinding
import com.android.argosapp.model.Application
import com.android.argosapp.util.validation.ApplicationFormValidator
import com.android.argosapp.view.adapter.ApplicationsManagementFragmentAdapter
import com.android.argosapp.util.navigation.FragmentNavHelper
import com.android.argosapp.viewmodel.ApplicationsManagementViewModel
import com.android.argosapp.viewmodel.RegisterLotApplicationBottomSheetViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ApplicationsManagementFragment : Fragment(R.layout.fragment_applications_management) {
    private var _binding: FragmentApplicationsManagementBinding? = null
    private val viewModel: ApplicationsManagementViewModel by viewModels()
    private val registrationOptionsViewModel: RegisterLotApplicationBottomSheetViewModel by viewModels()
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

        parentFragmentManager.setFragmentResultListener(
            RegisterLotApplicationBottomSheetFragment.REGISTERED_RESULT,
            viewLifecycleOwner,
        ) { _, _ ->
            viewModel.refresh(
                onRefreshLoading = ::setRefreshing,
                onApplicationsLoaded = ::showApplications,
                onError = { showLoadError() },
            )
        }

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
        val animalLots = mutableListOf<String>()
        val animalMedicines = mutableListOf<String>()
        setupAnimalOptionSelectors(content, animalLots, animalMedicines)

        val doseField = content.findViewById<EditText>(R.id.applicationDoseField)
        val earTagField = content.findViewById<EditText>(R.id.applicationEarTagField)
        val lotValue = content.findViewById<TextView>(R.id.applicationLotValue)
        val medicineValue = content.findViewById<TextView>(R.id.applicationMedicineValue)
        val dateValue = content.findViewById<TextView>(R.id.applicationDateValue)
        setDigitsOnlyFilter(earTagField)
        var formattingDose = false
        doseField.doAfterTextChanged { editable ->
            if (formattingDose) return@doAfterTextChanged
            val digits = editable.toString().filter { it in '0'..'9' }
            val formatted = if (digits.isEmpty()) "" else "$digits ml"
            if (editable.toString() != formatted) {
                formattingDose = true
                doseField.setText(formatted)
                doseField.setSelection(digits.length)
                formattingDose = false
            }
            doseField.error = null
        }
        earTagField.doAfterTextChanged { earTagField.error = null }

        val registerButton = content.findViewById<com.google.android.material.button.MaterialButton>(R.id.applicationRegisterButton)
        registerButton.setOnClickListener {
            if (!validateAnimalApplication(doseField, earTagField, lotValue, medicineValue, animalLots, animalMedicines)) return@setOnClickListener
            val dose = doseField.text.toString().removeSuffix(" ml")
            val applicationDate = LocalDate.parse(dateValue.text.toString(), DateTimeFormatter.ofPattern("dd/MM/yyyy")).toString()
            registrationOptionsViewModel.registerApplication(
                data = RegisterLotApplicationDto(
                    lotName = lotValue.text.toString(),
                    medicineName = medicineValue.text.toString(),
                    applicationDate = applicationDate,
                    dose = dose.toBigDecimal(),
                    earCode = earTagField.text.toString(),
                ),
                onLoading = { loading ->
                    registerButton.isEnabled = !loading
                    registerButton.isClickable = !loading
                    content.findViewById<View>(R.id.applicationRegisterLoadingIndicator).visibility = if (loading) View.VISIBLE else View.GONE
                    registerButton.text = if (loading) "" else getString(R.string.applications_register_submit)
                    registerButton.backgroundTintList = android.content.res.ColorStateList.valueOf(
                        requireContext().getColor(if (loading) R.color.argos_border else R.color.argos_green),
                    )
                    dialog.setCancelable(!loading)
                    content.findViewById<View>(R.id.applicationLaterButton).isEnabled = !loading
                },
                onSuccess = {
                    Toast.makeText(requireContext(), R.string.applications_register_success, Toast.LENGTH_SHORT).show()
                    viewModel.refresh(
                        onRefreshLoading = ::setRefreshing,
                        onApplicationsLoaded = ::showApplications,
                        onError = { showLoadError() },
                    )
                    dialog.dismiss()
                },
                onError = {
                    Toast.makeText(requireContext(), R.string.applications_register_error, Toast.LENGTH_LONG).show()
                },
            )
        }
        content.findViewById<View>(R.id.applicationLaterButton).setOnClickListener {
            dialog.dismiss()
        }
        dialog.setContentView(content)
        dialog.show()
    }

    private fun setupAnimalOptionSelectors(content: View, lots: MutableList<String>, medicines: MutableList<String>) {
        var medicineHasMore = false
        val lotField = content.findViewById<View>(R.id.applicationLotField)
        val medicineField = content.findViewById<View>(R.id.applicationMedicineField)
        val lotValue = content.findViewById<TextView>(R.id.applicationLotValue)
        val medicineValue = content.findViewById<TextView>(R.id.applicationMedicineValue)

        var lotLoadFailed = false
        var medicineLoadFailed = false

        fun setOptionIndicatorState(
            field: View,
            value: TextView,
            loadingId: Int,
            errorId: Int,
            dropdownId: Int,
            loading: Boolean = false,
            failed: Boolean = false,
            hasOptions: Boolean = false,
        ) {
            value.error = null
            field.isEnabled = !loading
            field.findViewById<View>(loadingId).visibility = if (loading) View.VISIBLE else View.GONE
            field.findViewById<View>(errorId).visibility = if (failed) View.VISIBLE else View.GONE
            field.findViewById<View>(dropdownId).visibility = if (hasOptions && !loading && !failed) View.VISIBLE else View.GONE
            when {
                loading -> {
                    value.setText(R.string.applications_register_options_loading)
                    value.setTextColor(requireContext().getColor(R.color.argos_placeholder))
                }
                failed -> {
                    value.setText(R.string.applications_register_options_error)
                    value.setTextColor(requireContext().getColor(R.color.argos_error))
                }
                else -> {
                    value.setText(
                        if (hasOptions) {
                            if (field.id == R.id.applicationLotField) R.string.applications_register_lot_hint
                            else R.string.applications_register_medicine_hint
                        } else R.string.applications_register_options_empty,
                    )
                    value.setTextColor(requireContext().getColor(R.color.argos_placeholder))
                }
            }
        }

        fun loadLots() {
            registrationOptionsViewModel.loadLots(
                onLoading = { loading ->
                    if (loading) {
                        lotLoadFailed = false
                        setOptionIndicatorState(lotField, lotValue, R.id.applicationLotLoadingIndicator, R.id.applicationLotErrorIcon, R.id.applicationLotDropdownIcon, loading = true)
                    }
                },
                onLoaded = { options ->
                    lots.clear()
                    lots.addAll(options)
                    lotLoadFailed = false
                    setOptionIndicatorState(lotField, lotValue, R.id.applicationLotLoadingIndicator, R.id.applicationLotErrorIcon, R.id.applicationLotDropdownIcon, hasOptions = options.isNotEmpty())
                },
                onError = {
                    if (lots.isEmpty()) {
                        lotLoadFailed = true
                        setOptionIndicatorState(lotField, lotValue, R.id.applicationLotLoadingIndicator, R.id.applicationLotErrorIcon, R.id.applicationLotDropdownIcon, failed = true)
                    }
                },
            )
        }

        fun loadMedicines() {
            registrationOptionsViewModel.loadMedicines(
                onLoading = { loading ->
                    if (loading) {
                        medicineLoadFailed = false
                        setOptionIndicatorState(medicineField, medicineValue, R.id.applicationMedicineLoadingIndicator, R.id.applicationMedicineErrorIcon, R.id.applicationMedicineDropdownIcon, loading = true)
                    }
                },
                onLoaded = { options, hasMore ->
                    medicines.clear()
                    medicines.addAll(options)
                    medicineHasMore = hasMore
                    medicineLoadFailed = false
                    setOptionIndicatorState(medicineField, medicineValue, R.id.applicationMedicineLoadingIndicator, R.id.applicationMedicineErrorIcon, R.id.applicationMedicineDropdownIcon, hasOptions = options.isNotEmpty())
                },
                onError = {
                    if (medicines.isEmpty()) {
                        medicineLoadFailed = true
                        setOptionIndicatorState(medicineField, medicineValue, R.id.applicationMedicineLoadingIndicator, R.id.applicationMedicineErrorIcon, R.id.applicationMedicineDropdownIcon, failed = true)
                    }
                },
            )
        }

        loadLots()
        loadMedicines()
        fun showOptions(anchor: View, value: TextView, options: MutableList<String>, isMedicine: Boolean) {
            if (options.isEmpty()) return
            fun dp(size: Int) = (size * resources.displayMetrics.density).toInt()
            val dropdownIcon = anchor.findViewById<View>(
                if (isMedicine) R.id.applicationMedicineDropdownIcon else R.id.applicationLotDropdownIcon,
            )
            val list = android.widget.ListView(requireContext()).apply {
                divider = android.graphics.drawable.ColorDrawable(requireContext().getColor(R.color.argos_border))
                dividerHeight = dp(1)
                setPadding(0, dp(8), 0, dp(8))
                clipToPadding = false
            }
            val footer = android.widget.LinearLayout(requireContext()).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                minimumHeight = dp(48)
                setPadding(dp(16), dp(12), dp(16), dp(12))
                isFocusable = true
            }
            val spinner = android.widget.ProgressBar(requireContext()).apply {
                visibility = View.GONE
                layoutParams = android.widget.LinearLayout.LayoutParams(dp(20), dp(20))
                indeterminateTintList = android.content.res.ColorStateList.valueOf(requireContext().getColor(R.color.argos_green))
            }
            val footerLabel = TextView(requireContext()).apply {
                textSize = 13f
                typeface = androidx.core.content.res.ResourcesCompat.getFont(requireContext(), R.font.dm_sans)
                setTextColor(requireContext().getColor(R.color.argos_green))
                setPadding(dp(12), 0, 0, 0)
            }
            footer.addView(spinner)
            footer.addView(footerLabel)
            if (isMedicine) list.addFooterView(footer, null, false)

            val adapter = object : ArrayAdapter<String>(requireContext(), android.R.layout.simple_list_item_1, options.toMutableList()) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
                    (super.getView(position, convertView, parent) as TextView).apply {
                        typeface = androidx.core.content.res.ResourcesCompat.getFont(requireContext(), R.font.dm_sans)
                        textSize = 14f
                        setTextColor(requireContext().getColor(R.color.argos_text_primary))
                        setPadding(dp(16), dp(12), dp(16), dp(12))
                        minHeight = dp(48)
                    }
            }
            list.adapter = adapter
            var loadingPage = false
            var pageLoadFailed = false
            lateinit var loadNextPage: () -> Unit

            fun updateFooter() {
                if (!isMedicine || !medicineHasMore) {
                    footer.visibility = View.GONE
                    return
                }
                footer.visibility = View.VISIBLE
                spinner.visibility = if (loadingPage) View.VISIBLE else View.GONE
                footerLabel.text = when {
                    loadingPage -> getString(R.string.applications_register_medicine_page_loading)
                    pageLoadFailed -> getString(R.string.applications_register_medicine_page_error)
                    else -> getString(R.string.applications_register_medicine_page_more)
                }
            }

            loadNextPage = load@{
                if (!medicineHasMore || loadingPage) return@load
                pageLoadFailed = false
                loadingPage = true
                updateFooter()
                registrationOptionsViewModel.loadMoreMedicines(
                    onLoading = { loading ->
                        loadingPage = loading
                        anchor.isEnabled = !loading
                        updateFooter()
                    },
                    onLoaded = { names, hasMore ->
                        val first = list.firstVisiblePosition
                        val top = list.getChildAt(0)?.top ?: 0
                        options.addAll(names)
                        medicineHasMore = hasMore
                        adapter.clear()
                        adapter.addAll(options)
                        list.setSelectionFromTop(first, top)
                        pageLoadFailed = false
                        updateFooter()
                    },
                    onError = {
                        pageLoadFailed = true
                        updateFooter()
                    },
                )
            }
            footer.setOnClickListener { loadNextPage() }
            list.setOnScrollListener(object : android.widget.AbsListView.OnScrollListener {
                override fun onScrollStateChanged(view: android.widget.AbsListView?, scrollState: Int) = Unit
                override fun onScroll(view: android.widget.AbsListView?, firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int) {
                    if (isMedicine && medicineHasMore && !loadingPage && !pageLoadFailed && totalItemCount > 0 &&
                        firstVisibleItem + visibleItemCount >= totalItemCount - 3
                    ) loadNextPage()
                }
            })
            updateFooter()

            val popup = android.widget.PopupWindow(
                list,
                anchor.width,
                minOf(dp(options.size * 48 + 16 + if (isMedicine && medicineHasMore) 48 else 0), dp(288), resources.displayMetrics.heightPixels / 2),
                true,
            ).apply {
                setBackgroundDrawable(android.graphics.drawable.GradientDrawable().apply {
                    setColor(android.graphics.Color.WHITE)
                    cornerRadius = dp(16).toFloat()
                    setStroke(dp(1), requireContext().getColor(R.color.argos_border))
                })
                elevation = dp(8).toFloat()
                isOutsideTouchable = true
                inputMethodMode = android.widget.PopupWindow.INPUT_METHOD_NOT_NEEDED
                setOnDismissListener { dropdownIcon.rotation = 0f }
            }
            list.setOnItemClickListener { _, _, position, _ ->
                if (position < adapter.count) {
                    value.error = null
                    value.text = adapter.getItem(position).orEmpty()
                    value.setTextColor(requireContext().getColor(R.color.argos_text_primary))
                    popup.dismiss()
                }
            }
            val location = IntArray(2)
            anchor.getLocationInWindow(location)
            popup.showAtLocation(
                anchor,
                android.view.Gravity.TOP or android.view.Gravity.LEFT,
                location[0],
                location[1] + anchor.height + dp(6),
            )
            dropdownIcon.rotation = 180f
        }
        lotField.setOnClickListener {
            if (lots.isEmpty()) {
                if (lotLoadFailed) loadLots()
                return@setOnClickListener
            }
            showOptions(lotField, lotValue, lots, false)
        }
        medicineField.setOnClickListener {
            if (medicines.isEmpty()) {
                if (medicineLoadFailed) loadMedicines()
                return@setOnClickListener
            }
            showOptions(medicineField, medicineValue, medicines, true)
        }
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

    private fun validateAnimalApplication(
        doseField: EditText,
        earTagField: EditText,
        lotValue: TextView,
        medicineValue: TextView,
        lots: List<String>,
        medicines: List<String>,
    ): Boolean {
        val validDose = applicationFormValidator.isValidDose(doseField.text.toString().removeSuffix(" ml"))
        val validEarTag = applicationFormValidator.isValidEarTag(earTagField.text.toString())
        val validLot = lotValue.text.toString() in lots
        val validMedicine = medicineValue.text.toString() in medicines

        if (!validDose) doseField.error = getString(R.string.applications_register_dose_invalid)
        if (!validEarTag) earTagField.error = getString(R.string.applications_register_ear_tag_invalid)
        if (!validLot) lotValue.error = getString(R.string.applications_register_lot_required)
        if (!validMedicine) medicineValue.error = getString(R.string.applications_register_medicine_required)
        return validDose && validEarTag && validLot && validMedicine
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
