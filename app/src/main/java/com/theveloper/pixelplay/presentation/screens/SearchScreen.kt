package com.theveloper.pixelplay.presentation.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.theveloper.pixelplay.data.model.Album // 专辑
import com.theveloper.pixelplay.data.model.Artist // 艺人
import com.theveloper.pixelplay.data.model.Playlist // 歌单
import com.theveloper.pixelplay.data.model.SearchFilterType // 搜索筛选类型
import com.theveloper.pixelplay.data.model.SearchHistoryItem // 搜索历史项
import com.theveloper.pixelplay.data.model.SearchResultItem // 搜索结果项
import com.theveloper.pixelplay.data.model.Song // 歌曲
import com.theveloper.pixelplay.presentation.components.SmartImage // 智能图片组件
import com.theveloper.pixelplay.presentation.components.SongInfoBottomSheet // 歌曲信息底部面板
import com.theveloper.pixelplay.presentation.viewmodel.PlayerViewModel // 播放器视图模型
import android.util.Log
import com.theveloper.pixelplay.ui.theme.LocalPixelPlayDarkTheme // 暗黑主题本地配置
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavHostController
import com.theveloper.pixelplay.R
import com.theveloper.pixelplay.data.repository.MusicRepository // 音乐仓库
import com.theveloper.pixelplay.presentation.components.MiniPlayerHeight // 迷你播放器高度
import com.theveloper.pixelplay.presentation.components.NavBarContentHeight // 导航栏内容高度
import com.theveloper.pixelplay.presentation.components.PlaylistBottomSheet // 歌单底部面板
import com.theveloper.pixelplay.presentation.navigation.Screen // 导航页面（用于构建GenreDetail路由）
import com.theveloper.pixelplay.presentation.screens.search.components.GenreCategoriesGrid // 曲风分类网格
import com.theveloper.pixelplay.presentation.viewmodel.PlaylistViewModel // 歌单视图模型
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape // 绝对平滑圆角形状
import timber.log.Timber


