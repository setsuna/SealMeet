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
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
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
                                        MeetingListContract.Intent.SelectMeeting(meeting.meetingId)
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
 * 会议卡片
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 会议名称
            Text(
                text = meeting.meetingName,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.textPrimary
            )

            // 会议时间
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = AppColors.textSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = formatDateTime(meeting.startTime),
                    fontSize = 14.sp,
                    color = AppColors.textSecondary
                )
            }

            // 会议地点
            if (meeting.location.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = AppColors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = meeting.location,
                        fontSize = 14.sp,
                        color = AppColors.textSecondary
                    )
                }
            }

            // 参会人数
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.People,
                    contentDescription = null,
                    tint = AppColors.textSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "${meeting.participantCount}人",
                    fontSize = 14.sp,
                    color = AppColors.textSecondary
                )
            }

            // 会议描述
            if (meeting.description.isNotBlank()) {
                Text(
                    text = meeting.description,
                    fontSize = 13.sp,
                    color = AppColors.textTertiary,
                    maxLines = 2
                )
            }
        }
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

/**
 * 格式化日期时间
 */
private fun formatDateTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
