package com.example.ssbmax.ui.tests

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.ssbmax.databinding.FragmentGtoTestBinding

class GTOTestFragment : Fragment() {

    private var _binding: FragmentGtoTestBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGtoTestBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setupGTOTest()
        return root
    }

    private fun setupGTOTest() {
        binding.testTitle.text = "Group Testing Officer (GTO) Tests"
        binding.testDescription.text = """
            GTO tests assess your leadership, teamwork, and problem-solving abilities in group settings.
            
            Test Components:
            • Group Discussion (GD) - 20-30 minutes
            • Group Planning Exercise (GPE) - 20 minutes
            • Progressive Group Task (PGT) - 40 minutes
            • Group Obstacle Race (GOR) - 40 minutes
            • Half Group Task (HGT) - 20 minutes
            • Individual Obstacles - 3 minutes each
            • Command Task - 15 minutes
            • Final Group Task (FGT) - 15 minutes
            • Lecturette - 3 minutes
            
            Key Qualities to Display:
            • Leadership and initiative
            • Cooperation and team spirit
            • Problem-solving approach
            • Physical and mental courage
            • Effective communication
        """.trimIndent()

        binding.startTestButton.setOnClickListener {
            // Start GTO test logic
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Start GTO Tests")
                .setMessage("Are you ready to begin the Group Testing Officer tests?")
                .setPositiveButton("Start") { _, _ ->
                    // Navigate to test activity
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
