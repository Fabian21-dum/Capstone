package com.example.capstone.ui.alphabets

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.capstone.data.model.Alphabet
import com.example.capstone.data.model.Word
import com.example.capstone.databinding.ItemAlphabetBinding
import com.example.capstone.databinding.ItemWordBinding

class AlphabetListAdapter : ListAdapter<Alphabet, AlphabetListAdapter.ViewHolder>(
    DIFF_CALLBACK
) {

    var onItemClickListener: ((Alphabet) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlphabetListAdapter.ViewHolder {
        val binding = ItemAlphabetBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AlphabetListAdapter.ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class ViewHolder(private val binding: ItemAlphabetBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                onItemClickListener?.invoke(getItem(adapterPosition))
            }
        }

        fun bind(alphabet: Alphabet) {}
    }
    
    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Alphabet>() {
            override fun areItemsTheSame(oldItem: Alphabet, newItem: Alphabet): Boolean = oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Alphabet, newItem: Alphabet): Boolean = oldItem.alphabet == newItem.alphabet && oldItem.description == newItem.description
        }
    }
}