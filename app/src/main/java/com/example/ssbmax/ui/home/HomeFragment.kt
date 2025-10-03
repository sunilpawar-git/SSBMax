package com.example.ssbmax.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ssbmax.databinding.FragmentHomeBinding
import com.example.ssbmax.R
import com.google.android.material.card.MaterialCardView

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setupDashboard()
        return root
    }

    private fun setupDashboard() {
        // Set welcome message
        binding.textViewWelcome.text = "Welcome to SSBMax!"

        // Set up quick action cards
        setupQuickActions()

        // Set up today's tip
        setupDailyTip()

        // Set up recent activity
        setupRecentActivity()
    }

    private fun setupQuickActions() {
        val recyclerView: RecyclerView = binding.recyclerViewQuickActions
        recyclerView.layoutManager = GridLayoutManager(context, 2)

        val quickActions = listOf(
            QuickAction("Study Materials", "Access comprehensive study guides", com.example.ssbmax.R.color.purple_500),
            QuickAction("Practice Tests", "Take mock tests", com.example.ssbmax.R.color.teal_200),
            QuickAction("Progress Tracker", "View your progress", com.example.ssbmax.R.color.purple_700),
            QuickAction("Tips & Tricks", "Get daily tips", com.example.ssbmax.R.color.teal_700)
        )

        recyclerView.adapter = QuickActionsAdapter(quickActions) { action ->
            // Handle quick action click - navigate to respective fragment
            when (action.title) {
                "Study Materials" -> navigateToFragment("study_materials")
                "Practice Tests" -> navigateToFragment("practice_tests")
                "Progress Tracker" -> navigateToFragment("progress")
                "Tips & Tricks" -> navigateToFragment("tips")
            }
        }
    }

    private fun navigateToFragment(fragmentName: String) {
        // This would typically use Navigation component
        // For now, we'll show a simple toast or dialog
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Navigation")
            .setMessage("Navigate to $fragmentName")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun setupDailyTip() {
        val dailyTip = "Remember: Stay confident and be yourself during SSB interviews. Authenticity is key to success."
        binding.textViewDailyTip.text = dailyTip
    }

    private fun setupRecentActivity() {
        val recentActivities = listOf(
            "Completed TAT practice test",
            "Studied GTO planning exercises",
            "Reviewed interview questions",
            "Updated progress tracker"
        )

        binding.textViewRecentActivity.text = recentActivities.joinToString("\n• ", "• ")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

data class QuickAction(
    val title: String,
    val description: String,
    val colorRes: Int
)

class QuickActionsAdapter(
    private val actions: List<QuickAction>,
    private val onItemClick: (QuickAction) -> Unit
) : RecyclerView.Adapter<QuickActionsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardView: MaterialCardView = view as MaterialCardView
        val titleTextView: android.widget.TextView = view.findViewById(android.R.id.text1)
        val descriptionTextView: android.widget.TextView = view.findViewById(android.R.id.text2)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = MaterialCardView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            radius = 8f
            setContentPadding(16, 16, 16, 16)
        }

        val titleTextView = android.widget.TextView(parent.context).apply {
            id = android.R.id.text1
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(16, 16, 16, 8)
            textSize = 16f
            setTextColor(parent.context.getColor(android.R.color.white))
        }

        val descriptionTextView = android.widget.TextView(parent.context).apply {
            id = android.R.id.text2
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(16, 0, 16, 16)
            textSize = 12f
            setTextColor(parent.context.getColor(android.R.color.white))
        }

        view.apply {
            addView(titleTextView)
            addView(descriptionTextView)
        }

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val action = actions[position]
        holder.titleTextView.text = action.title
        holder.descriptionTextView.text = action.description
        holder.cardView.setCardBackgroundColor(holder.itemView.context.getColor(action.colorRes))
        holder.cardView.setOnClickListener { onItemClick(action) }
    }

    override fun getItemCount() = actions.size
}