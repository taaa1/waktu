package io.ta.waktushalat

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.ta.waktushalat.databinding.FragmentItemBinding

class MyTiRecyclerViewAdapter(
    private val title: Array<String>,
    private val dt: ArrayList<String>
) : RecyclerView.Adapter<MyTiRecyclerViewAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        return ViewHolder(
            FragmentItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.title.text = title[position]
        holder.vale.text = dt[position]
    }

    override fun getItemCount(): Int = dt.size

    inner class ViewHolder(binding: FragmentItemBinding) : RecyclerView.ViewHolder(binding.root) {
        val title: TextView = binding.timeTitle
        val vale: TextView = binding.time
    }

}