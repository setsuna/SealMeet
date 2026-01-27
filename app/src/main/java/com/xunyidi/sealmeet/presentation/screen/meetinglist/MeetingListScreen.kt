package com.xunyidi.sealmeet.presentation.screen.meetinglist

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xunyidi.sealmeet.data.local.database.entity.MeetingEntity
import com.xunyidi.sealmeet.presentation.theme.AppColors
import com.xunyidi.sealmeet.presentation.theme.TextInverse
import java.text.SimpleDateFormat
import java.util.*

/**
 * 会议列表页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetingListScreen(
    meetingType: String,
    viewModel: MeetingListViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToDetail: (String) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // 设置会议类型
    LaunchedEffect(meetingType) {
        viewModel.handleIntent(MeetingListContract.Intent.SetMeetingType(meetingType))
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is MeetingListContract.Effect.NavigateToMeetingDetail -> {
                    onNavigateToDetail(effect.meetingId)
                }
                is MeetingListContract.Effect.ShowError -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val titleText = when (state.meetingType) {
        "all" -> "全部会议"
        "standard" -> "标准会议"
        "tablet" -> "快速会议"
        else -> "会议列表"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = titleText,
                        color = TextInverse
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回",
                            tint = TextInverse
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.primaryDefault
                )
            )
        }
    ) { paddingValues ->
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
                state.meetingType == "all" -> {
                    // 分组显示：两个可折叠的Card
                    GroupedMeetingList(
                        standardMeetings = state.standardMeetings,
                        tabletMeetings = state.tabletMeetings,
                        isStandardExpanded = state.isStandardExpanded,
                        isTabletExpanded = state.isTabletExpanded,
                        onToggleStandardExpanded = {
                            viewModel.handleIntent(MeetingListContract.Intent.ToggleStandardExpanded)
                        },
                        onToggleTabletExpanded = {
                            viewModel.handleIntent(MeetingListContract.Intent.ToggleTabletExpanded)
                        },
                        onMeetingClick = { meetingId ->
                            viewModel.handleIntent(
                                MeetingListContract.Intent.SelectMeeting(meetingId)
                            )
                        }
                    )
                }
                state.meetings.isEmpty() -> {
                    EmptyState(
                        meetingType = state.meetingType,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    MeetingTimelineList(
                        meetings = state.meetings,
                        onMeetingClick = { meetingId ->
                            viewModel.handleIntent(
                                MeetingListContract.Intent.SelectMeeting(meetingId)
                            )
                        }
                    )
                }
            }
        }
    }
}

/**
 * 分组会议列表 - 两个可折叠的Card
 */
@Composable
private fun GroupedMeetingList(
    standardMeetings: List<MeetingEntity>,
    tabletMeetings: List<MeetingEntity>,
    isStandardExpanded: Boolean,
    isTabletExpanded: Boolean,
    onToggleStandardExpanded: () -> Unit,
    onToggleTabletExpanded: () -> Unit,
    onMeetingClick: (String) -> Unit
) {
    val isEmpty = standardMeetings.isEmpty() && tabletMeetings.isEmpty()
    
    if (isEmpty) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            EmptyState(meetingType = "all")
        }
        return
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 标准会议 Card
        MeetingGroupCard(
            title = "标准会议",
            count = standardMeetings.size,
            isExpanded = isStandardExpanded,
            onToggleExpanded = onToggleStandardExpanded,
            meetings = standardMeetings,
            onMeetingClick = onMeetingClick,
            emptyText = "暂无标准会议"
        )
        
        // 快速会议 Card
        MeetingGroupCard(
            title = "快速会议",
            count = tabletMeetings.size,
            isExpanded = isTabletExpanded,
            onToggleExpanded = onToggleTabletExpanded,
            meetings = tabletMeetings,
            onMeetingClick = onMeetingClick,
            emptyText = "暂无快速会议"
        )
    }
}

/**
 * 会议分组卡片
 */
