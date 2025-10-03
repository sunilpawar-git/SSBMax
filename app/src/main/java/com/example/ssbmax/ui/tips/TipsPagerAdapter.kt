package com.example.ssbmax.ui.tips

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class TipsPagerAdapter(
    fragment: Fragment,
    private val tipsData: Map<String, List<Tip>>
) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = tipsData.size

    override fun createFragment(position: Int): Fragment {
        val category = when (position) {
            0 -> "Psychology"
            1 -> "GTO"
            2 -> "Interview"
            else -> "Psychology"
        }

        val tips = tipsData[category] ?: emptyList()
        return TipsCategoryFragment.newInstance(category, tips)
    }
}

