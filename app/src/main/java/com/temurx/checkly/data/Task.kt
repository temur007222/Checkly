package com.temurx.checkly.data

import com.google.firebase.Timestamp

data class Task(
    var id: String = "",
    val title: String = "",
    val description: String = "",
    val startTime: Timestamp? = null,
    val dueTime: Timestamp = Timestamp.now(),
    val createdAt: Timestamp = Timestamp.now(),
    val completedAt: Timestamp? = null,
    val finishedAt: Timestamp? = null,
    val updatedAt: Timestamp = Timestamp.now(),
    var isCompleted: Boolean = false,
    val startedAt: Timestamp? = null,
    val status: String = TaskStatus.NOT_YET_AVAILABLE,
    val photoUrls: List<String> = emptyList(),
    val requiresPhoto: Boolean = false,
    val createdBy: String = "Oybek"
)