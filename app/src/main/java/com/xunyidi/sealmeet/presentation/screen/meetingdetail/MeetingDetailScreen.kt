package com.xunyidi.sealmeet.presentation.screen.meetingdetail

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.xunyidi.sealmeet.data.local.database.entity.MeetingEntity
import com.xunyidi.sealmeet.presentation.theme.AppColors
import java.text.SimpleDateFormat
import java.util.*

/**
 * 会议详情页面 - 全屏沉浸式设计
 */
@Composable
fun MeetingDetailScreen(
    meetingId: String,
    viewModel: MeetingDetailViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToAgendas: (String) -> Unit = {},
    onNavigateToLogin: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // 加载会议详情
    LaunchedEffect(meetingId) {
        viewModel.handleIntent(MeetingDetailContract.Intent.LoadMeetingDetail(meetingId))
    }

    // 处理副作用
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is MeetingDetailContract.Effect.NavigateBack -> {
                    onNavigateBack()
                }
                is MeetingDetailContract.Effect.NavigateToAgendas -> {
                    onNavigateToAgendas(effect.meetingId)
                }
                is MeetingDetailContract.Effect.NavigateToLogin -> {
                    onNavigateToLogin()
                }
                is MeetingDetailContract.Effect.ShowError -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is MeetingDetailContract.Effect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is MeetingDetailContract.Effect.OpenFileViewer -> {
                    Toast.makeText(context, "打开文件: ${effect.fileName}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 主界面
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        AppColors.primaryDefault,
                        AppColors.primaryActive
                    )
                )
            )
    ) {
        when {
            state.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White
                )
            }
            state.meeting != null -> {
                MeetingDetailContent(
                    state = state,
                    onBackClick = {
                        viewModel.handleIntent(MeetingDetailContract.Intent.NavigateBack)
                    },
                    onExitClick = {
                        viewModel.handleIntent(MeetingDetailContract.Intent.ShowExitConfirmDialog)
                    },
                    onSignInClick = {
                        viewModel.handleIntent(MeetingDetailContract.Intent.ClickSignIn)
                    },
                    onInfoClick = {
                        viewModel.handleIntent(MeetingDetailContract.Intent.ClickMeetingInfo)
                    },
                    onAgendasClick = {
                        viewModel.handleIntent(MeetingDetailContract.Intent.ClickAgendas)
                    },
                    onVotingClick = {
                        viewModel.handleIntent(MeetingDetailContract.Intent.ClickVoting)
                    },
                    onRecordsClick = {
                        viewModel.handleIntent(MeetingDetailContract.Intent.ClickRecords)
                    }
                )
            }
            else -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = state.errorMessage ?: "加载失败",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 16.sp
                    )
                }
            }
        }
    }

    // 弹窗
    if (state.showInfoDialog && state.meeting != null) {
        MeetingInfoDialog(
            meeting = state.meeting!!,
            participants = state.participants,
            onDismiss = {
                viewModel.handleIntent(MeetingDetailContract.Intent.DismissInfoDialog)
            },
            onShowParticipants = {
                viewModel.handleIntent(MeetingDetailContract.Intent.DismissInfoDialog)
                viewModel.handleIntent(MeetingDetailContract.Intent.ShowParticipantsDialog)
            }
        )
    }

    if (state.showParticipantsDialog) {
        ParticipantsDialog(
            participants = state.participants,
            onDismiss = {
                viewModel.handleIntent(MeetingDetailContract.Intent.DismissParticipantsDialog)
            }
        )
    }

    if (state.showExitConfirmDialog) {
        ExitConfirmDialog(
            onConfirm = {
                viewModel.handleIntent(MeetingDetailContract.Intent.ConfirmExit)
            },
            onDismiss = {
                viewModel.handleIntent(MeetingDetailContract.Intent.DismissExitConfirmDialog)
            }
        )
    }

    if (state.showPasswordSignInDialog) {
        PasswordSignInDialog(
            password = state.signInPassword,
            error = state.signInPasswordError,
            onPasswordChange = { password ->
                viewModel.handleIntent(MeetingDetailContract.Intent.UpdateSignInPassword(password))
            },
            onConfirm = {
                viewModel.handleIntent(MeetingDetailContract.Intent.SubmitPasswordSignIn)
            },
            onDismiss = {
                viewModel.handleIntent(MeetingDetailContract.Intent.DismissPasswordSignInDialog)
            }
        )
    }

    if (state.showManualSignInDialog) {
        ManualSignInDialog(
            onConfirm = { bitmap ->
                viewModel.handleIntent(MeetingDetailContract.Intent.SubmitManualSignIn(bitmap))
            },
            onDismiss = {
                viewModel.handleIntent(MeetingDetailContract.Intent.DismissManualSignInDialog)
            }
        )
    }
}

