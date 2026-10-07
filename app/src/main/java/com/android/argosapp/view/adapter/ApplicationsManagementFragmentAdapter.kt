package com.android.argosapp.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.android.argosapp.R
import com.android.argosapp.databinding.ItemApplicationsManagementBinding
import com.android.argosapp.model.Application
import java.time.format.DateTimeFormatter
import java.util.Locale

class ApplicationsManagementFragmentAdapter(
    private var applications: List<Application> = emptyList(),
    private val onApplicationClick: (Application) -> Unit,
) : RecyclerView.Adapter<ApplicationsManagementFragmentAdapter.ApplicationViewHolder>() {

    fun submitList(applications: List<Application>) {
        this.applications = applications
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ApplicationViewHolder {
        val binding = ItemApplicationsManagementBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return ApplicationViewHolder(binding, onApplicationClick)
    }

    override fun onBindViewHolder(holder: ApplicationViewHolder, position: Int) {
        holder.bind(applications[position])
    }

    override fun getItemCount(): Int {
        return applications.size
    }

    class ApplicationViewHolder(
        private val binding: ItemApplicationsManagementBinding,
        private val onApplicationClick: (Application) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(application: Application) {
            val context = binding.root.context
            val status = application.status.orEmpty()
            val isCompleted = status.equals("Concluida", ignoreCase = true) ||
                status.equals("Concluída", ignoreCase = true)
            val isPending = status.equals("Pendente", ignoreCase = true)

            binding.applicationMedicineName.text = application.medicineName.orEmpty()
            binding.applicationAnimalCode.text = context.getString(
                R.string.applications_card_animal_code,
                application.earCode ?: context.getString(R.string.home_calendar_unknown_value),
            )
            binding.applicationLotName.text = context.getString(
                R.string.applications_card_lot_name,
                application.lotName ?: application.lotCode
                    ?: context.getString(R.string.home_calendar_unknown_value),
            )
            binding.applicationDate.text = context.getString(
                R.string.applications_card_date,
                application.applicationDate?.toLocalDate()?.format(DATE_FORMATTER)
                    ?: context.getString(R.string.home_calendar_unknown_value),
            )
            binding.applicationStatus.text = when {
                isCompleted -> context.getString(R.string.applications_status_completed)
                isPending -> context.getString(R.string.applications_status_pending)
                else -> status
            }
            binding.applicationStatusChip.setCardBackgroundColor(
                ContextCompat.getColor(
                    context,
                    when {
                        isCompleted -> R.color.argos_accent
                        isPending -> R.color.argos_notice_important
                        else -> R.color.argos_muted
                    },
                ),
            )
            binding.applicationStatusChip.visibility =
                if (status.isBlank()) View.GONE else View.VISIBLE
            binding.root.setOnClickListener {
                onApplicationClick(application)
            }
        }

        companion object {
            private val DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("pt", "BR"))
        }
    }
}