@Composable
private fun MeetingGroupCard(
    title: String,
    count: Int,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    meetings: List<MeetingEntity>,
    onMeetingClick: (String) -> Unit,
    emptyText: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = AppColors.bgCard
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column {
            // 标题栏（可点击展开/收起）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpanded)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.textPrimary
                    )
                    // 数量标签
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = AppColors.primaryDefault.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = count.toString(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = AppColors.primaryDefault,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "收起" else "展开",
                    tint = AppColors.textSecondary
                )
            }
            
            // 分隔线
            if (isExpanded) {
                HorizontalDivider(
                    color = AppColors.divider,
                    thickness = 1.dp
                )
            }
            
            // 内容区域（带动画）
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                if (meetings.isEmpty()) {
                    // 空状态
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = emptyText,
                            fontSize = 14.sp,
                            color = AppColors.textTertiary
                        )
                    }
                } else {
                    // 会议列表
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        meetings.forEachIndexed { index, meeting ->
                            MeetingCompactItem(
                                meeting = meeting,
                                isAlternate = index % 2 == 1,
                                onClick = { onMeetingClick(meeting.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 紧凑的会议项（用于分组卡片内）
 */
@Composable
private fun MeetingCompactItem(
    meeting: MeetingEntity,
    isAlternate: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isAlternate) {
        AppColors.bgContainer
    } else {
        AppColors.bgCard
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧：时间
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(60.dp)
            ) {
                TimeDisplayCompact(timestamp = meeting.startTime)
                DateDisplayCompact(timestamp = meeting.startTime)
            }
            
            // 分隔线
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(40.dp)
                    .background(AppColors.divider)
            )
            
            // 中间：会议信息
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = meeting.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 地点
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = AppColors.textTertiary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = meeting.location?.takeIf { it.isNotBlank() } ?: "--",
                            fontSize = 12.sp,
                            color = AppColors.textTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    // 主持人
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = AppColors.textTertiary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = meeting.hostName?.takeIf { it.isNotBlank() } ?: "--",
                            fontSize = 12.sp,
                            color = AppColors.textTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            
            // 右侧：密级标签
            SecurityLevelTagCompact(securityLevel = meeting.securityLevel)
        }
    }
}

/**
 * 紧凑时间显示
 */
@Composable
private fun TimeDisplayCompact(timestamp: Long) {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = timestamp
    }
    
    val hour12 = calendar.get(Calendar.HOUR)
    val displayHour = if (hour12 == 0) 12 else hour12
    val minute = calendar.get(Calendar.MINUTE)
    val timeText = String.format("%d:%02d", displayHour, minute)
    
    Text(
        text = timeText,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = AppColors.textPrimary
    )
}

/**
 * 紧凑日期显示
 */
@Composable
private fun DateDisplayCompact(timestamp: Long) {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = timestamp
    }
    
    val dateFormat = SimpleDateFormat("M/d", Locale.getDefault())
    
    Text(
        text = dateFormat.format(calendar.time),
        fontSize = 11.sp,
        color = AppColors.textTertiary
    )
}

/**
 * 紧凑密级标签
 */
@Composable
private fun SecurityLevelTagCompact(securityLevel: String) {
    val (bgColor, textColor, labelText) = when (securityLevel.lowercase()) {
        "internal" -> Triple(Color(0xFF4CAF50), Color.White, "内部")
        "confidential" -> Triple(Color(0xFFFFC107), Color.Black, "秘密")
        "secret" -> Triple(Color(0xFFF44336), Color.White, "机密")
        else -> Triple(Color(0xFF9E9E9E), Color.White, "未知")
    }
    
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = bgColor
    ) {
        Text(
            text = labelText,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}

/**
 * 会议时间线列表 - 按日期分组
 */
@Composable
private fun MeetingTimelineList(
    meetings: List<MeetingEntity>,
    onMeetingClick: (String) -> Unit
) {
    // 按日期分组
    val groupedMeetings = meetings.groupBy { meeting ->
        val calendar = Calendar.getInstance().apply {
            timeInMillis = meeting.startTime
        }
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
    }.toSortedMap()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        groupedMeetings.forEach { (dateKey, meetingsInDay) ->
            // 日期标题
            item(key = "header_$dateKey") {
                DateHeader(timestamp = meetingsInDay.first().startTime)
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // 该日期下的会议列表
            itemsIndexed(
                items = meetingsInDay,
                key = { _, meeting -> meeting.id }
            ) { index, meeting ->
                MeetingTimelineItem(
                    meeting = meeting,
                    isAlternate = index % 2 == 1,
                    onClick = { onMeetingClick(meeting.id) }
                )
                
                if (index < meetingsInDay.size - 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            
            // 日期组之间的间距
            item(key = "spacer_$dateKey") {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

/**
 * 日期标题
 */
@Composable
private fun DateHeader(timestamp: Long) {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = timestamp
    }
    
    val weekFormat = SimpleDateFormat("EEEE", Locale.CHINESE)
    val dateFormat = SimpleDateFormat("M月d日 yyyy", Locale.CHINESE)
    
    val weekText = weekFormat.format(calendar.time)
    val dateText = dateFormat.format(calendar.time)
    
    Row(
        modifier = Modifier.padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = weekText,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.textPrimary
        )
        Text(
            text = ",",
            fontSize = 18.sp,
            fontWeight = FontWeight.Normal,
            color = AppColors.textPrimary
        )
        Text(
            text = dateText,
            fontSize = 18.sp,
            fontWeight = FontWeight.Normal,
            color = AppColors.textPrimary
        )
    }
}

/**
 * 会议时间线项
 */
@Composable
private fun MeetingTimelineItem(
    meeting: MeetingEntity,
    isAlternate: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isAlternate) {
        AppColors.bgContainer
    } else {
        AppColors.bgCard
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 左侧：圆点 + 时间（横向排列，居中）
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                modifier = Modifier
                    .width(100.dp)
                    .align(Alignment.CenterVertically)
            ) {
                // 圆点
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            color = getSecurityLevelColor(meeting.securityLevel),
                            shape = CircleShape
                        )
                )
                
                // 时间
                TimeDisplay(timestamp = meeting.startTime)
            }
            
            // 分隔线
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(50.dp)
                    .background(AppColors.divider)
                    .align(Alignment.CenterVertically)
            )
            
            // 中间：会议内容
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 会议标题（加粗强调）
                Text(
                    text = meeting.name,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                // 地点和主持人
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 地点（图标始终显示）
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = AppColors.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = meeting.location?.takeIf { it.isNotBlank() } ?: "--",
                            fontSize = 14.sp,
                            color = AppColors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    // 主持人（图标始终显示）
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = AppColors.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = meeting.hostName?.takeIf { it.isNotBlank() } ?: "--",
                            fontSize = 14.sp,
                            color = AppColors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            
            // 右侧：密级标签
            SecurityLevelTag(securityLevel = meeting.securityLevel)
        }
    }
}

