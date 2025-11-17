package com.xunyidi.sealmeet.presentation.screen.meetingdetail

import android.content.ComponentName
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.xunyidi.sealmeet.R
import com.xunyidi.sealmeet.data.local.database.entity.MeetingAgendaEntity
import com.xunyidi.sealmeet.data.local.database.entity.MeetingEntity
import com.xunyidi.sealmeet.data.local.database.entity.MeetingFileEntity
import com.xunyidi.sealmeet.data.local.database.entity.MeetingParticipantEntity
import com.xunyidi.sealmeet.presentation.theme.AppColors
import com.xunyidi.sealmeet.presentation.theme.TextInverse
import java.text.SimpleDateFormat
import java.util.*

/**
 * 会议详情页面
 */
@Composable
fun MeetingDetailScreen(
    meetingId: String,
    viewModel: MeetingDetailViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // 初始化加载数据
    LaunchedEffect(meetingId) {
        viewModel.handleIntent(MeetingDetailContract.Intent.LoadMeetingDetail(meetingId))
    }

    // 监听副作用
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is MeetingDetailContract.Effect.ShowError -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is MeetingDetailContract.Effect.OpenFileViewer -> {
                    try {
                        val intent = createFileViewerIntent(effect.filePath, effect.fileName)
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "打开文件失败: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                is MeetingDetailContract.Effect.NavigateBack -> {
                    onNavigateBack()
                }
            }
        }
    }

    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(AppColors.bgPage)
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = AppColors.primaryDefault
                    )
                }
                state.meeting == null -> {
                    ErrorState(
                        message = state.errorMessage ?: "会议不存在",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    MeetingDetailContent(
                        meeting = state.meeting!!,
                        agendas = state.agendas,
                        onIntent = viewModel::handleIntent
                    )
                }
            }

            // 参会人员对话框
            if (state.showParticipantsDialog && state.participants.isNotEmpty()) {
                ParticipantsDialog(
                    participants = state.participants,
                    onDismiss = {
                        viewModel.handleIntent(MeetingDetailContract.Intent.DismissParticipantsDialog)
                    }
                )
            }

            // 退出确认对话框
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
        }
    }
}

/**
 * 会议详情内容
 */
@Composable
private fun MeetingDetailContent(
    meeting: MeetingEntity,
    agendas: List<MeetingDetailContract.AgendaWithFiles>,
    onIntent: (MeetingDetailContract.Intent) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        // 顶部红色大头部区域
        item {
            MeetingHeader(
                meeting = meeting,
                onSelectMeetingClick = { onIntent(MeetingDetailContract.Intent.ShowExitConfirmDialog) },
                onShowParticipants = { onIntent(MeetingDetailContract.Intent.ShowParticipantsDialog) }
            )
        }

        // 会议说明（如果有description）
        if (!meeting.description.isNullOrBlank()) {
            item {
                MeetingDescriptionCard(
                    description = meeting.description
                )
            }
        }

        // 议程列表
        item {
            AgendaListSection(
                agendas = agendas,
                onFileClick = { fileId ->
                    onIntent(MeetingDetailContract.Intent.OpenFile(fileId))
                }
            )
        }

        // 底部操作按钮
//        item {
//            BottomActionButton()
//        }
    }
}

/**
 * 顶部红色大头部区域
 */
@Composable
private fun MeetingHeader(
    meeting: MeetingEntity,
    onSelectMeetingClick: () -> Unit,
    onShowParticipants: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.primaryDefault)
            .padding(20.dp)
    ) {
        // 顶部栏：标题 + 右上角选择会议按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // 会议标题
                Text(
                    text = meeting.name,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextInverse,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // 元信息行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // 时间
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = TextInverse.copy(alpha = 0.9f),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = formatDateTime(meeting.startTime),
                            fontSize = 14.sp,
                            color = TextInverse.copy(alpha = 0.95f)
                        )
                    }

                    // 地点
                    meeting.location?.let { location ->
                        if (location.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = TextInverse.copy(alpha = 0.9f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = location,
                                    fontSize = 14.sp,
                                    color = TextInverse.copy(alpha = 0.95f)
                                )
                            }
                        }
                    }

                    // 密级
                    Text(
                        text = getSecurityLevelText(meeting.securityLevel),
                        fontSize = 14.sp,
                        color = TextInverse.copy(alpha = 0.95f)
                    )
                }

                // 会议分类
                meeting.category?.let { category ->
                    if (category.isNotBlank()) {
                        Text(
                            text = "分类：$category",
                            fontSize = 14.sp,
                            color = TextInverse.copy(alpha = 0.95f),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            // 右上角：选择会议按钮
            IconButton(onClick = onSelectMeetingClick) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "选择会议",
                    tint = TextInverse
                )
            }
        }

        // 快捷按钮（只有快速会议才显示参会人员按钮）
        if (meeting.type == "tablet") {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onShowParticipants,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.width(140.dp)
                ) {
                    Text(
                        text = "参会人员名单",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextInverse
                    )
                }
            }
        }
    }
}

