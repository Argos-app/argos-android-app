package com.android.argosapp.view.fragment

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.Toast
import android.graphics.drawable.GradientDrawable
import android.widget.PopupWindow
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.viewModels
import com.android.argosapp.data.dto.RegisterLotApplicationDto
import com.android.argosapp.R
import androidx.core.widget.doAfterTextChanged
import com.android.argosapp.util.validation.ApplicationFormValidator
import com.android.argosapp.viewmodel.RegisterLotApplicationBottomSheetViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class RegisterLotApplicationBottomSheetFragment : BottomSheetDialogFragment() {
    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val applicationFormValidator = ApplicationFormValidator()
    private val optionsViewModel: RegisterLotApplicationBottomSheetViewModel by viewModels()
    private var lots: List<String> = emptyList()
    private var medicines: List<String> = emptyList()
    private var medicineHasMore = false
    private var selectedLot: String? = null
    private var selectedMedicine: String? = null
    private var optionsPopup: PopupWindow? = null

    override fun onCreateDialog(savedInstanceState: Bundle?) =
        BottomSheetDialog(requireContext(), theme)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.bottom_sheet_register_lot_application, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val doseField = view.findViewById<EditText>(R.id.applicationDoseField)
        val dateValue = view.findViewById<TextView>(R.id.applicationDateValue)
        val lotValue = view.findViewById<TextView>(R.id.applicationLotValue)
        val medicineValue = view.findViewById<TextView>(R.id.applicationMedicineValue)
        val lotField = view.findViewById<View>(R.id.applicationLotField)
        val medicineField = view.findViewById<View>(R.id.applicationMedicineField)
        var lotLoadFailed = false
        var medicineLoadFailed = false
        var cacheRefreshErrorToastShown = false

        fun showCacheRefreshError() {
            if (cacheRefreshErrorToastShown) return
            cacheRefreshErrorToastShown = true
            Toast.makeText(requireContext(), R.string.applications_register_cache_refresh_error, Toast.LENGTH_LONG).show()
        }

        fun loadLots() {
            optionsViewModel.loadLots(
                onLoading = { loading -> setOptionFieldLoading(lotField, lotValue, R.id.applicationLotLoadingIndicator, R.id.applicationLotDropdownIcon, R.id.applicationLotErrorIcon, loading) },
                onLoaded = { items ->
                    lots = items
                    lotLoadFailed = false
                    lotValue.text = selectedLot ?: if (items.isEmpty()) getString(R.string.applications_register_options_empty) else getString(R.string.applications_register_lot_hint)
                    setOptionFieldReady(lotField, lotValue, R.id.applicationLotLoadingIndicator, R.id.applicationLotDropdownIcon, R.id.applicationLotErrorIcon, items.isNotEmpty(), items.isEmpty())
                },
                onError = {
                    if (lots.isNotEmpty()) {
                        showCacheRefreshError()
                    } else {
                        lotLoadFailed = true
                        setOptionFieldError(lotField, lotValue, R.id.applicationLotLoadingIndicator, R.id.applicationLotDropdownIcon, R.id.applicationLotErrorIcon)
                    }
                },
            )
        }

        fun loadMedicines() {
            optionsViewModel.loadMedicines(
                onLoading = { loading -> setOptionFieldLoading(medicineField, medicineValue, R.id.applicationMedicineLoadingIndicator, R.id.applicationMedicineDropdownIcon, R.id.applicationMedicineErrorIcon, loading) },
                onLoaded = { items, hasMore ->
                    medicines = items
                    medicineHasMore = hasMore
                    medicineLoadFailed = false
                    medicineValue.text = selectedMedicine ?: if (items.isEmpty()) getString(R.string.applications_register_options_empty) else getString(R.string.applications_register_medicine_hint)
                    setOptionFieldReady(medicineField, medicineValue, R.id.applicationMedicineLoadingIndicator, R.id.applicationMedicineDropdownIcon, R.id.applicationMedicineErrorIcon, items.isNotEmpty(), items.isEmpty())
                },
                onError = {
                    if (medicines.isNotEmpty()) {
                        showCacheRefreshError()
                    } else {
                        medicineLoadFailed = true
                        setOptionFieldError(medicineField, medicineValue, R.id.applicationMedicineLoadingIndicator, R.id.applicationMedicineDropdownIcon, R.id.applicationMedicineErrorIcon)
                    }
                },
            )
        }

        lotField.setOnClickListener {
            if (lots.isEmpty()) {
                if (lotLoadFailed) loadLots()
                return@setOnClickListener
            }
            showOptions(lotField, lots) { lot ->
                lotValue.error = null
                selectedLot = lot
                lotValue.text = selectedLot
                lotValue.setTextColor(requireContext().getColor(R.color.argos_text_primary))
            }
        }
        medicineField.setOnClickListener {
            if (medicines.isEmpty()) {
                if (medicineLoadFailed) loadMedicines()
                return@setOnClickListener
            }
            showOptions(medicineField, medicines) { medicine ->
                medicineValue.error = null
                selectedMedicine = medicine
                medicineValue.text = selectedMedicine
                medicineValue.setTextColor(requireContext().getColor(R.color.argos_text_primary))
            }
        }

        loadLots()
        loadMedicines()

        val today = LocalDate.now()
        var selectedDate = today

        dateValue.text = today.format(dateFormatter)

        view.findViewById<View>(R.id.applicationDateField).setOnClickListener {
            DatePickerDialog(
                requireContext(),
                R.style.Theme_Argosapp_DatePickerDialog,
                { _, year, month, day ->
                    selectedDate = LocalDate.of(year, month + 1, day)
                    dateValue.text = selectedDate.format(dateFormatter)
                },
                today.year,
                today.monthValue - 1,
                today.dayOfMonth,
            ).show()
        }

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
        val registerButton = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.applicationRegisterButton)
        registerButton.setOnClickListener {
            val lot = selectedLot?.takeIf { it in lots }
            val medicine = selectedMedicine?.takeIf { it in medicines }
            val dose = doseField.text.toString().removeSuffix(" ml")
            val validDose = applicationFormValidator.isValidDose(dose)
            if (lot == null) lotValue.error = getString(R.string.applications_register_lot_required)
            if (medicine == null) medicineValue.error = getString(R.string.applications_register_medicine_required)
            if (!validDose) doseField.error = getString(R.string.applications_register_dose_invalid)
            if (lot == null || medicine == null || !validDose) return@setOnClickListener

            optionsViewModel.registerApplication(
                data = RegisterLotApplicationDto(lot, medicine, selectedDate.toString(), dose.toBigDecimal()),
                onLoading = { loading ->
                    registerButton.isEnabled = !loading
                    registerButton.isClickable = !loading
                    view.findViewById<View>(R.id.applicationRegisterLoadingIndicator).visibility = if (loading) View.VISIBLE else View.GONE
                    registerButton.text = if (loading) "" else getString(R.string.applications_register_submit)
                    registerButton.backgroundTintList = android.content.res.ColorStateList.valueOf(
                        view.context.getColor(if (loading) R.color.argos_border else R.color.argos_green),
                    )
                    isCancelable = !loading
                    view.findViewById<View>(R.id.applicationLaterButton).isEnabled = !loading
                },
                onSuccess = {
                    parentFragmentManager.setFragmentResult(REGISTERED_RESULT, Bundle.EMPTY)
                    Toast.makeText(view.context, R.string.applications_register_success, Toast.LENGTH_SHORT).show()
                    dismiss()
                },
                onError = {
                    Toast.makeText(view.context, R.string.applications_register_error, Toast.LENGTH_LONG).show()
                },
            )
        }
        view.findViewById<View>(R.id.applicationLaterButton).setOnClickListener { dismiss() }
    }

    override fun onDestroyView() {
        optionsPopup?.dismiss()
        super.onDestroyView()
    }

    private fun showOptions(
        anchor: View,
        items: List<String>,
        onSelected: (String) -> Unit,
    ) {
        optionsPopup?.dismiss()
        val context = requireContext()
        fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
        val isMedicine = anchor.id == R.id.applicationMedicineField
        val dropdownIcon = anchor.findViewById<View>(
            if (isMedicine) R.id.applicationMedicineDropdownIcon else R.id.applicationLotDropdownIcon,
        )
        val listView = ListView(context).apply {
            divider = android.graphics.drawable.ColorDrawable(context.getColor(R.color.argos_border))
            dividerHeight = dp(1)
            setPadding(0, dp(8), 0, dp(8))
            clipToPadding = false
        }
        val footer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            minimumHeight = dp(48)
            setPadding(dp(16), dp(12), dp(16), dp(12))
            isFocusable = true
        }
        val loadingIndicator = ProgressBar(context).apply {
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(dp(20), dp(20))
            indeterminateTintList = android.content.res.ColorStateList.valueOf(context.getColor(R.color.argos_green))
        }
        val footerLabel = TextView(context).apply {
            textSize = 13f
            typeface = ResourcesCompat.getFont(context, R.font.dm_sans)
            setTextColor(context.getColor(R.color.argos_green))
            setPadding(dp(12), 0, 0, 0)
        }
        footer.addView(loadingIndicator)
        footer.addView(footerLabel)
        if (isMedicine) listView.addFooterView(footer, null, false)

        val adapter = object : ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, items.toMutableList()) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
                (super.getView(position, convertView, parent) as TextView).apply {
                    typeface = ResourcesCompat.getFont(context, R.font.dm_sans)
                    textSize = 14f
                    setTextColor(context.getColor(R.color.argos_text_primary))
                    setPadding(dp(16), dp(12), dp(16), dp(12))
                    minHeight = dp(48)
                }
        }
        listView.adapter = adapter
        var loadingPage = false
        var pageLoadFailed = false
        lateinit var loadNextPage: () -> Unit

        fun updateFooter() {
            if (!isMedicine || !medicineHasMore) {
                footer.visibility = View.GONE
                return
            }
            footer.visibility = View.VISIBLE
            loadingIndicator.visibility = if (loadingPage) View.VISIBLE else View.GONE
            footerLabel.text = when {
                loadingPage -> context.getString(R.string.applications_register_medicine_page_loading)
                pageLoadFailed -> context.getString(R.string.applications_register_medicine_page_error)
                else -> context.getString(R.string.applications_register_medicine_page_more)
            }
        }

        loadNextPage = loadNext@{
            if (!medicineHasMore || loadingPage) return@loadNext
            pageLoadFailed = false
            loadingPage = true
            updateFooter()
            optionsViewModel.loadMoreMedicines(
                onLoading = { loading ->
                    loadingPage = loading
                    view?.findViewById<View>(R.id.applicationMedicineField)?.isEnabled = !loading
                    updateFooter()
                },
                onLoaded = { names, hasMore ->
                    val firstVisible = listView.firstVisiblePosition
                    val topOffset = listView.getChildAt(0)?.top ?: 0
                    medicines = medicines + names
                    medicineHasMore = hasMore
                    adapter.clear()
                    adapter.addAll(medicines)
                    listView.setSelectionFromTop(firstVisible, topOffset)
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
        listView.setOnScrollListener(object : android.widget.AbsListView.OnScrollListener {
            override fun onScrollStateChanged(view: android.widget.AbsListView?, scrollState: Int) = Unit

            override fun onScroll(
                view: android.widget.AbsListView?,
                firstVisibleItem: Int,
                visibleItemCount: Int,
                totalItemCount: Int,
            ) {
                if (isMedicine && medicineHasMore && !loadingPage && !pageLoadFailed && totalItemCount > 0 &&
                    firstVisibleItem + visibleItemCount >= totalItemCount - 3
                ) {
                    loadNextPage()
                }
            }
        })

        updateFooter()
        val popupHeight = minOf(
            dp(items.size * 48 + 16 + if (isMedicine && medicineHasMore) 48 else 0),
            dp(288),
            resources.displayMetrics.heightPixels / 2,
        )
        val popup = PopupWindow(listView, anchor.width, popupHeight, true).apply {
            setBackgroundDrawable(GradientDrawable().apply {
                setColor(android.graphics.Color.WHITE)
                cornerRadius = dp(16).toFloat()
                setStroke(dp(1), context.getColor(R.color.argos_border))
            })
            elevation = dp(8).toFloat()
            isOutsideTouchable = true
            inputMethodMode = PopupWindow.INPUT_METHOD_NOT_NEEDED
            setOnDismissListener {
                dropdownIcon.rotation = 0f
                optionsPopup = null
            }
        }
        listView.setOnItemClickListener { _, _, position, _ ->
            if (position < adapter.count) {
                onSelected(adapter.getItem(position).orEmpty())
                popup.dismiss()
            }
        }
        optionsPopup = popup
        val anchorLocation = IntArray(2)
        anchor.getLocationInWindow(anchorLocation)
        popup.showAtLocation(anchor, android.view.Gravity.TOP or android.view.Gravity.LEFT, anchorLocation[0], anchorLocation[1] + anchor.height + dp(6))
        dropdownIcon.rotation = 180f
    }

    private fun setOptionFieldLoading(
        field: View,
        value: TextView,
        loadingId: Int,
        dropdownId: Int,
        errorId: Int,
        loading: Boolean,
    ) {
        field.isEnabled = !loading
        field.alpha = 1f
        field.setBackgroundResource(R.drawable.application_field_background)
        value.error = null
        field.findViewById<View>(loadingId).visibility = if (loading) View.VISIBLE else View.GONE
        if (loading) {
            value.text = getString(R.string.applications_register_options_loading)
            value.setTextColor(requireContext().getColor(R.color.argos_placeholder))
            field.findViewById<View>(dropdownId).visibility = View.GONE
            field.findViewById<View>(errorId).visibility = View.GONE
        }
    }

    private fun setOptionFieldReady(
        field: View,
        value: TextView,
        loadingId: Int,
        dropdownId: Int,
        errorId: Int,
        hasOptions: Boolean,
        empty: Boolean,
    ) {
        field.isEnabled = true
        field.alpha = 1f
        field.setBackgroundResource(R.drawable.application_field_background)
        if (empty) value.text = getString(R.string.applications_register_options_empty)
        value.setTextColor(requireContext().getColor(if (empty) R.color.argos_placeholder else R.color.argos_text_primary))
        field.findViewById<View>(loadingId).visibility = View.GONE
        field.findViewById<View>(dropdownId).visibility = if (hasOptions) View.VISIBLE else View.GONE
        field.findViewById<View>(errorId).visibility = View.GONE
    }

    private fun setOptionFieldError(
        field: View,
        value: TextView,
        loadingId: Int,
        dropdownId: Int,
        errorId: Int,
    ) {
        field.isEnabled = true
        field.alpha = 1f
        field.setBackgroundResource(R.drawable.application_field_background)
        value.error = null
        value.text = getString(R.string.applications_register_options_error)
        value.setTextColor(requireContext().getColor(R.color.argos_error))
        field.findViewById<View>(loadingId).visibility = View.GONE
        field.findViewById<View>(dropdownId).visibility = View.GONE
        field.findViewById<View>(errorId).visibility = View.VISIBLE
    }

    companion object {
        const val TAG = "register_lot_application"
        const val REGISTERED_RESULT = "lot_application_registered"
    }
}
