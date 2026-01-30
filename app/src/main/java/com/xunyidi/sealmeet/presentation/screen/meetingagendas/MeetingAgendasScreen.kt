package com.xunyidi.sealmeet.presentation.screen.meetingagendas

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xunyidi.sealmeet.R
import com.xunyidi.sealmeet.data.local.database.entity.MeetingAgendaEntity
import com.xunyidi.sealmeet.data.local.database.entity.MeetingFileEntity
import com.xunyidi.sealmeet.presentation.service.FloatingFileService
import com.xunyidi.sealmeet.presentation.theme.AppColors
import java.io.File

/**
 * 会议议题页面 - 纯列表展开式
 */
@Composable
fun MeetingAgendasScreen(
    meetingId: String,
    viewModel: MeetingAgendasViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // 页面退出时停止悬浮服务
    DisposableEffect(Unit) {
        onDispose {
            FloatingFileService.stop(context)
        }
    }

    // 加载数据
    LaunchedEffect(meetingId) {
        viewModel.handleIntent(MeetingAgendasContract.Intent.LoadAgendas(meetingId))
    }

    // 处理副作用
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is MeetingAgendasContract.Effect.NavigateBack -> {
                    // 返回时停止悬浮服务
                    FloatingFileService.stop(context)
                    onNavigateBack()
                }
                is MeetingAgendasContract.Effect.ShowError -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is MeetingAgendasContract.Effect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is MeetingAgendasContract.Effect.OpenFileViewer -> {
                    try {
                        // 启动悬浮文件选择器
                        if (Settings.canDrawOverlays(context)) {
                            FloatingFileService.start(context, meetingId)
                        } else {
                            // 没有悬浮窗权限，提示用户
                            Toast.makeText(context, "请授予悬浮窗权限以启用快速切换文件", Toast.LENGTH_LONG).show()
                            // 跳转到权限设置页面
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        }
                        
                        // 打开文件
                        val intent = createFileViewerIntent(effect.filePath, effect.fileName)
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "打开文件失败: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            AgendasTopBar(
                meetingName = state.meeting?.name ?: "",
                securityLevel = state.meeting?.securityLevel ?: "internal",
                onBackClick = {
                    viewModel.handleIntent(MeetingAgendasContract.Intent.NavigateBack)
                }
            )
        },
        containerColor = AppColors.bgPage
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = AppColors.primaryDefault
                    )
                }
                state.agendas.isEmpty() -> {
                    EmptyState(modifier = Modifier.align(Alignment.Center))
                }
                else -> {
                    AgendaList(
                        agendas = state.agendas,
                        expandedAgendaIds = state.expandedAgendaIds,
                        onToggleExpand = { agendaId ->
                            viewModel.handleIntent(MeetingAgendasContract.Intent.ToggleAgendaExpanded(agendaId))
                        },
                        onFileClick = { file ->
                            viewModel.handleIntent(MeetingAgendasContract.Intent.OpenFile(file))
                        }
                    )
                }
            }
        }
    }
}

/**
 * 顶部栏
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AgendasTopBar(
    meetingName: String,
    securityLevel: String,
    onBackClick: () -> Unit
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "会议议题",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                if (meetingName.isNotBlank()) {
                    Text(
                        text = "|",
                        fontSize = 18.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                    Text(
                        text = meetingName,
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "返回",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        actions = {
            // 密级标签
            SecurityLevelTag(securityLevel = securityLevel)
            Spacer(modifier = Modifier.width(16.dp))
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = AppColors.primaryDefault,
            titleContentColor = Color.White
        )
    )
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
        shape = RoundedCornerShape(4.dp),
        color = bgColor
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/**
 * 议程列表
 */
@Composable
private fun AgendaList(
    agendas: List<MeetingAgendasContract.AgendaWithFiles>,
    expandedAgendaIds: Set<String>,
    onToggleExpand: (String) -> Unit,
    onFileClick: (MeetingFileEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(agendas) { index, agendaWithFiles ->
            AgendaItem(
                index = index + 1,
                agenda = agendaWithFiles.agenda,
                files = agendaWithFiles.files,
                isExpanded = expandedAgendaIds.contains(agendaWithFiles.agenda.id),
                onToggleExpand = { onToggleExpand(agendaWithFiles.agenda.id) },
                onFileClick = onFileClick
            )
        }
    }
}

/**
 * 议程项（可展开/折叠）
 */
