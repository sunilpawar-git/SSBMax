package com.example.ssbmax.ui.tips

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ssbmax.databinding.FragmentTipsCategoryBinding
import com.example.ssbmax.R
import com.google.android.material.card.MaterialCardView

class TipsCategoryFragment : Fragment() {

    private var _binding: FragmentTipsCategoryBinding? = null
    private val binding get() = _binding!!

    private var categoryName: String? = null
    private var tips: List<Tip>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            categoryName = it.getString(ARG_CATEGORY_NAME)
            tips = it.getParcelableArrayList(ARG_TIPS, Tip::class.java)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTipsCategoryBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setupRecyclerView()
        return root
    }

    private fun setupRecyclerView() {
        val recyclerView: RecyclerView = binding.recyclerViewTips
        recyclerView.layoutManager = LinearLayoutManager(context)

        tips?.let { tipsList ->
            recyclerView.adapter = TipsAdapter(tipsList)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_CATEGORY_NAME = "category_name"
        private const val ARG_TIPS = "tips"

        fun newInstance(categoryName: String, tips: List<Tip>): TipsCategoryFragment {
            return TipsCategoryFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_CATEGORY_NAME, categoryName)
                    putParcelableArrayList(ARG_TIPS, tips as ArrayList<out android.os.Parcelable>)
                }
            }
        }
    }
}

class TipsAdapter(private val tips: List<Tip>) :
    RecyclerView.Adapter<TipsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardView: MaterialCardView = view as MaterialCardView
        val titleTextView: android.widget.TextView = view.findViewById(android.R.id.text1)
        val contentTextView: android.widget.TextView = view.findViewById(android.R.id.text2)
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
            setTextColor(parent.context.getColor(android.R.color.black))
        }

        val contentTextView = android.widget.TextView(parent.context).apply {
            id = android.R.id.text2
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(16, 0, 16, 16)
            textSize = 14f
            setTextColor(parent.context.getColor(android.R.color.darker_gray))
        }

        view.apply {
            addView(titleTextView)
            addView(contentTextView)
        }

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val tip = tips[position]
        holder.titleTextView.text = tip.title
        holder.contentTextView.text = tip.content
    }

    override fun getItemCount() = tips.size
}

