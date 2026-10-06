package com.android.argosapp.view.fragment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.android.argosapp.R
import com.android.argosapp.databinding.FragmentProfileEditBinding

class ProfileEditFragment : Fragment(R.layout.fragment_profile_edit) {
    private var _binding: FragmentProfileEditBinding? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProfileEditBinding.bind(view)

        _binding?.profileEditBack?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val TAG = "profile_edit"
    }
}
