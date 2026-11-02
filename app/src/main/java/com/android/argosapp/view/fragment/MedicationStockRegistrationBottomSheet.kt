package com.android.argosapp.view.fragment

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.activityViewModels
import com.android.argosapp.R
import com.android.argosapp.data.dto.MedicineCatalogDto
import com.android.argosapp.data.dto.RegisterMedicineDto
import com.android.argosapp.databinding.BottomSheetMedicationStockRegistrationBinding
import com.android.argosapp.view.componentmanager.SelectManager
import com.android.argosapp.util.validation.MedicationStockFormValidator
import com.android.argosapp.viewmodel.MedicationStockViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import android.graphics.drawable.GradientDrawable
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MedicationStockRegistrationBottomSheet : BottomSheetDialogFragment() {
    private var _binding: BottomSheetMedicationStockRegistrationBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val viewModel: MedicationStockViewModel by activityViewModels()
    private val selectManager = SelectManager()
    private val validator = MedicationStockFormValidator()
    private val catalog = mutableListOf<MedicineCatalogDto>()
    private var dropdown: SelectManager.Dropdown? = null
    private var updatingProductText = false
    private var selectedProduct: MedicineCatalogDto? = null
    private var catalogFooter: View? = null
    private var catalogFooterSpinner: ProgressBar? = null
    private var catalogFooterLabel: TextView? = null
    private var manufactureDate: LocalDate? = null
    private var expiryDate: LocalDate? = null
    private var nextCatalogPage = 0
    private var hasMoreCatalog = true
    private var loadingCatalog = false
    private var catalogFailed = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = BottomSheetMedicationStockRegistrationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        selectedProduct = savedInstanceState?.let {
            val registration = it.getString(STATE_PRODUCT_REGISTRATION) ?: return@let null
            MedicineCatalogDto(registration, it.getString(STATE_PRODUCT_NAME).orEmpty())
        }
        manufactureDate = savedInstanceState?.getString(STATE_MANUFACTURE)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        expiryDate = savedInstanceState?.getString(STATE_EXPIRY)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        savedInstanceState?.getString(STATE_BATCH)?.let(binding.batchNumberInput::setText)
        savedInstanceState?.getString(STATE_QUANTITY)?.let(binding.quantityInput::setText)
        savedInstanceState?.getString(STATE_SUPPLIER)?.let(binding.supplierInput::setText)
        savedInstanceState?.getString(STATE_CNPJ)?.let(binding.cnpjInput::setText)
        renderSelection()

        binding.productInput.doAfterTextChanged { editable ->
            if (updatingProductText) return@doAfterTextChanged
            if (selectedProduct?.medicineName != editable?.toString()) {
                selectedProduct = null
                binding.registrationCodeInput.text.clear()
                binding.productInput.error = null
            }
            if (catalog.isNotEmpty()) showCatalogSuggestions(editable?.toString().orEmpty())
        }
        binding.productInput.setOnClickListener {
            if (catalog.isEmpty() && catalogFailed) loadCatalogPage(0)
            else if (catalog.isNotEmpty()) showCatalogSuggestions(binding.productInput.text.toString())
        }
        binding.manufactureInput.setOnClickListener {
            selectDate(manufactureDate) { manufactureDate = it; binding.manufactureInput.error = null; renderSelection() }
        }
        binding.expirationInput.setOnClickListener {
            selectDate(expiryDate) { expiryDate = it; binding.expirationInput.error = null; renderSelection() }
        }
        binding.discardButton.setOnClickListener { dismiss() }
        binding.registerButton.setOnClickListener { register() }
        val bottomPadding = binding.root.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { root, insets ->
            val bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()).bottom
            root.setPadding(root.paddingLeft, root.paddingTop, root.paddingRight, bottomPadding + bottom)
            insets
        }
        ViewCompat.requestApplyInsets(binding.root)
        loadCatalogPage(0)
    }

    override fun onStart() {
        super.onStart()
        (dialog as? BottomSheetDialog)?.apply {
            window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.background = GradientDrawable().apply {
                setColor(requireContext().getColor(R.color.argos_background))
                val radius = 40f * resources.displayMetrics.density
                cornerRadii = floatArrayOf(radius, radius, radius, radius, 0f, 0f, 0f, 0f)
            }
            behavior.isHideable = true
            behavior.skipCollapsed = true
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    private fun loadCatalogPage(page: Int) {
        if (loadingCatalog) return
        loadingCatalog = true
        if (page == 0) {
            binding.productInput.isEnabled = false
            binding.productInput.error = null
            binding.productInput.setHint(R.string.stock_catalog_loading)
            binding.productLoadingIndicator.visibility = View.VISIBLE
            binding.productDropdownIcon.visibility = View.GONE
        }
        renderCatalogFooter()
        viewModel.loadCatalogPage(page) { products, more, error ->
            if (_binding == null) return@loadCatalogPage
            loadingCatalog = false
            binding.productInput.isEnabled = true
            binding.productInput.setHint(R.string.stock_product_hint)
            binding.productLoadingIndicator.visibility = View.GONE
            binding.productDropdownIcon.visibility = View.VISIBLE
            if (error != null) {
                catalogFailed = true
                if (page == 0) binding.productInput.error = getString(R.string.stock_product_error)
                dropdown?.let { if (it.popup.isShowing) updateCatalogDropdown(it) }
                renderCatalogFooter()
                return@loadCatalogPage
            }
            catalogFailed = false
            if (page == 0) catalog.clear()
            catalog += products.filter { product -> catalog.none { it.productRegistration == product.productRegistration } }
            nextCatalogPage = page + 1
            hasMoreCatalog = more
            if (page == 0 && catalog.isEmpty()) binding.productInput.error = getString(R.string.stock_products_empty)
            dropdown?.let { if (it.popup.isShowing) updateCatalogDropdown(it) }
            renderCatalogFooter()
        }
    }

    private fun showCatalogSuggestions(query: String) {
        val existing = dropdown
        if (existing?.popup?.isShowing == true) {
            updateCatalogDropdown(existing, query)
            return
        }
        val footer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(48)
            setPadding(dp(16), dp(10), dp(16), dp(10))
        }
        val spinner = ProgressBar(requireContext()).apply {
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(dp(20), dp(20))
            indeterminateTintList = android.content.res.ColorStateList.valueOf(requireContext().getColor(R.color.argos_green))
        }
        val label = TextView(requireContext()).apply {
            textSize = 13f
            setTextColor(requireContext().getColor(R.color.argos_green))
            setPadding(dp(12), 0, 0, 0)
        }
        footer.addView(spinner)
        footer.addView(label)
        val dropdown = selectManager.showOptions(
            anchor = binding.productInput,
            options = matchingCatalog(query),
            dropdownIcon = binding.productDropdownIcon,
            footer = footer.takeIf { hasMoreCatalog },
            onSelected = { selected ->
                selectedProduct = catalog.firstOrNull { catalogLabel(it) == selected }
                binding.productInput.error = null
                updatingProductText = true
                binding.productInput.setText(selectedProduct?.medicineName.orEmpty())
                binding.productInput.setSelection(binding.productInput.text.length)
                updatingProductText = false
                renderSelection()
            },
            onDismiss = { this.dropdown = null },
        )
        this.dropdown = dropdown
        catalogFooter = footer
        catalogFooterSpinner = spinner
        catalogFooterLabel = label
        fun loadNextPage() {
            if (!hasMoreCatalog || loadingCatalog) return
            catalogFailed = false
            loadCatalogPage(nextCatalogPage)
        }
        footer.setOnClickListener { loadNextPage() }
        dropdown.list.setOnScrollListener(object : android.widget.AbsListView.OnScrollListener {
            override fun onScrollStateChanged(view: android.widget.AbsListView?, scrollState: Int) = Unit
            override fun onScroll(view: android.widget.AbsListView?, firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int) {
                if (hasMoreCatalog && !loadingCatalog && !catalogFailed && totalItemCount > 0 &&
                    firstVisibleItem + visibleItemCount >= totalItemCount - 2
                ) loadNextPage()
            }
        })
        renderCatalogFooter()
    }

    private fun renderCatalogFooter() {
        catalogFooter?.visibility = if (hasMoreCatalog) View.VISIBLE else View.GONE
        catalogFooterSpinner?.visibility = if (loadingCatalog) View.VISIBLE else View.GONE
        catalogFooterLabel?.text = when {
            loadingCatalog -> getString(R.string.stock_catalog_loading)
            catalogFailed -> getString(R.string.stock_catalog_retry)
            else -> getString(R.string.stock_catalog_more)
        }
    }

    private fun matchingCatalog(query: String): List<String> = catalog
        .filter { it.medicineName.contains(query.trim(), ignoreCase = true) || it.productRegistration.contains(query.trim(), ignoreCase = true) }
        .map(::catalogLabel)

    private fun updateCatalogDropdown(dropdown: SelectManager.Dropdown, query: String = binding.productInput.text.toString()) {
        dropdown.replaceOptions(matchingCatalog(query))
    }

    private fun register() {
        val product = selectedProduct
        val batch = binding.batchNumberInput.text.toString().trim()
        val quantityText = binding.quantityInput.text.toString().trim()
        val quantity = quantityText.replace(',', '.').toBigDecimalOrNull()
        val supplier = binding.supplierInput.text.toString().trim()
        val cnpj = binding.cnpjInput.text.toString().filter(Char::isDigit)
        val manufacture = manufactureDate
        val expiry = expiryDate
        val invalidFields = validator.invalidFields(
            product?.productRegistration,
            batch,
            quantityText,
            manufacture,
            expiry,
            supplier,
            cnpj,
        )
        binding.productInput.error = when {
            MedicationStockFormValidator.Field.PRODUCT in invalidFields -> getString(R.string.stock_product_required)
            else -> null
        }
        binding.batchNumberInput.error = if (MedicationStockFormValidator.Field.BATCH_NUMBER in invalidFields) getString(R.string.stock_required) else null
        binding.quantityInput.error = if (MedicationStockFormValidator.Field.QUANTITY in invalidFields) getString(R.string.stock_positive_quantity) else null
        binding.manufactureInput.error = if (MedicationStockFormValidator.Field.MANUFACTURE_DATE in invalidFields) getString(R.string.stock_required) else null
        binding.expirationInput.error = when {
            MedicationStockFormValidator.Field.EXPIRY_DATE !in invalidFields -> null
            expiry == null -> getString(R.string.stock_required)
            else -> getString(R.string.stock_invalid_dates)
        }
        binding.supplierInput.error = if (MedicationStockFormValidator.Field.SUPPLIER in invalidFields) getString(R.string.stock_required) else null
        binding.cnpjInput.error = if (MedicationStockFormValidator.Field.SUPPLIER_CNPJ in invalidFields) getString(R.string.stock_invalid_cnpj) else null
        if (invalidFields.isNotEmpty()) return

        viewModel.register(
            RegisterMedicineDto(
                productRegistration = requireNotNull(product).productRegistration,
                batchNumber = batch,
                manufactureDate = requireNotNull(manufacture).toString(),
                expiryDate = requireNotNull(expiry).toString(),
                quantity = requireNotNull(quantity),
                supplierName = supplier,
                supplierCnpj = cnpj,
            ),
            onLoading = { loading ->
                _binding?.let { current ->
                    current.registerButton.isEnabled = !loading
                    current.registerButton.text = if (loading) getString(R.string.stock_register_loading) else getString(R.string.stock_register)
                    isCancelable = !loading
                }
            },
            onSuccess = {
                _binding?.let {
                    parentFragmentManager.setFragmentResult(REGISTERED_RESULT, Bundle.EMPTY)
                    Toast.makeText(it.root.context, R.string.stock_register_success, Toast.LENGTH_SHORT).show()
                    dismiss()
                }
            },
            onError = { _ ->
                _binding?.let { Toast.makeText(it.root.context, R.string.stock_register_error, Toast.LENGTH_LONG).show() }
            },
        )
    }

    private fun selectDate(current: LocalDate?, onSelected: (LocalDate) -> Unit) {
        val date = current ?: LocalDate.now()
        DatePickerDialog(requireContext(), R.style.Theme_Argosapp_DatePickerDialog, { _, year, month, day ->
            onSelected(LocalDate.of(year, month + 1, day))
        }, date.year, date.monthValue - 1, date.dayOfMonth).show()
    }

    private fun renderSelection() {
        selectedProduct?.let { product ->
            if (binding.productInput.text.toString() != product.medicineName) {
                updatingProductText = true
                binding.productInput.setText(product.medicineName)
                binding.productInput.setSelection(binding.productInput.text.length)
                updatingProductText = false
            }
        }
        binding.registrationCodeInput.setText(selectedProduct?.productRegistration.orEmpty())
        binding.manufactureInput.setText(manufactureDate?.format(DATE_FORMAT).orEmpty())
        binding.expirationInput.setText(expiryDate?.format(DATE_FORMAT).orEmpty())
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_PRODUCT_REGISTRATION, selectedProduct?.productRegistration)
        outState.putString(STATE_PRODUCT_NAME, selectedProduct?.medicineName)
        outState.putString(STATE_MANUFACTURE, manufactureDate?.toString())
        outState.putString(STATE_EXPIRY, expiryDate?.toString())
        outState.putString(STATE_BATCH, binding.batchNumberInput.text.toString())
        outState.putString(STATE_QUANTITY, binding.quantityInput.text.toString())
        outState.putString(STATE_SUPPLIER, binding.supplierInput.text.toString())
        outState.putString(STATE_CNPJ, binding.cnpjInput.text.toString())
    }

    override fun onDestroyView() {
        selectManager.dismiss()
        dropdown = null
        catalogFooter = null
        catalogFooterSpinner = null
        catalogFooterLabel = null
        _binding = null
        super.onDestroyView()
    }

    private fun catalogLabel(item: MedicineCatalogDto) = "${item.medicineName} • ${item.productRegistration}"
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    companion object {
        const val TAG = "medication_stock_registration"
        const val REGISTERED_RESULT = "medication_stock_registered"
        private const val STATE_PRODUCT_REGISTRATION = "product_registration"
        private const val STATE_PRODUCT_NAME = "product_name"
        private const val STATE_MANUFACTURE = "manufacture_date"
        private const val STATE_EXPIRY = "expiry_date"
        private const val STATE_BATCH = "batch_number"
        private const val STATE_QUANTITY = "quantity"
        private const val STATE_SUPPLIER = "supplier"
        private const val STATE_CNPJ = "cnpj"
        private val DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    }
}
