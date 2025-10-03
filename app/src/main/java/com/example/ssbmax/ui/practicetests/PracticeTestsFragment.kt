package com.example.ssbmax.ui.practicetests

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ssbmax.databinding.FragmentPracticeTestsBinding
import com.example.ssbmax.R
import com.google.android.material.card.MaterialCardView

class PracticeTestsFragment : Fragment() {

    private var _binding: FragmentPracticeTestsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPracticeTestsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setupTestGrid()
        return root
    }

    private fun setupTestGrid() {
        val recyclerView: RecyclerView = binding.recyclerViewPracticeTests
        recyclerView.layoutManager = GridLayoutManager(context, 2)

        // Sample practice test categories
        val testCategories = listOf(
            TestCategory("TAT Practice", "Thematic Apperception Test", "Practice writing stories for pictures", 45),
            TestCategory("WAT Practice", "Word Association Test", "Respond to words quickly", 60),
            TestCategory("SRT Practice", "Situation Reaction Test", "React to various situations", 50),
            TestCategory("SDT Practice", "Self Description Test", "Describe yourself from different angles", 40),
            TestCategory("Group Discussion", "GD Practice", "Practice group discussions", 55),
            TestCategory("Mock Interview", "Interview Prep", "Prepare for personal interviews", 35)
        )

        recyclerView.adapter = PracticeTestsAdapter(testCategories) { testCategory ->
            // Handle test category click
            showTestDialog(testCategory)
        }
    }

    private fun showTestDialog(testCategory: TestCategory) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(testCategory.name)
            .setMessage("Ready to start ${testCategory.name}? This test has ${testCategory.questionCount} questions.")
            .setPositiveButton("Start Test") { _, _ ->
                // Navigate to test activity or show test fragment
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Test Started!")
                    .setMessage("Good luck with your ${testCategory.name} practice!")
                    .setPositiveButton("OK", null)
                    .show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

data class TestCategory(
    val name: String,
    val fullName: String,
    val description: String,
    val questionCount: Int
)

class PracticeTestsAdapter(
    private val testCategories: List<TestCategory>,
    private val onItemClick: (TestCategory) -> Unit
) : RecyclerView.Adapter<PracticeTestsAdapter.ViewHolder>() {

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
        }

        val descriptionTextView = android.widget.TextView(parent.context).apply {
            id = android.R.id.text2
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        view.addView(titleTextView)
        view.addView(descriptionTextView)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val testCategory = testCategories[position]
        holder.titleTextView.text = testCategory.name
        holder.descriptionTextView.text = "${testCategory.questionCount} questions"
        holder.cardView.setOnClickListener { onItemClick(testCategory) }
    }

    override fun getItemCount() = testCategories.size
}

