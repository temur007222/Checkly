package com.temurx.checkly.data

import com.google.firebase.Timestamp

data class Staff(

    var fullName: String = "",
    var overdueCount: Int = 0,
    val photoUrl: String = "https://i.pinimg.com/1200x/79/9e/02/799e023c66397048a739096c6244ba4a.jpg",
    val role: String = "",
    val staffId: String = "",
    var totalTask: Int = 0,
    var punctualityRate: Int? = 100,
    val createdAt: Timestamp? = Timestamp.now()
)
