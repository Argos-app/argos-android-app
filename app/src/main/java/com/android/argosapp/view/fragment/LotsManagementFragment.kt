package com.android.argosapp.view.fragment

import android.os.Bundle
import android.view.View
import android.view.animation.PathInterpolator
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.argosapp.R
import com.android.argosapp.databinding.FragmentLotsManagementBinding
import com.android.argosapp.model.Lot
import com.android.argosapp.util.navigation.FragmentNavHelper
import com.android.argosapp.view.adapter.LotsManagementAdapter
import com.android.argosapp.view.componentmanager.LotCardsManager
import com.android.argosapp.viewmodel.LotsManagementViewModel
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

class LotsManagementFragment : Fragment(R.layout.fragment_lots_management) {
    private var _binding: FragmentLotsManagementBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val viewModel: LotsManagementViewModel by viewModels()
    private var lotsAdapter: LotsManagementAdapter? = null
    private var lotCardsManager: LotCardsManager? = null
    private var selectedIndex = 0
    private var showSlaughter = false
    private var isLoadingLots = false
    private var hasLotLoadFailed = false
    private var isSlaughtering = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLotsManagementBinding.bind(view)
        lotCardsManager = LotCardsManager(
            root = binding.lotsCarousel,
            onSearchChanged = {
                selectedIndex = 0
                renderLots()
            },
            onNavigate = ::changeLot,
            onSearch = ::renderLots,
            onSuggestionSelected = { lot ->
                selectedIndex = matchingLots().indexOf(lot).coerceAtLeast(0)
                renderLots()
            },
        )
        applySystemBarInsets()
        setupBackButton()
        setupLotsList()
        setupFilters()
        setupPullToRefresh()
        setupAddButton()
        setupLotRegistrationResult()
        renderLots()
        viewModel.render(
            onLoading = ::setLoading,
            onRefreshing = ::setRefreshing,
            onLotsLoaded = { showLots() },
            onError = { _, cached -> if (cached) showRefreshError() else showLoadError() },
        )
    }

    override fun onDestroyView() {
        lotCardsManager?.dispose()
        lotCardsManager = null
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

    private fun changeLot(direction: Int) {
        val matches = matchingLots()
        val count = matches.size
        if (count < 2) return
        val nextIndex = (selectedIndex + direction + count) % count
        lotCardsManager?.animateTo(matches[nextIndex].lotName) {
            selectedIndex = nextIndex
            renderLots()
        }
    }

    private fun setupFilters() {
        binding.lotsFilterAll.setOnClickListener { selectFilter(false, true) }
        binding.lotsFilterSlaughter.setOnClickListener { selectFilter(true, true) }
        binding.lotsSlaughterButton.setOnClickListener {
            val lot = matchingLots().getOrNull(selectedIndex) ?: return@setOnClickListener
            if (isSlaughtering) return@setOnClickListener
            isSlaughtering = true
            renderLoadingState()
            viewModel.slaughter(lot) { success, cacheUpdated ->
                isSlaughtering = false
                if (_binding == null) return@slaughter
                if (success) {
                    selectedIndex = 0
                    renderLots()
                    Toast.makeText(requireContext(), R.string.lots_slaughter_success, Toast.LENGTH_SHORT).show()
                    if (!cacheUpdated) {
                        viewModel.refresh(::setRefreshing, { showLots() }, { _, cached -> if (cached) showRefreshError() else showLoadError() })
                    }
                } else {
                    renderLoadingState()
                    Toast.makeText(requireContext(), R.string.lots_slaughter_error, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun selectFilter(slaughter: Boolean, animate: Boolean) {
        showSlaughter = slaughter
        val target = if (slaughter) binding.lotsFilterSlaughter else binding.lotsFilterAll
        listOf(binding.lotsFilterAll, binding.lotsFilterSlaughter).forEach { tab ->
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
            val targetCenter = target.left + target.width / 2f
            val containerCenter = binding.lotsFilterContainer.width / 2f
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
        renderLots()
    }

    private fun setupPullToRefresh() {
        binding.lotsSwipeRefresh.setColorSchemeResources(R.color.argos_green)
        binding.lotsSwipeRefresh.setOnChildScrollUpCallback { _, _ ->
            if (binding.lotsStateContainer.visibility == View.VISIBLE) {
                binding.lotsStateContainer.canScrollVertically(-1)
            } else {
                binding.lotsRecyclerView.canScrollVertically(-1)
            }
        }
        binding.lotsSwipeRefresh.setOnRefreshListener {
            viewModel.refresh(
                onRefreshing = ::setRefreshing,
                onLotsLoaded = { showLots() },
                onError = { _, cached -> if (cached) showRefreshError() else showLoadError() },
            )
        }
    }

    private fun setupAddButton() {
        binding.lotsAddButton.setOnClickListener { openCreateLot() }
        binding.lotsEmptyAddButton.setOnClickListener { openCreateLot() }
    }

    private fun openCreateLot() {
        FragmentNavHelper(parentFragmentManager).showBottomSheet(
            CreateLotBottomSheetFragment(),
            CreateLotBottomSheetFragment.TAG,
        )
    }

    private fun setupLotRegistrationResult() {
        parentFragmentManager.setFragmentResultListener(
            CreateLotBottomSheetFragment.REGISTERED_RESULT,
            viewLifecycleOwner,
        ) { _, _ ->
            viewModel.refresh(
                onRefreshing = ::setRefreshing,
                onLotsLoaded = { showLots() },
                onError = { _, cached -> if (cached) showRefreshError() else showLoadError() },
            )
        }
    }

    private fun showLots() {
        hasLotLoadFailed = false
        renderLots()
    }

    private fun matchingLots(): List<Lot> = viewModel.filterLots(
        query = lotCardsManager?.query.orEmpty(),
        category = null,
    )

    private fun renderLots() {
        val currentBinding = _binding ?: return
        val matches = matchingLots()
        if (selectedIndex >= matches.size) selectedIndex = 0
        val selected = matches.getOrNull(selectedIndex)
        lotsAdapter?.submitList(if (selected != null && !showSlaughter) listOf(selected) else emptyList())
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

    private fun showRefreshError() {
        _binding?.let { Toast.makeText(it.root.context, R.string.refresh_failed, Toast.LENGTH_SHORT).show() }
    }

    private fun showLoadError() {
        if (_binding == null) return
        hasLotLoadFailed = true
        renderLoadingState()
    }

    private fun renderLoadingState() {
        val currentBinding = _binding ?: return
        val hasAnyLots = viewModel.filterLots(query = "", category = null).isNotEmpty()
        val selected = matchingLots().getOrNull(selectedIndex)
        val filterBecameVisible = hasAnyLots && currentBinding.lotsFilterContainer.visibility != View.VISIBLE
        lotCardsManager?.render(hasAnyLots, selected?.lotName)
        lotCardsManager?.showSuggestions(viewModel.suggestLots(lotCardsManager?.query.orEmpty()))
        currentBinding.lotsFilterContainer.visibility = if (hasAnyLots) View.VISIBLE else View.GONE
        if (filterBecameVisible) {
            currentBinding.lotsFilterContainer.post {
                if (_binding != null) selectFilter(showSlaughter, false)
            }
        }
        currentBinding.lotsRecyclerView.visibility = if (selected != null && !showSlaughter) View.VISIBLE else View.GONE
        currentBinding.lotsSlaughterCard.visibility = if (selected != null && showSlaughter) View.VISIBLE else View.GONE
        currentBinding.lotsLoadingIndicator.visibility = (if (isLoadingLots && !hasAnyLots) View.VISIBLE else View.GONE)
        val showState = !isLoadingLots && selected == null

        currentBinding.lotsStateContainer.visibility = if (showState) View.VISIBLE else View.GONE
        val showFirstLotPrompt = showState && !hasAnyLots && !hasLotLoadFailed

        currentBinding.lotsStateImageCard.visibility = if (showFirstLotPrompt) View.VISIBLE else View.GONE
        currentBinding.lotsEmptyAddButton.visibility = if (showFirstLotPrompt) View.VISIBLE else View.GONE
        currentBinding.lotsAddButton.visibility = if (selected != null && !showSlaughter) View.VISIBLE else View.GONE

        if (showState) {
            currentBinding.lotsStateMessage.setText(
                if (hasLotLoadFailed) R.string.lots_load_error else if (hasAnyLots) R.string.lots_empty else R.string.lots_no_registered,
            )
            currentBinding.lotsStateMessage.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (hasLotLoadFailed) R.color.argos_notice_important else R.color.argos_text_secondary,
                ),
            )
        }
        if (selected != null && showSlaughter) renderSlaughterCard(selected)
    }

    private fun renderSlaughterCard(lot: Lot) {
        val release = lot.releaseSlaughterDate
        val available = release != null && viewModel.isSlaughterAvailable(lot)
        binding.lotsSlaughterCard.strokeWidth = if (available) resources.displayMetrics.density.roundToInt() else 0
        binding.lotsSlaughterTitle.text = getString(
            if (available) R.string.lots_slaughter_confirmation else R.string.lots_slaughter_unavailable,
            lot.lotName,
        )
        binding.lotsSlaughterMessage.text = if (available) {
            getString(R.string.lots_slaughter_confirmation_message)
        } else if (release != null) {
            getString(R.string.lots_slaughter_unavailable_message, release.format(DATE_FORMATTER))
        } else {
            getString(R.string.lots_slaughter_unknown_date)
        }
        binding.lotsSlaughterButton.alpha = if (available) 1f else 0.35f
        binding.lotsSlaughterButton.isEnabled = available && !isSlaughtering
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
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("pt", "BR"))
    }
}
