package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.repository.FamilyRepository
import com.example.ui.components.CelebrationPop
import com.example.ui.components.PinDialog
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ChildDashboardScreen
import com.example.ui.screens.ParentDashboardScreen
import com.example.ui.screens.signOutUser
import com.example.ui.strings.AppStrings
import com.example.ui.theme.FamilyStarsTheme
import com.example.ui.theme.StarGold
import com.example.ui.theme.StarGoldDark
import com.example.ui.viewmodel.AppMode
import com.example.ui.viewmodel.FamilyViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            FamilyStarsTheme {
                AppRootContent()
            }
        }
    }
}

@Composable
fun AppRootContent() {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    var isCheckingAuth by remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
            isCheckingAuth = false
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    if (isCheckingAuth) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0xFF1E1B4B)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = StarGold)
        }
    } else if (currentUser == null) {
        AuthScreen(
            isVi = true,
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
            }
        )
    } else {
        FamilyMainScreen(currentUser = currentUser!!)
    }
}

@Composable
fun FamilyMainScreen(currentUser: FirebaseUser) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val snackbarHostState = remember { SnackbarHostState() }

    val databaseId = context.getString(R.string.firestore_database_id)
    val firestore = remember(databaseId) { FirebaseFirestore.getInstance(databaseId) }
    val repository = remember(firestore) { FamilyRepository(firestore) }

    val viewModel: FamilyViewModel = viewModel(
        key = currentUser.uid,
        factory = androidx.lifecycle.viewmodel.viewModelFactory {
            initializer {
                FamilyViewModel(
                    repository = repository,
                    currentUserId = currentUser.uid,
                    parentDisplayName = currentUser.displayName ?: "Ba Mẹ"
                )
            }
        }
    )

    val currentMode by viewModel.currentMode.collectAsState()
    val isVi by viewModel.isVietnamese.collectAsState()
    val celebrationEvent by viewModel.celebrationEvent.collectAsState()
    val snackbarMsg by viewModel.snackbarMessage.collectAsState()
    val isPinOpen by viewModel.isPinDialogOpen.collectAsState()

    val strings = if (isVi) AppStrings.VI else AppStrings.EN

    // BackHandler: If in Parent mode, back button returns to Child mode
    BackHandler(enabled = currentMode == AppMode.PARENT) {
        viewModel.switchToChildMode()
    }

    LaunchedEffect(snackbarMsg) {
        snackbarMsg?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissSnackbar()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        topBar = {
            TopAppBarRow(
                currentMode = currentMode,
                isVi = isVi,
                strings = strings,
                onToggleLanguage = { viewModel.toggleLanguage() },
                onSwitchModeClick = {
                    if (currentMode == AppMode.CHILD) {
                        viewModel.requestSwitchToParent()
                    } else {
                        viewModel.switchToChildMode()
                    }
                },
                onSignOutClick = {
                    signOutUser(
                        context = context,
                        credentialManager = credentialManager,
                        onSignOutComplete = {
                            // Handled by AuthStateListener
                        },
                        scope = scope
                    )
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentMode) {
                AppMode.CHILD -> ChildDashboardScreen(viewModel = viewModel)
                AppMode.PARENT -> ParentDashboardScreen(viewModel = viewModel)
            }

            // Celebration Pop Animation
            CelebrationPop(
                trigger = celebrationEvent,
                onDismiss = { viewModel.dismissCelebration() },
                isVi = isVi
            )

            // Pin Dialog for switching to parent mode
            PinDialog(
                isOpen = isPinOpen,
                onDismiss = { viewModel.isPinDialogOpen.value = false },
                onConfirm = { pin ->
                    viewModel.verifyPinAndSwitchToParent(pin)
                },
                isVi = isVi
            )
        }
    }
}

@Composable
fun TopAppBarRow(
    currentMode: AppMode,
    isVi: Boolean,
    strings: com.example.ui.strings.Strings,
    onToggleLanguage: () -> Unit,
    onSwitchModeClick: () -> Unit,
    onSignOutClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Mode Toggle Switcher
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (currentMode == AppMode.PARENT) Color(0xFFFAF5FF) else Color(0xFFFEF3C7),
                border = androidx.compose.foundation.BorderStroke(
                    1.5.dp,
                    if (currentMode == AppMode.PARENT) Color(0xFF7E22CE) else StarGold
                ),
                modifier = Modifier
                    .clickable(onClick = onSwitchModeClick)
                    .testTag("mode_switcher_pill")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (currentMode == AppMode.PARENT) Icons.Default.SupervisorAccount else Icons.Default.ChildCare,
                        contentDescription = null,
                        tint = if (currentMode == AppMode.PARENT) Color(0xFF7E22CE) else Color(0xFFB45309),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (currentMode == AppMode.PARENT) strings.parentMode else strings.childMode,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (currentMode == AppMode.PARENT) Color(0xFF6B21A8) else Color(0xFF78350F)
                    )
                    if (currentMode == AppMode.CHILD) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Lock",
                            tint = Color(0xFFB45309),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Right side: Language Toggle and Sign Out
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .clickable(onClick = onToggleLanguage)
                        .testTag("language_toggle_btn")
                ) {
                    Text(
                        text = if (isVi) "🇻🇳 VI" else "🇬🇧 EN",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = onSignOutClick,
                    modifier = Modifier.size(36.dp).testTag("sign_out_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "Sign Out",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