@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SearchScreen(
    paddingValues: PaddingValues, // 内边距
    playerViewModel: PlayerViewModel = hiltViewModel(), // 播放器视图模型（默认Hilt注入）
    playlistViewModel: PlaylistViewModel = hiltViewModel(), // 歌单视图模型（默认Hilt注入）
    navController: NavHostController, // 导航控制器
    onSearchBarActiveChange: (Boolean) -> Unit = {} // 搜索栏激活状态变更回调
) {
    var searchQuery by remember { mutableStateOf("") } // 搜索关键词
    var active by remember { mutableStateOf(false) } // 搜索栏是否激活
    val systemNavBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() // 系统导航栏底部内边距
    val bottomBarHeightDp = NavBarContentHeight + systemNavBarInset // 底部栏总高度
    var showPlaylistBottomSheet by remember { mutableStateOf(false) } // 是否显示歌单底部面板
    val uiState by playerViewModel.playerUiState.collectAsState() // 播放器UI状态
    val currentFilter by remember { derivedStateOf { uiState.selectedSearchFilter } } // 当前选中的搜索筛选类型
    val searchHistory = uiState.searchHistory // 搜索历史列表
    val genres by playerViewModel.genres.collectAsState() // 曲风列表
    val stablePlayerState by playerViewModel.stablePlayerState.collectAsState() // 播放器稳定状态
    val favoriteSongIds by playerViewModel.favoriteSongIds.collectAsState() // 收藏歌曲ID列表
    var showSongInfoBottomSheet by remember { mutableStateOf(false) } // 是否显示歌曲信息底部面板
    var selectedSongForInfo by remember { mutableStateOf<Song?>(null) } // 选中的待查看信息的歌曲

    // 监听搜索关键词、搜索栏激活状态、筛选条件变化，执行搜索
    LaunchedEffect(searchQuery, active, currentFilter) {
        if (searchQuery.isNotBlank()) {
            playerViewModel.performSearch(searchQuery)
        } else if (active) {
            playerViewModel.performSearch("")
        }
    }
    val searchResults = uiState.searchResults // 搜索结果列表
    // 处理歌曲更多选项点击事件
    val handleSongMoreOptionsClick: (Song) -> Unit = { song ->
        selectedSongForInfo = song
        playerViewModel.selectSongForInfo(song)
        showSongInfoBottomSheet = true
    }

    // 搜索栏水平内边距动画
    val searchbarHorizontalPadding by animateDpAsState(
        targetValue = if (!active) 24.dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium), // 低弹性、中等刚度的弹簧动画
        label = "搜索栏水平内边距动画"
    )

    val searchbarCornerRadius = 28.dp // 搜索栏圆角半径

    val dm = LocalPixelPlayDarkTheme.current // 当前是否为暗黑主题

    // 暗黑主题渐变颜色
    val gradientColorsDark = listOf(
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
        Color.Transparent
    ).toImmutableList()

    // 亮色主题渐变颜色
    val gradientColorsLight = listOf(
        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
        Color.Transparent
    ).toImmutableList()

    // 根据主题选择渐变颜色
    val gradientColors = if (dm) gradientColorsDark else gradientColorsLight

    // 渐变画笔
    val gradientBrush = remember(gradientColors) {
        Brush.verticalGradient(colors = gradientColors)
    }

    val colorScheme = MaterialTheme.colorScheme // 主题配色方案

    // 搜索栏激活状态变更时回调外部方法
    LaunchedEffect(active) {
        onSearchBarActiveChange(active)
    }

    // 组件销毁时重置搜索栏激活状态
    DisposableEffect(Unit) {
        onDispose { onSearchBarActiveChange(false) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 顶部渐变背景
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(gradientBrush)
        )

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 修复：添加最小内边距避免崩溃
            val safePadding = maxOf(0.dp, searchbarHorizontalPadding)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = safePadding) // 使用安全内边距
            ) {
                SearchBar(
                    query = searchQuery, // 搜索关键词
                    onQueryChange = { searchQuery = it }, // 关键词变更回调
                    onSearch = {
                        // 提交搜索（关键词非空时）
                        if (searchQuery.isNotBlank()) {
                            playerViewModel.onSearchQuerySubmitted(searchQuery)
                        }
                        active = false
                    },
                    active = active, // 搜索栏激活状态
                    onActiveChange = {
                        // 搜索栏激活状态变更
                        if (!it) {
                            if (searchQuery.isNotBlank()) {
                                playerViewModel.onSearchQuerySubmitted(searchQuery)
                            }
                        }
                        active = it
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize() // 内容大小动画
                        .clip(RoundedCornerShape(searchbarCornerRadius)), // 裁剪为圆角
                    placeholder = {
                        // 占位提示文本
                        Text(
                            "搜索...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    leadingIcon = {
                        // 左侧搜索图标
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "搜索",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    trailingIcon = {
                        // 右侧清除按钮（关键词非空时显示）
                        if (searchQuery.isNotBlank()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier
                                    .size(48.dp)
                                    .padding(end = 10.dp)
                                    .clip(CircleShape)
                                    .background(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "清空",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    colors = SearchBarDefaults.colors(
                        // 搜索栏容器颜色
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        // 分隔线颜色
                        dividerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        // 输入框颜色配置
                        inputFieldColors = TextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface, // 聚焦时文本色
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f), // 未聚焦时文本色
                            focusedContainerColor = Color.Transparent, // 聚焦时容器色
                            unfocusedContainerColor = Color.Transparent, // 未聚焦时容器色
                            cursorColor = MaterialTheme.colorScheme.primary // 光标颜色
                        )
                    ),
                    content = {
                        // 搜索栏展开后的内容
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            // 筛选标签组
                            FlowRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp), // 水平间距
                            ) {
                                // 各类筛选标签
                                SearchFilterChip(SearchFilterType.ALL, currentFilter, playerViewModel) // 全部
                                SearchFilterChip(SearchFilterType.SONGS, currentFilter, playerViewModel) // 歌曲
                                SearchFilterChip(SearchFilterType.ALBUMS, currentFilter, playerViewModel) // 专辑
                                SearchFilterChip(SearchFilterType.ARTISTS, currentFilter, playerViewModel) // 艺人
                                SearchFilterChip(SearchFilterType.PLAYLISTS, currentFilter, playerViewModel) // 歌单
                            }

                            // 搜索栏激活且关键词为空、有搜索历史时，显示搜索历史
                            if (searchQuery.isBlank() && active && searchHistory.isNotEmpty()) {
                                // 记忆化回调，避免重组重复创建
                                val rememberedOnHistoryClick: (String) -> Unit = remember(playerViewModel) {
                                    { query -> searchQuery = query }
                                }
                                val rememberedOnHistoryDelete: (String) -> Unit = remember(playerViewModel) {
                                    { query -> playerViewModel.deleteSearchHistoryItem(query) }
                                }
                                val rememberedOnClearAllHistory: () -> Unit = remember(playerViewModel) {
                                    { playerViewModel.clearSearchHistory() }
                                }

                                // 搜索历史列表
                                SearchHistoryList(
                                    historyItems = searchHistory,
                                    onHistoryClick = rememberedOnHistoryClick,
                                    onHistoryDelete = rememberedOnHistoryDelete,
                                    onClearAllHistory = rememberedOnClearAllHistory
                                )
                            } else if (searchQuery.isNotBlank() && searchResults.isEmpty()) {
                                // 关键词非空但无搜索结果时，显示空结果提示
                                EmptySearchResults(
                                    searchQuery = searchQuery,
                                    colorScheme = colorScheme
                                )
                            } else if (searchResults.isNotEmpty()) {
                                // 有搜索结果时，显示结果列表
                                val rememberedOnItemSelected = remember { { active = false } }
                                SearchResultsList(
                                    results = searchResults,
                                    playerViewModel = playerViewModel,
                                    onItemSelected = rememberedOnItemSelected,
                                    currentPlayingSongId = stablePlayerState.currentSong?.id,
                                    isPlaying = stablePlayerState.isPlaying,
                                    onSongMoreOptionsClick = handleSongMoreOptionsClick,
                                    navController = navController
                                )
                            } else if (searchQuery.isBlank() && active && searchHistory.isEmpty()) {
                                // 搜索栏激活、关键词为空且无搜索历史时，显示无最近搜索提示
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("暂无最近搜索", style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        }
                    }
                )
            }

            // 搜索栏未激活时显示的内容
            if (!active) {
                if (searchQuery.isBlank()) {
                    // 关键词为空时，显示曲风分类网格
                    Box {
                        GenreCategoriesGrid(
                            genres = genres,
                            onGenreClick = { genre ->
                                Timber.tag("搜索页面")
                                    .d("点击曲风: ${genre.name} (ID: ${genre.id})")
                                val encodedGenreId = java.net.URLEncoder.encode(genre.id, "UTF-8")
                                navController.navigate(Screen.GenreDetail.createRoute(encodedGenreId))
                            },
                            playerViewModel = playerViewModel,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                        // 底部渐变遮罩
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .height(80.dp)
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            MaterialTheme.colorScheme.surfaceContainerLowest.copy(0.5f),
                                            MaterialTheme.colorScheme.surfaceContainerLowest
                                        )
                                    )
                                )
                        )
                    }
                } else {
                    // 关键词非空时，显示筛选标签和搜索结果
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 筛选标签组
                            SearchFilterChip(SearchFilterType.ALL, currentFilter, playerViewModel)
                            SearchFilterChip(SearchFilterType.SONGS, currentFilter, playerViewModel)
                            SearchFilterChip(SearchFilterType.ALBUMS, currentFilter, playerViewModel)
                            SearchFilterChip(SearchFilterType.ARTISTS, currentFilter, playerViewModel)
                            SearchFilterChip(SearchFilterType.PLAYLISTS, currentFilter, playerViewModel)
                        }
                        // 搜索结果列表
                        SearchResultsList(
                            results = searchResults,
                            playerViewModel = playerViewModel,
                            onItemSelected = { },
                            currentPlayingSongId = stablePlayerState.currentSong?.id,
                            isPlaying = stablePlayerState.isPlaying,
                            onSongMoreOptionsClick = handleSongMoreOptionsClick,
                            navController = navController
                        )
                    }
                }
            }
        }
    }

    // 显示歌曲信息底部面板（选中歌曲非空时）
    if (showSongInfoBottomSheet && selectedSongForInfo != null) {
        val currentSong = selectedSongForInfo
        // 衍生状态：当前歌曲是否为收藏状态
        val isFavorite = remember(currentSong?.id, favoriteSongIds) {
            derivedStateOf {
                currentSong?.let { favoriteSongIds.contains(it.id) }
            }
        }.value ?: false
        // 从列表移除触发回调（刷新搜索结果）
        val removeFromListTrigger = remember(currentSong) {
            {
                searchQuery = "$searchQuery "
            }
        }

        if (currentSong != null) {
            SongInfoBottomSheet(
                song = currentSong,
                isFavorite = isFavorite,
                removeFromListTrigger = removeFromListTrigger,
                onToggleFavorite = {
                    // 切换歌曲收藏状态
                    playerViewModel.toggleFavoriteSpecificSong(currentSong)
                },
                onDismiss = { showSongInfoBottomSheet = false }, // 关闭面板
                onPlaySong = {
                    // 播放歌曲
                    playerViewModel.showAndPlaySong(currentSong)
                    showSongInfoBottomSheet = false
                },
                onAddToQueue = {
                    // 添加到播放队列
                    playerViewModel.addSongToQueue(currentSong)
                    showSongInfoBottomSheet = false
                },
                onAddNextToQueue = {
                    // 下一首播放
                    playerViewModel.addSongNextToQueue(currentSong)
                    showSongInfoBottomSheet = false
                },
                onAddToPlayList = {
                    // 显示添加到歌单面板
                    showPlaylistBottomSheet = true;
                },
                onDeleteFromDevice = playerViewModel::deleteFromDevice, // 从设备删除歌曲
                onNavigateToAlbum = {
                    // 跳转到专辑详情页
                    navController.navigate(Screen.AlbumDetail.createRoute(currentSong.albumId))
                    showSongInfoBottomSheet = false
                },
                onNavigateToArtist = {
                    // 跳转到艺人详情页
                    navController.navigate(Screen.ArtistDetail.createRoute(currentSong.artistId))
                    showSongInfoBottomSheet = false
                },
                onEditSong = { newTitle, newArtist, newAlbum, newGenre, newLyrics, newTrackNumber, coverArtUpdate ->
                    // 编辑歌曲元数据
                    playerViewModel.editSongMetadata(
                        currentSong,
                        newTitle,
                        newArtist,
                        newAlbum,
                        newGenre,
                        newLyrics,
                        newTrackNumber,
                        coverArtUpdate
                    )
                },
                generateAiMetadata = { fields ->
                    // AI生成歌曲元数据
                    playerViewModel.generateAiMetadata(currentSong, fields)
                },
            )
            // 显示添加到歌单面板
            if (showPlaylistBottomSheet) {
                val playlistUiState by playlistViewModel.uiState.collectAsState()

                PlaylistBottomSheet(
                    playlistUiState = playlistUiState,
                    song = currentSong,
                    onDismiss = { showPlaylistBottomSheet = false },
                    bottomBarHeight = bottomBarHeightDp,
                    playerViewModel = playerViewModel,
                )
            }
        }
    }
}

