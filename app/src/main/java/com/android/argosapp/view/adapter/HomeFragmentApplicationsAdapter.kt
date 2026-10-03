package com.android.argosapp.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.recyclerview.widget.RecyclerView
import com.android.argosapp.databinding.ItemHomeEventBinding

class HomeFragmentApplicationsAdapter(
    private var applications: List<HomeFragmentApplication> = emptyList(),
) : RecyclerView.Adapter<HomeFragmentApplicationsAdapter.ApplicationViewHolder>() {

    fun submitList(applications: List<HomeFragmentApplication>) {
        this.applications = applications
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ApplicationViewHolder {
        val binding = ItemHomeEventBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return ApplicationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ApplicationViewHolder, position: Int) {
        holder.bind(applications[position])
    }

    override fun getItemCount(): Int = applications.size

    class ApplicationViewHolder(
        private val binding: ItemHomeEventBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(application: HomeFragmentApplication) {
            binding.eventTitle.text = application.title
            binding.eventTitle.setTextColor(application.titleColor)
            binding.eventDetail.text = application.detail
            binding.eventApplicationDate.text = application.dateLabel
            binding.eventAccent.setBackgroundColor(application.accentColor)
        }
    }
}

data class HomeFragmentApplication(
    val title: String,
    val detail: String,
    val dateLabel: String,
    val titleColor: Int,
    val accentColor: Int,
)
