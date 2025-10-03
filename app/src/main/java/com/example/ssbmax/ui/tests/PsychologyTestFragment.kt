package com.example.ssbmax.ui.tests

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.ssbmax.databinding.FragmentPsychologyTestBinding

class PsychologyTestFragment : Fragment() {

    private var _binding: FragmentPsychologyTestBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPsychologyTestBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setupPsychologyTest()
        return root
    }

    private fun setupPsychologyTest() {
        binding.testTitle.text = "Psychology Tests"
        binding.testDescription.text = """
            Psychology tests evaluate your personality, mental abilities, and psychological traits.
            
            Test Components:
            • Thematic Apperception Test (TAT) - 12 pictures, 4 minutes each
            • Word Association Test (WAT) - 60 words, 15 seconds each
            • Situation Reaction Test (SRT) - 60 situations, 30 minutes
            • Self Description Test (SDT) - 5 parts, 15 minutes
            
            Key Points:
            • Be natural and spontaneous
            • Show positive personality traits
            • Demonstrate leadership qualities
            • Maintain consistency across tests
            • Think from an officer's perspective
        """.trimIndent()

        binding.startTestButton.setOnClickListener {
            // Start Psychology test logic
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Start Psychology Tests")
                .setMessage("Are you ready to begin the Psychology test battery?")
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
