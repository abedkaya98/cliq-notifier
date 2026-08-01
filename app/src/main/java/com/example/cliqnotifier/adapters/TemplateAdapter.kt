package com.example.cliqnotifier.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cliqnotifier.R
import com.example.cliqnotifier.models.BankTemplate

class TemplateAdapter(
    private val templates: MutableList<BankTemplate>,
    private val onDeleteClick: (BankTemplate) -> Unit,
    private val onTestClick: (BankTemplate) -> Unit
) : RecyclerView.Adapter<TemplateAdapter.TemplateViewHolder>() {

    class TemplateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvBankName: TextView = itemView.findViewById(R.id.tvBankName)
        val tvTemplatePattern: TextView = itemView.findViewById(R.id.tvTemplatePattern)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
        val btnTestTemplate: Button = itemView.findViewById(R.id.btnTestTemplate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TemplateViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bank_card, parent, false)
        return TemplateViewHolder(view)
    }

    override fun onBindViewHolder(holder: TemplateViewHolder, position: Int) {
        val item = templates[position]
        holder.tvBankName.text = item.bankName
        holder.tvTemplatePattern.text = item.templatePattern

        holder.btnDelete.setOnClickListener { onDeleteClick(item) }
        holder.btnTestTemplate.setOnClickListener { onTestClick(item) }
    }

    override fun getItemCount(): Int = templates.size

    fun updateData(newTemplates: List<BankTemplate>) {
        templates.clear()
        templates.addAll(newTemplates)
        notifyDataSetChanged()
    }
}
