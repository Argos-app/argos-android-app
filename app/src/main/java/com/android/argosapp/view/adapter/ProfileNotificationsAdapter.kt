package com.android.argosapp.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.argosapp.R
import com.android.argosapp.databinding.ItemProfileNotificationBinding
import com.android.argosapp.model.NotificationRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProfileNotificationsAdapter : RecyclerView.Adapter<ProfileNotificationsAdapter.Holder>() {
    private var notifications = emptyList<NotificationRecord>()

    fun show(items: List<NotificationRecord>) {
        notifications = items
        notifyDataSetChanged()
    }

    override fun getItemCount() = notifications.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemProfileNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val notification = notifications[position]
        holder.binding.notificationTitle.text = notification.title?.takeIf { it.isNotBlank() }
            ?: holder.itemView.context.getString(R.string.profile_notification_untitled)
        holder.binding.notificationBody.text = notification.body.orEmpty()
        holder.binding.notificationBody.visibility = if (notification.body.isNullOrBlank()) View.GONE else View.VISIBLE
        holder.binding.notificationDate.text = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(notification.receivedAt))
    }

    class Holder(val binding: ItemProfileNotificationBinding) : RecyclerView.ViewHolder(binding.root)
}
