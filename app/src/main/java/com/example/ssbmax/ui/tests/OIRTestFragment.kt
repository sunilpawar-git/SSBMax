package com.example.ssbmax.ui.tests

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.ssbmax.databinding.FragmentOirTestBinding

class OIRTestFragment : Fragment() {

    private var _binding: FragmentOirTestBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOirTestBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setupOIRTest()
        return root
    }

    private fun setupOIRTest() {
        binding.testTitle.text = "Officer Intelligence Rating (OIR) Test"
        binding.testDescription.text = """
            The OIR test evaluates your reasoning ability, verbal and non-verbal intelligence.
            
            Test Structure:
            • Verbal Reasoning - 40 questions
            • Non-Verbal Reasoning - 40 questions  
            • Numerical Ability - 40 questions
            • Time Duration: 40 minutes
            
            Instructions:
            • Read each question carefully
            • Choose the best answer from given options
            • Manage your time effectively
            • No negative marking
        """.trimIndent()

        binding.startTestButton.setOnClickListener {
            // Start OIR test logic
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Start OIR Test")
                .setMessage("Are you ready to begin the Officer Intelligence Rating test?")
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