/**
 * 时间显示：9:00 上午（横向排列，12小时制）
 */
@Composable
private fun TimeDisplay(timestamp: Long) {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = timestamp
    }
    
    val hour12 = calendar.get(Calendar.HOUR)
    val displayHour = if (hour12 == 0) 12 else hour12
    val minute = calendar.get(Calendar.MINUTE)
    val timeText = String.format("%d:%02d", displayHour, minute)
    val periodText = if (calendar.get(Calendar.HOUR_OF_DAY) < 12) "上午" else "下午"
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = timeText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.textPrimary
        )
        Text(
            text = periodText,
            fontSize = 12.sp,
            color = AppColors.textSecondary
        )
    }
}

/**
 * 根据密级获取圆点颜色
 */
private fun getSecurityLevelColor(securityLevel: String): Color {
    return when (securityLevel.lowercase()) {
        "internal" -> Color(0xFF4CAF50)      // 绿色
        "confidential" -> Color(0xFFFFC107)  // 黄色
        "secret" -> Color(0xFFF44336)        // 红色
        else -> Color(0xFF9E9E9E)            // 灰色
    }
}

/**
 * 密级标签 - 竖排显示
 */
@Composable
private fun SecurityLevelTag(securityLevel: String) {
    val (bgColor, textColor, labelText) = when (securityLevel.lowercase()) {
        "internal" -> Triple(Color(0xFF4CAF50), Color.White, "内\n部")
        "confidential" -> Triple(Color(0xFFFFC107), Color.Black, "秘\n密")
        "secret" -> Triple(Color(0xFFF44336), Color.White, "机\n密")
        else -> Triple(Color(0xFF9E9E9E), Color.White, "未\n知")
    }
    
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = bgColor
    ) {
        Text(
            text = labelText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            lineHeight = 14.sp,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp)
        )
    }
}

/**
 * 空状态
 */
@Composable
private fun EmptyState(
    meetingType: String,
    modifier: Modifier = Modifier
) {
    val (title, subtitle) = when (meetingType) {
        "all" -> Pair("暂无会议", "请等待会议数据同步")
        "standard" -> Pair("暂无标准会议", "请等待会议数据同步")
        "tablet" -> Pair("暂无快速会议", "请等待会议数据同步")
        else -> Pair("暂无会议", "请等待会议数据同步")
    }
    
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
            text = title,
            fontSize = 16.sp,
            color = AppColors.textSecondary
        )
        Text(
            text = subtitle,
            fontSize = 14.sp,
            color = AppColors.textTertiary
        )
    }
}
