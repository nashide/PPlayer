package com.theveloper.pixelplay.presentation.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.theveloper.pixelplay.R
import com.theveloper.pixelplay.data.model.SyncedLine // 同步歌词行
import com.theveloper.pixelplay.data.model.SyncedWord // 同步歌词单词
import com.theveloper.pixelplay.data.repository.LyricsSearchResult // 歌词搜索结果
import com.theveloper.pixelplay.presentation.screens.TabAnimation // 标签页动画
import com.theveloper.pixelplay.presentation.components.subcomps.FetchLyricsDialog // 获取歌词弹窗
import com.theveloper.pixelplay.presentation.components.subcomps.PlayerSeekBar // 播放器进度条
import com.theveloper.pixelplay.presentation.viewmodel.LyricsSearchUiState // 歌词搜索UI状态
import com.theveloper.pixelplay.presentation.viewmodel.PlayerUiState // 播放器UI状态
import com.theveloper.pixelplay.presentation.viewmodel.StablePlayerState // 播放器稳定状态
import com.theveloper.pixelplay.ui.theme.GoogleSansRounded // GoogleSansRounded字体
import com.theveloper.pixelplay.utils.BubblesLine // 气泡行（无歌词占位）
import com.theveloper.pixelplay.utils.ProviderText // 来源文本
import com.theveloper.pixelplay.presentation.components.snapping.ExperimentalSnapperApi
import com.theveloper.pixelplay.presentation.components.snapping.SnapperLayoutInfo // 吸附布局信息
import com.theveloper.pixelplay.presentation.components.snapping.rememberLazyListSnapperLayoutInfo // 获取列表吸附布局信息
import com.theveloper.pixelplay.presentation.components.snapping.rememberSnapperFlingBehavior // 获取吸附滑动行为
import com.theveloper.pixelplay.utils.LyricsUtils // 歌词工具类
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape // 绝对平滑圆角形状
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsSheet(
    stablePlayerStateFlow: StateFlow<StablePlayerState>, // 播放器稳定状态流
    playerUiStateFlow: StateFlow<PlayerUiState>, // 播放器UI状态流
    lyricsSearchUiState: LyricsSearchUiState, // 歌词搜索UI状态
    resetLyricsForCurrentSong: () -> Unit, // 重置当前歌曲歌词
    onSearchLyrics: (Boolean) -> Unit, // 搜索歌词回调
    onPickResult: (LyricsSearchResult) -> Unit, // 选择搜索结果回调
    onImportLyrics: () -> Unit, // 导入歌词回调
    onDismissLyricsSearch: () -> Unit, // 关闭歌词搜索回调
    lyricsTextStyle: TextStyle, // 歌词文本样式
    backgroundColor: Color, // 背景色
    onBackgroundColor: Color, // 背景内容色
    containerColor: Color, // 容器色
    contentColor: Color, // 内容色
    accentColor: Color, // 强调色
    onAccentColor: Color, // 强调内容色
    tertiaryColor: Color, // 第三色
    onTertiaryColor: Color, // 第三内容色
    onBackClick: () -> Unit, // 返回按钮点击回调
    onSeekTo: (Long) -> Unit, // 进度跳转回调
    onPlayPause: () -> Unit, // 播放/暂停回调（新增参数）
    modifier: Modifier = Modifier,
    highlightZoneFraction: Float = 0.22f, // 高亮区域占比
    highlightOffsetDp: Dp = 32.dp, // 高亮偏移量
    autoscrollAnimationSpec: AnimationSpec<Float> = tween(durationMillis = 450, easing = FastOutSlowInEasing) // 自动滚动动画配置
) {
    BackHandler { onBackClick() }
    val stablePlayerState by stablePlayerStateFlow.collectAsState()

    // 衍生状态：是否正在加载歌词
    val isLoadingLyrics by remember { derivedStateOf { stablePlayerState.isLoadingLyrics } }
    // 衍生状态：歌词内容
    val lyrics by remember { derivedStateOf { stablePlayerState.lyrics } }
    // 衍生状态：是否正在播放
    val isPlaying by remember { derivedStateOf { stablePlayerState.isPlaying } }
    // 衍生状态：当前歌曲
    val currentSong by remember { derivedStateOf { stablePlayerState.currentSong } }

    val context = LocalContext.current

    // 是否显示获取歌词弹窗
    var showFetchLyricsDialog by remember { mutableStateOf(false) }

    // 监听歌曲/歌词/加载状态变化，控制弹窗显示
    LaunchedEffect(currentSong, lyrics, isLoadingLyrics) {
        if (currentSong != null && lyrics == null && !isLoadingLyrics) {
            showFetchLyricsDialog = true
        } else if (lyrics != null || isLoadingLyrics) {
            showFetchLyricsDialog = false
        }
    }

    // 显示获取歌词弹窗
    if (showFetchLyricsDialog) {
        FetchLyricsDialog(
            uiState = lyricsSearchUiState,
            currentSong = currentSong,
            onConfirm = onSearchLyrics,
            onPickResult = onPickResult,
            onDismiss = {
                showFetchLyricsDialog = false
                onDismissLyricsSearch()
                // 若无歌词且未加载，返回上一级
                if (lyrics == null && !isLoadingLyrics) {
                    onBackClick()
                }
            },
            onImport = onImportLyrics
        )
    }

    // 是否显示同步歌词（区分同步/静态歌词）
    var showSyncedLyrics by remember(lyrics) {
        mutableStateOf(
            when {
                lyrics?.synced != null -> true // 有同步歌词则显示
                lyrics?.plain != null -> false // 只有静态歌词则不显示
                else -> null // 无歌词则为null
            }
        )
    }

    // 播放按钮圆角动画
    val fabShapeCornerRadius by animateDpAsState(
        targetValue = if (isPlaying) 24.dp else 50.dp,
        label = "悬浮按钮形状动画"
    )

    // 构建悬浮按钮平滑圆角形状
    var fabShape = AbsoluteSmoothCornerShape(
        cornerRadiusTL = fabShapeCornerRadius,
        smoothnessAsPercentBL = 60,
        cornerRadiusTR = fabShapeCornerRadius,
        smoothnessAsPercentBR = 60,
        cornerRadiusBL = fabShapeCornerRadius,
        smoothnessAsPercentTL = 60,
        cornerRadiusBR = fabShapeCornerRadius,
        smoothnessAsPercentTR = 60
    )

    // 标签页标题
    val tabTitles = listOf("同步歌词", "静态歌词")

    // 脚手架布局
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(32.dp)), // 裁剪为圆角
        containerColor = containerColor,
        contentColor = contentColor,
        // 顶部导航栏
        topBar = {
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                // 顶部渐变背景
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .height(218.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    containerColor,
                                    containerColor,
                                    containerColor,
                                    containerColor,
                                    containerColor,
                                    containerColor,
                                    containerColor,
                                    Color.Transparent
                                )
                            )
                        )
                )

                Column(
                    Modifier.align(Alignment.TopCenter)
                ) {
                    // 居中顶部应用栏
                    CenterAlignedTopAppBar(
                        title = {
                            Text(
                                text = "歌词",
                                fontWeight = FontWeight.Bold,
                                color = onBackgroundColor
                            )
                        },
                        // 返回按钮
                        navigationIcon = {
                            FilledIconButton(
                                modifier = Modifier.padding(start = 12.dp),
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = backgroundColor,
                                    contentColor = onBackgroundColor
                                ),
                                onClick = onBackClick
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ArrowBack,
                                    contentDescription = context.resources.getString(R.string.close_lyrics_sheet) // 关闭歌词面板
                                )
                            }
                        },
                        // 更多操作按钮
                        actions = {
                            var expanded by remember { mutableStateOf(false) }
                            IconButton(
                                colors = IconButtonDefaults.iconButtonColors(
                                    contentColor = onBackgroundColor
                                ),
                                onClick = { expanded = !expanded }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MoreVert,
                                    contentDescription = "歌词选项",
                                    tint = onBackgroundColor
                                )
                                // 下拉菜单
                                DropdownMenu(
                                    shape = AbsoluteSmoothCornerShape(
                                        cornerRadiusBL = 20.dp,
                                        smoothnessAsPercentTL = 60,
                                        cornerRadiusBR = 20.dp,
                                        smoothnessAsPercentTR = 60,
                                        cornerRadiusTL = 20.dp,
                                        smoothnessAsPercentBL = 60,
                                        cornerRadiusTR = 20.dp,
                                        smoothnessAsPercentBR = 60
                                    ),
                                    containerColor = backgroundColor,
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    DropdownMenuItem(
                                        leadingIcon = {
                                            Icon(
                                                painter = painterResource(R.drawable.outline_restart_alt_24),
                                                contentDescription = null
                                            )
                                        },
                                        text = { Text(text = "重置已导入歌词") },
                                        onClick = {
                                            expanded = false
                                            resetLyricsForCurrentSong()
                                        }
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = Color.Transparent
                        )
                    )

                    // 同时有同步和静态歌词时显示标签页
                    if (lyrics?.synced != null && lyrics?.plain != null) {
                        val selectedTabIndex = if (showSyncedLyrics == true) 0 else 1

                        TabRow(
                            modifier = Modifier
                                .fillMaxWidth(),
                            selectedTabIndex = selectedTabIndex,
                            containerColor = Color.Transparent,
                            // 自定义指示器（透明隐藏）
                            indicator = { tabPositions ->
                                if (selectedTabIndex < tabPositions.size) {
                                    TabRowDefaults.PrimaryIndicator(
                                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                        height = 3.dp,
                                        color = Color.Transparent
                                    )
                                }
                            },
                            divider = {} // 隐藏分割线
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Spacer(modifier = Modifier.width(14.dp))
                                // 遍历标签页标题
                                tabTitles.forEachIndexed { index, title ->
                                    TabAnimation(
                                        modifier = Modifier.weight(1f),
                                        selectedColor = accentColor,
                                        onSelectedColor = onAccentColor,
                                        unselectedColor = contentColor.copy(alpha = 0.15f),
                                        onUnselectedColor = contentColor,
                                        index = index,
                                        title = title,
                                        selectedIndex = selectedTabIndex,
                                        onClick = {
                                            showSyncedLyrics = (index == 0)
                                        },
                                        content = {
                                            Text(
                                                text = title,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = GoogleSansRounded
                                            )
                                        }
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                            }
                        }
                    }
                }
            }
        },
        // 悬浮播放/暂停按钮
        floatingActionButton = {
            LargeFloatingActionButton(
                modifier = Modifier.padding(bottom = 64.dp),
                onClick = onPlayPause,
                shape = fabShape,
                containerColor = tertiaryColor,
                contentColor = onTertiaryColor
            ) {
                AnimatedContent(
                    targetState = isPlaying,
                    label = "播放暂停图标动画"
                ) { playing ->
                    if (playing) {
                        Icon(
                            modifier = Modifier.size(36.dp),
                            imageVector = Icons.Rounded.Pause,
                            contentDescription = "暂停"
                        )
                    } else {
                        Icon(
                            modifier = Modifier.size(36.dp),
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "播放"
                        )
                    }
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Center, // 悬浮按钮居中
    ) { paddingValues ->
        // 同步/静态歌词列表状态
        val syncedListState = rememberLazyListState()
        val staticListState = rememberLazyListState()
        val playerUiState by playerUiStateFlow.collectAsState()
        // 播放进度流
        val positionFlow = remember(playerUiStateFlow) {
            playerUiStateFlow.map { it.currentPosition }
        }

        // 歌词变化时滚动到顶部
        LaunchedEffect(lyrics) {
            syncedListState.scrollToItem(0)
            staticListState.scrollToItem(0)
        }

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // 根据歌词类型显示不同内容
            when (showSyncedLyrics) {
                // 无歌词时显示加载/空状态
                null -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            top = paddingValues.calculateTopPadding(),
                            bottom = paddingValues.calculateBottomPadding() + 180.dp,
                            start = 24.dp,
                            end = 24.dp
                        )
                    ) {
                        item(key = "加载或空状态") {
                            Box(
                                modifier = Modifier
                                    .fillParentMaxSize()
                                    .padding(bottom = 160.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isLoadingLyrics) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = context.resources.getString(R.string.loading_lyrics), // 加载歌词中
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        LinearProgressIndicator(
                                            trackColor = accentColor.copy(alpha = .5f),
                                            modifier = Modifier.width(100.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 显示同步歌词
                true -> {
                    lyrics?.synced?.let { synced ->
                        SyncedLyricsList(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(
                                    start = 24.dp,
                                    end = 24.dp,
                                ),
                            lines = synced,
                            listState = syncedListState,
                            positionFlow = positionFlow,
                            accentColor = accentColor,
                            textStyle = lyricsTextStyle,
                            onLineClick = { syncedLine -> onSeekTo(syncedLine.time.toLong()) },
                            highlightZoneFraction = highlightZoneFraction,
                            highlightOffsetDp = highlightOffsetDp,
                            autoscrollAnimationSpec = autoscrollAnimationSpec,
                            footer = {
                                // 远程歌词显示来源信息
                                if (lyrics?.areFromRemote == true) {
                                    item(key = "来源文本") {
                                        ProviderText(
                                            providerText = context.resources.getString(R.string.lyrics_provided_by), // 歌词由以下提供
                                            uri = context.resources.getString(R.string.lrclib_uri),
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 16.dp)
                                        )
                                    }
                                }
                            }
                        )
                    }
                }

                // 显示静态歌词
                false -> {
                    lyrics?.plain?.let { plain ->
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            state = staticListState,
                            contentPadding = PaddingValues(
                                start = 24.dp,
                                end = 24.dp,
                                top = paddingValues.calculateTopPadding(),
                                bottom = paddingValues.calculateBottomPadding() + 180.dp
                            )
                        ) {
                            itemsIndexed(
                                items = plain,
                                key = { index, line -> "$index-$line" }
                            ) { _, line ->
                                PlainLyricsLine(
                                    line = line,
                                    style = lyricsTextStyle,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }
                }
            }

            // 底部渐变背景
            val bottomPadding = paddingValues.calculateBottomPadding() + 10.dp
            val footerBaseHeight = 76.dp

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .heightIn(min = footerBaseHeight + bottomPadding)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                containerColor
                            )
                        )
                    )
            )

            // 播放器进度条
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = bottomPadding)
                    .padding(horizontal = 24.dp)
            ) {
                PlayerSeekBar(
                    backgroundColor = backgroundColor,
                    onBackgroundColor = onBackgroundColor,
                    primaryColor = accentColor,
                    currentPosition = playerUiState.currentPosition,
                    totalDuration = stablePlayerState.totalDuration,
                    onSeek = onSeekTo,
                    isPlaying = isPlaying,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalSnapperApi::class)
@Composable
fun SyncedLyricsList(
    lines: List<SyncedLine>, // 同步歌词行列表
    listState: LazyListState, // 列表状态
    positionFlow: Flow<Long>, // 播放进度流
    accentColor: Color, // 强调色
    textStyle: TextStyle, // 文本样式
    onLineClick: (SyncedLine) -> Unit, // 点击歌词行回调
    highlightZoneFraction: Float, // 高亮区域占比
    highlightOffsetDp: Dp, // 高亮偏移量
    autoscrollAnimationSpec: AnimationSpec<Float>, // 自动滚动动画配置
    modifier: Modifier = Modifier,
    footer: LazyListScope.() -> Unit = {} // 列表底部内容
) {
    val density = LocalDensity.current
    val position by positionFlow.collectAsState(initial = 0L)
    // 计算当前播放进度对应的歌词行索引
    val currentLineIndex by remember(position, lines) {
        derivedStateOf {
            if (lines.isEmpty()) return@derivedStateOf -1
            val currentPosition = position
            lines.withIndex().lastOrNull { (index, line) ->
                val nextTime = lines.getOrNull(index + 1)?.time?.toLong() ?: Long.MAX_VALUE
                currentPosition in line.time.toLong()..<nextTime
            }?.index ?: -1
        }
    }

    BoxWithConstraints(modifier = modifier) {
        // 计算高亮区域参数
        val metrics = remember(maxHeight, highlightZoneFraction, highlightOffsetDp) {
            calculateHighlightMetrics(maxHeight, highlightZoneFraction, highlightOffsetDp)
        }
        val highlightOffsetPx = remember(highlightOffsetDp, density) { with(density) { highlightOffsetDp.toPx() } }

        // 配置列表吸附布局
        val snapperLayoutInfo = rememberLazyListSnapperLayoutInfo(
            lazyListState = listState,
            snapOffsetForItem = { layoutInfo, item ->
                val viewportHeight = layoutInfo.endScrollOffset - layoutInfo.startScrollOffset
                highlightSnapOffsetPx(viewportHeight, item.size, highlightOffsetPx)
            }
        )
        val flingBehavior = rememberSnapperFlingBehavior(layoutInfo = snapperLayoutInfo)

        // 自动滚动到当前歌词行
        LaunchedEffect(currentLineIndex, lines.size, metrics) {
            if (lines.isEmpty()) return@LaunchedEffect
            if (currentLineIndex !in lines.indices) return@LaunchedEffect
            if (listState.isScrollInProgress) return@LaunchedEffect
            if (listState.layoutInfo.totalItemsCount == 0) return@LaunchedEffect

            animateToSnapIndex(
                listState = listState,
                layoutInfo = snapperLayoutInfo,
                targetIndex = currentLineIndex,
                animationSpec = autoscrollAnimationSpec
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                flingBehavior = flingBehavior,
                contentPadding = PaddingValues(
                    top = metrics.topPadding,
                    bottom = metrics.bottomPadding
                )
            ) {
                // 遍历同步歌词行
                itemsIndexed(
                    items = lines,
                    key = { index, item -> "${item.time}_$index" }
                ) { index, line ->
                    val nextTime = lines.getOrNull(index + 1)?.time ?: Int.MAX_VALUE
                    if (line.line.isNotBlank()) {
                        // 显示歌词行
                        LyricLineRow(
                            line = line,
                            nextTime = nextTime,
                            position = position,
                            accentColor = accentColor,
                            style = textStyle,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("同步歌词行_${line.time}"),
                            onClick = { onLineClick(line) }
                        )
                    } else {
                        // 空行显示气泡占位
                        BubblesLine(
                            positionFlow = positionFlow,
                            time = line.time,
                            color = LocalContentColor.current.copy(alpha = 0.6f),
                            nextTime = nextTime,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                // 添加底部内容
                footer()
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LyricLineRow(
    line: SyncedLine, // 同步歌词行
    nextTime: Int, // 下一行时间戳
    position: Long, // 当前播放进度
    accentColor: Color, // 强调色
    style: TextStyle, // 文本样式
    modifier: Modifier = Modifier,
    onClick: () -> Unit // 点击回调
) {
    // 清理歌词行文本（移除多余标签/时间戳）
    val sanitizedLine = remember(line.line) { sanitizeLyricLineText(line.line) }
    // 清理歌词单词列表
    val sanitizedWords = remember(line.words) {
        line.words?.let(::sanitizeSyncedWords)
    }
    // 判断当前行是否为播放中歌词行
    val isCurrentLine by remember(position, line.time, nextTime) {
        derivedStateOf { position in line.time.toLong()..<nextTime.toLong() }
    }
    val unhighlightedColor = LocalContentColor.current.copy(alpha = 0.45f)
    // 歌词行颜色动画
    val lineColor by animateColorAsState(
        targetValue = if (isCurrentLine) accentColor else unhighlightedColor,
        animationSpec = tween(durationMillis = 250),
        label = "歌词行颜色动画"
    )

    // 无单词级同步时显示整行文本
    if (sanitizedWords.isNullOrEmpty()) {
        Text(
            text = sanitizedLine,
            style = style,
            color = lineColor,
            fontWeight = if (isCurrentLine) FontWeight.Bold else FontWeight.Normal,
            modifier = modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable { onClick() }
                .padding(vertical = 4.dp, horizontal = 2.dp)
        )
    } else {
        // 有单词级同步时按单词分行显示
        FlowRow(
            modifier = modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable { onClick() }
                .padding(vertical = 4.dp, horizontal = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            sanitizedWords.forEachIndexed { wordIndex, word ->
                key("${line.time}_${word.time}_${word.word}") {
                    val nextWordTime = sanitizedWords.getOrNull(wordIndex + 1)?.time?.toLong() ?: nextTime.toLong()
                    // 判断当前单词是否为播放中单词
                    val isCurrentWord by remember(position, word.time, nextWordTime) {
                        derivedStateOf { position in word.time.toLong()..<nextWordTime }
                    }
                    LyricWordSpan(
                        word = word,
                        isHighlighted = isCurrentLine && isCurrentWord,
                        style = style,
                        highlightedColor = accentColor,
                        unhighlightedColor = unhighlightedColor
                    )
                }
            }
        }
    }
}

@Composable
fun LyricWordSpan(
    word: SyncedWord, // 同步歌词单词
    isHighlighted: Boolean, // 是否高亮
    style: TextStyle, // 文本样式
    highlightedColor: Color, // 高亮颜色
    unhighlightedColor: Color, // 非高亮颜色
    modifier: Modifier = Modifier
) {
    // 单词颜色动画
    val color by animateColorAsState(
        targetValue = if (isHighlighted) highlightedColor else unhighlightedColor,
        animationSpec = tween(durationMillis = 200),
        label = "单词颜色动画"
    )

    Text(
        text = word.word,
        style = style,
        color = color,
        fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal,
        modifier = modifier
    )
}

@Composable
fun PlainLyricsLine(
    line: String, // 静态歌词行文本
    style: TextStyle, // 文本样式
    modifier: Modifier = Modifier
) {
    // 清理静态歌词文本
    val sanitizedLine = remember(line) { sanitizeLyricLineText(line) }
    Text(
        text = sanitizedLine,
        style = style,
        color = LocalContentColor.current.copy(alpha = 0.7f),
        modifier = modifier
    )
}

// 歌词行文本清理正则（移除开头的v数字标签）
private val LeadingTagRegex = Regex("^v\\d+:\\s*", RegexOption.IGNORE_CASE)

/**
 * 清理歌词行文本
 * 移除LRC时间戳和开头的版本标签
 */
internal fun sanitizeLyricLineText(raw: String): String =
    LyricsUtils.stripLrcTimestamps(raw).replace(LeadingTagRegex, "").trimStart()

/**
 * 清理同步歌词单词列表
 * 移除空单词和开头标签
 */
internal fun sanitizeSyncedWords(words: List<SyncedWord>): List<SyncedWord> =
    words.mapIndexedNotNull { index, word ->
        val sanitized = if (index == 0) LeadingTagRegex.replace(word.word, "") else word.word
        val trimmed = sanitized.trim()
        if (trimmed.isEmpty()) null else word.copy(word = trimmed)
    }

/**
 * 高亮区域参数模型
 * @param topPadding 顶部内边距
 * @param bottomPadding 底部内边距
 * @param zoneHeight 高亮区域高度
 * @param centerFromTop 高亮区域中心距顶部距离
 */
internal data class HighlightZoneMetrics(
    val topPadding: Dp,
    val bottomPadding: Dp,
    val zoneHeight: Dp,
    val centerFromTop: Dp
)

/**
 * 计算高亮区域参数
 * @param containerHeight 容器高度
 * @param highlightZoneFraction 高亮区域占比
 * @param highlightOffset 高亮偏移量
 */
internal fun calculateHighlightMetrics(
    containerHeight: Dp,
    highlightZoneFraction: Float,
    highlightOffset: Dp
): HighlightZoneMetrics {
    val container = containerHeight.value
    val zoneHeight = (containerHeight * highlightZoneFraction).value.coerceAtLeast(0f)
    val offset = highlightOffset.value
    val minCenter = zoneHeight / 2f
    val maxCenter = (container - zoneHeight / 2f).coerceAtLeast(minCenter)
    val unclampedCenter = container / 2f - offset
    val center = unclampedCenter.coerceIn(minCenter, maxCenter)
    val topPadding = (center - zoneHeight / 2f).coerceAtLeast(0f)
    val bottomPadding = (container - center - zoneHeight / 2f).coerceAtLeast(0f)

    return HighlightZoneMetrics(
        topPadding = topPadding.dp,
        bottomPadding = bottomPadding.dp,
        zoneHeight = zoneHeight.dp,
        centerFromTop = center.dp
    )
}

/**
 * 计算高亮吸附偏移量（像素）
 * @param viewportHeight 视口高度
 * @param itemSize 列表项高度
 * @param highlightOffsetPx 高亮偏移量（像素）
 */
internal fun highlightSnapOffsetPx(
    viewportHeight: Int,
    itemSize: Int,
    highlightOffsetPx: Float
): Int {
    if (viewportHeight <= 0 || itemSize <= 0) return 0
    if (itemSize >= viewportHeight) return 0
    val viewport = viewportHeight.toFloat()
    val halfItem = itemSize / 2f
    val targetCenter = (viewport / 2f) - highlightOffsetPx
    val clampedCenter = targetCenter.coerceIn(halfItem, viewport - halfItem)
    return (clampedCenter - halfItem).roundToInt()
}

/**
 * 动画滚动到指定索引的列表项（吸附对齐）
 * @param listState 列表状态
 * @param layoutInfo 吸附布局信息
 * @param targetIndex 目标索引
 * @param animationSpec 动画配置
 */
internal suspend fun animateToSnapIndex(
    listState: LazyListState,
    layoutInfo: SnapperLayoutInfo,
    targetIndex: Int,
    animationSpec: AnimationSpec<Float>
) {
    val distance = layoutInfo.distanceToIndexSnap(targetIndex)
    if (distance == 0) return

    listState.scroll {
        var previous = 0f
        AnimationState(initialValue = 0f).animateTo(
            targetValue = distance.toFloat(),
            animationSpec = animationSpec
        ) {
            val delta = value - previous
            val consumed = scrollBy(delta)
            previous = value
            // 滚动未完全消费时取消动画（避免卡顿）
            if (abs(delta - consumed) > 0.5f) cancelAnimation()
        }
    }
}
