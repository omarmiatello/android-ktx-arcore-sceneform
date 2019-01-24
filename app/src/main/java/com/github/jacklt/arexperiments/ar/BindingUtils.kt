package com.github.jacklt.arexperiments.ar

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding

class BindingHolder<out T : ViewDataBinding>(val binding: T) :
    androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {
    constructor(
        parent: ViewGroup,
        layout: Int
    ) : this(DataBindingUtil.inflate<T>(LayoutInflater.from(parent.context), layout, parent, false))
}