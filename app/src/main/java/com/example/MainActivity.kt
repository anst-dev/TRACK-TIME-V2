package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.feature.screens.*
import com.example.feature.task.TaskViewModel
import com.example.navigation.Screen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.*
import android.Manifest
import android.os.Build
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val requestPermissionLauncher = registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                Log.d("MainActivity", "Notification permission granted: $isGranted")
            }
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                AppMainLayout(intent = intent)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppMainLayout(intent: android.content.Intent?) {
    val viewModel: TaskViewModel = viewModel()
    val navController = rememberNavController()

    val screenTitleState = remember { mutableStateOf("Quick Note & Time Tracker") }

    val isPinEnabled by viewModel.isPinEnabled.collectAsState()
    val pinCode by viewModel.pinCode.collectAsState()

    var isUnlocked by remember { mutableStateOf(false) }

    // Toggle dialog layers
    var showAddTask by remember { mutableStateOf(intent?.action == "ACTION_OPEN_ADD_TASK") }
    var showSettings by remember { mutableStateOf(false) }

    // If PIN enabled and not unlocked yet, show lock gate
    if (isPinEnabled && pinCode.isNotEmpty() && !isUnlocked) {
        LockScreen(
            correctPinCode = pinCode,
            onSuccess = { isUnlocked = true }
        )
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                // Top App Bar matching screenshot 1
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = screenTitleState.value,
                            color = ChronoText,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { /* Menu action */ }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = ChronoPrimary)
                        }
                    },
                    actions = {
                        IconButton(onClick = { navController.navigate(Screen.Ai.route) }) {
                            Icon(Icons.Default.Face, contentDescription = "AI insights", tint = ChronoPrimary)
                        }
                        IconButton(onClick = { showSettings = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = ChronoPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = ChronoBackground
                    )
                )
            },
            bottomBar = {
                AppBottomNavigationBar(
                    navController = navController,
                    onAddClick = { showAddTask = true },
                    onSettingsClick = { showSettings = true }
                )
            },
            containerColor = ChronoBackground
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                NavHost(
                    navController = navController,
                    startDestination = Screen.Home.route,
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable(Screen.Home.route) {
                        screenTitleState.value = "Quick Note & Time Tracker"
                        HomeScreen(
                            viewModel = viewModel,
                            onNavigateToList = { navController.navigate(Screen.List.route) },
                            onNavigateToStats = { navController.navigate(Screen.Stats.route) },
                            onNavigateToSync = { navController.navigate(Screen.Sync.route) },
                            onNavigateToAi = { navController.navigate(Screen.Ai.route) },
                            onOpenAddTask = { showAddTask = true },
                            onOpenSettings = { showSettings = true }
                        )
                    }
                    composable(Screen.List.route) {
                        screenTitleState.value = "Nhật ký Sessions"
                        ListScreen(viewModel = viewModel)
                    }
                    composable(Screen.Stats.route) {
                        screenTitleState.value = "Hiệu suất Công việc"
                        StatsScreen(viewModel = viewModel)
                    }
                    composable(Screen.Sync.route) {
                        screenTitleState.value = "Đồng bộ đám mây"
                        SyncScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.Ai.route) {
                        screenTitleState.value = "AI Khuyên & Phân Tích"
                        AiInsightsScreen(viewModel = viewModel)
                    }
                }

                // Add Task Overlay sheet
                if (showAddTask) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f))
                            .clickable { showAddTask = false },
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        // Prevent click propagation to background
                        Box(modifier = Modifier.clickable(enabled = false) {}) {
                            AddTaskDialog(
                                viewModel = viewModel,
                                onDismiss = { showAddTask = false }
                            )
                        }
                    }
                }

                // Settings configurations popup
                if (showSettings) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f))
                            .clickable { showSettings = false },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(modifier = Modifier.clickable(enabled = false) {}) {
                            SettingsDialog(
                                viewModel = viewModel,
                                onDismiss = { showSettings = false }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppBottomNavigationBar(
    navController: NavController,
    onAddClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Surface(
        color = ChronoSurface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tab 1: Trang chủ
            BottomBarItem(
                label = "Trang chủ",
                icon = Icons.Default.Home,
                isActive = currentRoute == Screen.Home.route,
                onClick = { navController.navigate(Screen.Home.route) }
            )

            // Tab 2: Danh sách
            BottomBarItem(
                label = "Tác vụ",
                icon = Icons.Default.FormatListBulleted,
                isActive = currentRoute == Screen.List.route,
                onClick = { navController.navigate(Screen.List.route) }
            )

            // Oversized Floating Action Center Button
            Box(
                modifier = Modifier
                    .offset(y = (-15).dp)
                    .size(56.dp)
                    .background(AccentBlue, RoundedCornerShape(16.dp))
                    .border(4.dp, ChronoBackground, RoundedCornerShape(16.dp))
                    .clickable { onAddClick() }
                    .testTag("add_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Thêm mới",
                    tint = ChronoOnPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Tab 3: Báo cáo
            BottomBarItem(
                label = "Báo cáo",
                icon = Icons.Default.InsertChart,
                isActive = currentRoute == Screen.Stats.route,
                onClick = { navController.navigate(Screen.Stats.route) }
            )

            // Tab 4: Sync / Settings
            BottomBarItem(
                label = "Đồng bộ",
                icon = Icons.Default.CloudQueue,
                isActive = currentRoute == Screen.Sync.route,
                onClick = { navController.navigate(Screen.Sync.route) }
            )
        }
    }
}

@Composable
fun BottomBarItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable { onClick() }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) AccentBlue else ChronoTextDim,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            color = if (isActive) AccentBlue else ChronoTextDim,
            fontSize = 11.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
        )
    }
}
