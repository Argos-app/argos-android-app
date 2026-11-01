package com.android.argosapp.view.fragment

import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import android.os.Bundle
import android.view.View
import com.android.argosapp.R
import com.android.argosapp.databinding.FragmentProfileNotificationsBinding
import com.android.argosapp.view.adapter.ProfileNotificationsAdapter
import com.android.argosapp.viewmodel.ProfileNotificationsViewModel
import com.android.argosapp.util.logging.AppLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class ProfileNotificationsFragment : Fragment(R.layout.fragment_profile_notifications) {
    private val viewModel: ProfileNotificationsViewModel by viewModels()
    private val adapter = ProfileNotificationsAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentProfileNotificationsBinding.bind(view)
        binding.profileNotificationsList.layoutManager = LinearLayoutManager(requireContext())
        binding.profileNotificationsList.adapter = adapter
        binding.profileNotificationsBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!NotificationManagerCompat.from(requireContext()).areNotificationsEnabled()) {
            parentFragmentManager.popBackStack()
        } else {
            val binding = FragmentProfileNotificationsBinding.bind(requireView())
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val notifications = viewModel.load()
                    adapter.show(notifications)
                    binding.profileNotificationsList.visibility = View.VISIBLE
                    binding.profileNotificationsMessage.setText(R.string.profile_notifications_empty)
                    binding.profileNotificationsMessage.visibility = if (notifications.isEmpty()) View.VISIBLE else View.GONE
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    AppLogger.error(AppLogger.REPOSITORY_TAG, "notification_cache_load_failed", error)
                    binding.profileNotificationsList.visibility = View.GONE
                    binding.profileNotificationsMessage.setText(R.string.profile_notifications_load_error)
                    binding.profileNotificationsMessage.visibility = View.VISIBLE
                }
            }
        }
    }

    companion object {
        const val TAG = "profile_notifications"
    }
}
