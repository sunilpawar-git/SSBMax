package com.example.ssbmax.ui.tests

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.ssbmax.databinding.FragmentIoTestBinding

class IOTestFragment : Fragment() {

    private var _binding: FragmentIoTestBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIoTestBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setupIOTest()
        return root
    }

    private fun setupIOTest() {
        binding.testTitle.text = "Interview Officer (IO) Test"
        binding.testDescription.text = """
            The Interview Officer conducts personal interviews to assess your personality, knowledge, and suitability for officer rank.
            
            Interview Components:
            • Personal Information - Background, family, education
            • Current Affairs - National and international events
            • Service Knowledge - Armed forces, ranks, traditions
            • Situational Questions - Leadership scenarios
            • Technical Knowledge - Related to your background
            • Hobbies and Interests - Depth of knowledge
            
            Preparation Tips:
            • Be honest and confident
            • Stay updated with current affairs
            • Know your background thoroughly
            • Practice speaking clearly
            • Show enthusiasm for service
            • Demonstrate leadership examples
            
            Duration: 30-45 minutes
        """.trimIndent()

        binding.startTestButton.setOnClickListener {
            // Start IO test logic
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Start IO Interview")
                .setMessage("Are you ready to begin the Interview Officer session?")
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
