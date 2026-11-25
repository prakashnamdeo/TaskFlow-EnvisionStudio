package com.taskflow.presentation.tasklist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.taskflow.R
import com.taskflow.databinding.FragmentTaskListBinding
import com.taskflow.domain.model.Assignee
import com.taskflow.domain.model.Task
import com.taskflow.domain.model.TaskFilter
import com.taskflow.domain.model.TaskStatusFilter
import com.taskflow.presentation.common.UiState
import com.taskflow.presentation.tasklist.adapter.TaskAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TaskListFragment : Fragment() {

    private var _binding: FragmentTaskListBinding? = null
    private val binding get() = _binding!!

    // Shared activityViewModels ensures filter state is preserved across navigation (Decision 2)
    private val viewModel: TaskListViewModel by activityViewModels()

    private lateinit var taskAdapter: TaskAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTaskListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSwipeRefresh()
        setupFilters()
        setupButtons()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter(
            onTaskClick = { task ->
                val bundle = bundleOf("taskId" to task.id)
                findNavController().navigate(R.id.action_taskList_to_taskDetail, bundle)
            },
            onToggleClick = { task ->
                viewModel.toggleTaskCompletion(task)
            }
        )

        binding.recyclerViewTasks.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = taskAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeResources(R.color.primary)
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadTasks(isSwipeRefresh = true)
        }
    }

    private fun setupFilters() {
        // Assignee chips
        binding.chipAssigneeAll.setOnClickListener { viewModel.setAssigneeFilter(null) }
        binding.chipAssigneeAarav.setOnClickListener { viewModel.setAssigneeFilter(Assignee.AARAV) }
        binding.chipAssigneePriya.setOnClickListener { viewModel.setAssigneeFilter(Assignee.PRIYA) }
        binding.chipAssigneeRohan.setOnClickListener { viewModel.setAssigneeFilter(Assignee.ROHAN) }

        // Status chips
        binding.chipStatusAll.setOnClickListener { viewModel.setStatusFilter(TaskStatusFilter.ALL) }
        binding.chipStatusActive.setOnClickListener { viewModel.setStatusFilter(TaskStatusFilter.ACTIVE) }
        binding.chipStatusCompleted.setOnClickListener { viewModel.setStatusFilter(TaskStatusFilter.COMPLETED) }
    }

    private fun setupButtons() {
        val openAddTask = {
            findNavController().navigate(R.id.action_taskList_to_addTask)
        }

        binding.btnHeaderAdd.setOnClickListener { openAddTask() }
        binding.fabAddTask.setOnClickListener { openAddTask() }
        binding.btnEmptyAdd.setOnClickListener { openAddTask() }

        binding.btnRetry.setOnClickListener {
            viewModel.loadTasks()
        }

        binding.btnClearFilters.setOnClickListener {
            viewModel.clearFilters()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Collect UI state
                launch {
                    viewModel.uiState.collect { state ->
                        renderUiState(state)
                    }
                }

                // Collect summary stats
                launch {
                    viewModel.summary.collect { summary ->
                        binding.tvCountTotal.text = summary.total.toString()
                        binding.tvCountActive.text = summary.active.toString()
                        binding.tvCountCompleted.text = summary.completed.toString()
                    }
                }

                // Sync filter chips with ViewModel filter state (ensures persistence on return)
                launch {
                    viewModel.filter.collect { filter ->
                        syncChipsWithFilter(filter)
                    }
                }

                // One-shot user messages (e.g. rollback alerts)
                launch {
                    viewModel.userMessage.collect { message ->
                        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun renderUiState(state: UiState<List<Task>>) {
        binding.swipeRefresh.isRefreshing = false

        binding.layoutLoading.visibility = if (state is UiState.Loading) View.VISIBLE else View.GONE
        binding.layoutError.visibility = if (state is UiState.Error) View.VISIBLE else View.GONE
        binding.layoutEmpty.visibility = if (state is UiState.Empty) View.VISIBLE else View.GONE
        binding.layoutEmptyFiltered.visibility = if (state is UiState.EmptyFiltered) View.VISIBLE else View.GONE
        binding.recyclerViewTasks.visibility = if (state is UiState.Success) View.VISIBLE else View.GONE

        when (state) {
            is UiState.Success -> {
                taskAdapter.submitList(state.data)
            }
            is UiState.Error -> {
                binding.tvErrorMessage.text = state.message
            }
            else -> {
                // Handled by visibility toggles above
            }
        }
    }

    private fun syncChipsWithFilter(filter: TaskFilter) {
        when (filter.assignee) {
            null -> binding.chipAssigneeAll.isChecked = true
            Assignee.AARAV -> binding.chipAssigneeAarav.isChecked = true
            Assignee.PRIYA -> binding.chipAssigneePriya.isChecked = true
            Assignee.ROHAN -> binding.chipAssigneeRohan.isChecked = true
        }

        when (filter.status) {
            TaskStatusFilter.ALL -> binding.chipStatusAll.isChecked = true
            TaskStatusFilter.ACTIVE -> binding.chipStatusActive.isChecked = true
            TaskStatusFilter.COMPLETED -> binding.chipStatusCompleted.isChecked = true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
