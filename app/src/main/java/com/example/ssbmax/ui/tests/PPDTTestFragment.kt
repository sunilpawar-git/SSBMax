package com.example.ssbmax.ui.tests

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.ssbmax.databinding.FragmentPpdtTestBinding

class PPDTTestFragment : Fragment() {

    private var _binding: FragmentPpdtTestBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPpdtTestBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setupPPDTTest()
        return root
    }

    private fun setupPPDTTest() {
        binding.testTitle.text = "Picture Perception & Discussion Test (PPDT)"
        binding.testDescription.text = """
            PPDT assesses your perception, imagination, and group discussion skills.
            
            Test Structure:
            • Picture observation - 30 seconds
            • Story writing - 4 minutes
            • Group discussion - 10-15 minutes
            
            Instructions:
            • Observe the picture carefully
            • Write a meaningful story with characters, situation, and action
            • Participate actively in group discussion
            • Be confident and express your views clearly
            
            Tips:
            • Focus on positive themes
            • Show leadership qualities in discussion
            • Listen to others and build on their ideas
        """.trimIndent()

        binding.startTestButton.setOnClickListener {
            // Start PPDT test logic
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Start PPDT Test")
                .setMessage("Are you ready to begin the Picture Perception & Discussion Test?")
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
