package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Child
import com.example.data.model.RewardItem
import com.example.data.model.TaskCategory
import com.example.data.model.TaskDifficulty
import com.example.data.model.TaskItem
import com.example.ui.components.ChildAvatarBadge
import com.example.ui.components.EncouragingMessageCard
import com.example.ui.components.StarBadge
import com.example.ui.strings.AppStrings
import com.example.ui.theme.MintSuccess
import com.example.ui.theme.StarGold
import com.example.ui.theme.StarGoldDark
import com.example.ui.theme.StarGoldLight
import com.example.ui.viewmodel.FamilyViewModel

@Composable
fun ChildDashboardScreen(
    viewModel: FamilyViewModel,
    modifier: Modifier = Modifier
) {
    val isVi by viewModel.isVietnamese.collectAsState()
    val strings = if (isVi) AppStrings.VI else AppStrings.EN

    val children by viewModel.children.collectAsState()
    val activeChild by viewModel.activeChild.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val rewards by viewModel.rewards.collectAsState()
    val messages by viewModel.messages.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = if (isVi) {
        listOf("Nhiệm vụ", "Đổi quà", "Thư khen")
    } else {
        listOf("My Tasks", "Rewards", "Love Letters")
    }

    if (children.isEmpty() || activeChild == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = StarGold)
        }
        return
    }

    val child = activeChild!!
    val childTasks = tasks.filter { it.childId == child.id }
    val childMessages = messages.filter { it.childId == child.id }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Children Selector Row
        if (children.size > 1) {
            item {
                Text(
                    text = strings.switchChild,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("children_selector_row")
                ) {
                    items(children) { c ->
                        ChildAvatarBadge(
                            avatar = c.avatar,
                            name = c.name,
                            isSelected = c.id == child.id,
                            onClick = { viewModel.selectChild(c.id) }
                        )
                    }
                }
            }
        }

        // 2. Kid Star Hero Card
        item {
            KidStarHeroCard(
                child = child,
                rewards = rewards,
                strings = strings,
                onRegisterTaskClick = { viewModel.isRegisterTaskDialogOpen.value = true }
            )
        }

        // 3. Tab Switcher
        item {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 0.dp,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 15.sp
                            )
                        },
                        modifier = Modifier.testTag("child_tab_$index")
                    )
                }
            }
        }

        // 4. Tab Content
        when (selectedTab) {
            0 -> {
                // Tasks Tab
                val inProgressTasks = childTasks.filter { it.status == "ASSIGNED" }
                val pendingReviewTasks = childTasks.filter { it.status == "COMPLETED_PENDING" }
                val approvedTasks = childTasks.filter { it.status == "APPROVED" }

                if (inProgressTasks.isNotEmpty()) {
                    item {
                        Text(
                            text = "${strings.inProgress} (${inProgressTasks.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    items(inProgressTasks) { task ->
                        ChildTaskCard(
                            task = task,
                            strings = strings,
                            onComplete = { viewModel.markTaskAsCompleted(task) }
                        )
                    }
                }

                if (pendingReviewTasks.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${strings.pendingReview} (${pendingReviewTasks.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFFD97706)
                        )
                    }
                    items(pendingReviewTasks) { task ->
                        ChildTaskCard(
                            task = task,
                            strings = strings,
                            onComplete = {}
                        )
                    }
                }

                if (approvedTasks.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${strings.approved} (${approvedTasks.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MintSuccess
                        )
                    }
                    items(approvedTasks) { task ->
                        ChildTaskCard(
                            task = task,
                            strings = strings,
                            onComplete = {}
                        )
                    }
                }

                if (childTasks.isEmpty()) {
                    item {
                        EmptyStateCard(
                            message = if (isVi) "Chưa có việc nào! Bé hãy bấm nút bên trên để tự đăng ký việc tốt nhé!"
                            else "No tasks yet! Tap the button above to register your own task!"
                        )
                    }
                }
            }

            1 -> {
                // Rewards Shop Tab
                item {
                    Text(
                        text = strings.rewardShop,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                }
                items(rewards.filter { it.active }) { reward ->
                    ChildRewardCard(
                        reward = reward,
                        childStars = child.starsBalance,
                        strings = strings,
                        onRedeem = { viewModel.redeemReward(reward) }
                    )
                }
            }

            2 -> {
                // Love Letters / Encouraging Messages Tab
                item {
                    Text(
                        text = strings.loveMessages,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                }
                if (childMessages.isEmpty()) {
                    item {
                        EmptyStateCard(message = strings.noMessagesYet)
                    }
                } else {
                    items(childMessages.reversed()) { msg ->
                        EncouragingMessageCard(message = msg)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Register Task Dialog for Child
    val isRegisterOpen by viewModel.isRegisterTaskDialogOpen.collectAsState()
    if (isRegisterOpen) {
        ChildRegisterTaskDialog(
            strings = strings,
            isVi = isVi,
            onDismiss = { viewModel.isRegisterTaskDialogOpen.value = false },
            onConfirm = { title, desc, cat, diff, stars ->
                viewModel.registerChildTask(title, desc, cat, diff, stars)
            }
        )
    }
}

@Composable
fun KidStarHeroCard(
    child: Child,
    rewards: List<RewardItem>,
    strings: com.example.ui.strings.Strings,
    onRegisterTaskClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Find target reward or highest reward child is saving for
    val targetReward = rewards.find { it.id == child.targetRewardId } ?: rewards.maxByOrNull { it.starCost }
    val progress = if (targetReward != null && targetReward.starCost > 0) {
        (child.starsBalance.toFloat() / targetReward.starCost.toFloat()).coerceIn(0f, 1f)
    } else 1f

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1B4B)
        ),
        elevation = CardDefaults.cardElevation(6.dp),
        modifier = modifier.fillMaxWidth().testTag("kid_star_hero_card")
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF312E81), Color(0xFF1E1B4B))
                    )
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4338CA))
                            .border(2.dp, StarGold, CircleShape)
                    ) {
                        Text(text = child.avatar, fontSize = 28.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = child.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color.White
                        )
                        Text(
                            text = "${strings.totalStars}: ${child.totalStarsEarned} ⭐",
                            fontSize = 13.sp,
                            color = StarGoldLight
                        )
                    }
                }

                // Star Balance Badge
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = StarGold.copy(alpha = 0.25f),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, StarGold)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Star",
                            tint = StarGold,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${child.starsBalance}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            color = Color.White
                        )
                    }
                }
            }

            // Target Reward Progress
            if (targetReward != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF3730A3).copy(alpha = 0.6f))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = targetReward.icon, fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = targetReward.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                        Text(
                            text = "${child.starsBalance} / ${targetReward.starCost} ⭐",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = StarGoldLight
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = StarGold,
                        trackColor = Color(0xFF1E1B4B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Button: Kid registers own task
            Button(
                onClick = onRegisterTaskClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = StarGold,
                    contentColor = Color(0xFF78350F)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("child_register_task_button")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = strings.registerTask,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
fun ChildTaskCard(
    task: TaskItem,
    strings: com.example.ui.strings.Strings,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val category = TaskCategory.entries.find { it.key == task.category } ?: TaskCategory.OTHER
    val isApproved = task.status == "APPROVED"
    val isPending = task.status == "COMPLETED_PENDING"

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isApproved -> Color(0xFFECFDF5)
                isPending -> Color(0xFFFFFBEB)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = when {
                isApproved -> Color(0xFFA7F3D0)
                isPending -> Color(0xFFFDE68A)
                else -> MaterialTheme.colorScheme.outlineVariant
            }
        ),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = modifier.fillMaxWidth().testTag("child_task_card_${task.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = category.icon, fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = task.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (task.description.isNotBlank()) {
                            Text(
                                text = task.description,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                StarBadge(stars = task.stars)
            }

            Spacer(modifier = Modifier.height(12.dp))

            when {
                isApproved -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = MintSuccess, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = strings.approved, color = MintSuccess, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    if (!task.encouragingMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "💌 \"${task.encouragingMessage}\"",
                            fontSize = 13.sp,
                            color = Color(0xFF047857),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                isPending -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.HourglassEmpty, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = strings.pendingReview, color = Color(0xFFD97706), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
                else -> {
                    // Task can be completed
                    Button(
                        onClick = onComplete,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MintSuccess,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth().height(44.dp).testTag("mark_task_completed_${task.id}")
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = strings.markComplete, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ChildRewardCard(
    reward: RewardItem,
    childStars: Long,
    strings: com.example.ui.strings.Strings,
    onRedeem: () -> Unit,
    modifier: Modifier = Modifier
) {
    val canRedeem = childStars >= reward.starCost

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth().testTag("child_reward_card_${reward.id}")
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Text(text = reward.icon, fontSize = 32.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = reward.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    if (reward.description.isNotBlank()) {
                        Text(
                            text = reward.description,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${reward.starCost} ⭐",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = StarGoldDark
                    )
                }
            }

            Button(
                onClick = onRedeem,
                enabled = canRedeem,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = StarGold,
                    contentColor = Color(0xFF78350F)
                ),
                modifier = Modifier.testTag("redeem_button_${reward.id}")
            ) {
                Text(
                    text = strings.redeem,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun EmptyStateCard(message: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "🌟", fontSize = 40.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun ChildRegisterTaskDialog(
    strings: com.example.ui.strings.Strings,
    isVi: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, Long) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("CHORES") }
    var selectedDifficulty by remember { mutableStateOf("EASY") }
    var stars by remember { mutableLongStateOf(1L) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = strings.registerTask, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = strings.registerTaskDesc, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(strings.taskTitle) },
                    placeholder = { Text(if (isVi) "Ví dụ: Đọc sách 15 phút, tưới cây..." else "e.g. Read 15 mins...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("child_task_title_input")
                )

                // Category selector
                Text(text = strings.category, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(TaskCategory.entries.toTypedArray()) { cat ->
                        val isSel = selectedCategory == cat.key
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSel) StarGold.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (isSel) androidx.compose.foundation.BorderStroke(1.5.dp, StarGold) else null,
                            modifier = Modifier.clickable { selectedCategory = cat.key }
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(text = cat.icon, fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = if (isVi) cat.viLabel else cat.enLabel, fontSize = 12.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }

                // Difficulty selector with suggested stars
                Text(text = "${strings.difficulty} & ${strings.starsValue}:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    TaskDifficulty.entries.forEach { diff ->
                        val isSel = selectedDifficulty == diff.key
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSel) StarGold.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (isSel) androidx.compose.foundation.BorderStroke(1.5.dp, StarGold) else null,
                            modifier = Modifier.weight(1f).clickable {
                                selectedDifficulty = diff.key
                                stars = diff.defaultStars
                            }
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = if (isVi) diff.viLabel else diff.enLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(title, description, selectedCategory, selectedDifficulty, stars)
                    }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.testTag("child_confirm_task_btn")
            ) {
                Text(strings.confirm)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}
