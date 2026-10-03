package com.android.argosapp.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.argosapp.databinding.ItemHomeEventBinding
import com.android.argosapp.model.ApplicationWithdrawal

class HomeFragmentApplicationsAdapter(
    private var applications: List<HomeFragmentApplication> = emptyList(),
    private val onApplicationClick: (ApplicationWithdrawal) -> Unit,
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
        return ApplicationViewHolder(binding, onApplicationClick)
    }

    override fun onBindViewHolder(holder: ApplicationViewHolder, position: Int) {
        holder.bind(applications[position])
    }

    override fun getItemCount(): Int = applications.size

    class ApplicationViewHolder(
        private val binding: ItemHomeEventBinding,
        private val onApplicationClick: (ApplicationWithdrawal) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(application: HomeFragmentApplication) {
            val lotName = application.applicationWithdrawal.lotName?.takeIf { it.isNotBlank() }
            binding.eventLotName.text = lotName.orEmpty()
            binding.eventLotName.visibility = if (lotName == null) View.GONE else View.VISIBLE
            binding.eventTitle.text = application.title
            binding.eventTitle.setTextColor(application.titleColor)
            binding.eventDetail.text = application.detail
            binding.eventApplicationDate.text = application.dateLabel
            binding.eventAccent.setBackgroundColor(application.accentColor)
            binding.eventCard.setOnClickListener {
                onApplicationClick(application.applicationWithdrawal)
            }
        }
    }
}

data class HomeFragmentApplication(
    val title: String,
    val detail: String,
    val dateLabel: String,
    val titleColor: Int,
    val accentColor: Int,
    val applicationWithdrawal: ApplicationWithdrawal,
)
