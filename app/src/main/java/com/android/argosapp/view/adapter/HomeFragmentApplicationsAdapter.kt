package com.android.argosapp.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import com.android.argosapp.databinding.ItemHomeEventBinding

class HomeFragmentApplicationsAdapter(
    private val applications: List<HomeFragmentApplication>,
) : RecyclerView.Adapter<HomeFragmentApplicationsAdapter.ApplicationViewHolder>() {

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
            binding.eventTitle.setText(application.titleRes)
            binding.eventTitle.setTextColor(application.titleColor)
            binding.eventDetail.setText(application.detailRes)
            binding.eventApplicationDate.setText(application.applicationDateRes)
            binding.eventAccent.setBackgroundColor(application.accentColor)
        }
    }
}

data class HomeFragmentApplication(
    @StringRes val titleRes: Int,
    @StringRes val detailRes: Int,
    @StringRes val applicationDateRes: Int,
    @ColorInt val titleColor: Int,
    @ColorInt val accentColor: Int,
)
