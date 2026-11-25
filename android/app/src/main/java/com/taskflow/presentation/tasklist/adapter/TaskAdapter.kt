package com.taskflow.presentation.tasklist.adapter

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.taskflow.R
import com.taskflow.databinding.ItemTaskCardBinding
import com.taskflow.domain.model.Priority
import com.taskflow.domain.model.Task

class TaskAdapter(
    private val onTaskClick: (Task) -> Unit,
    private val onToggleClick: (Task) -> Unit
) : ListAdapter<Task, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TaskViewHolder(
        private val binding: ItemTaskCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(task: Task) {
            val context = binding.root.context

            // Title and completed styling
            binding.tvTaskTitle.text = task.title
            if (task.completed) {
                binding.tvTaskTitle.paintFlags = binding.tvTaskTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                binding.tvTaskTitle.setTextColor(ContextCompat.getColor(context, R.color.text_tertiary))
                binding.cardTask.strokeColor = ContextCompat.getColor(context, R.color.card_stroke)
                binding.cardTask.setCardBackgroundColor(ContextCompat.getColor(context, R.color.background))
            } else {
                binding.tvTaskTitle.paintFlags = binding.tvTaskTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                binding.tvTaskTitle.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                binding.cardTask.strokeColor = ContextCompat.getColor(context, R.color.card_stroke)
                binding.cardTask.setCardBackgroundColor(ContextCompat.getColor(context, R.color.surface))
            }

            // Checkbox: avoid trigger loop by detaching listener during binding
            binding.checkboxTask.setOnCheckedChangeListener(null)
            binding.checkboxTask.isChecked = task.completed
            binding.checkboxTask.setOnClickListener {
                onToggleClick(task)
            }

            // Due Date
            binding.tvDueDate.text = task.displayDueDate

            // Assignee
            binding.tvAssignee.text = task.assignee.displayName

            // Priority Badge
            binding.tvPriorityBadge.text = task.priority.displayName
            when (task.priority) {
                Priority.HIGH -> {
                    binding.tvPriorityBadge.setBackgroundResource(R.drawable.bg_priority_high)
                    binding.tvPriorityBadge.setTextColor(ContextCompat.getColor(context, R.color.priority_high))
                }
                Priority.MEDIUM -> {
                    binding.tvPriorityBadge.setBackgroundResource(R.drawable.bg_priority_medium)
                    binding.tvPriorityBadge.setTextColor(ContextCompat.getColor(context, R.color.priority_medium))
                }
                Priority.LOW -> {
                    binding.tvPriorityBadge.setBackgroundResource(R.drawable.bg_priority_low)
                    binding.tvPriorityBadge.setTextColor(ContextCompat.getColor(context, R.color.priority_low))
                }
            }

            // Overdue indicator: only visible if task is active and due date is past
            val isOverdue = task.isOverdue()
            binding.tvOverdueBadge.visibility = if (isOverdue) View.VISIBLE else View.GONE

            // Whole card click
            binding.root.setOnClickListener {
                onTaskClick(task)
            }
        }
    }
}
