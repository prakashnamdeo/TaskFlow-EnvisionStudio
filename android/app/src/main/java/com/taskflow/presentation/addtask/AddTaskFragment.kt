package com.taskflow.presentation.addtask

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.taskflow.R
import com.taskflow.databinding.FragmentAddTaskBinding
import com.taskflow.domain.model.Assignee
import com.taskflow.domain.model.Priority
import com.taskflow.presentation.tasklist.TaskListViewModel
import com.taskflow.utils.DateUtils
import dagger.hilt.android.AndroidEntryPoint
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AddTaskFragment : Fragment() {

    private var _binding: FragmentAddTaskBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddTaskViewModel by viewModels()
    private val listViewModel: TaskListViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddTaskBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupInputs()
        setupDatePicker()
        setupPriorityChips()
        setupAssigneeChips()
        setupSubmitButton()
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupInputs() {
        binding.etTitle.doAfterTextChanged { editable ->
            viewModel.onTitleChanged(editable?.toString() ?: "")
        }

        binding.etDescription.doAfterTextChanged { editable ->
            viewModel.onDescriptionChanged(editable?.toString() ?: "")
        }
    }

    private fun setupDatePicker() {
        binding.btnPickDueDate.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.select_date)
                .build()

            datePicker.addOnPositiveButtonClickListener { selectionEpochMs ->
                val localDate = Instant.ofEpochMilli(selectionEpochMs)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                val isoDate = DateUtils.toIsoDateString(localDate)
                viewModel.onDueDateSelected(isoDate)
            }

            datePicker.show(parentFragmentManager, "DUE_DATE_PICKER")
        }

        binding.btnClearDueDate.setOnClickListener {
            viewModel.onDueDateSelected(null)
        }
    }

    private fun setupPriorityChips() {
        binding.chipPriorityLow.setOnClickListener { viewModel.onPrioritySelected(Priority.LOW) }
        binding.chipPriorityMedium.setOnClickListener { viewModel.onPrioritySelected(Priority.MEDIUM) }
        binding.chipPriorityHigh.setOnClickListener { viewModel.onPrioritySelected(Priority.HIGH) }
    }

    private fun setupAssigneeChips() {
        binding.chipAssigneeAarav.setOnClickListener { viewModel.onAssigneeSelected(Assignee.AARAV) }
        binding.chipAssigneePriya.setOnClickListener { viewModel.onAssigneeSelected(Assignee.PRIYA) }
        binding.chipAssigneeRohan.setOnClickListener { viewModel.onAssigneeSelected(Assignee.ROHAN) }
    }

    private fun setupSubmitButton() {
        binding.btnCreateTask.setOnClickListener {
            viewModel.submitTask()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Collect form state
                launch {
                    viewModel.formState.collect { state ->
                        renderFormState(state)
                    }
                }

                // Handle task created event
                launch {
                    viewModel.taskCreatedEvent.collect {
                        Snackbar.make(
                            requireActivity().findViewById(android.R.id.content),
                            R.string.task_created_success,
                            Snackbar.LENGTH_SHORT
                        ).show()
                        // Refresh task list
                        listViewModel.loadTasks()
                        // Return to list screen
                        findNavController().navigateUp()
                    }
                }
            }
        }
    }

    private fun renderFormState(state: AddTaskFormState) {
        // Due Date display
        if (state.dueDate != null) {
            binding.btnPickDueDate.text = DateUtils.formatDisplayDate(state.dueDate)
            binding.btnClearDueDate.visibility = View.VISIBLE
        } else {
            binding.btnPickDueDate.setText(R.string.select_date)
            binding.btnClearDueDate.visibility = View.GONE
        }

        // Priority chips check
        when (state.priority) {
            Priority.LOW -> binding.chipPriorityLow.isChecked = true
            Priority.MEDIUM -> binding.chipPriorityMedium.isChecked = true
            Priority.HIGH -> binding.chipPriorityHigh.isChecked = true
        }

        // Assignee chips check
        when (state.assignee) {
            Assignee.AARAV -> binding.chipAssigneeAarav.isChecked = true
            Assignee.PRIYA -> binding.chipAssigneePriya.isChecked = true
            Assignee.ROHAN -> binding.chipAssigneeRohan.isChecked = true
        }

        // Field errors (Edge Case 4)
        val titleError = state.fieldErrors["title"]
        binding.tilTitle.error = titleError
        binding.tilTitle.isErrorEnabled = !titleError.isNullOrBlank()

        val dueDateError = state.fieldErrors["dueDate"]
        if (!dueDateError.isNullOrBlank()) {
            binding.tvDueDateError.text = dueDateError
            binding.tvDueDateError.visibility = View.VISIBLE
        } else {
            binding.tvDueDateError.visibility = View.GONE
        }

        // General error banner
        if (!state.generalError.isNullOrBlank()) {
            binding.tvFormGeneralError.text = state.generalError
            binding.tvFormGeneralError.visibility = View.VISIBLE
        } else {
            binding.tvFormGeneralError.visibility = View.GONE
        }

        // Submitting loading state
        if (state.isSubmitting) {
            binding.btnCreateTask.isEnabled = false
            binding.btnCreateTask.text = ""
            binding.progressSubmitting.visibility = View.VISIBLE
        } else {
            binding.btnCreateTask.isEnabled = true
            binding.btnCreateTask.setText(R.string.create_task_button)
            binding.progressSubmitting.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
