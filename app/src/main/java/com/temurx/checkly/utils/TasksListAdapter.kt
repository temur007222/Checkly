package com.temurx.checkly.utils

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.temurx.checkly.R
import com.temurx.checkly.data.Task
import java.text.SimpleDateFormat
import java.util.Locale

class TasksListAdapter(
    tasksList: MutableList<Task>,
    private val onTaskClick: (Task) -> Unit
) : RecyclerView.Adapter<TasksListAdapter.TaskViewHolder>() {

    private val tasks = mutableListOf<Task>() // keep mutable list inside

    inner class TaskViewHolder(val binding: View) : RecyclerView.ViewHolder(binding)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.task_list_item, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]

        val statusText = holder.itemView.findViewById<TextView>(R.id.taskStatus)
        val titleText = holder.itemView.findViewById<TextView>(R.id.taskTitle)
        val dueTimeText = holder.itemView.findViewById<TextView>(R.id.taskTratTime)
        val rootLayout = holder.itemView.findViewById<LinearLayout>(R.id.taskItemLayout)

        statusText.text = task.status
        titleText.text = task.title

        val formattedTime = task.dueTime?.toDate()?.let {
            val sdf = SimpleDateFormat("dd.MM.yyyy 'at' HH:mm", Locale.getDefault())
            sdf.format(it)
        } ?: "No due date"
        dueTimeText.text = "Due Time: $formattedTime"

        val bgColor = when {
            task.isCompleted -> ContextCompat.getColor(holder.itemView.context, R.color.light_green)
            task.status == "IN PROGRESS" -> ContextCompat.getColor(holder.itemView.context, R.color.yellow)
            task.dueTime != null && task.dueTime < Timestamp.now() && !task.isCompleted ->
                ContextCompat.getColor(holder.itemView.context, R.color.red)
            else -> ContextCompat.getColor(holder.itemView.context, android.R.color.white)
        }
        rootLayout.setBackgroundColor(bgColor)

        holder.itemView.setOnClickListener { onTaskClick(task) }
    }

    override fun getItemCount(): Int = tasks.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateTasks(newTasks: List<Task>) {
        val now = Timestamp.now()

        val sorted = newTasks.sortedWith(compareBy<Task> { task ->
            when (task.status) {
                "IN PROGRESS" -> 0
                "AVAILABLE" -> 1
                "NOT YET AVAILABLE" -> 2
                "OVERDUE" -> 3
                "FINISHED" -> 4
                else -> 5
            }
        }.thenComparator { t1, t2 ->
            if (t1.status == "NOT YET AVAILABLE" && t2.status == "NOT YET AVAILABLE") {
                val diff1 = kotlin.math.abs((t1.startTime?.toDate()?.time ?: Long.MAX_VALUE) - now.toDate().time)
                val diff2 = kotlin.math.abs((t2.startTime?.toDate()?.time ?: Long.MAX_VALUE) - now.toDate().time)
                diff1.compareTo(diff2)
            } else 0
        })

        tasks.clear()
        tasks.addAll(sorted)
        notifyDataSetChanged()
    }
}


