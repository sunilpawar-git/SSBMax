package com.example.ssbmax.ui.progress

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.ssbmax.databinding.FragmentProgressBinding

class ProgressFragment : Fragment() {

    private var _binding: FragmentProgressBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProgressBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setupProgressData()
        return root
    }

    private fun setupProgressData() {
        // Sample progress data - in a real app, this would come from a database or shared preferences
        val overallProgress = 75 // percentage
        val studyStreak = 12 // days
        val testsCompleted = 8
        val studyHours = 45

        // Update UI elements
        binding.progressBarOverall.progress = overallProgress
        binding.textViewOverallProgress.text = "$overallProgress%"

        binding.textViewStudyStreak.text = studyStreak.toString()
        binding.textViewTestsCompleted.text = testsCompleted.toString()
        binding.textViewStudyHours.text = studyHours.toString()

        // Set up progress for different categories
        setupCategoryProgress()
    }

    private fun setupCategoryProgress() {
        // Psychology Tests Progress
        binding.progressBarPsychology.progress = 80
        binding.textViewPsychologyProgress.text = "80%"

        // GTO Progress
        binding.progressBarGto.progress = 65
        binding.textViewGtoProgress.text = "65%"

        // Interview Progress
        binding.progressBarInterview.progress = 70
        binding.textViewInterviewProgress.text = "70%"

        // General Knowledge Progress
        binding.progressBarGk.progress = 85
        binding.textViewGkProgress.text = "85%"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

