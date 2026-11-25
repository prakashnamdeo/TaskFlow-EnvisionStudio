package com.taskflow.presentation.taskdetail

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.taskflow.R
import com.taskflow.databinding.FragmentTaskDetailBinding
import com.taskflow.domain.model.Priority
import com.taskflow.domain.model.Task
import com.taskflow.presentation.common.UiState
import com.taskflow.presentation.tasklist.TaskListViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TaskDetailFragment : Fragment() {

    private var _binding: FragmentTaskDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TaskDetailViewModel by viewModels()
    // Shared list viewModel to keep task list data in sync
    private val listViewModel: TaskListViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTaskDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnToggleComplete.setOnClickListener {
            viewModel.toggleCompletion()
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        when (state) {
                            is UiState.Success -> {
                                bindTask(state.data)
                                // Keep listViewModel in sync so back navigation shows updated status
                                listViewModel.loadTasks(isSwipeRefresh = true)
                            }
                            is UiState.Error -> {
                                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                            }
                            else -> {}
                        }
                    }
                }

                launch {
                    viewModel.userMessage.collect { message ->
                        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun bindTask(task: Task) {
        val context = requireContext()

        // Title
        binding.tvDetailTitle.text = task.title

        // Description
        if (!task.description.isNullOrBlank()) {
            binding.tvDetailDescription.text = task.description
            binding.tvDetailDescription.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
        } else {
            binding.tvDetailDescription.text = getString(R.string.no_description_provided)
            binding.tvDetailDescription.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
        }

        // Due date
        binding.tvDetailDueDate.text = task.displayDueDate

        // Assignee
        binding.tvDetailAssignee.text = task.assignee.displayName

        // Priority
        binding.tvDetailPriority.text = task.priority.displayName
        when (task.priority) {
            Priority.HIGH -> {
                binding.tvDetailPriority.setBackgroundResource(R.drawable.bg_priority_high)
                binding.tvDetailPriority.setTextColor(ContextCompat.getColor(context, R.color.priority_high))
            }
            Priority.MEDIUM -> {
                binding.tvDetailPriority.setBackgroundResource(R.drawable.bg_priority_medium)
                binding.tvDetailPriority.setTextColor(ContextCompat.getColor(context, R.color.priority_medium))
            }
            Priority.LOW -> {
                binding.tvDetailPriority.setBackgroundResource(R.drawable.bg_priority_low)
                binding.tvDetailPriority.setTextColor(ContextCompat.getColor(context, R.color.priority_low))
            }
        }

        // Status badge
        if (task.completed) {
            binding.tvDetailStatusBadge.text = getString(R.string.status_completed_text)
            binding.tvDetailStatusBadge.setBackgroundResource(R.drawable.bg_priority_low)
            binding.tvDetailStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.completed_green))
        } else {
            binding.tvDetailStatusBadge.text = getString(R.string.status_active_text)
            binding.tvDetailStatusBadge.setBackgroundResource(R.drawable.bg_priority_medium)
            binding.tvDetailStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.priority_medium))
        }

        // Overdue status (Edge Case 2: completed tasks never show overdue warning)
        val isOverdue = task.isOverdue()
        binding.tvDetailOverdueBadge.visibility = if (isOverdue) View.VISIBLE else View.GONE

        // Completion Action Button (Prominent Action)
        if (task.completed) {
            binding.btnToggleComplete.text = getString(R.string.action_mark_incomplete)
            binding.btnToggleComplete.setIconResource(R.drawable.ic_refresh)
            binding.btnToggleComplete.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.surface_variant)
            )
            binding.btnToggleComplete.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            binding.btnToggleComplete.iconTint = ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.text_primary)
            )
        } else {
            binding.btnToggleComplete.text = getString(R.string.action_mark_complete)
            binding.btnToggleComplete.setIconResource(R.drawable.ic_check_circle)
            binding.btnToggleComplete.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.primary)
            )
            binding.btnToggleComplete.setTextColor(ContextCompat.getColor(context, R.color.on_primary))
            binding.btnToggleComplete.iconTint = ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.on_primary)
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
