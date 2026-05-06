package com.temurx.checkly.data

object TaskStatus {
    const val NOT_YET_AVAILABLE = "NOT_YET_AVAILABLE"
    const val AVAILABLE = "AVAILABLE"
    const val IN_PROGRESS = "IN_PROGRESS"
    const val FINISHED = "FINISHED"
    const val OVERDUE = "OVERDUE"

    /** Parses any historical wire form into the canonical underscore form. */
    fun fromWire(raw: String?): String = when (raw?.trim()?.uppercase()?.replace(' ', '_')) {
        "NOT_YET_AVAILABLE" -> NOT_YET_AVAILABLE
        "AVAILABLE" -> AVAILABLE
        "IN_PROGRESS", "STARTED" -> IN_PROGRESS
        "FINISHED", "COMPLETED" -> FINISHED
        "OVERDUE" -> OVERDUE
        else -> NOT_YET_AVAILABLE
    }
}
