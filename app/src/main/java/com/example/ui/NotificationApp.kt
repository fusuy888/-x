package com.example.ui

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.NotificationHistory
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationApp(viewModel: MainViewModel) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(0) } // 0: History, 1: Settings

    // States from viewmodel
    val isEnabled by viewModel.isEnabled.collectAsState()
    val notificationText by viewModel.notificationText.collectAsState()
    val startTime by viewModel.startTime.collectAsState()
    val intervalMinutes by viewModel.intervalMinutes.collectAsState()
    val isDndEnabled by viewModel.isDndEnabled.collectAsState()
    val dndStartTime by viewModel.dndStartTime.collectAsState()
    val dndEndTime by viewModel.dndEndTime.collectAsState()
    val nextTriggerTime by viewModel.nextTriggerTime.collectAsState()
    val historyList by viewModel.historyList.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val activeAlertToShow by viewModel.activeAlertToShow.collectAsState()

    // Clipboard for copying long notifications
    val clipboardManager = LocalClipboardManager.current

    // Permission flags & requesting
    var hasNotifyPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    var showExactAlarmWarning by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotifyPermission = isGranted
        if (isGranted) {
            Toast.makeText(context, "通知权限已获取", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "通知可能不会在桌面上悬浮弹出，请在设置中开通", Toast.LENGTH_LONG).show()
        }
    }

    // Check exact alarm capability
    fun checkExactAlarmPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            return alarmManager?.canScheduleExactAlarms() ?: false
        }
        return true
    }

    // Request permissions on launch
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotifyPermission) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (!checkExactAlarmPermission()) {
            showExactAlarmWarning = true
        }
        viewModel.updateNextTriggerTimeState()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.secondary
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = "App Icon",
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "盯盘提醒",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "PRO MARKET FEEDER",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = { 
                        Icon(
                            imageVector = if (currentTab == 0) Icons.Default.Timeline else Icons.Outlined.Timeline, 
                            contentDescription = "历史时间轴"
                        ) 
                    },
                    label = { Text("历史通知", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("nav_tab_history")
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = { 
                        Icon(
                            imageVector = if (currentTab == 1) Icons.Default.Tune else Icons.Outlined.Tune, 
                            contentDescription = "通知设置"
                        ) 
                    },
                    label = { Text("通知设置", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("nav_tab_settings")
                )
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> -width } + fadeOut())
                    } else {
                        (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> width } + fadeOut())
                    }
                },
                label = "TabTransition"
            ) { tab ->
                when (tab) {
                    0 -> HistoryScreen(
                        isEnabled = isEnabled,
                        nextTriggerTime = nextTriggerTime,
                        historyList = historyList,
                        onCalibrate = {
                            viewModel.calibrateTimesNow()
                            Toast.makeText(context, "时间轮询已校准同步！", Toast.LENGTH_SHORT).show()
                        },
                        onDeleteHistory = { id -> viewModel.deleteHistoryItem(id) },
                        onClearAll = { viewModel.clearHistory() },
                        onSelectHistoryItem = { text ->
                            viewModel.showAlertDetails(text)
                        }
                    )
                    1 -> SettingsScreen(
                        isEnabled = isEnabled,
                        notificationText = notificationText,
                        startTime = startTime,
                        intervalMinutes = intervalMinutes,
                        isDndEnabled = isDndEnabled,
                        dndStartTime = dndStartTime,
                        dndEndTime = dndEndTime,
                        themeMode = themeMode,
                        onThemeModeChange = { viewModel.updateThemeMode(it) },
                        onEnabledChange = { viewModel.setEnabled(it) },
                        onTextChange = { viewModel.updateNotificationText(it) },
                        onStartTimeChange = { viewModel.updateStartTime(it) },
                        onIntervalChange = { viewModel.updateInterval(it) },
                        onDndEnabledChange = { viewModel.setDndEnabled(it) },
                        onDndStartTimeChange = { viewModel.updateDndStartTime(it) },
                        onDndEndTimeChange = { viewModel.updateDndEndTime(it) },
                        onCalibrate = { viewModel.calibrateTimesNow() }
                    )
                }
            }
        }
    }

    // Exact alarm warning dialog
    if (showExactAlarmWarning) {
        AlertDialog(
            onDismissRequest = { showExactAlarmWarning = false },
            confirmButton = {
                Button(
                    onClick = {
                        showExactAlarmWarning = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            try {
                                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "无法跳转，请手动开启精确闹钟权限", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) {
                    Text("前往设置")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExactAlarmWarning = false }) {
                    Text("暂不设置")
                }
            },
            title = { Text("需要精确通知权限") },
            text = { Text("为了确保不漏发、准时通知和高精准定时轮询，应用需要“允许设置精确闹钟”权限。请在系统设置中开通。") }
        )
    }

    // 🌟 ACTIVE ALERT POPUP DIALOG - Displays full detailed text perfectly (with Scroll & Copy feature!) 🌟
    if (activeAlertToShow != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissAlertDetails() },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "盯盘提醒完整内容",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "由于通知可能包含长篇策略或多行表格，您可以在下方窗口中浏览完整信息：",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // High-contrast, dark terminal vibe scrolling container
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = activeAlertToShow ?: "",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    lineHeight = 22.sp,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissAlertDetails() }
                ) {
                    Text("关闭")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(activeAlertToShow ?: ""))
                        Toast.makeText(context, "已复制到剪贴板！", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "复制", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("一键复制")
                }
            }
        )
    }
}

