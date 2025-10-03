package com.example.ssbmax.ui.studymaterials

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ssbmax.data.SSBDataSource
import com.example.ssbmax.data.SSBStudyMaterial
import com.example.ssbmax.databinding.FragmentStudyMaterialsBinding

class StudyMaterialsFragment : Fragment() {

    private var _binding: FragmentStudyMaterialsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStudyMaterialsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setupRecyclerView()
        return root
    }

    private fun setupRecyclerView() {
        val recyclerView: RecyclerView = binding.recyclerViewStudyMaterials
        recyclerView.layoutManager = LinearLayoutManager(context)

        // Use data from SSBDataSource
        val studyMaterials = SSBDataSource.studyMaterials.map { material ->
            StudyMaterial(material.title, material.description, material.category.name)
        }

        recyclerView.adapter = StudyMaterialsAdapter(studyMaterials)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

data class StudyMaterial(
    val title: String,
    val description: String,
    val category: String
)

class StudyMaterialsAdapter(private val materials: List<StudyMaterial>) :
    RecyclerView.Adapter<StudyMaterialsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleTextView: android.widget.TextView = view.findViewById(android.R.id.text1)
        val descriptionTextView: android.widget.TextView = view.findViewById(android.R.id.text2)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val material = materials[position]
        holder.titleTextView.text = material.title
        holder.descriptionTextView.text = material.description
    }

    override fun getItemCount() = materials.size
}
