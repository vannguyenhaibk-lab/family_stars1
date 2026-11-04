package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Child(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val avatar: String = "🌟",
    val age: Int = 9,
    val starsBalance: Long = 0L,
    val totalStarsEarned: Long = 0L,
    val targetRewardId: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
