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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Child
import com.example.data.model.RedemptionItem
import com.example.data.model.RewardItem
import com.example.data.model.TaskCategory
import com.example.data.model.TaskDifficulty
import com.example.data.model.TaskItem
import com.example.ui.components.StarBadge
import com.example.ui.strings.AppStrings
import com.example.ui.theme.MintSuccess
import com.example.ui.theme.StarGold
import com.example.ui.theme.StarGoldDark
import com.example.ui.viewmodel.FamilyViewModel

@Composable
fun ParentDashboardScreen(
    viewModel: FamilyViewModel,
    modifier: Modifier = Modifier
) {
    val isVi by viewModel.isVietnamese.collectAsState()
    val strings = if (isVi) AppStrings.VI else AppStrings.EN

    val children by viewModel.children.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val redemptions by viewModel.redemptions.collectAsState()
    val rewards by viewModel.rewards.collectAsState()

    val pendingTasks = tasks.filter { it.status == "COMPLETED_PENDING" || it.status == "PROPOSED" }
    val pendingRedemptions = redemptions.filter { it.status == "REQUESTED" }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.parentDashboard,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // 1. Children Overview Cards
        item {
            Text(
                text = if (isVi) "Danh Sách Các Bé" else "Children Overview",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("parent_children_row")
            ) {
                items(children) { child ->
                    val childPendingCount = tasks.count { it.childId == child.id && it.status == "COMPLETED_PENDING" }
                    ParentChildSummaryCard(
                        child = child,
                        pendingCount = childPendingCount,
                        strings = strings,
                        onSwitchToChild = { viewModel.switchToChildMode(child.id) }
                    )
                }
            }
        }

        // 2. Action Buttons (Assign Task & Add Reward)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { viewModel.isAssignTaskDialogOpen.value = true },
                    modifier = Modifier.weight(1f).height(48.dp).testTag("parent_assign_task_btn"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = strings.assignTask, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = { viewModel.isAddRewardDialogOpen.value = true },
                    modifier = Modifier.weight(1f).height(48.dp).testTag("parent_add_reward_btn"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(imageVector = Icons.Default.CardGiftcard, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = strings.addReward, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        // 3. Pending Task Approvals (Crucial Flow with Encouraging Messages!)
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${strings.pendingApprovals} (${pendingTasks.size})",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (pendingTasks.isNotEmpty()) Color(0xFFD97706) else MaterialTheme.colorScheme.onSurface
                )
                if (pendingTasks.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFFEF3C7)
                    ) {
                        Text(
                            text = "!",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB45309),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        if (pendingTasks.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "✨", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isVi) "Không có nhiệm vụ nào cần duyệt lúc này. Mọi việc đều tuyệt vời!"
                            else "No tasks pending review right now!",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(pendingTasks) { task ->
                val child = children.find { it.id == task.childId }
                ParentPendingTaskCard(
                    task = task,
                    childName = child?.name ?: "",
                    childAvatar = child?.avatar ?: "⭐",
                    strings = strings,
                    onApproveClick = { viewModel.approvingTask.value = task }
                )
            }
        }

        // 4. Pending Reward Redemptions
        if (pendingRedemptions.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isVi) "Yêu Cầu Đổi Quà Chờ Trao (${pendingRedemptions.size})" else "Reward Redemptions (${pendingRedemptions.size})",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4338CA)
                )
            }
            items(pendingRedemptions) { red ->
                ParentRedemptionCard(
                    redemption = red,
                    strings = strings,
                    onFulfill = { viewModel.fulfillRedemption(red) }
                )
            }
        }

        // 5. Educational Philosophy & Positive Parenting Guide Card
        item {
            PositiveParentingPhilosophyCard(isVi = isVi)
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Modal: Approve Task with 3-5 Encouraging Messages + Custom Input + Bonus Stars
    val taskToApprove by viewModel.approvingTask.collectAsState()
    if (taskToApprove != null) {
        val child = children.find { it.id == taskToApprove!!.childId }
        ApproveTaskWithEncouragementDialog(
            task = taskToApprove!!,
            childName = child?.name ?: "",
            strings = strings,
            isVi = isVi,
            onDismiss = { viewModel.approvingTask.value = null },
            onConfirm = { bonusStars, message, sender ->
                viewModel.approveTaskWithPraise(taskToApprove!!, bonusStars, message, sender)
            }
        )
    }

    // Dialog: Assign Task
    val isAssignOpen by viewModel.isAssignTaskDialogOpen.collectAsState()
    if (isAssignOpen) {
        ParentAssignTaskDialog(
            children = children,
            strings = strings,
            isVi = isVi,
            onDismiss = { viewModel.isAssignTaskDialogOpen.value = false },
            onConfirm = { childId, title, desc, cat, diff, stars ->
                viewModel.assignParentTask(childId, title, desc, cat, diff, stars)
            }
        )
    }

    // Dialog: Add Reward
    val isAddRewardOpen by viewModel.isAddRewardDialogOpen.collectAsState()
    if (isAddRewardOpen) {
        ParentAddRewardDialog(
            strings = strings,
            isVi = isVi,
            onDismiss = { viewModel.isAddRewardDialogOpen.value = false },
            onConfirm = { title, desc, cost, icon ->
                viewModel.addReward(title, desc, cost, icon)
            }
        )
    }
}

