package com.temurx.checkly.utils

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.temurx.checkly.R
import com.temurx.checkly.data.Task
import com.temurx.checkly.data.TaskStatus
import java.text.SimpleDateFormat
import java.util.Locale

class TasksListAdapter(
    @Suppress("UNUSED_PARAMETER") tasksList: MutableList<Task>,
    private val onTaskClick: (Task) -> Unit
) : RecyclerView.Adapter<TasksListAdapter.TaskViewHolder>() {

    private val tasks = mutableListOf<Task>()

    inner class TaskViewHolder(val itemRoot: View) : RecyclerView.ViewHolder(itemRoot)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.task_list_item, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        val title = holder.itemView.findViewById<TextView>(R.id.taskTitle)
        val time = holder.itemView.findViewById<TextView>(R.id.taskTime)
        val photoBadge = holder.itemView.findViewById<TextView>(R.id.photoBadge)

        title.text = task.title
        photoBadge.visibility = if (task.requiresPhoto) View.VISIBLE else View.GONE

        val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
        val startStr = task.startTime?.toDate()?.let { fmt.format(it) } ?: "—"
        val dueStr = fmt.format(task.dueTime.toDate())
        time.text = "$startStr → $dueStr"

        holder.itemView.setOnClickListener { onTaskClick(task) }
    }

    override fun getItemCount(): Int = tasks.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateTasks(newTasks: List<Task>) {
        val now = Timestamp.now()

        val sorted = newTasks.sortedWith(compareBy<Task> { task ->
            when (TaskStatus.fromWire(task.status)) {
                TaskStatus.IN_PROGRESS -> 0
                TaskStatus.AVAILABLE -> 1
                TaskStatus.NOT_YET_AVAILABLE -> 2
                TaskStatus.OVERDUE -> 3
                TaskStatus.FINISHED -> 4
                else -> 5
            }
        }.thenComparator { t1, t2 ->
            val s1 = TaskStatus.fromWire(t1.status)
            val s2 = TaskStatus.fromWire(t2.status)
            if (s1 == TaskStatus.NOT_YET_AVAILABLE && s2 == TaskStatus.NOT_YET_AVAILABLE) {
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