@Composable
private fun AgendaItem(
    index: Int,
    agenda: MeetingAgendaEntity,
    files: List<MeetingFileEntity>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onFileClick: (MeetingFileEntity) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.bgCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // 议程头部（点击展开/折叠）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧：序号 + 标题 + 汇报人
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 序号
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = AppColors.primaryDefault
                    ) {
                        Text(
                            text = "$index",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                    
                    // 标题和汇报人
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = agenda.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = AppColors.textPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        if (!agenda.presenter.isNullOrBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = AppColors.textTertiary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = agenda.presenter,
                                    fontSize = 13.sp,
                                    color = AppColors.textSecondary
                                )
                            }
                        }
                    }
                }
                
                // 右侧：文件数量 + 展开/折叠图标
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (files.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = AppColors.primaryDefault.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = "${files.size}个文件",
                                fontSize = 12.sp,
                                color = AppColors.primaryDefault,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "折叠" else "展开",
                        tint = AppColors.textTertiary
                    )
                }
            }
            
            // 文件列表（展开时显示）
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                if (files.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AppColors.bgPage.copy(alpha = 0.5f))
                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        files.forEach { file ->
                            FileItem(
                                file = file,
                                onClick = { onFileClick(file) }
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AppColors.bgPage.copy(alpha = 0.5f))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "暂无文件",
                            fontSize = 14.sp,
                            color = AppColors.textTertiary
                        )
                    }
                }
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
            .clip(RoundedCornerShape(8.dp))
            .background(AppColors.bgCard)
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
        Column(modifier = Modifier.weight(1f)) {
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
        
        // 文件密级标签
        FileSecurityLevelTag(securityLevel = file.securityLevel)
        
        // 打开图标
        Icon(
            imageVector = Icons.Default.OpenInNew,
            contentDescription = "打开",
            tint = AppColors.textTertiary,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * 文件密级标签
 */
@Composable
private fun FileSecurityLevelTag(securityLevel: String) {
    val (bgColor, text) = when (securityLevel.lowercase()) {
        "internal" -> Pair(Color(0xFF4CAF50), "内部")
        "confidential" -> Pair(Color(0xFFFFC107), "秘密")
        "secret" -> Pair(Color(0xFFF44336), "机密")
        else -> Pair(Color(0xFF9E9E9E), "未知")
    }
    
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = bgColor.copy(alpha = 0.15f)
    ) {
        Text(
            text = text,
            color = bgColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

/**
 * 空状态
 */
@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.FolderOpen,
            contentDescription = null,
            tint = AppColors.textTertiary,
            modifier = Modifier.size(64.dp)
        )
        Text(
            text = "暂无议题",
            fontSize = 16.sp,
            color = AppColors.textSecondary
        )
    }
}

// ========== 工具函数 ==========

/**
 * 获取文件图标资源
 */
private fun getFileIconRes(mimeType: String, fileName: String): Int {
    return when {
        mimeType.contains("word", ignoreCase = true) ||
        mimeType.contains("document", ignoreCase = true) ||
        fileName.endsWith(".doc", ignoreCase = true) ||
        fileName.endsWith(".docx", ignoreCase = true) -> R.drawable.ic_file_word

        mimeType.contains("excel", ignoreCase = true) ||
        mimeType.contains("spreadsheet", ignoreCase = true) ||
        fileName.endsWith(".xls", ignoreCase = true) ||
        fileName.endsWith(".xlsx", ignoreCase = true) -> R.drawable.ic_file_excel

        mimeType.contains("powerpoint", ignoreCase = true) ||
        mimeType.contains("presentation", ignoreCase = true) ||
        fileName.endsWith(".ppt", ignoreCase = true) ||
        fileName.endsWith(".pptx", ignoreCase = true) -> R.drawable.ic_file_ppt

        mimeType.contains("pdf", ignoreCase = true) ||
        fileName.endsWith(".pdf", ignoreCase = true) -> R.drawable.ic_file_pdf

        mimeType.contains("text", ignoreCase = true) ||
        fileName.endsWith(".txt", ignoreCase = true) -> R.drawable.ic_file_txt

        else -> R.drawable.ic_file_other
    }
}

/**
 * 获取文件类型文本
 */
private fun getFileTypeText(mimeType: String): String {
    return when {
        mimeType.contains("pdf", ignoreCase = true) -> "PDF"
        mimeType.contains("word", ignoreCase = true) || mimeType.contains("document", ignoreCase = true) -> "Word"
        mimeType.contains("excel", ignoreCase = true) || mimeType.contains("spreadsheet", ignoreCase = true) -> "Excel"
        mimeType.contains("powerpoint", ignoreCase = true) || mimeType.contains("presentation", ignoreCase = true) -> "PPT"
        mimeType.contains("text", ignoreCase = true) -> "文本"
        else -> "文件"
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
        "doc", "docx", "xls", "xlsx", "ppt", "pptx", "pdf" -> {
            // 打开Office文件和PDF
            Intent("Start_YOZO_Office").apply {
                putExtra("File_Name", filePath)
                putExtra("File_Path", filePath)
            }
        }
        else -> {
            // 其他文件类型，使用系统默认方式打开
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(
                    Uri.fromFile(File(filePath)),
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
