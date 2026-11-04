package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class EncouragingMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val childId: String = "",
    val taskId: String? = null,
    val taskTitle: String? = null,
    val message: String = "",
    val senderName: String = "Ba Mẹ",
    @ServerTimestamp val createdAt: Timestamp? = null
)
