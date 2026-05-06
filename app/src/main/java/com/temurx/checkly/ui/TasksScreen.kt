package com.temurx.checkly.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.temurx.checkly.R
import com.temurx.checkly.data.Task
import com.temurx.checkly.data.TaskStatus
import com.temurx.checkly.databinding.FragmentTasksScreenBinding
import com.temurx.checkly.utils.TasksListAdapter

class TasksScreen : Fragment() {

    private var _binding: FragmentTasksScreenBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: TasksListAdapter
    private val tasksList = mutableListOf<Task>()
    private val fullList = mutableListOf<Task>()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private enum class Filter { ALL, NOW, DONE }
    private var currentFilter: Filter = Filter.ALL

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTasksScreenBinding.inflate(inflater, container, false)

        adapter = TasksListAdapter(tasksList) { task ->
            val bundle = Bundle().apply {
                putString("taskId", task.id)
            }
            findNavController().navigate(R.id.action_tasksScreen_to_taskDetails, bundle)
        }
        binding.taskListAdapter.layoutManager = LinearLayoutManager(requireContext())
        binding.taskListAdapter.adapter = adapter

        binding.filterChips.setOnCheckedStateChangeListener { _, checkedIds ->
            currentFilter = when (checkedIds.firstOrNull()) {
                R.id.chipNow -> Filter.NOW
                R.id.chipDone -> Filter.DONE
                else -> Filter.ALL
            }
            applyFilter()
        }

        fetchStaffTasks()

        return binding.root
    }

    private fun fetchStaffTasks() {
        val currentUser = auth.currentUser ?: return
        val staffId = currentUser.uid

        db.collection("staff_task")
            .document(staffId)
            .collection("tasks")
            .addSnapshotListener { snapshot, e ->
                if (_binding == null) return@addSnapshotListener
                if (e != null) {
                    Toast.makeText(requireContext(), getString(R.string.tasks_load_error), Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    fullList.clear()
                    for (doc in snapshot.documents) {
                        val task = doc.toObject(Task::class.java)
                        if (task != null) {
                            task.id = doc.id
                            fullList.add(task)
                        }
                    }
                    binding.heroCount.text = getString(R.string.tasks_count, fullList.size)
                    applyFilter()
                }
            }
    }

    private fun applyFilter() {
        val filtered = when (currentFilter) {
            Filter.ALL -> fullList
            Filter.NOW -> fullList.filter {
                val s = TaskStatus.fromWire(it.status)
                s == TaskStatus.AVAILABLE || s == TaskStatus.IN_PROGRESS
            }
            Filter.DONE -> fullList.filter {
                TaskStatus.fromWire(it.status) == TaskStatus.FINISHED
            }
        }
        adapter.updateTasks(filtered)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
