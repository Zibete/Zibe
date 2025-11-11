package com.zibete.proyecto1.adapters

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.zibete.proyecto1.R

data class OnboardingPage(
    val animationRes: Int,
    val title: String,
    val description: String
)

class OnboardingAdapter(
    private val pages: List<OnboardingPage>
) : RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder>() {

    inner class OnboardingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: LottieAnimationView = view.findViewById(R.id.imageView)
        val title: TextView = view.findViewById(R.id.title)
        val description: TextView = view.findViewById(R.id.parrafo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.slide_layout, parent, false)
        return OnboardingViewHolder(view)
    }

    override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {
        val page = pages[position]
        val context = holder.itemView.context
        val scale = context.resources.displayMetrics.density

        holder.image.setAnimation(page.animationRes)
        holder.title.text = page.title
        holder.title.textSize = 40f
        holder.description.text = page.description

        // Mantengo tu comportamiento especial para "Socializa"
        val size250 = (250 * scale + 0.5f).toInt()
        val params = if (page.title == "Socializa") {
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                size250
            )
        } else {
            LinearLayout.LayoutParams(size250, size250)
        }.apply {
            gravity = Gravity.CENTER
        }

        holder.image.layoutParams = params
    }

    override fun getItemCount(): Int = pages.size
}
