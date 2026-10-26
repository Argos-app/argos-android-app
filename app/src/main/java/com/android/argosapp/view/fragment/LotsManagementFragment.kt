package com.android.argosapp.view.fragment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.android.argosapp.R
import com.android.argosapp.databinding.FragmentLotsManagementBinding
import com.android.argosapp.util.navigation.FragmentNavHelper

class LotsManagementFragment : Fragment(R.layout.fragment_lots_management) {
    private var _binding: FragmentLotsManagementBinding? = null
    private val binding get() = requireNotNull(_binding)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLotsManagementBinding.bind(view)
        binding.lotsBackButton.setOnClickListener {
            FragmentNavHelper(parentFragmentManager).popBackStack()
        }
        binding.lotsRecyclerView.visibility = View.GONE
        binding.lotsStateContainer.visibility = View.VISIBLE
        binding.lotsStateContainer.bringToFront()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val TAG = "lots_management"
    }
}