/**
 * 会议说明卡片
 */
@Composable
private fun MeetingDescriptionCard(description: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = AppColors.bgCard
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "会议说明",
                fontSize = 13.sp,
                color = AppColors.textSecondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = description,
                fontSize = 14.sp,
                color = AppColors.textPrimary,
                lineHeight = 22.sp
            )
        }
    }
}

/**
 * 议程列表区域
 */
@Composable
private fun AgendaListSection(
    agendas: List<MeetingDetailContract.AgendaWithFiles>,
    onFileClick: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = AppColors.bgCard
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            agendas.forEachIndexed { index, agendaWithFiles ->
                AgendaItem(
                    index = index + 1,
                    agenda = agendaWithFiles.agenda,
                    files = agendaWithFiles.files,
                    onFileClick = onFileClick
                )

                if (index < agendas.size - 1) {
                    Divider(
                        modifier = Modifier.padding(vertical = 20.dp),
                        color = AppColors.divider
                    )
                }
            }
        }
    }
}

/**
 * 单个议程项
 */
@Composable
private fun AgendaItem(
    index: Int,
    agenda: MeetingAgendaEntity,
    files: List<MeetingFileEntity>,
    onFileClick: (String) -> Unit
) {
    Column {
        // 议程标题行
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "议题$index",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.textPrimary,
                modifier = Modifier.padding(end = 12.dp)
            )
        }

        // 议题标题
        Text(
            text = agenda.title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = AppColors.textPrimary,
            lineHeight = 22.sp,
            modifier = Modifier.padding(top = 12.dp, bottom = 16.dp)
        )

        // 文件列表
        if (files.isNotEmpty()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                files.forEach { file ->
                    FileItem(
                        file = file,
                        onClick = { onFileClick(file.id) }
                    )
                }
            }
        }

        // 汇报人信息
        agenda.presenter?.let { presenter ->
            if (presenter.isNotBlank()) {
                PresenterInfo(presenterName = presenter)
            }
        }
    }
}

/**
 * 文件项
 */
@Composable
private fun FileItem(
    file: MeetingFileEntity,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.bgPage, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 文件图标
        Icon(
            painter = painterResource(id = getFileIconRes(file.mimeType, file.originalName)),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(40.dp)
        )

        // 文件信息
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = file.originalName,
                fontSize = 14.sp,
                color = AppColors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${getFileTypeText(file.mimeType)} · ${formatFileSize(file.fileSize)}",
                fontSize = 12.sp,
                color = AppColors.textTertiary,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

/**
 * 汇报人信息
 */
@Composable
private fun PresenterInfo(presenterName: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color(0xFFFFF3E0),
                shape = RoundedCornerShape(4.dp)
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            tint = Color(0xFFE65100),
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = "汇报人：",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFFE65100)
        )
        Text(
            text = presenterName,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFE65100)
        )
    }
}

/**
 * 退出确认对话框
 */
@Composable
private fun ExitConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "选择会议",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.textPrimary
            )
        },
        text = {
            Text(
                text = "是否退出当前会议，返回会议列表？",
                fontSize = 14.sp,
                color = AppColors.textSecondary
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = "确认",
                    color = AppColors.primaryDefault,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "取消",
                    color = AppColors.textSecondary
                )
            }
        },
        containerColor = AppColors.bgCard
    )
}

/**
 * 参会人员对话框
 */
@Composable
private fun ParticipantsDialog(
    participants: List<MeetingParticipantEntity>,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = AppColors.bgCard
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // 标题
                Text(
                    text = "参会人员名单",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.textPrimary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // 人员列表
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    participants.forEach { participant ->
                        ParticipantItem(participant = participant)
                    }
                }

                // 关闭按钮
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("关闭", color = AppColors.primaryDefault)
                }
            }
        }
    }
}

/**
 * 参会人员项
 */
@Composable
private fun ParticipantItem(participant: MeetingParticipantEntity) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.bgPage, RoundedCornerShape(8.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 头像
        Surface(
            shape = RoundedCornerShape(50),
            color = AppColors.primaryDefault,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = participant.userName.take(1),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextInverse
                )
            }
        }

        // 信息
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = participant.userName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = AppColors.textPrimary
            )
            Text(
                text = getRoleText(participant.role),
                fontSize = 12.sp,
                color = if (participant.role == "host") AppColors.primaryDefault else AppColors.textTertiary,
                fontWeight = if (participant.role == "host") FontWeight.Medium else FontWeight.Normal,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

/**
 * 底部操作按钮
 */
@Composable
private fun BottomActionButton() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AppColors.bgCard,
        shadowElevation = 8.dp
    ) {
        Button(
            onClick = { /* TODO: 实现票决逻辑 */ },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1976D2)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "进入票决",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}

