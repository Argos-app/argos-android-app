package com.android.argosapp.view.fragment

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.android.argosapp.ArgosApplication
import com.android.argosapp.R
import com.android.argosapp.data.cache.storage.ApplicationsStorage
import com.android.argosapp.data.cache.storage.AnimalExceptionStorage
import com.android.argosapp.data.cache.storage.MonthApplicationsStorage
import com.android.argosapp.data.cache.storage.TokenStorage
import com.android.argosapp.data.cache.storage.UserStorage
import com.android.argosapp.data.repository.FirebaseRepository
import com.android.argosapp.data.repository.CloudinaryPhotoRepository
import com.android.argosapp.databinding.FragmentProfileBinding
import com.android.argosapp.util.auth.AuthManager
import com.android.argosapp.util.io.CameraManager
import com.android.argosapp.util.logging.AppLogger
import com.android.argosapp.view.activity.HomeActivity
import com.android.argosapp.view.activity.LoginActivity
import com.android.argosapp.view.activity.PasswordResetActivity
import com.android.argosapp.viewmodel.ProfileViewModel
import com.bumptech.glide.Glide
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.io.File

class ProfileFragment : Fragment(R.layout.fragment_profile) {
    private var _binding: FragmentProfileBinding? = null
    private var pendingSelfieFile: File? = null
    private val authManager by lazy {
        AuthManager(
            (requireActivity().application as ArgosApplication).argosApiRepository,
            FirebaseRepository(),
            TokenStorage(requireContext()),
            UserStorage(requireContext()),
            MonthApplicationsStorage(requireContext()),
            ApplicationsStorage(requireContext()),
            AnimalExceptionStorage(requireContext()),
        )
    }
    private val cloudinaryPhotoRepository by lazy {
        CloudinaryPhotoRepository(requireContext().applicationContext)
    }
    private val viewModel: ProfileViewModel by viewModels {
        viewModelFactory {
            initializer { ProfileViewModel(authManager, cloudinaryPhotoRepository) }
        }
    }
    private val selfieCameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val photoFile = pendingSelfieFile
        pendingSelfieFile = null

        if (result.resultCode == Activity.RESULT_OK && photoFile != null && photoFile.length() > 0L) {
            uploadProfilePhoto(photoFile)
        } else {
            photoFile?.delete()
            if (result.resultCode == Activity.RESULT_OK) {
                showPhotoMessage(R.string.profile_photo_update_failed)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProfileBinding.bind(view)

        _binding?.profileEditOption?.setOnClickListener(::onEditOptionClicked)
        _binding?.profilePhotoCameraIcon?.setOnClickListener { openSelfieCamera() }
        _binding?.profileChangePasswordOption?.setOnClickListener {
            startActivity(PasswordResetActivity.createProfileIntent(requireContext()))
        }

        _binding?.profileLogoutButton?.setOnClickListener(::onLogoutButtonClicked)

        loadUserDetails()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun loadUserDetails() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val user = UserStorage(requireContext()).getUser()
                val unknownValue = getString(R.string.home_calendar_unknown_value)

                _binding?.profileUserName?.text = user?.userName?: unknownValue
                _binding?.profileFarmName?.text = user?.farmName?: unknownValue
                loadProfilePhoto(user?.photoUrl)

            } catch (error: CancellationException) {
                throw error

            } catch (error: Exception) {
                AppLogger.error(AppLogger.SESSION_TAG, "profile_user_read_failed", error)

            }
        }
    }

    private fun loadProfilePhoto(photoUrl: String?) {
        val imageView = _binding?.profilePhotoImage ?: return
        val url = photoUrl?.trim()?.takeIf(::isValidProfilePhotoUrl)
        if (url == null) {
            Glide.with(this).clear(imageView)
            imageView.setImageDrawable(null)
            return
        }

        Glide.with(this)
            .load(url)
            .circleCrop()
            .placeholder(R.drawable.profile_photo_placeholder)
            .error(R.drawable.profile_photo_placeholder)
            .into(imageView)
    }

    private fun isValidProfilePhotoUrl(url: String): Boolean {
        val uri = runCatching { android.net.Uri.parse(url) }.getOrNull() ?: return false
        return (uri.scheme == "https" || uri.scheme == "http") && !uri.host.isNullOrBlank()
    }

    private fun openSelfieCamera() {
        val context = context ?: return
        val capture = try {
            CameraManager(context).createSelfieCapture()
        } catch (error: Exception) {
            AppLogger.error(AppLogger.SESSION_TAG, "profile_photo_camera_prepare_failed", error)
            showPhotoMessage(R.string.profile_photo_camera_unavailable)
            return
        }

        pendingSelfieFile = capture.photoFile
        try {
            selfieCameraLauncher.launch(capture.intent)
        } catch (error: Exception) {
            pendingSelfieFile = null
            capture.photoFile.delete()
            AppLogger.error(AppLogger.SESSION_TAG, "profile_photo_camera_launch_failed", error)
            showPhotoMessage(R.string.profile_photo_camera_unavailable)
        }
    }

    private fun uploadProfilePhoto(photoFile: File) {
        viewModel.uploadProfilePhoto(
            photoFile = photoFile,
            onLoading = ::setPhotoUploadLoading,
            onSuccess = { secureUrl ->
                loadProfilePhoto(secureUrl)
                showPhotoMessage(R.string.profile_photo_updated)
            },
            onError = { error ->
                AppLogger.error(AppLogger.SESSION_TAG, "profile_photo_update_failed", error)
                showPhotoMessage(R.string.profile_photo_update_failed)
            },
        )
    }

    private fun setPhotoUploadLoading(loading: Boolean) {
        _binding?.apply {
            profilePhotoCameraIcon.isEnabled = !loading
            profilePhotoCameraIcon.visibility = if (loading) View.INVISIBLE else View.VISIBLE
            profilePhotoUploadProgress.visibility = if (loading) View.VISIBLE else View.GONE
        }
    }

    private fun showPhotoMessage(messageRes: Int) {
        context?.let { Toast.makeText(it, messageRes, Toast.LENGTH_LONG).show() }
    }

    private fun navigateToLogin() {
        startActivity(
            Intent(requireContext(), LoginActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK,
            ),
        )
        requireActivity().finish()
    }

    private fun onEditOptionClicked(view: View) {
        (activity as? HomeActivity)?.showProfileEdit()
    }

    private fun onLogoutButtonClicked(view: View) {
        viewModel.logout(
            onComplete = ::navigateToLogin,
            onError = ::showLogoutError,
        )
    }

    private fun showLogoutError(error: Exception) {
        AppLogger.error(AppLogger.SESSION_TAG, "profile_logout_failed", error)
        Toast.makeText(requireContext(), R.string.profile_logout_failed, Toast.LENGTH_LONG).show()
    }
}
