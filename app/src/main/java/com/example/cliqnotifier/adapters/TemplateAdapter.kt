package com.example.cliqnotifier.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.cliqnotifier.databinding.ItemBankCardBinding
import com.example.cliqnotifier.models.BankTemplate

class TemplateAdapter(
    private val templates: List<BankTemplate>,
    private val onDeleteClick: (Int) -> Unit
) : RecyclerView.Adapter<TemplateAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemBankCardBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBankCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val template = templates[position]
        holder.binding.tvBankName.text = template.bankName
        holder.binding.tvTemplatePattern.text = template.templatePattern

        holder.binding.btnDelete.setOnClickListener {
            val currentPos = holder.bindingAdapterPosition
            if (currentPos != RecyclerView.NO_POSITION) {
                onDeleteClick(currentPos)
            }
        }
    }

    override fun getItemCount(): Int = templates.size
}
