package com.temurx.checkly.ui

import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.temurx.checkly.R
import com.temurx.checkly.data.TaskStatus
import com.temurx.checkly.databinding.FragmentTaskDetailsBinding
import com.temurx.checkly.utils.PhotoAdapter
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TaskDetails : Fragment() {
    private var _binding: FragmentTaskDetailsBinding? = null
    private val binding get() = _binding!!
    private lateinit var photoAdapter: PhotoAdapter
    private var photoUri: Uri? = null
    private var currentTaskId: String? = null
    private val db = FirebaseFirestore.getInstance()

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            photoUri?.let { uri ->
                photoAdapter.addPhoto(uri)
                binding.photoText.text = "Add Another Photo"

                // Upload photo to Firestore
                uploadPhotoToFirestore(uri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTaskDetailsBinding.inflate(inflater, container, false)

        // Get taskId from arguments bundle
        currentTaskId = arguments?.getString("taskId")

        // Load task if taskId is available
        currentTaskId?.let { taskId ->
            loadTask(taskId)
        } ?: run {
            Toast.makeText(requireContext(), getString(R.string.task_id_missing), Toast.LENGTH_SHORT).show()
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupPhotoAdapter()
        setupClickListeners()

        binding.toolbar.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupPhotoAdapter() {
        photoAdapter = PhotoAdapter(mutableListOf()) { photoUrl ->
            // Handle photo deletion from Firestore
           // removePhotoFromFirestore(photoUrl)
        }
        binding.photoList.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.photoList.adapter = photoAdapter
    }

    private fun setupClickListeners() {
        // Take photo button
        binding.photoProve.setOnClickListener {
            takePhoto()
        }

        // Start Task button
        binding.startTaskBtn.setOnClickListener {
            handleStartTask()
        }

        // Finish Task button
        binding.finishTaskBtn.setOnClickListener {
            handleFinishTask()
        }
    }

    private fun takePhoto() {
        val photoFile = File(
            requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            "task_${System.currentTimeMillis()}.jpg"
        )

        photoUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            photoFile
        )

        takePictureLauncher.launch(photoUri)
    }

    private fun updatePhotoList(photoUrls: List<String>) {
        photoAdapter.updatePhotos(photoUrls)
    }

    private fun uploadPhotoToFirestore(imageUri: Uri) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val staffId = currentUser.uid
        val taskId = currentTaskId ?: return

        // Here you would typically upload to Firebase Storage first
        // For now, we'll just add the local URI to the photoUrls array
        val photoUrl = imageUri.toString()

        db.collection("staff_task")
            .document(staffId)
            .collection("tasks")
            .document(taskId)
            .update("photoUrls", FieldValue.arrayUnion(photoUrl))
            .addOnSuccessListener {
                Toast.makeText(requireContext(), getString(R.string.task_photo_uploaded_toast), Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), getString(R.string.task_photo_upload_error, e.message ?: ""), Toast.LENGTH_SHORT).show()
            }
    }

    // Load task info from Firestore with real-time updates
    private fun loadTask(taskId: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: run {
            Toast.makeText(requireContext(), getString(R.string.task_user_not_authed), Toast.LENGTH_SHORT).show()
            return
        }
        val staffId = currentUser.uid

        db.collection("staff_task")
            .document(staffId)
            .collection("tasks")
            .document(taskId)
            .addSnapshotListener(requireActivity()) { doc, e ->
                if (e != null) {
                    if (_binding != null) {
                        Toast.makeText(requireContext(), getString(R.string.task_load_error, e.message ?: ""), Toast.LENGTH_SHORT).show()
                    }
                    return@addSnapshotListener
                }

                if (doc != null && doc.exists()) {
                    if (_binding != null) {
                        updateUI(doc)
                    }
                } else {
                    if (_binding != null) {
                        Toast.makeText(requireContext(), getString(R.string.task_id_missing), Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }


    private fun updateUI(doc: DocumentSnapshot) {
        binding.taskTitle.text = doc.getString("title") ?: "No Title"
        binding.taskDescription.text = doc.getString("description") ?: "No Description"

        val status = doc.getString("status") ?: "PENDING"
        binding.taskStatus.text = status

        val startTime = doc.getTimestamp("startTime")
        val requiresPhoto = doc.getBoolean("requiresPhoto") ?: false
        val photoUrls = doc.get("photoUrls") as? List<String> ?: emptyList()

        // Display photo requirement status
        binding.photoStatus.text = if (requiresPhoto) "Yes" else "No"

        // Update button states based on status and time
        when (TaskStatus.fromWire(status)) {
            TaskStatus.AVAILABLE, TaskStatus.NOT_YET_AVAILABLE -> {
                // Check if it's too early to start (more than 10 minutes before start time)
                val canStart = canStartTask(startTime)
                binding.startTaskBtn.isEnabled = canStart
                binding.startTaskBtn.text = if (canStart) "Start Task" else "Too Early"
                binding.finishTaskBtn.isEnabled = false
                binding.finishTaskBtn.text = "Finish Task"
            }
            TaskStatus.IN_PROGRESS -> {
                binding.startTaskBtn.isEnabled = false
                binding.startTaskBtn.text = "Started"

                // Enable finish button for in-progress tasks
                binding.finishTaskBtn.isEnabled = true
                binding.finishTaskBtn.text = "Finish Task"
            }
            TaskStatus.FINISHED -> {
                binding.startTaskBtn.isEnabled = false
                binding.startTaskBtn.text = "Completed"
                binding.finishTaskBtn.isEnabled = false
                binding.finishTaskBtn.text = "Finished"
            }
            TaskStatus.OVERDUE -> {
                // Phase 6 will rewrite this UI; for now mirror the AVAILABLE branch
                // disable-only behavior and surface a simple overdue label.
                binding.taskStatus.text = "Overdue"
                binding.startTaskBtn.isEnabled = false
                binding.startTaskBtn.text = "Start Task"
                binding.finishTaskBtn.isEnabled = false
                binding.finishTaskBtn.text = "Finish Task"
            }
        }

        // Update photo section visibility and status
        if (requiresPhoto) {
            binding.photoProve.visibility = View.VISIBLE
            updatePhotoList(photoUrls)
        } else {
            binding.photoProve.visibility = View.GONE
        }

        // Display times
        val dueTime = doc.getTimestamp("dueTime")
        binding.startTime.text = startTime?.toDate()?.let { formatDateTime(it) } ?: "N/A"
        binding.taskDueTime.text = dueTime?.toDate()?.let { formatDateTime(it) } ?: "N/A"
    }

    private fun canStartTask(startTime: Timestamp?): Boolean {
        if (startTime == null) return true // If no start time set, allow starting

        val now = Timestamp.now()
        val tenMinutesBefore = Timestamp(Date(startTime.toDate().time - 10 * 60 * 1000))

        return now >= tenMinutesBefore
    }

    private fun handleStartTask() {
        val taskId = currentTaskId ?: run {
            Toast.makeText(requireContext(), "Task ID not available", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUser = FirebaseAuth.getInstance().currentUser ?: run {
            Toast.makeText(requireContext(), getString(R.string.task_user_not_authed), Toast.LENGTH_SHORT).show()
            return
        }
        val staffId = currentUser.uid

        // Get task document to check start time
        db.collection("staff_task")
            .document(staffId)
            .collection("tasks")
            .document(taskId)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val startTime = doc.getTimestamp("startTime")

                    // Check if it's too early to start (more than 10 minutes before scheduled time)
                    if (!canStartTask(startTime)) {
                        Toast.makeText(requireContext(), getString(R.string.task_too_early_toast), Toast.LENGTH_LONG).show()
                        return@addOnSuccessListener
                    }

                    // If we can start, update the task status
                    val now = Timestamp.now()
                    updateTaskStatus(staffId, taskId, now)
                } else {
                    Toast.makeText(requireContext(), getString(R.string.task_id_missing), Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), getString(R.string.task_load_error, e.message ?: ""), Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateTaskStatus(staffId: String, taskId: String, now: Timestamp) {
        db.collection("staff_task")
            .document(staffId)
            .collection("tasks")
            .document(taskId)
            .update(
                mapOf(
                    "status" to TaskStatus.IN_PROGRESS,
                    "startedAt" to now
                )
            )
            .addOnSuccessListener {
                Toast.makeText(requireContext(), getString(R.string.task_started_toast), Toast.LENGTH_SHORT).show()
                // UI will be updated automatically through the snapshot listener
                // The button text will remain "Start Task" and status text will show "IN PROGRESS"
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), getString(R.string.task_update_error, e.message ?: ""), Toast.LENGTH_SHORT).show()
            }
    }

    private fun handleFinishTask() {
        val taskId = currentTaskId ?: run {
            Toast.makeText(requireContext(), "Task ID not available", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUser = FirebaseAuth.getInstance().currentUser ?: run {
            Toast.makeText(requireContext(), getString(R.string.task_user_not_authed), Toast.LENGTH_SHORT).show()
            return
        }
        val staffId = currentUser.uid

        // Get task document to check photo requirements
        db.collection("staff_task")
            .document(staffId)
            .collection("tasks")
            .document(taskId)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val requiresPhoto = doc.getBoolean("requiresPhoto") ?: false
                    val photoUrls = doc.get("photoUrls") as? List<String> ?: emptyList()

                    // Check if photo is required but not uploaded
                    if (requiresPhoto && photoUrls.isEmpty()) {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.task_photo_required_toast),
                            Toast.LENGTH_LONG
                        ).show()
                        return@addOnSuccessListener
                    }

                    // If validation passes, finish the task
                    finishTask(staffId, taskId)
                } else {
                    Toast.makeText(requireContext(), getString(R.string.task_id_missing), Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), getString(R.string.task_load_error, e.message ?: ""), Toast.LENGTH_SHORT).show()
            }
    }

    private fun finishTask(staffId: String, taskId: String) {
        val now = Timestamp.now()

        db.collection("staff_task")
            .document(staffId)
            .collection("tasks")
            .document(taskId)
            .update(
                mapOf(
                    "status" to TaskStatus.FINISHED,
                    "finishedAt" to now,
                    "updatedAt" to now,
                    "completedAt" to now,
                    "completed" to true
                )
            )
            .addOnSuccessListener {
                Toast.makeText(requireContext(), getString(R.string.task_finished_toast), Toast.LENGTH_SHORT).show()

                // Navigate back to previous page
                findNavController().popBackStack()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), getString(R.string.task_update_error, e.message ?: ""), Toast.LENGTH_SHORT).show()
            }
    }

    private fun formatDateTime(date: Date): String {
        val formatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        return formatter.format(date)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(taskId: String): TaskDetails {
            val fragment = TaskDetails()
            val bundle = Bundle().apply {
                putString("taskId", taskId)
            }
            fragment.arguments = bundle
            return fragment
        }
    }
}