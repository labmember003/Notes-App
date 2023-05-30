package com.falcon.notesapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.falcon.notesapp.R
import com.falcon.notesapp.models.OnBoardingItem

class OnBoardingItemAdapter(private val onBoardingItems: List<OnBoardingItem>): RecyclerView.Adapter<OnBoardingItemAdapter.OnboardingItemViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingItemViewHolder {
        return OnboardingItemViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.onboarding_item_container,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: OnboardingItemViewHolder, position: Int) {
        holder.bind(onBoardingItems[position])
    }

    override fun getItemCount(): Int {
        return onBoardingItems.size
    }

    inner class OnboardingItemViewHolder(view: View): RecyclerView.ViewHolder(view) {
        private val textTitle = view.findViewById<TextView>(R.id.textTitle)
        private val textDescription = view.findViewById<TextView>(R.id.textDescription)
        private val animationView = view.findViewById<LottieAnimationView>(R.id.animationView)

        fun bind(onBoardingItems: OnBoardingItem) {
            textTitle.text = onBoardingItems.title
            textDescription.text = onBoardingItems.description
            animationView.visibility = View.VISIBLE
            animationView.setAnimation(onBoardingItems.animation)
            animationView.playAnimation()
        }
    }
}