/**
 * 错误状态
 */
@Composable
private fun ErrorState(
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CalendarToday,
            contentDescription = null,
            tint = AppColors.textTertiary,
            modifier = Modifier.size(64.dp)
        )
        Text(
            text = message,
            fontSize = 16.sp,
            color = AppColors.textSecondary
        )
    }
}

// ========== 工具函数 ==========

/**
 * 格式化日期时间
 */
private fun formatDateTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

/**
 * 获取密级文本
 */
private fun getSecurityLevelText(level: String): String {
    return when (level.lowercase()) {
        "internal" -> "🔒 内部"
        "confidential" -> "🔒 秘密"
        "secret" -> "🔒 机密"
        else -> "🔒 未知"
    }
}

/**
 * 获取文件图标资源
 */
private fun getFileIconRes(mimeType: String, fileName: String): Int {
    return when {
        // Word文档
        mimeType.contains("word", ignoreCase = true) ||
        mimeType.contains("document", ignoreCase = true) ||
        fileName.endsWith(".doc", ignoreCase = true) ||
        fileName.endsWith(".docx", ignoreCase = true) -> R.drawable.ic_file_word
        
        // Excel表格
        mimeType.contains("excel", ignoreCase = true) ||
        mimeType.contains("spreadsheet", ignoreCase = true) ||
        fileName.endsWith(".xls", ignoreCase = true) ||
        fileName.endsWith(".xlsx", ignoreCase = true) -> R.drawable.ic_file_excel
        
        // PowerPoint演示文稿
        mimeType.contains("powerpoint", ignoreCase = true) ||
        mimeType.contains("presentation", ignoreCase = true) ||
        fileName.endsWith(".ppt", ignoreCase = true) ||
        fileName.endsWith(".pptx", ignoreCase = true) -> R.drawable.ic_file_ppt
        
        // 文本文件
        mimeType.contains("text", ignoreCase = true) ||
        fileName.endsWith(".txt", ignoreCase = true) -> R.drawable.ic_file_txt
        
        // 其他文件类型
        else -> R.drawable.ic_file_other
    }
}

/**
 * 获取角色文本
 */
private fun getRoleText(role: String): String {
    return when (role) {
        "host" -> "主持人"
        "participant" -> "参会人"
        "observer" -> "列席人员"
        else -> "未知角色"
    }
}

/**
 * 获取文件类型文本
 */
private fun getFileTypeText(mimeType: String): String {
    return when {
        mimeType.contains("pdf", ignoreCase = true) -> "PDF格式"
        mimeType.contains("word", ignoreCase = true) || mimeType.contains("document", ignoreCase = true) -> "DOCX格式"
        mimeType.contains("excel", ignoreCase = true) || mimeType.contains("spreadsheet", ignoreCase = true) -> "XLSX格式"
        mimeType.contains("powerpoint", ignoreCase = true) || mimeType.contains("presentation", ignoreCase = true) -> "PPTX格式"
        else -> "文档"
    }
}

/**
 * 格式化文件大小
 */
private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
    }
}

/**
 * 创建文件查看器Intent
 */
private fun createFileViewerIntent(filePath: String, fileName: String): Intent {
    val extension = fileName.substringAfterLast('.', "").lowercase()
    
    return when (extension) {
        "ofd" -> {
            // 打开OFD文件
            Intent(Intent.ACTION_VIEW).apply {
                component = ComponentName(
                    "com.westone.ofdreader",
                    "com.westone.ofdreader.MainActivity"
                )
                putExtra("ofdFilePath", filePath)
            }
        }
        "doc", "docx", "xls", "xlsx", "ppt", "pptx" -> {
            // 打开Office文件
            Intent("Start_YOZO_Office").apply {
                putExtra("File_Name", filePath)
                putExtra("File_Path", filePath)
            }
        }
        else -> {
            // 其他文件类型，使用系统默认方式打开
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(
                    android.net.Uri.parse(filePath),
                    getMimeType(extension)
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }
}

/**
 * 根据文件扩展名获取MIME类型
 */
private fun getMimeType(extension: String): String {
    return when (extension) {
        "pdf" -> "application/pdf"
        "doc" -> "application/msword"
        "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        "xls" -> "application/vnd.ms-excel"
        "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        "ppt" -> "application/vnd.ms-powerpoint"
        "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
        "txt" -> "text/plain"
        else -> "*/*"
    }
}
