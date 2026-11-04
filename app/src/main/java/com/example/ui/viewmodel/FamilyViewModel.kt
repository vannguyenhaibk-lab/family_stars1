package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Child
import com.example.data.model.EncouragingMessage
import com.example.data.model.RedemptionItem
import com.example.data.model.RewardItem
import com.example.data.model.TaskItem
import com.example.data.model.UserProfile
import com.example.data.repository.FamilyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppMode {
    CHILD,
    PARENT
}

class FamilyViewModel(
    private val repository: FamilyRepository,
    val currentUserId: String,
    parentDisplayName: String
) : ViewModel() {

    private val _isVietnamese = MutableStateFlow(true)
    val isVietnamese: StateFlow<Boolean> = _isVietnamese.asStateFlow()

    private val _currentMode = MutableStateFlow(AppMode.CHILD)
    val currentMode: StateFlow<AppMode> = _currentMode.asStateFlow()

    private val _selectedChildId = MutableStateFlow<String?>(null)
    val selectedChildId: StateFlow<String?> = _selectedChildId.asStateFlow()

    // Dialog & UI interaction states
    val isPinDialogOpen = MutableStateFlow(false)
    val isRegisterTaskDialogOpen = MutableStateFlow(false)
    val isAssignTaskDialogOpen = MutableStateFlow(false)
    val isAddRewardDialogOpen = MutableStateFlow(false)
    val approvingTask = MutableStateFlow<TaskItem?>(null)
    val snackbarMessage = MutableStateFlow<String?>(null)
    val celebrationEvent = MutableStateFlow<Boolean>(false)

    // Data streams
    val userProfile: StateFlow<UserProfile?> = repository.observeUserProfile(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val children: StateFlow<List<Child>> = repository.observeChildren(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tasks: StateFlow<List<TaskItem>> = repository.observeTasks(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rewards: StateFlow<List<RewardItem>> = repository.observeRewards(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val redemptions: StateFlow<List<RedemptionItem>> = repository.observeRedemptions(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val messages: StateFlow<List<EncouragingMessage>> = repository.observeMessages(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeChild: StateFlow<Child?> = combine(children, selectedChildId) { list, id ->
        if (id != null) list.find { it.id == id } else list.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty(currentUserId, parentDisplayName)
        }
    }

    fun toggleLanguage() {
        _isVietnamese.value = !_isVietnamese.value
    }

    fun selectChild(childId: String) {
        _selectedChildId.value = childId
    }

    fun requestSwitchToParent() {
        isPinDialogOpen.value = true
    }

    fun verifyPinAndSwitchToParent(enteredPin: String): Boolean {
        val requiredPin = userProfile.value?.parentPin ?: "1234"
        return if (enteredPin == requiredPin) {
            _currentMode.value = AppMode.PARENT
            isPinDialogOpen.value = false
            true
        } else {
            false
        }
    }

    fun switchToChildMode(childId: String? = null) {
        if (childId != null) {
            _selectedChildId.value = childId
        }
        _currentMode.value = AppMode.CHILD
    }

    fun markTaskAsCompleted(task: TaskItem) {
        viewModelScope.launch {
            val result = repository.submitTaskForApproval(currentUserId, task)
            if (result.isSuccess) {
                celebrationEvent.value = true
                snackbarMessage.value = if (_isVietnamese.value) "Đã gửi nhiệm vụ! Ba mẹ sẽ sớm duyệt và tặng sao nhé! ✨" else "Submitted! Parents will review soon! ✨"
            }
        }
    }

    fun registerChildTask(title: String, desc: String, category: String, difficulty: String, stars: Long) {
        val child = activeChild.value ?: return
        viewModelScope.launch {
            val newTask = TaskItem(
                userId = currentUserId,
                childId = child.id,
                title = title.trim(),
                description = desc.trim(),
                category = category,
                difficulty = difficulty,
                stars = stars,
                status = "ASSIGNED",
                createdByRole = "CHILD"
            )
            val result = repository.createTask(currentUserId, newTask)
            if (result.isSuccess) {
                isRegisterTaskDialogOpen.value = false
                snackbarMessage.value = if (_isVietnamese.value) "Đã đăng ký nhiệm vụ mới! Cố lên nhé! ⭐" else "Task registered! Keep it up! ⭐"
            }
        }
    }

    fun assignParentTask(childId: String, title: String, desc: String, category: String, difficulty: String, stars: Long) {
        viewModelScope.launch {
            val newTask = TaskItem(
                userId = currentUserId,
                childId = childId,
                title = title.trim(),
                description = desc.trim(),
                category = category,
                difficulty = difficulty,
                stars = stars,
                status = "ASSIGNED",
                createdByRole = "PARENT"
            )
            val result = repository.createTask(currentUserId, newTask)
            if (result.isSuccess) {
                isAssignTaskDialogOpen.value = false
                snackbarMessage.value = if (_isVietnamese.value) "Đã giao việc thành công!" else "Task assigned successfully!"
            }
        }
    }

    fun approveTaskWithPraise(task: TaskItem, bonusStars: Long, message: String, senderName: String) {
        viewModelScope.launch {
            val result = repository.approveTaskAndAwardStars(
                userId = currentUserId,
                task = task,
                bonusStars = bonusStars,
                encouragingMessage = message.trim(),
                senderName = senderName.ifBlank { "Ba Mẹ" }
            )
            if (result.isSuccess) {
                approvingTask.value = null
                celebrationEvent.value = true
                snackbarMessage.value = if (_isVietnamese.value) "Đã tặng sao và gửi lời khen đến bé! 🌟" else "Awarded stars and sent encouraging words! 🌟"
            }
        }
    }

    fun addReward(title: String, desc: String, starCost: Long, icon: String) {
        viewModelScope.launch {
            val reward = RewardItem(
                userId = currentUserId,
                title = title.trim(),
                description = desc.trim(),
                starCost = starCost,
                icon = icon.ifBlank { "🎁" }
            )
            val result = repository.addReward(currentUserId, reward)
            if (result.isSuccess) {
                isAddRewardDialogOpen.value = false
                snackbarMessage.value = if (_isVietnamese.value) "Đã thêm phần thưởng mới!" else "Reward added!"
            }
        }
    }

    fun redeemReward(reward: RewardItem) {
        val child = activeChild.value ?: return
        if (child.starsBalance < reward.starCost) {
            snackbarMessage.value = if (_isVietnamese.value) "Chưa đủ sao! Cố gắng hoàn thành thêm nhiệm vụ nhé! ⭐" else "Not enough stars yet!"
            return
        }
        viewModelScope.launch {
            val result = repository.redeemReward(currentUserId, child, reward)
            if (result.isSuccess) {
                celebrationEvent.value = true
                snackbarMessage.value = if (_isVietnamese.value) "Đổi quà thành công! Ba mẹ sẽ sớm chuẩn bị cho bé! 🎉" else "Redeemed! Parent will fulfill soon! 🎉"
            }
        }
    }

    fun fulfillRedemption(redemption: RedemptionItem) {
        viewModelScope.launch {
            val result = repository.fulfillRedemption(currentUserId, redemption)
            if (result.isSuccess) {
                snackbarMessage.value = if (_isVietnamese.value) "Đã trao quà cho bé! ❤️" else "Reward marked as given! ❤️"
            }
        }
    }

    fun dismissSnackbar() {
        snackbarMessage.value = null
    }

    fun dismissCelebration() {
        celebrationEvent.value = false
    }
}