/**
 * 会议详情内容
 */
@Composable
private fun MeetingDetailContent(
    state: MeetingDetailContract.State,
    onBackClick: () -> Unit,
    onExitClick: () -> Unit,
    onSignInClick: () -> Unit,
    onInfoClick: () -> Unit,
    onAgendasClick: () -> Unit,
    onVotingClick: () -> Unit,
    onRecordsClick: () -> Unit
) {
    val meeting = state.meeting ?: return
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // 顶部栏
        TopBar(
            currentUserName = state.currentUserName,
            securityLevel = meeting.securityLevel,
            onBackClick = onBackClick,
            onExitClick = onExitClick
        )
        
        // 中间内容区
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 会议信息
            MeetingInfo(meeting = meeting)
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // 功能按钮（含签到按钮）
            FunctionButtons(
                needSignIn = state.needSignIn,
                isSignedIn = state.isSignedIn,
                isFunctionEnabled = state.isFunctionEnabled,
                onSignInClick = onSignInClick,
                onInfoClick = onInfoClick,
                onAgendasClick = onAgendasClick,
                onVotingClick = onVotingClick,
                onRecordsClick = onRecordsClick
            )
        }
    }
}

/**
 * 顶部栏
 */
@Composable
private fun TopBar(
    currentUserName: String?,
    securityLevel: String,
    onBackClick: () -> Unit,
    onExitClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onBackClick)
                .padding(8.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "返回",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (currentUserName != null) {
                    "$currentUserName，欢迎您参加会议"
                } else {
                    "欢迎参加会议"
                },
                color = Color.White,
                fontSize = 16.sp
            )
        }
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SecurityLevelTag(securityLevel = securityLevel)
            
            IconButton(
                onClick = onExitClick,
                modifier = Modifier
                    .background(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "退出",
                    tint = Color.White
                )
            }
        }
    }
}

/**
 * 密级标签
 */
@Composable
private fun SecurityLevelTag(securityLevel: String) {
    val (bgColor, text) = when (securityLevel.lowercase()) {
        "internal" -> Pair(Color(0xFF4CAF50), "内部")
        "confidential" -> Pair(Color(0xFFFFC107), "秘密")
        "secret" -> Pair(Color(0xFFF44336), "机密")
        else -> Pair(Color(0xFF9E9E9E), "未知")
    }
    
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bgColor
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

/**
 * 会议信息
 */
@Composable
private fun MeetingInfo(meeting: MeetingEntity) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = meeting.name,
            color = Color.White,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        if (!meeting.description.isNullOrBlank()) {
            Text(
                text = "会议主题：${meeting.description}",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )
        }
        
        val timeText = formatMeetingTime(meeting.startTime, meeting.endTime)
        Text(
            text = "会议时间：$timeText",
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
        
        if (!meeting.location.isNullOrBlank()) {
            Text(
                text = "会议地点：${meeting.location}",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * 功能按钮区（含签到按钮）
 */
@Composable
private fun FunctionButtons(
    needSignIn: Boolean,
    isSignedIn: Boolean,
    isFunctionEnabled: Boolean,
    onSignInClick: () -> Unit,
    onInfoClick: () -> Unit,
    onAgendasClick: () -> Unit,
    onVotingClick: () -> Unit,
    onRecordsClick: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(40.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 签到按钮（仅非免签模式显示）
        if (needSignIn) {
            FunctionButton(
                icon = if (isSignedIn) Icons.Default.CheckCircle else Icons.Default.HowToReg,
                label = if (isSignedIn) "已签到" else "签到",
                backgroundColor = if (isSignedIn) Color(0xFF4CAF50) else Color(0xFF9C27B0),
                enabled = true, // 签到按钮始终可点击（可更新签名）
                onClick = onSignInClick
            )
        }
        
        // 会议介绍
        FunctionButton(
            icon = Icons.Default.Description,
            label = "会议介绍",
            backgroundColor = Color(0xFFFF9800),
            enabled = isFunctionEnabled,
            onClick = onInfoClick
        )
        
        // 会议议题
        FunctionButton(
            icon = Icons.Default.Forum,
            label = "会议议题",
            backgroundColor = Color(0xFF4CAF50),
            enabled = isFunctionEnabled,
            onClick = onAgendasClick
        )
        
        // 会议投票
        FunctionButton(
            icon = Icons.Default.HowToVote,
            label = "会议投票",
            backgroundColor = Color(0xFFE91E63),
            enabled = isFunctionEnabled,
            onClick = onVotingClick
        )
        
        // 会议记录
        FunctionButton(
            icon = Icons.Default.FolderOpen,
            label = "会议记录",
            backgroundColor = Color(0xFF2196F3),
            enabled = isFunctionEnabled,
            onClick = onRecordsClick
        )
    }
}

/**
 * 功能按钮
 */
@Composable
private fun FunctionButton(
    icon: ImageVector,
    label: String,
    backgroundColor: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val alpha = if (enabled) 1f else 0.4f
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(enabled = enabled, onClick = onClick)
            .graphicsLayer { this.alpha = alpha }
    ) {
        Surface(
            modifier = Modifier.size(88.dp),
            shape = RoundedCornerShape(20.dp),
            color = backgroundColor
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = label,
            color = Color.White.copy(alpha = if (enabled) 1f else 0.5f),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun formatMeetingTime(startTime: Long, endTime: Long): String {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    val startStr = dateFormat.format(Date(startTime))
    val endStr = dateFormat.format(Date(endTime))
    return "$startStr 至 $endStr"
}

// ========== 弹窗组件 ==========

/**
 * 密码签到弹窗
 */
@Composable
private fun PasswordSignInDialog(
    password: String,
    error: String?,
    onPasswordChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = AppColors.bgCard)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "密码签到",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.textPrimary
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                OutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = { Text("请输入6位密码") },
                    isError = error != null,
                    supportingText = if (error != null) {
                        { Text(error, color = MaterialTheme.colorScheme.error) }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("取消")
                    }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.primaryDefault)
                    ) {
                        Text("确认签到")
                    }
                }
            }
        }
    }
}