// ---------------------- 历史通知页面 ----------------------
@Composable
fun HistoryScreen(
    isEnabled: Boolean,
    nextTriggerTime: Long,
    historyList: List<NotificationHistory>,
    onCalibrate: () -> Unit,
    onDeleteHistory: (Int) -> Unit,
    onClearAll: () -> Unit,
    onSelectHistoryItem: (String) -> Unit
) {
    var showConfirmClearDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Core status indicator dashboard
        StatusCard(
            isEnabled = isEnabled,
            nextTriggerTime = nextTriggerTime,
            onCalibrate = onCalibrate
        )

        // Title and actions row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "系统发送日志 (${historyList.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )

            if (historyList.isNotEmpty()) {
                TextButton(
                    onClick = { showConfirmClearDialog = true },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.testTag("clear_all_button")
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "清空所有", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("清空记录", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Logs list
        if (historyList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EventNote,
                        contentDescription = "暂无数据",
                        modifier = Modifier.size(60.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "暂无盯盘提醒历史",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "当策略通知轮询触发时，推送的详细数据及本地时间（精确到秒）将安全记录在这里。点击可查看多行全部内容、进行对比与一键复制。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(items = historyList, key = { it.id }) { item ->
                    HistoryItemCard(item = item, onDelete = onDeleteHistory, onClick = { onSelectHistoryItem(item.text) })
                }
            }
        }
    }

    if (showConfirmClearDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmClearDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAll()
                        showConfirmClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("彻底清除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmClearDialog = false }) {
                    Text("取消")
                }
            },
            title = { Text("确认清空列表") },
            text = { Text("你确定要彻底清除所有保存在手机本地的发送数据么？此操作无法恢复。") }
        )
    }
}

