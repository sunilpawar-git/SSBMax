package com.example.ssbmax.ui.tips

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.example.ssbmax.data.SSBDataSource
import com.example.ssbmax.data.SSBCategory
import com.example.ssbmax.databinding.FragmentTipsBinding
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class TipsFragment : Fragment() {

    private var _binding: FragmentTipsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTipsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setupViewPager()
        return root
    }

    private fun setupViewPager() {
        val viewPager: ViewPager2 = binding.viewPagerTips
        val tabLayout: TabLayout = binding.tabLayoutTips

        // Create tips data from SSBDataSource
        val tipsData = mapOf(
            "Psychology" to SSBDataSource.getTipsByCategory(SSBCategory.PSYCHOLOGY).map { tip ->
                Tip(tip.title, tip.content.replace("•", "\n•"))
            },
            "GTO" to SSBDataSource.getTipsByCategory(SSBCategory.GTO).map { tip ->
                Tip(tip.title, tip.content.replace("•", "\n•"))
            },
            "Interview" to SSBDataSource.getTipsByCategory(SSBCategory.INTERVIEW).map { tip ->
                Tip(tip.title, tip.content.replace("•", "\n•"))
            }
        )

        viewPager.adapter = TipsPagerAdapter(this, tipsData)

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Psychology"
                1 -> "GTO"
                2 -> "Interview"
                else -> "Tips"
            }
        }.attach()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

data class Tip(
    val title: String,
    val content: String
)