/**
 * 手写签到弹窗 - 使用贝塞尔曲线平滑
 */
@Composable
private fun ManualSignInDialog(
    onConfirm: (Bitmap) -> Unit,
    onDismiss: () -> Unit
) {
    // 存储所有点（用于贝塞尔曲线平滑）
    var allPoints by remember { mutableStateOf(listOf<List<Offset>>()) }
    var currentPoints by remember { mutableStateOf(listOf<Offset>()) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = AppColors.bgCard)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "手写签到",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.textPrimary
                )
                
                Text(
                    text = "请在下方区域签名",
                    fontSize = 14.sp,
                    color = AppColors.textSecondary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 签名区域
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .border(1.dp, AppColors.divider, RoundedCornerShape(8.dp))
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        currentPoints = listOf(offset)
                                    },
                                    onDrag = { change, _ ->
                                        currentPoints = currentPoints + change.position
                                    },
                                    onDragEnd = {
                                        if (currentPoints.isNotEmpty()) {
                                            allPoints = allPoints + listOf(currentPoints)
                                            currentPoints = emptyList()
                                        }
                                    }
                                )
                            }
                    ) {
                        canvasSize = IntSize(size.width.toInt(), size.height.toInt())
                        
                        // 绘制已保存的路径（贝塞尔曲线平滑）
                        allPoints.forEach { points ->
                            drawSmoothPath(points)
                        }
                        
                        // 绘制当前路径
                        drawSmoothPath(currentPoints)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            allPoints = emptyList()
                            currentPoints = emptyList()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("清除")
                    }
                    
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("取消")
                    }
                    
                    Button(
                        onClick = {
                            if (allPoints.isNotEmpty() && canvasSize.width > 0 && canvasSize.height > 0) {
                                val bitmap = createSignatureBitmap(allPoints, canvasSize)
                                onConfirm(bitmap)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = allPoints.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.primaryDefault)
                    ) {
                        Text("确认签到")
                    }
                }
            }
        }
    }
}

/**
 * 绘制平滑路径（贝塞尔曲线）
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSmoothPath(points: List<Offset>) {
    if (points.size < 2) return
    
    val path = Path().apply {
        moveTo(points.first().x, points.first().y)
        
        if (points.size == 2) {
            // 只有两个点，直接画线
            lineTo(points[1].x, points[1].y)
        } else {
            // 使用二次贝塞尔曲线平滑
            for (i in 1 until points.size) {
                val p0 = points[i - 1]
                val p1 = points[i]
                
                // 计算控制点（两点中点）
                val midX = (p0.x + p1.x) / 2
                val midY = (p0.y + p1.y) / 2
                
                if (i == 1) {
                    // 第一段，从起点到中点
                    lineTo(midX, midY)
                } else {
                    // 使用前一个中点作为控制点
                    val prevMidX = (points[i - 2].x + p0.x) / 2
                    val prevMidY = (points[i - 2].y + p0.y) / 2
                    quadraticBezierTo(p0.x, p0.y, midX, midY)
                }
            }
            
            // 最后一段到终点
            lineTo(points.last().x, points.last().y)
        }
    }
    
    drawPath(
        path = path,
        color = Color.Black,
        style = Stroke(
            width = 5f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}

/**
 * 创建签名Bitmap（使用贝塞尔曲线）
 */