/**
 * 搜索结果分区标题
 * @param title 标题文本
 */
@Composable
fun SearchResultSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 4.dp)
    )
}

/**
 * 搜索历史列表
 * @param historyItems 历史项列表
 * @param onHistoryClick 点击历史项回调
 * @param onHistoryDelete 删除单条历史回调
 * @param onClearAllHistory 清空所有历史回调
 */
@Composable
fun SearchHistoryList(
    historyItems: List<SearchHistoryItem>,
    onHistoryClick: (String) -> Unit,
    onHistoryDelete: (String) -> Unit,
    onClearAllHistory: () -> Unit
) {
    val localDensity = LocalDensity.current
    Column {
        // 历史列表标题栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "最近搜索",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            // 清空所有历史按钮（有历史时显示）
            if (historyItems.isNotEmpty()) {
                TextButton(onClick = onClearAllHistory) {
                    Text("清空全部")
                }
            }
        }
        // 历史项列表
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(top = 8.dp)
        ) {
            items(historyItems, key = { "历史项_${it.id ?: it.query}" }) { item ->
                SearchHistoryListItem(
                    item = item,
                    onHistoryClick = onHistoryClick,
                    onHistoryDelete = onHistoryDelete
                )
            }
        }
    }
}

/**
 * 搜索历史列表项
 * @param item 历史项
 * @param onHistoryClick 点击历史项回调
 * @param onHistoryDelete 删除该历史项回调
 */