@Composable
fun ParentChildSummaryCard(
    child: Child,
    pendingCount: Int,
    strings: com.example.ui.strings.Strings,
    onSwitchToChild: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = modifier.width(200.dp).testTag("child_summary_card_${child.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(StarGold.copy(alpha = 0.25f))
                        .border(1.5.dp, StarGold, CircleShape)
                ) {
                    Text(text = child.avatar, fontSize = 22.sp)
                }
                StarBadge(stars = child.starsBalance)
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = child.name,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                text = "${child.age} tuổi • ${child.totalStarsEarned} ⭐ tích lũy",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (pendingCount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFEF3C7)
                ) {
                    Text(
                        text = "⏳ $pendingCount chờ duyệt",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB45309),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = onSwitchToChild,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(36.dp)
            ) {
                Text(text = "Xem góc bé", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun ParentPendingTaskCard(
    task: TaskItem,
    childName: String,
    childAvatar: String,
    strings: com.example.ui.strings.Strings,
    onApproveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFDE68A)),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = modifier.fillMaxWidth().testTag("pending_task_card_${task.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = childAvatar, fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = childName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF92400E))
                }
                StarBadge(stars = task.stars)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = task.title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color(0xFF78350F)
            )
            if (task.description.isNotBlank()) {
                Text(
                    text = task.description,
                    fontSize = 13.sp,
                    color = Color(0xFF92400E)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onApproveClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF59E0B),
                    contentColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth().height(44.dp).testTag("approve_task_button_${task.id}")
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = strings.approveAndReward,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun ParentRedemptionCard(
    redemption: RedemptionItem,
    strings: com.example.ui.strings.Strings,
    onFulfill: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEEF2FF)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC7D2FE)),
        modifier = modifier.fillMaxWidth().testTag("redemption_card_${redemption.id}")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${redemption.childName} muốn đổi:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF4338CA)
                )
                Text(
                    text = redemption.rewardTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF1E1B4B)
                )
                Text(
                    text = "-${redemption.starCost} ⭐",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = StarGoldDark
                )
            }

            Button(
                onClick = onFulfill,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4338CA)),
                modifier = Modifier.testTag("fulfill_btn_${redemption.id}")
            ) {
                Text(text = strings.fulfill, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PositiveParentingPhilosophyCard(isVi: Boolean) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF5FF)),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFE9D5FF)),
        modifier = Modifier.fillMaxWidth().testTag("positive_parenting_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = Color(0xFF7E22CE),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isVi) "Triết Lý Nuôi Dạy Tích Cực 💡" else "Positive Parenting Philosophy 💡",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF6B21A8)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))

            val points = if (isVi) listOf(
                "🌱 Khuyến khích nỗ lực & sự tự giác, không chỉ nhìn vào kết quả.",
                "⭐ Tuyệt đối KHÔNG trừ sao như hình phạt: sao là phần thưởng cho việc tốt đã làm.",
                "❤️ KHÔNG so sánh giữa các con: mỗi bé có nhịp độ phát triển riêng biệt.",
                "👏 Khen ngợi cụ thể hành vi: giúp trẻ hiểu rõ giá trị của sự trách nhiệm."
            ) else listOf(
                "🌱 Reward effort and self-discipline, not just outcomes.",
                "⭐ Never deduct stars as punishment: stars are earned achievements.",
                "❤️ Never compare siblings: each child grows at their own unique pace.",
                "👏 Specific praise reinforces good habits and positive identity."
            )

            points.forEach { point ->
                Text(
                    text = point,
                    fontSize = 13.sp,
                    color = Color(0xFF581C87),
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
fun ApproveTaskWithEncouragementDialog(
    task: TaskItem,
    childName: String,
    strings: com.example.ui.strings.Strings,
    isVi: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Long, String, String) -> Unit
) {
    val presets = if (isVi) AppStrings.PRESET_ENCOURAGING_MESSAGES_VI else AppStrings.PRESET_ENCOURAGING_MESSAGES_EN
    var selectedPresetIndex by remember { mutableIntStateOf(0) }
    var customMessage by remember { mutableStateOf("") }
    var bonusStars by remember { mutableLongStateOf(0L) }
    var senderName by remember { mutableStateOf(if (isVi) "Ba Mẹ" else "Parents") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "${strings.approveAndReward}: $childName ✨",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(text = "Nhiệm vụ: ${task.title} (${task.stars} ⭐)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }

                // 3-5 Suggested Messages
                item {
                    Text(text = strings.chooseEncouragingMessage, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                items(presets.size) { index ->
                    val preset = presets[index]
                    val isSelected = selectedPresetIndex == index && customMessage.isBlank()
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) StarGold.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, StarGold) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedPresetIndex = index
                                customMessage = ""
                            }
                            .testTag("preset_message_$index")
                    ) {
                        Text(
                            text = preset,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(10.dp),
                            color = if (isSelected) Color(0xFF78350F) else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Custom Personalized Message Option
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = customMessage,
                        onValueChange = { customMessage = it },
                        placeholder = { Text(strings.customMessagePlaceholder, fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth().testTag("custom_message_input"),
                        minLines = 2
                    )
                }

                // Bonus Stars Option
                item {
                    Text(text = strings.bonusStars, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0L, 1L, 2L).forEach { b ->
                            val isSel = bonusStars == b
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSel) StarGold.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant,
                                border = if (isSel) androidx.compose.foundation.BorderStroke(1.5.dp, StarGold) else null,
                                modifier = Modifier.clickable { bonusStars = b }
                            ) {
                                Text(
                                    text = if (b == 0L) "0 ⭐" else "+$b ⭐",
                                    fontSize = 13.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalMsg = if (customMessage.isNotBlank()) customMessage else presets[selectedPresetIndex]
                    onConfirm(bonusStars, finalMsg, senderName)
                },
                colors = ButtonDefaults.buttonColors(containerColor = StarGold, contentColor = Color(0xFF78350F)),
                modifier = Modifier.testTag("confirm_award_button")
            ) {
                Text(strings.confirmReward, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}

@Composable
fun ParentAssignTaskDialog(
    children: List<Child>,
    strings: com.example.ui.strings.Strings,
    isVi: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, String, Long) -> Unit
) {
    var selectedChildId by remember { mutableStateOf(children.firstOrNull()?.id ?: "") }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("CHORES") }
    var selectedDifficulty by remember { mutableStateOf("EASY") }
    var stars by remember { mutableLongStateOf(1L) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = strings.assignTask, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Select Child
                Text(text = "Giao cho bé:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(children) { c ->
                        val isSel = selectedChildId == c.id
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSel) StarGold.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (isSel) androidx.compose.foundation.BorderStroke(1.5.dp, StarGold) else null,
                            modifier = Modifier.clickable { selectedChildId = c.id }
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(text = c.avatar, fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = c.name, fontSize = 13.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(strings.taskTitle) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("parent_task_title_input")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(strings.taskDesc) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Category
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
                            Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(text = cat.icon, fontSize = 13.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = if (isVi) cat.viLabel else cat.enLabel, fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Difficulty & Stars
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
                            Column(modifier = Modifier.padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = if (isVi) diff.viLabel else diff.enLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && selectedChildId.isNotBlank()) {
                        onConfirm(selectedChildId, title, description, selectedCategory, selectedDifficulty, stars)
                    }
                },
                enabled = title.isNotBlank() && selectedChildId.isNotBlank(),
                modifier = Modifier.testTag("parent_confirm_assign_task_btn")
            ) {
                Text(strings.confirm)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}

@Composable
fun ParentAddRewardDialog(
    strings: com.example.ui.strings.Strings,
    isVi: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Long, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var starCostText by remember { mutableStateOf("15") }
    var selectedIcon by remember { mutableStateOf("🎁") }
    val icons = listOf("🎁", "🍦", "🎮", "📚", "🎬", "🎡", "🍕", "🚴")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = strings.addReward, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(strings.rewardTitle) },
                    placeholder = { Text(if (isVi) "Ví dụ: Đi bơi công viên nước..." else "e.g. Park trip...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("reward_title_input")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(strings.taskDesc) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = starCostText,
                    onValueChange = { starCostText = it.filter { char -> char.isDigit() } },
                    label = { Text(strings.rewardCost) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("reward_cost_input")
                )

                Text(text = "Biểu tượng:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(icons) { icon ->
                        val isSel = selectedIcon == icon
                        Surface(
                            shape = CircleShape,
                            color = if (isSel) StarGold.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (isSel) androidx.compose.foundation.BorderStroke(1.5.dp, StarGold) else null,
                            modifier = Modifier.clickable { selectedIcon = icon }
                        ) {
                            Text(text = icon, fontSize = 22.sp, modifier = Modifier.padding(8.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cost = starCostText.toLongOrNull() ?: 10L
                    if (title.isNotBlank()) {
                        onConfirm(title, description, cost, selectedIcon)
                    }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.testTag("confirm_add_reward_btn")
            ) {
                Text(strings.confirm)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}
