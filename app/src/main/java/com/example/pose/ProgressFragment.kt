package com.example.pose

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.pose.databinding.FragmentProgressBinding
import com.example.pose.databinding.ItemWorkoutSessionBinding
import java.text.SimpleDateFormat
import java.util.*

class ProgressFragment : Fragment() {
    private var _binding: FragmentProgressBinding? = null
    private val binding get() = _binding!!
    private val viewModel: WorkoutViewModel by viewModels()
    private val adapter = WorkoutSessionAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProgressBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()
        setupSwipeRefresh()
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refreshSessions()
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ProgressFragment.adapter
            setHasFixedSize(true)
        }
    }

    private fun observeViewModel() {
        viewModel.allSessions.observe(viewLifecycleOwner) { sessions ->
            sessions?.let {
                if (it.isEmpty()) {
                    showEmptyState()
                } else {
                    hideEmptyState()
                    adapter.submitList(it.sortedByDescending { session -> session.date })
                }
            }
        }
    }

    private fun showEmptyState() {
        binding.emptyState.visibility = View.VISIBLE
        binding.recyclerView.visibility = View.GONE
    }

    private fun hideEmptyState() {
        binding.emptyState.visibility = View.GONE
        binding.recyclerView.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class WorkoutSessionAdapter : ListAdapter<WorkoutSession, WorkoutSessionAdapter.ViewHolder>(DIFF_CALLBACK) {
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy - HH:mm", Locale.getDefault())

    class ViewHolder(private val binding: ItemWorkoutSessionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(session: WorkoutSession, dateFormat: SimpleDateFormat) {
            with(binding) {
                textExerciseType.text = formatExerciseType(session.exerciseType)
                textDate.text = dateFormat.format(session.date)
                textReps.text = itemView.context.getString(R.string.reps_format, session.totalReps, session.targetReps)
                textSets.text = itemView.context.getString(R.string.sets_format, session.totalSets, session.targetSets)
                textRestTime.text = itemView.context.getString(R.string.rest_time_format, session.restTimeSeconds)
            }
        }

        private fun formatExerciseType(type: String): String {
            return type.replace("_", " ")
                .lowercase()
                .replaceFirstChar { it.uppercase() }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemWorkoutSessionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), dateFormat)
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<WorkoutSession>() {
            override fun areItemsTheSame(oldItem: WorkoutSession, newItem: WorkoutSession): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: WorkoutSession, newItem: WorkoutSession): Boolean {
                return oldItem == newItem
            }
        }
    }
}