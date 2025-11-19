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
import com.temurx.checkly.databinding.FragmentTasksScreenBinding
import com.temurx.checkly.utils.TasksListAdapter

class TasksScreen : Fragment() {

    private var _binding: FragmentTasksScreenBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: TasksListAdapter
    private val tasksList = mutableListOf<Task>()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance() // To get current user

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTasksScreenBinding.inflate(inflater, container, false)

        // Set up adapter
        adapter = TasksListAdapter(tasksList) { task ->
            val bundle = Bundle().apply {
                putString("taskId", task.id)               // Task ID
            }
            findNavController().navigate(R.id.action_tasksScreen_to_taskDetails, bundle)
        }
        // RecyclerView setup
        binding.taskListAdapter.layoutManager = LinearLayoutManager(requireContext())
        binding.taskListAdapter.adapter = adapter

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
                if (e != null) {
                    Toast.makeText(requireContext(), "Error loading tasks", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val newTasks = mutableListOf<Task>()
                    for (doc in snapshot.documents) {
                        val task = doc.toObject(Task::class.java)
                        if (task != null) {
                            task.id = doc.id // ensure the ID is set for navigation
                            newTasks.add(task)
                        }
                    }
                    // ✅ Always update via adapter method (with sorting)
                    adapter.updateTasks(newTasks)
                }
            }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}