package com.theveloper.pixelplay.presentation.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Hearing
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedscroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.theveloper.pixelplay.data.stats.PlaybackStatsRepository
import com.theveloper.pixelplay.data.stats.StatsTimeRange
import com.theveloper.pixelplay.presentation.components.ExpressiveTopBarContent
import com.theveloper.pixelplay.presentation.components.MiniPlayerHeight
import com.theveloper.pixelplay.presentation.components.SmartImage
import com.theveloper.pixelplay.presentation.screens.TabAnimation
import com.theveloper.pixelplay.presentation.viewmodel.StatsViewModel
import com.theveloper.pixelplay.utils.formatListeningDurationCompact
import com.theveloper.pixelplay.utils.formatListeningDurationLong
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalFoundationApi::class)
@Composable
fun StatsScreen(
    navController: NavController,
    statsViewModel: StatsViewModel = hiltViewModel()
) {
    val uiState by statsViewModel.uiState.collectAsState()
    val summary = uiState.summary
    val lazyListState = rememberLazyListState()
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    // 状态栏高度
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    // 顶部导航栏最小/最大高度
    val minTopBarHeight = 62.dp + statusBarHeight
    val maxTopBarHeight = 176.dp

    // 转换为像素值（便于动画计算）
    val minTopBarHeightPx = with(density) { minTopBarHeight.toPx() }
    val maxTopBarHeightPx = with(density) { maxTopBarHeight.toPx() }

    // 导航栏高度动画（可折叠/展开）
    val topBarHeight = remember { Animatable(maxTopBarHeightPx) }
    // 折叠比例（0=完全展开，1=完全折叠）
    var collapseFraction by remember { mutableStateOf(0f) }

    // 监听导航栏高度变化，更新折叠比例
    LaunchedEffect(topBarHeight.value) {
        collapseFraction = 1f - ((topBarHeight.value - minTopBarHeightPx) / (maxTopBarHeightPx - minTopBarHeightPx)).coerceIn(0f, 1f)
    }

    // 嵌套滚动监听（实现导航栏折叠/展开）
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y // 滚动增量（负数=向下滚动，正数=向上滚动）
                val scrollingDown = delta < 0

                // 列表非顶部时，不处理导航栏折叠
                if (!scrollingDown && (lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 0)) {
                    return Offset.Zero
                }

                // 计算新的导航栏高度（限制在最小/最大值之间）
                val previousHeight = topBarHeight.value
                val newHeight = (previousHeight + delta).coerceIn(minTopBarHeightPx, maxTopBarHeightPx)
                val consumed = newHeight - previousHeight

                // 高度有变化时，更新导航栏高度
                if (consumed.roundToInt() != 0) {
                    coroutineScope.launch {
                        topBarHeight.snapTo(newHeight)
                    }
                }

                // 判断是否可继续消耗滚动事件（导航栏已折叠到底时，不再消耗）
                val canConsume = !(scrollingDown && newHeight == minTopBarHeightPx)
                return if (canConsume) Offset(0f, consumed) else Offset.Zero
            }
        }
    }

    // 滚动停止后，自动展开/折叠导航栏（回弹效果）
    LaunchedEffect(lazyListState.isScrollInProgress) {
        if (!lazyListState.isScrollInProgress) {
            val shouldExpand = topBarHeight.value > (minTopBarHeightPx + maxTopBarHeightPx) / 2
            val canExpand = lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset == 0
            val target = if (shouldExpand && canExpand) maxTopBarHeightPx else minTopBarHeightPx

            if (topBarHeight.value != target) {
                coroutineScope.launch {
                    topBarHeight.animateTo(target, spring(stiffness = Spring.StiffnessMedium))
                }
            }
        }
    }

    val currentTopBarHeightDp = with(density) { topBarHeight.value.toDp() }
    val tabsHeight = 62.dp
    val tabIndicatorExtraSpacing = 8.dp
    val tabContentSpacing = 20.dp
    // 选中的时间轴指标（听歌时长/播放次数/平均听歌时长）
    var selectedTimelineMetric by rememberSaveable { mutableStateOf(TimelineMetric.ListeningTime) }
    // 选中的分类维度（歌曲/专辑/歌手/流派）
    var selectedCategoryDimension by rememberSaveable { mutableStateOf(CategoryDimension.Song) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .nestedScroll(nestedScrollConnection)
    ) {
        // 加载中状态
        if (uiState.isLoading && summary == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            // 判断是否显示每日听歌节奏板块（日/周维度显示）
            val showDailyRhythm = summary?.range == StatsTimeRange.DAY || summary?.range == StatsTimeRange.WEEK

            // 主内容列表
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize(),
                contentPadding = PaddingValues(
                    top = currentTopBarHeightDp + tabsHeight + tabIndicatorExtraSpacing + tabContentSpacing + 20.dp,
                    bottom = 32.dp + MiniPlayerHeight
                ),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // 每日听歌节奏板块
                if (showDailyRhythm) {
                    item {
                        DailyListeningDistributionSection(
                            summary = summary,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }
                }
                // 听歌时间轴板块
                item {
                    ListeningTimelineSection(
                        summary = summary,
                        selectedMetric = selectedTimelineMetric,
                        onMetricSelected = { selectedTimelineMetric = it },
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
                // 热门分类榜单板块
                item {
                    CategoryMetricsSection(
                        summary = summary,
                        selectedDimension = selectedCategoryDimension,
                        onDimensionSelected = { selectedCategoryDimension = it },
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
                // 听歌数据总览卡片
                item {
                    StatsSummaryCard(
                        summary = summary,
                        isLoading = uiState.isLoading,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
                // 听歌习惯卡片
                item {
                    ListeningHabitsCard(
                        summary = summary,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
                // 热门歌手卡片
                item {
                    TopArtistsCard(
                        summary = summary,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
                // 热门专辑卡片
                item {
                    TopAlbumsCard(
                        summary = summary,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
                // 歌曲统计卡片
                item {
                    SongStatsCard(
                        summary = summary,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }
        }

        // 顶部导航栏 + 时间范围标签栏
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(currentTopBarHeightDp + tabsHeight + tabIndicatorExtraSpacing + tabContentSpacing)
        ) {
            Column {
                // 统计页面顶部导航栏
                StatsTopBar(
                    collapseFraction = collapseFraction,
                    height = currentTopBarHeightDp + 8.dp,
                    onBackClick = { navController.popBackStack() }
                )

                // 时间范围标签栏（日/周/月/年）
                RangeTabsHeader(
                    ranges = uiState.availableRanges,
                    selected = uiState.selectedRange,
                    onRangeSelected = statsViewModel::onRangeSelected,
                    indicatorSpacing = tabIndicatorExtraSpacing,
                )

                // 标签栏下方间距
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(tabContentSpacing)
                        .background(MaterialTheme.colorScheme.surface)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatsTopBar(
    collapseFraction: Float,
    height: Dp,
    onBackClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 12.dp)
        ) {
            // 返回按钮
            FilledIconButton(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 8.dp),
                onClick = onBackClick,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(imageVector = Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
            }

            // 动态折叠的标题栏内容
            ExpressiveTopBarContent(
                title = "听歌数据统计",
                collapseFraction = collapseFraction,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 0.dp, end = 0.dp),
                containerHeightRange = 80.dp to 56.dp,
                titlePaddingRange = 28.dp to 44.dp,
                collapsedTitleVerticalBias = -0.4f
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatsSummaryCard(
    summary: PlaybackStatsRepository.PlaybackStatsSummary?,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    // 平滑圆角形状
    val shape = AbsoluteSmoothCornerShape(
        cornerRadiusTL = 20.dp,
        smoothnessAsPercentBR = 60,
        cornerRadiusTR = 20.dp,
        smoothnessAsPercentBL = 60,
        cornerRadiusBL = 20.dp,
        smoothnessAsPercentTR = 60,
        cornerRadiusBR = 20.dp,
        smoothnessAsPercentTL = 60
    )
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                            MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    )
                )
                .padding(horizontal = 28.dp, vertical = 26.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // 时间范围标题
                    Text(
                        text = summary?.range?.displayName ?: "暂无听歌记录",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // 总听歌时长
                    Text(
                        text = formatListeningDurationLong(summary?.totalDurationMs ?: 0L),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    // 总播放次数
                    if (summary != null) {
                        Text(
                            text = "累计播放 ${summary.totalPlayCount} 次",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * 数据概览胶囊组件
 * @param label 标签文本
 * @param value 数值文本
 */
@Composable
private fun SummaryPill(
    label: String,
    value: String
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.85f))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * 核心数据展示卡片
 * @param title 标题
 * @param value 核心数值
 * @param supporting 辅助说明文本
 * @param modifier 修饰符
 */
@Composable
private fun SummaryHeroTile(
    title: String,
    value: String,
    supporting: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.22f)
                    )
                )
            )
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .widthIn(min = 160.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = supporting,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 带进度条的统计行组件
 * @param title 标题
 * @param label 数值标签
 * @param supporting 辅助说明
 * @param progress 进度值（0-1）
 * @param modifier 修饰符
 */
@Composable
private fun SummaryProgressRow(
    title: String,
    label: String?,
    supporting: String,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val displayLabel = label ?: "—"
    val progressValue = progress.coerceIn(0f, 1f)
    
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.9f))
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = displayLabel,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        
        LinearProgressIndicator(
            progress = { progressValue },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(50)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        
        Text(
            text = supporting,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 时间范围标签栏（日/周/月/年）
 * @param ranges 可选时间范围列表
 * @param selected 当前选中的时间范围
 * @param onRangeSelected 选中回调
 * @param indicatorSpacing 指示器底部间距
 * @param modifier 修饰符
 */
@Composable
private fun RangeTabsHeader(
    ranges: List<StatsTimeRange>,
    selected: StatsTimeRange,
    onRangeSelected: (StatsTimeRange) -> Unit,
    indicatorSpacing: Dp,
    modifier: Modifier = Modifier
) {
    val selectedIndex = remember(ranges, selected) { ranges.indexOf(selected).coerceAtLeast(0) }
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .zIndex(1f),
        color = MaterialTheme.colorScheme.surface,
    ) {
        ScrollableTabRow(
            modifier = Modifier.padding(bottom = indicatorSpacing),
            selectedTabIndex = selectedIndex,
            edgePadding = 20.dp,
            divider = {}, // 隐藏默认分割线
            containerColor = Color.Transparent,
            // 自定义选中指示器
            indicator = { positions ->
                if (selectedIndex in positions.indices) {
                    TabRowDefaults.PrimaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(positions[selectedIndex]),
                        color = MaterialTheme.colorScheme.primary,
                        height = 3.dp
                    )
                }
            }
        ) {
            ranges.forEachIndexed { index, range ->
                TabAnimation(
                    index = index,
                    selectedIndex = selectedIndex,
                    onClick = { onRangeSelected(range) },
                    selectedColor = MaterialTheme.colorScheme.primary,
                    onSelectedColor = MaterialTheme.colorScheme.onPrimary,
                    unselectedColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    onUnselectedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    title = range.displayName
                ) {
                    Text(
                        text = range.displayName,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (index == selectedIndex) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * 听歌习惯卡片
 * @param summary 播放统计摘要
 * @param modifier 修饰符
 */
@Composable
private fun ListeningHabitsCard(
    summary: PlaybackStatsRepository.PlaybackStatsSummary?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 标题
            Text(
                text = "听歌习惯",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            // 无数据状态
            if (summary == null) {
                Text(
                    text = "待我们更了解你的偏好后，便会呈现你的听歌习惯。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                // 核心习惯指标
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    HabitMetric(
                        icon = Icons.Outlined.History,
                        label = "总听歌次数",
                        value = summary.totalSessions.toString()
                    )
                    HabitMetric(
                        icon = Icons.Outlined.Hearing,
                        label = "平均听歌时长",
                        value = formatListeningDurationCompact(summary.averageSessionDurationMs)
                    )
                    HabitMetric(
                        icon = Icons.Outlined.Bolt,
                        label = "最长听歌时段",
                        value = if (summary.longestSessionDurationMs > 0L) {
                            formatListeningDurationCompact(summary.longestSessionDurationMs)
                        } else {
                            "—"
                        }
                    )
                    HabitMetric(
                        icon = Icons.Outlined.AutoGraph,
                        label = "日均听歌次数",
                        value = String.format(Locale.US, "%.1f", summary.averageSessionsPerDay)
                    )
                }
                
                // 分割线
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                
                // 最活跃日
                HighlightRow(
                    title = "最活跃日",
                    value = summary.peakDayLabel ?: "—",
                    supporting = if (summary.peakDayDurationMs > 0L) {
                        formatListeningDurationCompact(summary.peakDayDurationMs)
                    } else {
                        "暂无播放记录"
                    },
                    icon = Icons.Outlined.CalendarMonth
                )
                
                // 听歌高峰时段
                summary.peakTimeline?.let { peak ->
                    HighlightRow(
                        title = "听歌高峰时段",
                        value = peak.label,
                        supporting = formatListeningDurationCompact(peak.totalDurationMs),
                        icon = Icons.Outlined.AutoGraph
                    )
                }
            }
        }
    }
}

/**
 * 习惯指标项
 * @param icon 图标
 * @param label 标签
 * @param value 数值
 */
@Composable
private fun HabitMetric(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 图标
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        
        // 文本内容
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * 格式化分钟区间标签（如 08:00 – 10:00）
 * @param startMinute 开始分钟（0-1440）
 * @param endMinute 结束分钟（0-1440）
 */
private fun formatMinutesWindowLabel(startMinute: Int, endMinute: Int): String {
    val safeStart = startMinute.coerceIn(0, 24 * 60)
    val safeEnd = endMinute.coerceIn(0, 24 * 60)
    return "${formatHourLabel(safeStart)} – ${formatHourLabel(safeEnd)}"
}

/**
 * 格式化小时标签（如 08:00）
 * @param minute 分钟数（0-1440）
 */
private fun formatHourLabel(minute: Int): String {
    val normalized = minute.coerceIn(0, 24 * 60)
    val hours = normalized / 60
    val mins = normalized % 60
    return String.format(Locale.getDefault(), "%02d:%02d", hours % 24, mins)
}

/**
 * 高亮数据行
 * @param title 标题
 * @param value 核心值
 * @param supporting 辅助说明
 * @param icon 图标
 */
@Composable
private fun HighlightRow(
    title: String,
    value: String,
    supporting: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 图标容器
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
        
        // 文本内容
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = supporting,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 时间轴指标枚举
 */
private enum class TimelineMetric(
    val displayName: String,
    val description: String,
    val extractValue: (PlaybackStatsRepository.TimelineEntry) -> Double,
    val formatValue: (PlaybackStatsRepository.TimelineEntry) -> String
) {
    // 听歌时长
    ListeningTime(
        displayName = "听歌时长",
        description = "所选时间范围内的累计听歌时长",
        extractValue = { it.totalDurationMs.toDouble() },
        formatValue = { formatListeningDurationCompact(it.totalDurationMs) }
    ),
    // 播放次数
    PlayCount(
        displayName = "播放次数",
        description = "各时段的听歌次数统计",
        extractValue = { it.playCount.toDouble() },
        formatValue = { "${it.playCount} 次" }
    ),
    // 平均听歌时长
    AverageSession(
        displayName = "平均听歌时长",
        description = "各时段平均听歌时长",
        extractValue = { entry ->
            if (entry.playCount > 0) entry.totalDurationMs.toDouble() / entry.playCount.toDouble() else 0.0
        },
        formatValue = { entry ->
            val average = if (entry.playCount > 0) entry.totalDurationMs / entry.playCount else 0L
            formatListeningDurationCompact(average)
        }
    )
}

/**
 * 分类维度枚举
 */
private enum class CategoryDimension(
    val displayName: String,
    val cardTitle: String,
    val highlightTitle: String
) {
    // 音乐流派
    Genre(
        displayName = "音乐流派",
        cardTitle = "按曲风分类的听歌数据",
        highlightTitle = "热门曲风"
    ),
    // 歌手
    Artist(
        displayName = "歌手",
        cardTitle = "按歌手分类的听歌数据",
        highlightTitle = "热门歌手"
    ),
    // 专辑
    Album(
        displayName = "专辑",
        cardTitle = "按专辑分类的听歌数据",
        highlightTitle = "热门专辑"
    ),
    // 歌曲
    Song(
        displayName = "歌曲",
        cardTitle = "按歌曲分类的听歌数据",
        highlightTitle = "热门歌曲"
    )
}

/**
 * 分类指标项数据类
 */
private data class CategoryMetricEntry(
    val label: String,       // 标签（流派/歌手/专辑/歌曲名）
    val durationMs: Long,    // 总时长（毫秒）
    val supporting: String   // 辅助说明文本
)

/**
 * 每日听歌节奏板块
 * @param summary 播放统计摘要
 * @param modifier 修饰符
 */
@Composable
private fun DailyListeningDistributionSection(
    summary: PlaybackStatsRepository.PlaybackStatsSummary?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 标题
        Text(
            text = "每日听歌节奏",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        // 副标题
        Text(
            text = "查看你一天中听歌最频繁的时段",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        val distribution = summary?.dayListeningDistribution
        val isWeeklyRange = summary?.range == StatsTimeRange.WEEK
        
        // 无数据状态
        if (distribution == null || distribution.buckets.isEmpty()) {
            Text(
                text = "点击播放，生成你的专属每日听歌印记",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            // 高峰时段
            val peakBucket = distribution.buckets.maxByOrNull { it.totalDurationMs }
            if (peakBucket != null) {
                HighlightRow(
                    title = "听歌高峰时段",
                    value = formatMinutesWindowLabel(peakBucket.startMinute, peakBucket.endMinuteExclusive),
                    supporting = formatListeningDurationCompact(peakBucket.totalDurationMs),
                    icon = Icons.Outlined.Bolt
                )
            }
            
            // 周维度/日维度时间轴
            if (isWeeklyRange) {
                WeeklyDailyListeningTimeline(
                    distribution = distribution,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                DailyListeningTimeline(
                    buckets = distribution.buckets,
                    maxBucketDurationMs = distribution.maxBucketDurationMs,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // 小时刻度
            HourMarkersRow()
        }
    }
}

/**
 * 每周每日听歌时间轴
 * @param distribution 每日听歌分布数据
 * @param modifier 修饰符
 */
@Composable
private fun WeeklyDailyListeningTimeline(
    distribution: PlaybackStatsRepository.DayListeningDistribution,
    modifier: Modifier = Modifier
) {
    val locale = Locale.getDefault()
    val dateFormatter = remember(locale) { DateTimeFormatter.ofPattern("MMM d", locale) }
    
    // 每日最大时长（用于归一化显示）
    val perDayMax = remember(distribution.days) {
        distribution.days.maxOfOrNull { day ->
            day.buckets.maxOfOrNull { it.totalDurationMs } ?: 0L
        }?.coerceAtLeast(1L) ?: 1L
    }
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        distribution.days.forEach { day ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // 日期标题（如 周一 · 10月24日）
                val dayOfWeek = day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
                val formattedDate = day.date.format(dateFormatter)
                Text(
                    text = "$dayOfWeek · $formattedDate",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // 当日听歌时间轴
                DailyListeningTimeline(
                    buckets = day.buckets,
                    maxBucketDurationMs = perDayMax,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * 单日听歌时间轴
 * @param buckets 时长桶列表
 * @param maxBucketDurationMs 最大桶时长（用于归一化）
 * @param modifier 修饰符
 */
@Composable
private fun DailyListeningTimeline(
    buckets: List<PlaybackStatsRepository.DailyListeningBucket>,
    maxBucketDurationMs: Long,
    modifier: Modifier = Modifier
) {
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val gradientStart = MaterialTheme.colorScheme.primary
    val gradientEnd = MaterialTheme.colorScheme.tertiary
    
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(trackColor)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val totalMinutes = 24f * 60f
            val maxDuration = maxBucketDurationMs.coerceAtLeast(1L).toFloat()
            
            buckets.forEach { bucket ->
                // 计算时段占比
                val startFraction = bucket.startMinute.coerceIn(0, 24 * 60).toFloat() / totalMinutes
                val endFraction = bucket.endMinuteExclusive.coerceIn(0, 24 * 60).toFloat() / totalMinutes
                val left = size.width * startFraction
                val right = size.width * endFraction
                
                // 跳过无效时段
                if (right <= left) return@forEach
                
                // 计算强度（颜色深浅）
                val intensity = (bucket.totalDurationMs.toFloat() / maxDuration).coerceIn(0f, 1f)
                val color = lerp(
                    gradientStart.copy(alpha = 0.3f),
                    gradientEnd.copy(alpha = 0.9f),
                    intensity
                )
                
                // 绘制时段色块
                drawRect(
                    color = color,
                    topLeft = Offset(left, 0f),
                    size = Size(right - left, size.height)
                )
            }
        }
    }
}

/**
 * 小时刻度行
 * @param modifier 修饰符
 */
@Composable
private fun HourMarkersRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // 0点、6点、12点、18点、24点
        listOf(0, 6 * 60, 12 * 60, 18 * 60, 24 * 60).forEach { minute ->
            Text(
                text = formatHourLabel(minute),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 听歌时间轴板块
 * @param summary 播放统计摘要
 * @param selectedMetric 选中的指标
 * @param onMetricSelected 指标选中回调
 * @param modifier 修饰符
 */
@Composable
private fun ListeningTimelineSection(
    summary: PlaybackStatsRepository.PlaybackStatsSummary?,
    selectedMetric: TimelineMetric,
    onMetricSelected: (TimelineMetric) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 标题
        Text(
            text = "听歌时间轴",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        // 指标筛选芯片
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            TimelineMetric.entries.forEach { metric ->
                val isSelected = metric == selectedMetric
                
                FilterChip(
                    selected = isSelected,
                    onClick = { onMetricSelected(metric) },
                    label = {
                        Text(
                            text = metric.displayName,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outlineVariant,
                        selectedBorderColor = Color.Transparent,
                        enabled = true,
                        selected = isSelected
                    )
                )
            }
        }
        
        // 指标描述
        Text(
            text = selectedMetric.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        val timeline = summary?.timeline.orEmpty()
        
        // 无数据状态
        if (timeline.isEmpty() || timeline.all { it.totalDurationMs == 0L && it.playCount == 0 }) {
            Text(
                text = "点击播放，生成你的专属听歌时间轴",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            // 时间轴图表容器
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                TimelineBarChart(entries = timeline, metric = selectedMetric)
            }
            
            // 高峰区间
            summary?.peakTimeline?.let { peak ->
                HighlightRow(
                    title = "听歌高峰区间",
                    value = peak.label,
                    supporting = when (selectedMetric) {
                        TimelineMetric.ListeningTime -> formatListeningDurationCompact(peak.totalDurationMs)
                        TimelineMetric.PlayCount -> "${peak.playCount} 次"
                        TimelineMetric.AverageSession -> {
                            val average = if (peak.playCount > 0) peak.totalDurationMs / peak.playCount else 0L
                            formatListeningDurationCompact(average)
                        }
                    },
                    icon = Icons.Outlined.AutoGraph
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryMetricsSection(
    summary: PlaybackStatsRepository.PlaybackStatsSummary?,
    selectedDimension: CategoryDimension,
    onDimensionSelected: (CategoryDimension) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 标题
        Text(
            text = "热门分类榜单",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        // 副标题
        Text(
            text = "对比你在不同曲风、歌手、专辑及单曲上的听歌偏好",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // 分类维度筛选芯片
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            CategoryDimension.entries.reversed().forEach { dimension ->
                val isSelected = dimension == selectedDimension
                
                FilterChip(
                    selected = isSelected,
                    onClick = { onDimensionSelected(dimension) },
                    label = {
                        Text(
                            text = dimension.displayName,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outlineVariant,
                        selectedBorderColor = Color.Transparent,
                        enabled = true,
                        selected = isSelected
                    )
                )
            }
        }

        // 构建分类指标数据
        val entries = remember(summary, selectedDimension) {
            val base = when (selectedDimension) {
                // 按流派分类
                CategoryDimension.Genre -> summary?.topGenres.orEmpty().map {
                    CategoryMetricEntry(
                        label = it.genre,
                        durationMs = it.totalDurationMs,
                        supporting = "${it.playCount} 次 • ${it.uniqueArtists} 位歌手"
                    )
                }

                // 按歌手分类
                CategoryDimension.Artist -> summary?.topArtists.orEmpty().map {
                    CategoryMetricEntry(
                        label = it.artist,
                        durationMs = it.totalDurationMs,
                        supporting = "${it.playCount} 次 • ${it.uniqueSongs} 首曲目"
                    )
                }

                // 按专辑分类
                CategoryDimension.Album -> summary?.topAlbums.orEmpty().map {
                    CategoryMetricEntry(
                        label = it.album,
                        durationMs = it.totalDurationMs,
                        supporting = "${it.playCount} 次 • ${it.uniqueSongs} 首曲目"
                    )
                }

                // 按歌曲分类
                CategoryDimension.Song -> summary?.topSongs.orEmpty().map {
                    val supportingParts = buildList {
                        add("${it.playCount} 次")
                        if (it.artist.isNotBlank()) {
                            add(it.artist)
                        }
                    }
                    CategoryMetricEntry(
                        label = it.title,
                        durationMs = it.totalDurationMs,
                        supporting = supportingParts.joinToString(separator = " • ")
                    )
                }
            }
            // 过滤掉时长为0的项
            base.filter { it.durationMs > 0L }
        }

        // 无数据状态
        if (entries.isEmpty()) {
            Text(
                text = "点击播放，在当前视图呈现你的听歌高光时刻",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            // 分类柱状图卡片
            Card(
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text(
                        text = selectedDimension.cardTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    CategoryVerticalBarChart(entries = entries)
                }
            }
        }
    }
}

/**
 * 分类垂直柱状图
 * @param entries 分类指标项列表
 * @param modifier 修饰符
 */
@Composable
private fun CategoryVerticalBarChart(
    entries: List<CategoryMetricEntry>,
    modifier: Modifier = Modifier
) {
    if (entries.isEmpty()) return
    
    // 最大时长（用于归一化）
    val maxDuration = entries.maxOf { it.durationMs }.coerceAtLeast(1L)
    val highlightDuration = entries.maxOf { it.durationMs }
    val highlightIndex = entries.indexOfFirst { it.durationMs == highlightDuration }.coerceAtLeast(0)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 柱状图区域
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            entries.forEachIndexed { index, entry ->
                val progress = (entry.durationMs.toFloat() / maxDuration.toFloat()).coerceIn(0f, 1f)
                val isHighlight = entry.durationMs == highlightDuration
                
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(min = 56.dp)
                        .padding(horizontal = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 时长文本
                    Text(
                        text = formatListeningDurationCompact(entry.durationMs),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                    
                    // 柱状图容器
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(CircleShape)
                            .background(Color.Transparent),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        // 柱状图
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(progress)
                                .clip(CircleShape)
                                .background(
                                    if (isHighlight) {
                                        // 高亮渐变（主色→第三色）
                                        Brush.verticalGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                                                MaterialTheme.colorScheme.tertiary
                                            )
                                        )
                                    } else {
                                        // 普通渐变（主色半透明→主色）
                                        Brush.verticalGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                                MaterialTheme.colorScheme.primary
                                            )
                                        )
                                    }
                                )
                        )
                    }
                    
                    // 排名指示器
                    CategoryMetricIndicator(
                        index = index,
                        highlighted = isHighlight
                    )
                }
            }
        }

        // 分割线
        HorizontalDivider(
            modifier = Modifier
                .height(2.dp)
                .clip(shape = CircleShape),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
        )

        // 图例
        CategoryMetricsLegend(entries = entries, highlightIndex = highlightIndex)
    }
}

/**
 * 分类指标排名指示器
 * @param index 排名索引
 * @param highlighted 是否高亮
 * @param modifier 修饰符
 */
@Composable
private fun CategoryMetricIndicator(
    index: Int,
    highlighted: Boolean,
    modifier: Modifier = Modifier
) {
    // 配色
    val containerColor = if (highlighted) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val contentColor = if (highlighted) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }
    
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = if (highlighted) 4.dp else 0.dp
    ) {
        Box(
            modifier = Modifier
                .defaultMinSize(minWidth = 36.dp, minHeight = 36.dp)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${index + 1}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * 分类指标图例
 * @param entries 分类指标项列表
 * @param highlightIndex 高亮索引
 */
@Composable
private fun CategoryMetricsLegend(
    entries: List<CategoryMetricEntry>,
    highlightIndex: Int
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        entries.forEachIndexed { index, entry ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 排名指示器
                CategoryMetricIndicator(index = index, highlighted = index == highlightIndex)
                
                // 文本内容
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = entry.label,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (entry.supporting.isNotBlank()) {
                        Text(
                            text = entry.supporting,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // 时长文本
                Text(
                    text = formatListeningDurationCompact(entry.durationMs),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 时间轴柱状图
 * @param entries 时间轴项列表
 * @param metric 选中的指标
 * @param modifier 修饰符
 */
@Composable
private fun TimelineBarChart(
    entries: List<PlaybackStatsRepository.TimelineEntry>,
    metric: TimelineMetric,
    modifier: Modifier = Modifier
) {
    // 最大指标值（用于归一化）
    val maxMetricValue = entries.maxOfOrNull { metric.extractValue(it) }?.coerceAtLeast(0.0) ?: 0.0
    
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        entries.forEach { entry ->
            val value = metric.extractValue(entry)
            val progress = if (maxMetricValue > 0) (value / maxMetricValue).toFloat().coerceIn(0f, 1f) else 0f
            val formattedValue = metric.formatValue(entry)
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // 标题+数值行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = entry.label,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formattedValue,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // 进度条容器
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                ) {
                    // 进度条
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress)
                            .clip(RoundedCornerShape(999.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.tertiary
                                    )
                                )
                            )
                    )
                }
            }
        }
    }
}

/**
 * 热门歌手卡片
 * @param summary 播放统计摘要
 * @param modifier 修饰符
 */
@Composable
private fun TopArtistsCard(
    summary: PlaybackStatsRepository.PlaybackStatsSummary?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 标题
            Text(
                text = "热门歌手",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            val artists = summary?.topArtists.orEmpty()
            
            // 无数据状态
            if (artists.isEmpty()) {
                Text(
                    text = "继续听歌，你喜欢的歌手就会在这里出现",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                // 最大时长（用于进度条归一化）
                val maxDuration = artists.maxOf { it.totalDurationMs }.coerceAtLeast(1L)
                
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    artists.forEachIndexed { index, artistSummary ->
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // 歌手信息行
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // 歌手头像（首字母）
                                ArtistAvatar(name = artistSummary.artist)
                                
                                // 歌手信息
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${index + 1}. ${artistSummary.artist}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${artistSummary.playCount} 次 • ${artistSummary.uniqueSongs} 首",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                
                                // 听歌时长
                                Text(
                                    text = formatListeningDurationCompact(artistSummary.totalDurationMs),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            // 进度条
                            LinearProgressIndicator(
                                progress = (artistSummary.totalDurationMs.toFloat() / maxDuration.toFloat()).coerceIn(0f, 1f),
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 歌手头像（首字母展示）
 * @param name 歌手名
 */
@Composable
private fun ArtistAvatar(name: String) {
    // 提取首字母（最多两个）
    val initials = remember(name) {
        name.split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString(separator = "") { it.first().uppercaseChar().toString() }
            .ifBlank { "?" }
    }
    
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * 热门专辑卡片
 * @param summary 播放统计摘要
 * @param modifier 修饰符
 */
@Composable
private fun TopAlbumsCard(
    summary: PlaybackStatsRepository.PlaybackStatsSummary?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 标题
            Text(
                text = "热门专辑",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            val albums = summary?.topAlbums.orEmpty()
            
            // 无数据状态
            if (albums.isEmpty()) {
                Text(
                    text = "你常重温的专辑，即将在这里呈现",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                // 最大时长（用于进度条归一化）
                val maxDuration = albums.maxOf { it.totalDurationMs }.coerceAtLeast(1L)
                
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    albums.forEachIndexed { index, albumSummary ->
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // 专辑信息行
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // 专辑封面
                                SmartImage(
                                    model = albumSummary.albumArtUri,
                                    contentDescription = albumSummary.album,
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(16.dp)),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                
                                // 专辑信息
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${index + 1}. ${albumSummary.album}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${albumSummary.playCount} 次 • ${albumSummary.uniqueSongs} 首",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                
                                // 听歌时长
                                Text(
                                    text = formatListeningDurationCompact(albumSummary.totalDurationMs),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            // 进度条
                            LinearProgressIndicator(
                                progress = (albumSummary.totalDurationMs.toFloat() / maxDuration.toFloat()).coerceIn(0f, 1f),
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 歌曲统计卡片
 * @param summary 播放统计摘要
 * @param modifier 修饰符
 */
@Composable
private fun SongStatsCard(
    summary: PlaybackStatsRepository.PlaybackStatsSummary?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            val songs = summary?.songs.orEmpty()
            // 控制是否显示全部歌曲（默认显示前8首）
            var showAll by rememberSaveable(songs) { mutableStateOf(songs.size <= 8) }
            val displayedSongs = remember(songs, showAll) {
                if (showAll || songs.size <= 8) songs else songs.take(8)
            }
            // 最大时长（用于进度条归一化）
            val maxDuration = songs.maxOfOrNull { it.totalDurationMs }?.coerceAtLeast(1L) ?: 1L
            // 构建歌曲ID到排名的映射
            val positions = remember(songs) { songs.mapIndexed { index, song -> song.songId to index }.toMap() }

            // 标题+显示全部/显示顶部按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "本时段播放曲目",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                // 超过8首时显示切换按钮
                if (songs.size > 8) {
                    TextButton(onClick = { showAll = !showAll }) {
                        Text(
                            text = if (showAll || songs.size <= 8) "显示前8首" else "显示全部",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // 无数据状态
            if (songs.isEmpty()) {
                Text(
                    text = "聆听你喜爱的曲目，即可在此看到它们的高光展示",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                // 歌曲列表
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    displayedSongs.forEach { songSummary ->
                        val position = positions[songSummary.songId] ?: songs.indexOf(songSummary)
                        
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // 歌曲信息行
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // 歌曲封面
                                SmartImage(
                                    model = songSummary.albumArtUri,
                                    contentDescription = songSummary.title,
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(16.dp)),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                
                                // 歌曲信息
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${position + 1}. ${songSummary.title}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = songSummary.artist,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${songSummary.playCount} 次 • ${formatListeningDurationCompact(songSummary.totalDurationMs)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            
                            // 进度条
                            LinearProgressIndicator(
                                progress = (songSummary.totalDurationMs.toFloat() / maxDuration.toFloat()).coerceIn(0f, 1f),
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
