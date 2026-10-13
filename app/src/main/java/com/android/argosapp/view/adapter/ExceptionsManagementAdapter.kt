package com.android.argosapp.view.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.android.argosapp.R
import com.android.argosapp.databinding.ItemExceptionBinding
import com.android.argosapp.model.AnimalExceptionRecord

class ExceptionsManagementAdapter :
    RecyclerView.Adapter<ExceptionsManagementAdapter.ExceptionViewHolder>() {

    private var exceptions: List<AnimalExceptionRecord> = emptyList()

    fun submitList(exceptions: List<AnimalExceptionRecord>) {
        this.exceptions = exceptions
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExceptionViewHolder {
        val binding = ItemExceptionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return ExceptionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExceptionViewHolder, position: Int) {
        holder.bind(exceptions[position])
    }

    override fun getItemCount(): Int = exceptions.size

    class ExceptionViewHolder(
        private val binding: ItemExceptionBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(exception: AnimalExceptionRecord) {
            val context = binding.root.context
            val statusColor = ContextCompat.getColor(
                context,
                if (exception.isFitForReturn) R.color.argos_accent else R.color.argos_notice_important,
            )
            val statusIcon = ContextCompat.getDrawable(
                context,
                if (exception.isFitForReturn) R.drawable.positive_icon else R.drawable.negative_icon,
            )

            binding.exceptionEarTag.text = context.getString(
                R.string.exceptions_ear_tag,
                exception.earTag,
            )
            binding.exceptionLotName.text = context.getString(
                R.string.exceptions_current_lot,
                exception.lotName,
            )
            binding.exceptionStatus.text = context.getString(
                if (exception.isFitForReturn) R.string.exceptions_status_fit else R.string.exceptions_status_unfit,
            )
            binding.exceptionStatus.setTextColor(statusColor)
            binding.exceptionStatusIcon.setImageDrawable(statusIcon)
            binding.exceptionStatusIcon.imageTintList = ColorStateList.valueOf(statusColor)
            binding.exceptionReason.text = context.getString(
                R.string.exceptions_reason,
                exception.reason,
            )
            binding.exceptionStartDate.text = exception.startDate
            binding.exceptionEndDate.text = exception.endDate
            binding.exceptionReturnProgressLabel.text = context.getString(
                R.string.exceptions_progress_percent,
                exception.returnProgress,
            )
            binding.exceptionReturnProgress.progress = exception.returnProgress
        }
    }
}
