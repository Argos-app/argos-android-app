package com.android.argosapp.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.android.argosapp.databinding.ItemChatSessionBinding
import com.android.argosapp.model.ChatSession

class ChatSessionAdapter(
    private val formatDate: (Long) -> String,
    private val onClick: (String) -> Unit,
) : ListAdapter<ChatSession, ChatSessionAdapter.Holder>(DIFF) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
        ItemChatSessionBinding.inflate(LayoutInflater.from(parent.context), parent, false),
    )

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val session = getItem(position)
        holder.binding.sessionTitle.isVisible = !session.title.isNullOrBlank()
        holder.binding.sessionTitle.text = session.title.orEmpty()
        holder.binding.sessionDate.text = formatDate(session.createdAt)
        holder.binding.root.setOnClickListener { onClick(session.id) }
    }

    class Holder(val binding: ItemChatSessionBinding) : RecyclerView.ViewHolder(binding.root)

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<ChatSession>() {
            override fun areItemsTheSame(oldItem: ChatSession, newItem: ChatSession) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: ChatSession, newItem: ChatSession) = oldItem == newItem
        }
    }
}
