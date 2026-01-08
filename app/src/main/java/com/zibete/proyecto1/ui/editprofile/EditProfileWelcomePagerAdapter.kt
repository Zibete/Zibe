package com.zibete.proyecto1.ui.editprofile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.zibete.proyecto1.databinding.ItemEditProfileWelcomePageBinding

class EditProfileWelcomePagerAdapter(
    private val pages: List<EditProfileWelcomePage>
) : RecyclerView.Adapter<EditProfileWelcomePagerAdapter.PageViewHolder>() {

    inner class PageViewHolder(
        private val binding: ItemEditProfileWelcomePageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(page: EditProfileWelcomePage) {
            binding.tvTitle.setText(page.titleRes)
            binding.tvBody.setText(page.bodyRes)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemEditProfileWelcomePageBinding.inflate(inflater, parent, false)
        return PageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        holder.bind(pages[position])
    }

    override fun getItemCount(): Int = pages.size
}
