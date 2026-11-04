package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Child
import com.example.data.model.EncouragingMessage
import com.example.data.model.RedemptionItem
import com.example.data.model.RewardItem
import com.example.data.model.TaskItem
import com.example.data.model.UserProfile
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FamilyRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    // User Profile
    fun observeUserProfile(userId: String): Flow<UserProfile?> {
        val path = "users/$userId"
        return db.collection("users").document(userId)
            .snapshots()
            .map { it.toObject(UserProfile::class.java) }
    }

    suspend fun saveUserProfile(profile: UserProfile): Result<Unit> {
        val path = "users/${profile.userId}"
        return try {
            val docRef = db.collection("users").document(profile.userId)
            val doc = docRef.get().await()
            if (!doc.exists()) {
                docRef.set(profile.copy(createdAt = null, updatedAt = null)).await()
            } else {
                val existing = doc.toObject(UserProfile::class.java)
                docRef.set(
                    profile.copy(
                        createdAt = existing?.createdAt,
                        updatedAt = null
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // Children
    fun observeChildren(userId: String): Flow<List<Child>> {
        return db.collection("users").document(userId).collection("children")
            .snapshots()
            .map { snapshot -> snapshot.toObjects(Child::class.java) }
    }

    suspend fun addChild(userId: String, child: Child): Result<String> {
        val childId = if (child.id.isNotBlank()) child.id else UUID.randomUUID().toString()
        val path = "users/$userId/children/$childId"
        return try {
            val newChild = child.copy(id = childId, userId = userId, createdAt = null, updatedAt = null)
            db.collection("users").document(userId).collection("children").document(childId)
                .set(newChild)
                .await()
            Result.success(childId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateChild(userId: String, child: Child): Result<Unit> {
        val path = "users/$userId/children/${child.id}"
        return try {
            val docRef = db.collection("users").document(userId).collection("children").document(child.id)
            val existing = docRef.get().await().toObject(Child::class.java)
            docRef.set(
                child.copy(
                    userId = userId,
                    createdAt = existing?.createdAt,
                    updatedAt = null
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // Tasks
    fun observeTasks(userId: String): Flow<List<TaskItem>> {
        return db.collection("users").document(userId).collection("tasks")
            .snapshots()
            .map { snapshot -> snapshot.toObjects(TaskItem::class.java) }
    }

    suspend fun createTask(userId: String, task: TaskItem): Result<String> {
        val taskId = if (task.id.isNotBlank()) task.id else UUID.randomUUID().toString()
        val path = "users/$userId/tasks/$taskId"
        return try {
            val newTask = task.copy(
                id = taskId,
                userId = userId,
                createdAt = null,
                updatedAt = null
            )
            db.collection("users").document(userId).collection("tasks").document(taskId)
                .set(newTask)
                .await()
            Result.success(taskId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateTask(userId: String, task: TaskItem): Result<Unit> {
        val path = "users/$userId/tasks/${task.id}"
        return try {
            val docRef = db.collection("users").document(userId).collection("tasks").document(task.id)
            val existing = docRef.get().await().toObject(TaskItem::class.java)
            docRef.set(
                task.copy(
                    userId = userId,
                    createdAt = existing?.createdAt,
                    updatedAt = null
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun submitTaskForApproval(userId: String, task: TaskItem): Result<Unit> {
        return updateTask(userId, task.copy(status = "COMPLETED_PENDING"))
    }

    suspend fun approveTaskAndAwardStars(
        userId: String,
        task: TaskItem,
        bonusStars: Long,
        encouragingMessage: String,
        senderName: String
    ): Result<Unit> {
        val totalStarsToAward = task.stars + bonusStars
        val path = "users/$userId/tasks/${task.id}"
        return try {
            // 1. Update task to APPROVED
            val taskRef = db.collection("users").document(userId).collection("tasks").document(task.id)
            val existingTask = taskRef.get().await().toObject(TaskItem::class.java)
            taskRef.set(
                task.copy(
                    userId = userId,
                    status = "APPROVED",
                    stars = totalStarsToAward,
                    encouragingMessage = encouragingMessage,
                    createdAt = existingTask?.createdAt,
                    updatedAt = null
                )
            ).await()

            // 2. Award stars to child
            val childRef = db.collection("users").document(userId).collection("children").document(task.childId)
            val childDoc = childRef.get().await()
            if (childDoc.exists()) {
                val currentChild = childDoc.toObject(Child::class.java)
                if (currentChild != null) {
                    val updatedChild = currentChild.copy(
                        starsBalance = currentChild.starsBalance + totalStarsToAward,
                        totalStarsEarned = currentChild.totalStarsEarned + totalStarsToAward,
                        createdAt = currentChild.createdAt,
                        updatedAt = null
                    )
                    childRef.set(updatedChild).await()
                }
            }

            // 3. Add Encouraging Message
            if (encouragingMessage.isNotBlank()) {
                val msgId = UUID.randomUUID().toString()
                val msg = EncouragingMessage(
                    id = msgId,
                    userId = userId,
                    childId = task.childId,
                    taskId = task.id,
                    taskTitle = task.title,
                    message = encouragingMessage,
                    senderName = senderName,
                    createdAt = null
                )
                db.collection("users").document(userId).collection("messages").document(msgId)
                    .set(msg)
                    .await()
            }

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // Rewards
    fun observeRewards(userId: String): Flow<List<RewardItem>> {
        return db.collection("users").document(userId).collection("rewards")
            .snapshots()
            .map { snapshot -> snapshot.toObjects(RewardItem::class.java) }
    }

    suspend fun addReward(userId: String, reward: RewardItem): Result<String> {
        val rewardId = if (reward.id.isNotBlank()) reward.id else UUID.randomUUID().toString()
        val path = "users/$userId/rewards/$rewardId"
        return try {
            val newReward = reward.copy(id = rewardId, userId = userId, createdAt = null, updatedAt = null)
            db.collection("users").document(userId).collection("rewards").document(rewardId)
                .set(newReward)
                .await()
            Result.success(rewardId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    // Redemptions
    fun observeRedemptions(userId: String): Flow<List<RedemptionItem>> {
        return db.collection("users").document(userId).collection("redemptions")
            .snapshots()
            .map { snapshot -> snapshot.toObjects(RedemptionItem::class.java) }
    }

    suspend fun redeemReward(
        userId: String,
        child: Child,
        reward: RewardItem
    ): Result<String> {
        val redemptionId = UUID.randomUUID().toString()
        val path = "users/$userId/redemptions/$redemptionId"
        return try {
            if (child.starsBalance < reward.starCost) {
                return Result.failure(IllegalStateException("Not enough stars to redeem this reward"))
            }

            // 1. Deduct star balance (never deduct totalStarsEarned!)
            val childRef = db.collection("users").document(userId).collection("children").document(child.id)
            val updatedChild = child.copy(
                starsBalance = child.starsBalance - reward.starCost,
                createdAt = child.createdAt,
                updatedAt = null
            )
            childRef.set(updatedChild).await()

            // 2. Record redemption
            val redemption = RedemptionItem(
                id = redemptionId,
                userId = userId,
                childId = child.id,
                childName = child.name,
                rewardId = reward.id,
                rewardTitle = reward.title,
                starCost = reward.starCost,
                status = "REQUESTED",
                createdAt = null,
                updatedAt = null
            )
            db.collection("users").document(userId).collection("redemptions").document(redemptionId)
                .set(redemption)
                .await()

            Result.success(redemptionId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun fulfillRedemption(userId: String, redemption: RedemptionItem): Result<Unit> {
        val path = "users/$userId/redemptions/${redemption.id}"
        return try {
            val docRef = db.collection("users").document(userId).collection("redemptions").document(redemption.id)
            val existing = docRef.get().await().toObject(RedemptionItem::class.java)
            docRef.set(
                redemption.copy(
                    userId = userId,
                    status = "FULFILLED",
                    createdAt = existing?.createdAt,
                    updatedAt = null
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // Encouraging Messages
    fun observeMessages(userId: String, childId: String? = null): Flow<List<EncouragingMessage>> {
        return db.collection("users").document(userId).collection("messages")
            .snapshots()
            .map { snapshot ->
                val all = snapshot.toObjects(EncouragingMessage::class.java)
                if (childId != null) all.filter { it.childId == childId } else all
            }
    }

    // Seed Initial Starter Data
    suspend fun seedInitialDataIfEmpty(userId: String, parentDisplayName: String) {
        try {
            val childrenRef = db.collection("users").document(userId).collection("children")
            val snapshot = childrenRef.limit(1).get().await()
            if (!snapshot.isEmpty) return // Already seeded

            // 1. Create User Profile
            saveUserProfile(
                UserProfile(
                    userId = userId,
                    displayName = parentDisplayName.ifBlank { "Ba Mẹ" },
                    parentPin = "1234"
                )
            )

            // 2. Starter Children
            val child1Id = UUID.randomUUID().toString()
            val child2Id = UUID.randomUUID().toString()

            val child1 = Child(
                id = child1Id,
                userId = userId,
                name = "Bé An",
                avatar = "👧",
                age = 9,
                starsBalance = 12L,
                totalStarsEarned = 12L
            )
            val child2 = Child(
                id = child2Id,
                userId = userId,
                name = "Bé Bình",
                avatar = "👦",
                age = 11,
                starsBalance = 15L,
                totalStarsEarned = 15L
            )
            addChild(userId, child1)
            addChild(userId, child2)

            // 3. Starter Rewards
            val rewards = listOf(
                RewardItem(id = UUID.randomUUID().toString(), userId = userId, title = "30 phút chơi iPad / Trò chơi", description = "Được chọn game yêu thích", starCost = 15L, icon = "🎮"),
                RewardItem(id = UUID.randomUUID().toString(), userId = userId, title = "Một que kem mát lạnh yêu thích", description = "Chọn vị kem con thích nhất", starCost = 10L, icon = "🍦"),
                RewardItem(id = UUID.randomUUID().toString(), userId = userId, title = "Chuyến đi nhà sách cuối tuần", description = "Mua quyển sách / truyện con chọn", starCost = 25L, icon = "📚"),
                RewardItem(id = UUID.randomUUID().toString(), userId = userId, title = "Chọn phim xem cả nhà tối thứ 7", description = "Con là đạo diễn chọn phim!", starCost = 20L, icon = "🎬"),
                RewardItem(id = UUID.randomUUID().toString(), userId = userId, title = "Chuyến đi chơi công viên nước", description = "Cả nhà cùng đi bơi thỏa thích", starCost = 35L, icon = "🎡")
            )
            for (r in rewards) {
                addReward(userId, r)
            }

            // 4. Starter Tasks
            val tasks = listOf(
                TaskItem(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    childId = child1Id,
                    title = "Dọn phòng & gấp chăn gối sau khi dậy",
                    description = "Gấp chăn gối vuông vắn, xếp gấu bông gọn gàng",
                    category = "CHORES",
                    difficulty = "EASY",
                    stars = 1L,
                    status = "ASSIGNED",
                    createdByRole = "PARENT"
                ),
                TaskItem(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    childId = child1Id,
                    title = "Đọc sách 20 phút trước khi ngủ",
                    description = "Đọc truyện hoặc sách khoa học tự chọn",
                    category = "STUDY",
                    difficulty = "MEDIUM",
                    stars = 2L,
                    status = "COMPLETED_PENDING",
                    createdByRole = "CHILD"
                ),
                TaskItem(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    childId = child2Id,
                    title = "Hoàn thành bài tập Toán & Tiếng Việt",
                    description = "Làm bài tập đầy đủ và tự kiểm tra lại",
                    category = "STUDY",
                    difficulty = "MEDIUM",
                    stars = 2L,
                    status = "ASSIGNED",
                    createdByRole = "PARENT"
                ),
                TaskItem(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    childId = child2Id,
                    title = "Giúp mẹ rửa chén bát sau bữa tối",
                    description = "Cẩn thận rửa sạch và úp lên giá",
                    category = "CHORES",
                    difficulty = "HARD",
                    stars = 3L,
                    status = "COMPLETED_PENDING",
                    createdByRole = "CHILD"
                )
            )
            for (t in tasks) {
                createTask(userId, t)
            }

            // 5. Initial encouraging messages
            val msg1 = EncouragingMessage(
                id = UUID.randomUUID().toString(),
                userId = userId,
                childId = child1Id,
                taskTitle = "Tự giác đọc sách",
                message = "Ba mẹ rất tự hào về tinh thần ham học và tự giác của con! 🌟",
                senderName = "Ba Mẹ",
                createdAt = null
            )
            val msg2 = EncouragingMessage(
                id = UUID.randomUUID().toString(),
                userId = userId,
                childId = child2Id,
                taskTitle = "Giúp đỡ gia đình",
                message = "Con thật chu đáo và biết quan tâm mọi người! Tiếp tục phát huy nhé! ❤️",
                senderName = "Ba Mẹ",
                createdAt = null
            )
            db.collection("users").document(userId).collection("messages").document(msg1.id).set(msg1).await()
            db.collection("users").document(userId).collection("messages").document(msg2.id).set(msg2).await()

        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$userId/seed")
        }
    }
}
