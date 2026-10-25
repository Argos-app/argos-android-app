package com.android.argosapp.view.fragment

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.android.argosapp.R
import com.android.argosapp.databinding.BottomSheetMedicationStockRegistrationBinding
import com.android.argosapp.model.StockCategory
import com.android.argosapp.model.StockProduct
import com.android.argosapp.model.MedicationStockRegistration
import com.android.argosapp.viewmodel.MedicationStockViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MedicationStockRegistrationBottomSheet : BottomSheetDialogFragment() {
    private var _binding: BottomSheetMedicationStockRegistrationBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val viewModel: MedicationStockViewModel by activityViewModels()
    private var selectedProduct: StockProduct? = null
    private var category: StockCategory? = null
    private var manufacture: LocalDate? = null
    private var expiration: LocalDate? = null
    private var loadingProducts = false

    override fun getTheme() = R.style.ThemeOverlay_Argos_StockRegistration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = true
        savedInstanceState?.let { state ->
            state.getString("productId")?.let { id ->
                selectedProduct = StockProduct(id, state.getString("productName").orEmpty(), state.getString("productCode").orEmpty())
            }
            category = state.getString("category")?.let(StockCategory::valueOf)
            manufacture = state.getString("manufacture")?.let(LocalDate::parse)
            expiration = state.getString("expiration")?.let(LocalDate::parse)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = BottomSheetMedicationStockRegistrationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        savedInstanceState?.getString("batchNumber")?.let(binding.batchNumberInput::setText)
        renderSelection()
        binding.productInput.setOnClickListener { selectProduct() }
        binding.categoryInput.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.stock_category_label)
                .setItems(arrayOf(getString(R.string.stock_category_vaccine), getString(R.string.stock_category_medication))) { _, index ->
                    if (_binding != null) {
                        category = if (index == 0) StockCategory.VACCINE else StockCategory.MEDICATION
                        binding.categoryInput.error = null
                        renderSelection()
                    }
                }.show()
        }
        binding.manufactureInput.setOnClickListener {
            selectDate(manufacture) { manufacture = it; binding.manufactureInput.error = null; renderSelection() }
        }
        binding.expirationInput.setOnClickListener {
            selectDate(expiration) { expiration = it; binding.expirationInput.error = null; renderSelection() }
        }
        binding.discardButton.setOnClickListener { dismiss() }
        binding.registerButton.setOnClickListener {
            submitRegistration()
        }
        val bottomPadding = binding.root.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { root, insets ->
            val bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()).bottom
            root.setPadding(root.paddingLeft, root.paddingTop, root.paddingRight, bottomPadding + bottom)
            insets
        }
        ViewCompat.requestApplyInsets(binding.root)
    }

    override fun onStart() {
        super.onStart()
        (dialog as? BottomSheetDialog)?.apply {
            setCanceledOnTouchOutside(true)
            window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            behavior.isHideable = true
            behavior.skipCollapsed = true
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    private fun selectProduct() {
        if (loadingProducts) return
        loadingProducts = true
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val products = viewModel.getProducts()
                if (_binding == null) return@launch
                if (products.isEmpty()) {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.stock_product_label)
                        .setMessage(if (viewModel.integrationPending) R.string.stock_product_pending else R.string.stock_products_empty)
                        .setPositiveButton(android.R.string.ok, null).show()
                } else {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.stock_product_label)
                        .setItems(products.map { it.name }.toTypedArray()) { _, index ->
                            if (_binding != null) {
                                selectedProduct = products[index]
                                binding.productInput.error = null
                                renderSelection()
                            }
                        }.setNegativeButton(R.string.stock_cancel, null).show()
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _binding?.let { Snackbar.make(it.root, R.string.stock_product_error, Snackbar.LENGTH_LONG).show() }
            } finally {
                loadingProducts = false
            }
        }
    }

    private fun selectDate(current: LocalDate?, onSelected: (LocalDate) -> Unit) {
        val date = current ?: LocalDate.now()
        DatePickerDialog(requireContext(), { _, year, month, day ->
            if (_binding != null) onSelected(LocalDate.of(year, month + 1, day))
        }, date.year, date.monthValue - 1, date.dayOfMonth).show()
    }

    private fun renderSelection() {
        binding.productInput.setText(selectedProduct?.name.orEmpty())
        binding.registrationCodeInput.setText(selectedProduct?.registrationCode.orEmpty())
        binding.manufactureInput.setText(manufacture?.format(DATE_FORMAT).orEmpty())
        binding.expirationInput.setText(expiration?.format(DATE_FORMAT).orEmpty())
        binding.categoryInput.setText(when (category) {
            StockCategory.VACCINE -> getString(R.string.stock_category_vaccine)
            StockCategory.MEDICATION -> getString(R.string.stock_category_medication)
            null -> ""
        })
    }

    private fun validate(): Boolean {
        var valid = true
        fun requireField(input: EditText, condition: Boolean, message: Int = R.string.stock_required) {
            input.error = if (condition) null else getString(message)
            if (!condition) valid = false
        }
        requireField(binding.productInput, selectedProduct != null, R.string.stock_product_required)
        requireField(binding.registrationCodeInput, !selectedProduct?.registrationCode.isNullOrBlank())
        requireField(binding.batchNumberInput, binding.batchNumberInput.text.isNotBlank())
        val quantity = binding.quantityInput.text.toString().replace(',', '.').toBigDecimalOrNull()
        requireField(binding.quantityInput, quantity != null && quantity.signum() > 0, R.string.stock_positive_quantity)
        requireField(binding.manufactureInput, manufacture != null)
        requireField(binding.expirationInput, expiration != null)
        if (manufacture != null && expiration != null) {
            requireField(binding.expirationInput, expiration!!.isAfter(manufacture), R.string.stock_invalid_dates)
        }
        requireField(binding.supplierInput, binding.supplierInput.text.isNotBlank())
        requireField(binding.cnpjInput, binding.cnpjInput.text.count { it.isDigit() } == 14, R.string.stock_invalid_cnpj)
        requireField(binding.doseInput, binding.doseInput.text.isNotBlank())
        val days = binding.withdrawalInput.text.toString().toIntOrNull()
        requireField(binding.withdrawalInput, days != null && days >= 0, R.string.stock_invalid_withdrawal)
        requireField(binding.categoryInput, category != null)
        return valid
    }

    private fun submitRegistration() {
        if (!validate()) return
        if (!viewModel.isMockedRepository) {
            Snackbar.make(binding.root, R.string.stock_api_registration_pending, Snackbar.LENGTH_LONG).show()
            return
        }

        val product = requireNotNull(selectedProduct)
        val request = MedicationStockRegistration(
            productRegistration = product.registrationCode,
            batchNumber = binding.batchNumberInput.text.toString().trim(),
            manufactureDate = requireNotNull(manufacture),
            expiryDate = requireNotNull(expiration),
            quantity = requireNotNull(binding.quantityInput.text.toString().replace(',', '.').toBigDecimalOrNull()),
            supplierName = binding.supplierInput.text.toString().trim(),
            supplierCnpj = binding.cnpjInput.text.toString().filter(Char::isDigit),
            propertyId = MOCK_PROPERTY_ID,
            category = category,
        )

        binding.registerButton.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                viewModel.registerMedicationStock(request)
                if (_binding == null) return@launch
                Snackbar.make(
                    requireActivity().findViewById(android.R.id.content),
                    R.string.stock_mock_registration_success,
                    Snackbar.LENGTH_LONG,
                ).show()
                dismiss()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _binding?.let { Snackbar.make(it.root, R.string.stock_mock_registration_error, Snackbar.LENGTH_LONG).show() }
            } finally {
                _binding?.registerButton?.isEnabled = true
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        selectedProduct?.let {
            outState.putString("productId", it.id)
            outState.putString("productName", it.name)
            outState.putString("productCode", it.registrationCode)
        }
        outState.putString("category", category?.name)
        outState.putString("manufacture", manufacture?.toString())
        outState.putString("expiration", expiration?.toString())
        outState.putString("batchNumber", _binding?.batchNumberInput?.text?.toString())
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val TAG = "medication_stock_registration"
        private const val MOCK_PROPERTY_ID = "mock-property-id"
        private val DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    }
}