@Composable
fun SearchHistoryListItem(
    item: SearchHistoryItem,
    onHistoryClick: (String) -> Unit,
    onHistoryDelete: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) { detectTapGestures(onTap = { onHistoryClick(item.query) }) } // 点击事件
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // 历史项文本区域
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
                imageVector = Icons.Rounded.History,
                contentDescription = "历史图标",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = item.query,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        // 删除按钮
        IconButton(onClick = { onHistoryDelete(item.query) }) {
            Icon(
                imageVector = Icons.Rounded.DeleteForever,
                contentDescription = "删除历史项",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * 空搜索结果提示
 * @param searchQuery 搜索关键词
 * @param colorScheme 配色方案
 */
@Composable
fun EmptySearchResults(searchQuery: String, colorScheme: ColorScheme) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Rounded.Search, // 通用搜索图标
            contentDescription = "无结果",
            modifier = Modifier
                .size(80.dp)
                .padding(bottom = 16.dp),
            tint = colorScheme.primary.copy(alpha = 0.6f)
        )

        // 无结果提示文本
        Text(
            text = if (searchQuery.isNotBlank()) "未找到“$searchQuery”相关结果" else "未找到任何内容",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 引导文案
        Text(
            text = "可尝试更换搜索关键词，或检查筛选条件。",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * 搜索结果列表
 * @param results 搜索结果列表
 * @param playerViewModel 播放器视图模型
 * @param onItemSelected 选中结果项回调
 * @param currentPlayingSongId 当前播放歌曲ID
 * @param isPlaying 是否正在播放
 * @param onSongMoreOptionsClick 歌曲更多选项点击回调
 * @param navController 导航控制器
 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun SearchResultsList(
    results: List<SearchResultItem>,
    playerViewModel: PlayerViewModel,
    onItemSelected: () -> Unit,
    currentPlayingSongId: String?,
    isPlaying: Boolean,
    onSongMoreOptionsClick: (Song) -> Unit,
    navController: NavHostController
) {
    val localDensity = LocalDensity.current
    val playerStableState by playerViewModel.stablePlayerState.collectAsState()

    // 无结果时显示提示
    if (results.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("未找到结果。", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    // 按类型分组搜索结果
    val groupedResults = results.groupBy { item ->
        when (item) {
            is SearchResultItem.SongItem -> SearchFilterType.SONGS
            is SearchResultItem.AlbumItem -> SearchFilterType.ALBUMS
            is SearchResultItem.ArtistItem -> SearchFilterType.ARTISTS
            is SearchResultItem.PlaylistItem -> SearchFilterType.PLAYLISTS
        }
    }

    // 分区显示顺序
    val sectionOrder = listOf(
        SearchFilterType.SONGS,
        SearchFilterType.ALBUMS,
        SearchFilterType.ARTISTS,
        SearchFilterType.PLAYLISTS
    )

    // 输入法面板底部内边距
    val imePadding = WindowInsets.ime.getBottom(localDensity).dp
    // 系统栏底部总内边距（含迷你播放器）
    val systemBarPaddingBottom = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding() + 94.dp

    // 结果列表
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = 8.dp,
            // 底部内边距：输入法显示时用输入法内边距，否则用系统栏+迷你播放器内边距
            bottom = if (imePadding <= 8.dp) (MiniPlayerHeight + systemBarPaddingBottom) else imePadding
        )
    ) {
        // 按顺序显示各分区
        sectionOrder.forEach { filterType ->
            val itemsForSection = groupedResults[filterType] ?: emptyList()

            if (itemsForSection.isNotEmpty()) {
                // 分区标题
                item(key = "标题_${filterType.name}") {
                    SearchResultSectionHeader(
                        title = when (filterType) {
                            SearchFilterType.SONGS -> "歌曲"
                            SearchFilterType.ALBUMS -> "专辑"
                            SearchFilterType.ARTISTS -> "艺人"
                            SearchFilterType.PLAYLISTS -> "歌单"
                            else -> "结果"
                        }
                    )
                }

                // 分区内容
                items(
                    count = itemsForSection.size,
                    key = { index ->
                        val item = itemsForSection[index]
                        // 为每个项生成唯一Key
                        when (item) {
                            is SearchResultItem.SongItem -> "歌曲_${item.song.id}"
                            is SearchResultItem.AlbumItem -> "专辑_${item.album.id}"
                            is SearchResultItem.ArtistItem -> "艺人_${item.artist.id}"
                            is SearchResultItem.PlaylistItem -> "歌单_${item.playlist.id}_${index}"
                        }
                    }
                ) { index ->
                    val item = itemsForSection[index]
                    Box(modifier = Modifier.padding(bottom = 12.dp)) {
                        // 根据类型显示不同的结果项
                        when (item) {
                            is SearchResultItem.SongItem -> {
                                // 歌曲项点击事件（记忆化避免重组）
                                val rememberedOnClick = remember(item.song, playerViewModel, onItemSelected) {
                                    {
                                        playerViewModel.showAndPlaySong(item.song)
                                        onItemSelected()
                                    }
                                }
                                // 增强版歌曲列表项
                                EnhancedSongListItem(
                                    song = item.song,
                                    isPlaying = isPlaying,
                                    isCurrentSong = currentPlayingSongId == item.song.id,
                                    onMoreOptionsClick = onSongMoreOptionsClick,
                                    onClick = rememberedOnClick
                                )
                            }

                            is SearchResultItem.AlbumItem -> {
                                // 播放专辑点击事件
                                val onPlayClick = remember(item.album, playerViewModel, onItemSelected) {
                                    {
                                        Timber.tag("搜索页面")
                                            .d("点击专辑: ${item.album.title}")
                                        playerViewModel.playAlbum(item.album)
                                        onItemSelected()
                                    }
                                }
                                // 打开专辑详情页点击事件
                                val onOpenClick = remember(item.album, playerViewModel, onItemSelected) {
                                    {
                                        navController.navigate(Screen.AlbumDetail.createRoute(item.album.id))
                                        onItemSelected()
                                    }
                                }
                                // 专辑搜索结果项
                                SearchResultAlbumItem(
                                    album = item.album,
                                    onPlayClick = onPlayClick,
                                    onOpenClick = onOpenClick
                                )
                            }

                            is SearchResultItem.ArtistItem -> {
                                // 播放艺人歌曲点击事件
                                val onPlayClick = remember(item.artist, playerViewModel, onItemSelected) {
                                    {
                                        Timber.tag("搜索页面")
                                            .d("点击艺人: ${item.artist.name}")
                                        playerViewModel.playArtist(item.artist)
                                        onItemSelected()
                                    }
                                }
                                // 打开艺人详情页点击事件
                                val onOpenClick = remember(item.artist, playerViewModel, onItemSelected) {
                                    {
                                        navController.navigate(Screen.ArtistDetail.createRoute(item.artist.id))
                                        onItemSelected()
                                    }
                                }
                                // 艺人搜索结果项
                                SearchResultArtistItem(
                                    artist = item.artist,
                                    onPlayClick = onPlayClick,
                                    onOpenClick = onOpenClick
                                )
                            }

                            is SearchResultItem.PlaylistItem -> {
                                // 歌单内歌曲列表
                                var songsInPlaylist by remember { mutableStateOf<List<Song>>(emptyList()) }
                                var fetchSongs by remember { mutableStateOf(false) }
                                // 加载歌单内歌曲
                                LaunchedEffect(fetchSongs) {
                                    songsInPlaylist = playerViewModel.getSongs(item.playlist.songIds)
                                }
                                // 播放歌单点击事件
                                val onPlayClick = remember(item.playlist, playerViewModel, onItemSelected) {
                                    {
                                        fetchSongs = true
                                        if (songsInPlaylist.isNotEmpty()) {
                                            playerViewModel.playSongs(
                                                songsInPlaylist,
                                                songsInPlaylist.first(),
                                                item.playlist.name
                                            )
                                            // 若开启随机播放则切换（重置）
                                            if (playerStableState.isShuffleEnabled) playerViewModel.toggleShuffle()
                                        } else {
                                            // 空歌单提示
                                            playerViewModel.sendToast("歌单为空")
                                        }
                                        onItemSelected()
                                    }
                                }
                                // 打开歌单详情页点击事件
                                val onOpenClick = remember(item.playlist, playerViewModel, onItemSelected) {
                                    {
                                        navController.navigate(Screen.PlaylistDetail.createRoute(item.playlist.id))
                                        onItemSelected()
                                    }
                                }
                                // 歌单搜索结果项
                                SearchResultPlaylistItem(
                                    playlist = item.playlist,
                                    onPlayClick = onPlayClick,
                                    onOpenClick = onOpenClick
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 专辑搜索结果项
 * @param album 专辑数据
 * @param onOpenClick 打开专辑详情页回调
 * @param onPlayClick 播放专辑回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResultAlbumItem(
    album: Album,
    onOpenClick: () -> Unit,
    onPlayClick: () -> Unit
) {
    // 项的圆角形状
    val itemShape = remember {
        AbsoluteSmoothCornerShape(
            cornerRadiusTL = 26.dp,
            smoothnessAsPercentTR = 60,
            cornerRadiusTR = 26.dp,
            smoothnessAsPercentBR = 60,
            cornerRadiusBR = 26.dp,
            smoothnessAsPercentBL = 60,
            cornerRadiusBL = 26.dp,
            smoothnessAsPercentTL = 60
        )
    }

    Card(
        onClick = onOpenClick, // 点击卡片打开详情页
        shape = itemShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 专辑封面
            SmartImage(
                model = album.albumArtUriString,
                contentDescription = "专辑封面: ${album.title}",
                modifier = Modifier
                    .size(56.dp)
                    .clip(itemShape)
            )
            Spacer(Modifier.width(12.dp))
            // 专辑信息
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = album.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = album.artist,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // 播放按钮
            FilledIconButton(
                onClick = onPlayClick,
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
                    contentColor = MaterialTheme.colorScheme.onSecondary
                )
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = "播放专辑", modifier = Modifier.size(24.dp))
            }
        }
    }
}

/**
 * 艺人搜索结果项
 * @param artist 艺人数据
 * @param onOpenClick 打开艺人详情页回调
 * @param onPlayClick 播放艺人歌曲回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResultArtistItem(
    artist: Artist,
    onOpenClick: () -> Unit,
    onPlayClick: () -> Unit
) {
    // 项的圆角形状
    val itemShape = remember {
        AbsoluteSmoothCornerShape(
            cornerRadiusTL = 26.dp,
            smoothnessAsPercentTR = 60,
            cornerRadiusTR = 26.dp,
            smoothnessAsPercentBR = 60,
            cornerRadiusBR = 26.dp,
            smoothnessAsPercentBL = 60,
            cornerRadiusBL = 26.dp,
            smoothnessAsPercentTL = 60
        )
    }

    Card(
        onClick = onOpenClick, // 点击卡片打开详情页
        shape = itemShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 艺人图标
            Icon(
                painter = painterResource(id = R.drawable.rounded_artist_24),
                contentDescription = "艺人",
                modifier = Modifier
                    .size(56.dp)
                    .background(MaterialTheme.colorScheme.tertiaryContainer, CircleShape)
                    .padding(12.dp),
                tint = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Spacer(Modifier.width(12.dp))
            // 艺人信息
            Column(Modifier.weight(1f)) {
                Text(
                    text = artist.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${artist.songCount} 首歌曲",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // 播放按钮
            FilledIconButton(
                onClick = onPlayClick,
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f),
                    contentColor = MaterialTheme.colorScheme.onTertiary
                )
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = "播放艺人歌曲", modifier = Modifier.size(24.dp))
            }
        }
    }
}

/**
 * 歌单搜索结果项
 * @param playlist 歌单数据
 * @param onOpenClick 打开歌单详情页回调
 * @param onPlayClick 播放歌单回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResultPlaylistItem(
    playlist: Playlist,
    onOpenClick: () -> Unit,
    onPlayClick: () -> Unit
) {
    // 项的圆角形状
    val itemShape = remember {
        AbsoluteSmoothCornerShape(
            cornerRadiusTL = 26.dp,
            smoothnessAsPercentTR = 60,
            cornerRadiusTR = 26.dp,
            smoothnessAsPercentBR = 60,
            cornerRadiusBR = 26.dp,
            smoothnessAsPercentBL = 60,
            cornerRadiusBL = 26.dp,
            smoothnessAsPercentTL = 60
        )
    }

    Card(
        onClick = onOpenClick, // 点击卡片打开详情页
        shape = itemShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 歌单图标
            Icon(
                imageVector = Icons.Rounded.PlaylistPlay,
                contentDescription = "歌单",
                modifier = Modifier
                    .size(56.dp)
                    .clip(itemShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .padding(8.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
            )
            Spacer(Modifier.width(12.dp))
            // 歌单信息
            Column(Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            // 播放按钮
            FilledIconButton(
                onClick = onPlayClick,
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = "播放歌单", modifier = Modifier.size(24.dp))
            }
        }
    }
}

/**
 * 搜索筛选标签
 * @param filterType 筛选类型
 * @param currentFilter 当前选中的筛选类型
 * @param playerViewModel 播放器视图模型
 * @param modifier 修饰符
 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun SearchFilterChip(
    filterType: SearchFilterType,
    currentFilter: SearchFilterType, // 该值应来自PlayerViewModel的状态
    playerViewModel: PlayerViewModel,
    modifier: Modifier = Modifier
) {
    val selected = filterType == currentFilter // 是否选中

    FilterChip(
        selected = selected, // 选中状态
        onClick = { playerViewModel.updateSearchFilter(filterType) }, // 切换筛选类型
        label = { Text(filterType.name.lowercase().replaceFirstChar { it.titlecase() }) }, // 标签文本（首字母大写）
        modifier = modifier,
        shape = CircleShape, // 圆形形状
        border = BorderStroke(
            width = 0.dp,
            color = Color.Transparent
        ),
        colors = FilterChipDefaults.filterChipColors(
            // 未选中状态颜色
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
            // 选中状态颜色
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
        // 选中时显示的左侧图标
        leadingIcon = if (selected) {
            {
                Icon(
                    painter = painterResource(R.drawable.rounded_check_circle_24),
                    contentDescription = "已选中",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                )
            }
        } else {
            null
        }
    )
}