@Composable
fun StatusCard(
    isEnabled: Boolean,
    nextTriggerTime: Long,
    onCalibrate: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val nextTriggerFormatted = remember(nextTriggerTime) {
        if (nextTriggerTime > 0) {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            sdf.format(Date(nextTriggerTime))
        } else "无"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        border = if (isEnabled) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Pulse LED indicator
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                color = if (isEnabled) {
                                    Color(0xFF10B981).copy(alpha = pulseAlpha)
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isEnabled) "智能监控轮询中" else "监控提醒已静默",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = if (isEnabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }

                // Mini Calibration Pill
                if (isEnabled) {
                    Button(
                        onClick = onCalibrate,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("calibrate_pill_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(
                            Icons.Default.Sync, 
                            contentDescription = "同步校准", 
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("立即校准", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "下一次通知发出时间（本地）：",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (isEnabled) nextTriggerFormatted else "请在右下角[通知设置]中开启",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                color = if (isEnabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "💡 手机本地时间保障：一旦激活后，将依据设定的基准时间点和轮询分片持续校验，支持开机自动对齐，即使锁屏状态或未常驻前台仍可接收悬浮弹窗。",
                style = MaterialTheme.typography.bodySmall,
                lineHeight = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun HistoryItemCard(
    item: NotificationHistory,
    onDelete: (Int) -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("history_item_${item.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = "详细内容",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp).offset(y = 2.dp)
                    )
                    Text(
                        text = item.text,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = "时间",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = item.formattedTime,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "点击可展开",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "展开",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = { onDelete(item.id) },
                modifier = Modifier.testTag("delete_button_${item.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "删除该条",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                )
            }
        }
    }
}

// ---------------------- 通知设置页面 ----------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    isEnabled: Boolean,
    notificationText: String,
    startTime: String,
    intervalMinutes: Int,
    isDndEnabled: Boolean,
    dndStartTime: String,
    dndEndTime: String,
    themeMode: Int,
    onThemeModeChange: (Int) -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onTextChange: (String) -> Unit,
    onStartTimeChange: (String) -> Unit,
    onIntervalChange: (Int) -> Unit,
    onDndEnabledChange: (Boolean) -> Unit,
    onDndStartTimeChange: (String) -> Unit,
    onDndEndTimeChange: (String) -> Unit,
    onCalibrate: () -> Unit
) {
    var textInput by remember { mutableStateOf(notificationText) }
    var startTimeInput by remember { mutableStateOf(startTime) }
    var intervalInputStr by remember { mutableStateOf(intervalMinutes.toString()) }
    var dndStartInput by remember { mutableStateOf(dndStartTime) }
    var dndEndInput by remember { mutableStateOf(dndEndTime) }

    // Validation States
    val isStartTimeValid = remember(startTimeInput) { isValidTimeFormat(startTimeInput) }
    val isDndStartValid = remember(dndStartInput) { isValidTimeFormat(dndStartInput) }
    val isDndEndValid = remember(dndEndInput) { isValidTimeFormat(dndEndInput) }
    val isIntervalValid = remember(intervalInputStr) {
        val intValue = intervalInputStr.toIntOrNull()
        intValue != null && intValue > 0 && intValue < 1440
    }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Master Switch
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isEnabled) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ),
                    border = if (isEnabled) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)) else null
                ) {
                    ListItem(
                        headlineContent = { 
                            Text("开启监控循环机制", fontWeight = FontWeight.Black, fontSize = 16.sp) 
                        },
                        supportingContent = { 
                            Text(
                                text = if (isEnabled) "正在精准拦截并推送 (完全退后台仍能完美响起)" else "循环彻底停止，处于静默休眠中",
                                style = MaterialTheme.typography.bodySmall
                            ) 
                        },
                        trailingContent = {
                            Switch(
                                checked = isEnabled,
                                onCheckedChange = { checked ->
                                    val finalInterval = intervalInputStr.toIntOrNull() ?: 15
                                    if (checked) {
                                        // Auto validates before allowing enabling
                                        if (textInput.isBlank()) {
                                            Toast.makeText(context, "提醒文字不能为空", Toast.LENGTH_SHORT).show()
                                            return@Switch
                                        }
                                        if (!isValidTimeFormat(startTimeInput)) {
                                            Toast.makeText(context, "开始时间格式不合法（格式须为 HH:mm:ss）", Toast.LENGTH_SHORT).show()
                                            return@Switch
                                        }
                                        if (finalInterval <= 0) {
                                            Toast.makeText(context, "时间间隔须大于0（分钟）", Toast.LENGTH_SHORT).show()
                                            return@Switch
                                        }
                                        // Dispatch current inputs
                                        onTextChange(textInput)
                                        onStartTimeChange(startTimeInput)
                                        onIntervalChange(finalInterval)
                                        
                                        if (isDndEnabled) {
                                            if (!isValidTimeFormat(dndStartInput) || !isValidTimeFormat(dndEndInput)) {
                                                Toast.makeText(context, "免打扰时刻不合法，已自动重置为默认", Toast.LENGTH_SHORT).show()
                                                dndStartInput = "01:00:00"
                                                dndEndInput = "06:00:00"
                                                onDndStartTimeChange("01:00:00")
                                                onDndEndTimeChange("06:00:00")
                                            } else {
                                                onDndStartTimeChange(dndStartInput)
                                                onDndEndTimeChange(dndEndInput)
                                            }
                                        }
                                    }
                                    onEnabledChange(checked)
                                },
                                modifier = Modifier.testTag("master_switch")
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }

            // 🎨 USER-THEME MODE CARD (浅色与深色主题切换)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Text(
                                text = "界面主题设置",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "选择您喜欢的视觉底色，支持浅色、极致暗黑，或完美融入系统：",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val options = listOf("跟随系统", "浅色模式", "深色模式")
                            options.forEachIndexed { index, label ->
                                SegmentedButton(
                                    selected = themeMode == index,
                                    onClick = { onThemeModeChange(index) },
                                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
                                ) {
                                    Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Group 1: Notification Core Config
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.BorderColor, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Text(
                                text = "核心策略提醒配置",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))

                        // Text input for reminder text
                        OutlinedTextField(
                            value = textInput,
                            onValueChange = {
                                textInput = it
                                if (isEnabled && it.isNotBlank()) {
                                    onTextChange(it)
                                }
                            },
                            label = { Text("提醒自定义文字 (支持长文或多行)") },
                            placeholder = { Text("例如：\n【BTC 订单通知】\n已到达 98,000 点位支撑价，策略已买入 0.5手，请注意风险监控！") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("text_input"),
                            trailingIcon = {
                                if (textInput.isNotEmpty()) {
                                    IconButton(onClick = { textInput = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "清除")
                                    }
                                }
                            },
                            minLines = 3,
                            maxLines = 8
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Input for Start Time
                        OutlinedTextField(
                            value = startTimeInput,
                            onValueChange = {
                                startTimeInput = it
                                if (isEnabled && isValidTimeFormat(it)) {
                                    onStartTimeChange(it)
                                }
                            },
                            label = { Text("首发基准时刻 (格式 HH:mm:ss)") },
                            placeholder = { Text("例如：00:12:00") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("start_time_input"),
                            isError = !isStartTimeValid,
                            supportingText = {
                                if (!isStartTimeValid) {
                                    Text("请输入 24 小时制标准的 HH:mm:ss 格式", color = MaterialTheme.colorScheme.error)
                                } else {
                                    Text("以手机本地时间为基准，配合下方间隔分钟生成轮询时刻表。")
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                            trailingIcon = {
                                Icon(
                                    imageVector = if (isStartTimeValid) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = if (isStartTimeValid) Color(0xFF10B981) else MaterialTheme.colorScheme.error
                                )
                            }
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Input for Interval
                        OutlinedTextField(
                            value = intervalInputStr,
                            onValueChange = {
                                intervalInputStr = it
                                val parsed = it.toIntOrNull()
                                if (isEnabled && parsed != null && parsed > 0 && parsed < 1440) {
                                    onIntervalChange(parsed)
                                }
                            },
                            label = { Text("轮询时间片间隔 (分钟)") },
                            placeholder = { Text("例如：15") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("interval_input"),
                            isError = !isIntervalValid,
                            supportingText = {
                                if (!isIntervalValid) {
                                    Text("请输入 1 到 1439 之间的整数", color = MaterialTheme.colorScheme.error)
                                } else {
                                    Text("提醒将在此间隔的算术整数级片准点发出。")
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            trailingIcon = {
                                if (isIntervalValid) {
                                    Text("分钟", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(end = 12.dp))
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Interval Presets
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                            Text("快捷间隔一键部署：", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val presets = listOf(1, 5, 10, 15, 30, 60)
                            presets.forEach { minutes ->
                                val textLabel = when (minutes) {
                                    60 -> "1小时"
                                    else -> "${minutes}分钟"
                                }
                                SuggestionChip(
                                    onClick = {
                                        intervalInputStr = minutes.toString()
                                        if (isEnabled) {
                                            onIntervalChange(minutes)
                                        }
                                    },
                                    label = { Text(textLabel, fontWeight = FontWeight.Bold) }
                                )
                            }
                        }
                    }
                }
            }

            // Group 2: DND Configuration
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Bedtime, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Text(
                                    text = "夜间静默免打扰 (DND)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Switch(
                                checked = isDndEnabled,
                                onCheckedChange = {
                                    if (it) {
                                        if (isValidTimeFormat(dndStartInput) && isValidTimeFormat(dndEndInput)) {
                                            onDndEnabledChange(true)
                                        } else {
                                            Toast.makeText(context, "请先修正合法的免打扰格式时间", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        onDndEnabledChange(false)
                                    }
                                },
                                modifier = Modifier.testTag("dnd_switch")
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "配置休市或睡眠时间的静默期，静默期间算术序列依然在校准，但系统不会发出铃声震动的悬浮弹窗以免打扰。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            lineHeight = 16.sp
                        )

                        if (isDndEnabled) {
                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = dndStartInput,
                                    onValueChange = {
                                        dndStartInput = it
                                        if (isEnabled && isValidTimeFormat(it)) {
                                            onDndStartTimeChange(it)
                                        }
                                    },
                                    label = { Text("静默开始 (HH:mm:ss)") },
                                    placeholder = { Text("01:00:00") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("dnd_start_input"),
                                    isError = !isDndStartValid,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                                )

                                OutlinedTextField(
                                    value = dndEndInput,
                                    onValueChange = {
                                        dndEndInput = it
                                        if (isEnabled && isValidTimeFormat(it)) {
                                            onDndEndTimeChange(it)
                                        }
                                    },
                                    label = { Text("静默结束 (HH:mm:ss)") },
                                    placeholder = { Text("06:00:00") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("dnd_end_input"),
                                    isError = !isDndEndValid,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                                )
                            }

                            if (!isDndStartValid || !isDndEndValid) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "静默期支持跨越子夜的时间。例如 22:30:00 到 07:00:00 均为合法。",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            // High Precision System Guard Tip
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Security,
                                contentDescription = "对齐硬件定时器",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "金融级硬核后台守护规约",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "1. 开机自启动(RECEIVE_BOOT_COMPLETED)与更新重载支持：无论是关机重启，或是版本更新，应用都会保持自愈重载对齐，保证循环监控不丢失。\n2. 微信级悬浮窗实现：底层依托 exact 闹钟，无需前台长存活的常驻通知也能百分百响铃、并显示消息详细文本，无流量与多余耗电消耗。",
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Save & Resync Button
        Button(
            onClick = {
                val finalInterval = intervalInputStr.toIntOrNull() ?: 15
                if (textInput.isBlank()) {
                    Toast.makeText(context, "提醒内容不能是空白", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (!isValidTimeFormat(startTimeInput)) {
                    Toast.makeText(context, "开始时间格式不合法（须为 HH:mm:ss）", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (finalInterval <= 0 || finalInterval >= 1440) {
                    Toast.makeText(context, "每个间隔应当大于0分钟并少于24小时", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                // Dispatch changes
                onTextChange(textInput)
                onStartTimeChange(startTimeInput)
                onIntervalChange(finalInterval)

                if (isDndEnabled) {
                    if (!isValidTimeFormat(dndStartInput) || !isValidTimeFormat(dndEndInput)) {
                        Toast.makeText(context, "免打扰时段格式不合法，已校正为默认时刻", Toast.LENGTH_SHORT).show()
                        dndStartInput = "01:00:00"
                        dndEndInput = "06:00:00"
                        onDndStartTimeChange("01:00:00")
                        onDndEndTimeChange("06:00:00")
                    } else {
                        onDndStartTimeChange(dndStartInput)
                        onDndEndTimeChange(dndEndInput)
                    }
                }

                // Call Calibration force
                onCalibrate()
                Toast.makeText(context, "配置已保存，下发定时轮询对齐刷新！", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("apply_settings_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.Check, contentDescription = "保存修改")
            Spacer(modifier = Modifier.width(8.dp))
            Text("应用配置并在本地校准", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

// Inline simple validator
fun isValidTimeFormat(time: String): Boolean {
    val regex = Regex("^([01]\\d|2[0-3]):[0-5]\\d:[0-5]\\d$")
    return regex.matches(time)
}
