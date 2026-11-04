package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.Child
import com.example.data.model.TaskItem
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class FamilyRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createAndObserveTask_authenticatedUser_success() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = FamilyRepository(firestore)

        val childId = UUID.randomUUID().toString()
        val taskId = UUID.randomUUID().toString()

        val task = TaskItem(
            id = taskId,
            userId = uid,
            childId = childId,
            title = "Dọn phòng",
            category = "CHORES",
            difficulty = "EASY",
            stars = 2L,
            status = "ASSIGNED",
            createdByRole = "PARENT"
        )

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createTask(uid, task)
        }
        assertTrue("Failed with: ${createResult.exceptionOrNull()?.message}", createResult.isSuccess)

        val tasks = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeTasks(uid).first { list: List<TaskItem> -> list.any { it.id == taskId } }
        }
        assertTrue(tasks.any { it.id == taskId })
    }

    @Test
    fun approveTaskAndAwardStars_updatesChildAndTask() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = FamilyRepository(firestore)

        val childId = UUID.randomUUID().toString()
        val taskId = UUID.randomUUID().toString()

        val child = Child(
            id = childId,
            userId = uid,
            name = "Bé An",
            starsBalance = 5L,
            totalStarsEarned = 5L
        )
        repository.addChild(uid, child).getOrThrow()

        val task = TaskItem(
            id = taskId,
            userId = uid,
            childId = childId,
            title = "Đọc sách 20 phút",
            stars = 2L,
            status = "COMPLETED_PENDING",
            createdByRole = "CHILD"
        )
        repository.createTask(uid, task).getOrThrow()

        val approveResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.approveTaskAndAwardStars(
                userId = uid,
                task = task,
                bonusStars = 1L,
                encouragingMessage = "Tuyệt vời lắm con yêu!",
                senderName = "Ba Mẹ"
            )
        }
        assertTrue(approveResult.isSuccess)

        val updatedChildren = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeChildren(uid).first { list: List<Child> -> list.any { it.id == childId && it.starsBalance == 8L } }
        }
        val foundChild = updatedChildren.first { it.id == childId }
        assertEquals(8L, foundChild.starsBalance)
        assertEquals(8L, foundChild.totalStarsEarned)
    }

    @Test
    fun observeTasks_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        val uid = UUID.randomUUID().toString()
        auth.signOut()
        val repository = FamilyRepository(firestore)

        var caughtException: Exception? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeTasks(uid).first()
            }
        } catch (e: Exception) {
            caughtException = e
        }

        assertNotNull("Expected exception on unauthenticated query", caughtException)
        val firestoreException = checkNotNull(caughtException?.unwrapFirestoreException()) {
            "Expected nested FirebaseFirestoreException, got $caughtException"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
    }

    companion object {
        const val ALICE_EMAIL = "alice_family@test.com"
        const val DEFAULT_TIMEOUT_MS = 6000L
        const val FLOW_TIMEOUT_MS = 6000L
    }
}