private fun createSignatureBitmap(allPoints: List<List<Offset>>, canvasSize: IntSize): Bitmap {
    val bitmap = Bitmap.createBitmap(canvasSize.width, canvasSize.height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.WHITE)
    
    val paint = android.graphics.Paint().apply {
        color = android.graphics.Color.BLACK
        strokeWidth = 5f
        style = android.graphics.Paint.Style.STROKE
        strokeCap = android.graphics.Paint.Cap.ROUND
        strokeJoin = android.graphics.Paint.Join.ROUND
        isAntiAlias = true
    }
    
    allPoints.forEach { points ->
        if (points.size >= 2) {
            val path = android.graphics.Path().apply {
                moveTo(points.first().x, points.first().y)
                
                if (points.size == 2) {
                    lineTo(points[1].x, points[1].y)
                } else {
                    for (i in 1 until points.size) {
                        val p0 = points[i - 1]
                        val p1 = points[i]
                        val midX = (p0.x + p1.x) / 2
                        val midY = (p0.y + p1.y) / 2
                        
                        if (i == 1) {
                            lineTo(midX, midY)
                        } else {
                            quadTo(p0.x, p0.y, midX, midY)
                        }
                    }
                    lineTo(points.last().x, points.last().y)
                }
            }
            canvas.drawPath(path, paint)
        }
    }
    
    return bitmap
}

/**
 * 会议介绍弹窗
 */
@Composable
private fun MeetingInfoDialog(
    meeting: MeetingEntity,
    participants: List<com.xunyidi.sealmeet.data.local.database.entity.MeetingParticipantEntity>,
    onDismiss: () -> Unit,
    onShowParticipants: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = AppColors.bgCard)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "会议介绍",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.textPrimary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = AppColors.divider)
                Spacer(modifier = Modifier.height(16.dp))
                
                InfoRow(label = "会议名称", value = meeting.name)
                InfoRow(label = "会议主题", value = meeting.description ?: "--")
                InfoRow(label = "会议时间", value = formatMeetingTime(meeting.startTime, meeting.endTime))
                InfoRow(label = "会议地点", value = meeting.location ?: "--")
                InfoRow(label = "主持人", value = meeting.hostName ?: "--")
                InfoRow(label = "组织者", value = meeting.organizerName ?: "--")
                InfoRow(label = "参会人数", value = "${participants.size}人")
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(onClick = onShowParticipants, modifier = Modifier.weight(1f)) {
                        Text("查看参会人员")
                    }
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.primaryDefault)
                    ) {
                        Text("关闭")
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = AppColors.textSecondary, fontSize = 14.sp)
        Text(
            text = value,
            color = AppColors.textPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 200.dp)
        )
    }
}

/**
 * 参会人员弹窗
 */
@Composable
private fun ParticipantsDialog(
    participants: List<com.xunyidi.sealmeet.data.local.database.entity.MeetingParticipantEntity>,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .fillMaxHeight(0.6f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = AppColors.bgCard)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "参会人员 (${participants.size})",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.textPrimary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = AppColors.textSecondary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = AppColors.divider)
                Spacer(modifier = Modifier.height(8.dp))
                
                if (participants.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "暂无参会人员信息", color = AppColors.textTertiary)
                    }
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(participants.size) { index ->
                            ParticipantItem(participant = participants[index])
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ParticipantItem(
    participant: com.xunyidi.sealmeet.data.local.database.entity.MeetingParticipantEntity
) {
    val roleText = when (participant.role) {
        "host" -> "主持人"
        "participant" -> "参会人"
        "observer" -> "列席"
        else -> participant.role
    }
    
    val roleColor = when (participant.role) {
        "host" -> AppColors.primaryDefault
        else -> AppColors.textSecondary
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.bgContainer, RoundedCornerShape(8.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(20.dp),
                color = AppColors.primaryDefault.copy(alpha = 0.2f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = participant.userName.take(1),
                        color = AppColors.primaryDefault,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Column {
                Text(
                    text = participant.userName,
                    color = AppColors.textPrimary,
                    fontWeight = FontWeight.Medium
                )
                if (!participant.department.isNullOrBlank()) {
                    Text(
                        text = participant.department,
                        color = AppColors.textTertiary,
                        fontSize = 12.sp
                    )
                }
            }
        }
        
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = roleColor.copy(alpha = 0.1f)
        ) {
            Text(
                text = roleText,
                color = roleColor,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

/**
 * 退出确认弹窗
 */
@Composable
private fun ExitConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "退出会议", fontWeight = FontWeight.Bold) },
        text = { Text("确定要退出当前会议吗？退出后将返回登录页面。") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
            ) {
                Text("确定退出")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
