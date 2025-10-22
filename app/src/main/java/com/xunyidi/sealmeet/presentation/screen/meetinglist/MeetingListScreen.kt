package com.xunyidi.sealmeet.presentation.screen.meetinglist

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.xunyidi.sealmeet.R
import com.xunyidi.sealmeet.data.local.database.entity.MeetingEntity
import com.xunyidi.sealmeet.presentation.theme.AppColors
import com.xunyidi.sealmeet.presentation.theme.TextInverse
import java.text.SimpleDateFormat
import java.util.*

/**
 * 会议列表页面 - 显示type=tablet的快速会议
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetingListScreen(
    viewModel: MeetingListViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToDetail: (String) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // 监听副作用
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "快速会议",
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
                    // 加载中
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = AppColors.primaryDefault
                    )
                }
                state.meetings.isEmpty() -> {
                    // 空状态
                    EmptyState(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    // 会议列表
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.meetings) { meeting ->
                            MeetingCard(
                                meeting = meeting,
                                onClick = {
                                    viewModel.handleIntent(
                                        MeetingListContract.Intent.SelectMeeting(meeting.id)
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 会议卡片 - 水平布局：书签 | 标题 | 地点 | 密级
 */
@Composable
private fun MeetingCard(
    meeting: MeetingEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = AppColors.bgCard
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧：书签（无padding，贴边显示）
            BookmarkTag(timestamp = meeting.startTime)
            
            // 中间：会议标题
            Text(
                text = meeting.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = AppColors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 12.dp)
            )
            
            // 右侧：地点
            meeting.location?.let { location ->
                if (location.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(vertical = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = AppColors.textSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = location,
                            fontSize = 13.sp,
                            color = AppColors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 100.dp)
                        )
                    }
                }
            }
            
            // 最右侧：密级标签（上下贴边）
            Box(modifier = Modifier.padding(top = 0.dp)) {
                SecurityLevelTag(securityLevel = meeting.securityLevel)
            }
        }
    }
}

/**
 * 书签标签 - 使用SVG图标
 */
@Composable
private fun BookmarkTag(timestamp: Long) {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = timestamp
    }
    val today = Calendar.getInstance()
    
    // 判断是否是今天
    val isToday = calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                  calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
    
    val dateText = if (isToday) {
        "今天"
    } else {
        SimpleDateFormat("MM-dd", Locale.getDefault()).format(Date(timestamp))
    }
    val timeText = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
    val weekText = when (calendar.get(Calendar.DAY_OF_WEEK)) {
        Calendar.SUNDAY -> "星\n期\n日"
        Calendar.MONDAY -> "星\n期\n一"
        Calendar.TUESDAY -> "星\n期\n二"
        Calendar.WEDNESDAY -> "星\n期\n三"
        Calendar.THURSDAY -> "星\n期\n四"
        Calendar.FRIDAY -> "星\n期\n五"
        Calendar.SATURDAY -> "星\n期\n六"
        else -> ""
    }
    
    val textColor = TextInverse
    
    Box(
        modifier = Modifier
            .width(80.dp)
            .height(70.dp)
    ) {
        // 书签背景图标（保持原始颜色）
        Icon(
            painter = painterResource(id = R.drawable.ic_bookmark),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.fillMaxSize()
        )
        
        // 文字内容（左右两列布局）
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 15.dp, top = 8.dp, end = 10.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // 左侧：日期和时间
            Column(
                verticalArrangement = Arrangement.spacedBy(3.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = dateText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Text(
                    text = timeText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }
            
            // 右侧：星期（竖排）
            Text(
                text = weekText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                lineHeight = 16.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
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
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
        )
    }
}

/**
 * 空状态
 */
@Composable
private fun EmptyState(
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
            text = "暂无快速会议",
            fontSize = 16.sp,
            color = AppColors.textSecondary
        )
        Text(
            text = "请等待会议数据同步",
            fontSize = 14.sp,
            color = AppColors.textTertiary
        )
    }
}
