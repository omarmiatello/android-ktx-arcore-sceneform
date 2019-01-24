package com.github.jacklt.arexperiments.generic

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.jacklt.arexperiments.R
import com.github.jacklt.arexperiments.ar.BindingHolder
import com.github.jacklt.arexperiments.databinding.ItemSimpleContentBinding

data class SimpleItem(val title: String? = null, val description: String? = null, val ref: Any? = null)

interface SimpleItemHandler {
    fun onClick(view: View, item: SimpleItem)
}

class SimpleItemAdapter(
    val items: List<SimpleItem>,
    val handler: (View, SimpleItem) -> Unit
) : RecyclerView.Adapter<BindingHolder<ItemSimpleContentBinding>>() {
    private val itemHandler = object : SimpleItemHandler {
        override fun onClick(view: View, item: SimpleItem) = handler(view, item)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BindingHolder<ItemSimpleContentBinding> =
        BindingHolder<ItemSimpleContentBinding>(
            parent,
            R.layout.item_simple_content
        ).apply {
            binding.handler = itemHandler
        }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: BindingHolder<ItemSimpleContentBinding>, position: Int) {
        val binding = holder.binding
        binding.item = items[position]
    }
}