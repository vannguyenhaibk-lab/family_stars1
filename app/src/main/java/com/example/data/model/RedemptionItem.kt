package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class RedemptionItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val childId: String = "",
    val childName: String = "",
    val rewardId: String = "",
    val rewardTitle: String = "",
    val starCost: Long = 10L,
    val status: String = "REQUESTED", // REQUESTED, FULFILLED
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
