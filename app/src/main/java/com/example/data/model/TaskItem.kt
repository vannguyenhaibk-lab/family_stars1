package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class TaskCategory(val key: String, val viLabel: String, val enLabel: String, val icon: String) {
    CHORES("CHORES", "Việc nhà", "Chores", "🧹"),
    STUDY("STUDY", "Học tập", "Study", "📚"),
    SELF_CARE("SELF_CARE", "Tự chăm sóc", "Self-Care", "🪥"),
    KINDNESS("KINDNESS", "Lòng tốt", "Kindness", "❤️"),
    HABIT("HABIT", "Thói quen tốt", "Good Habit", "🌱"),
    OTHER("OTHER", "Khác", "Other", "⭐")
}

enum class TaskDifficulty(val key: String, val defaultStars: Long, val viLabel: String, val enLabel: String) {
    EASY("EASY", 1L, "Dễ (1⭐)", "Easy (1⭐)"),
    MEDIUM("MEDIUM", 2L, "Vừa (2⭐)", "Medium (2⭐)"),
    HARD("HARD", 3L, "Khó (3-5⭐)", "Hard (3-5⭐)")
}

enum class TaskStatus(val key: String, val viLabel: String, val enLabel: String) {
    ASSIGNED("ASSIGNED", "Đang thực hiện", "In Progress"),
    PROPOSED("PROPOSED", "Con đăng ký", "Proposed by Kid"),
    COMPLETED_PENDING("COMPLETED_PENDING", "Chờ ba mẹ duyệt", "Pending Review"),
    APPROVED("APPROVED", "Đã hoàn thành", "Approved & Rewarded")
}

@IgnoreExtraProperties
data class TaskItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val childId: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "CHORES",
    val difficulty: String = "EASY",
    val stars: Long = 1L,
    val status: String = "ASSIGNED", // ASSIGNED, PROPOSED, COMPLETED_PENDING, APPROVED
    val createdByRole: String = "PARENT", // PARENT or CHILD
    val encouragingMessage: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
