package com.theveloper.pixelplay.data.model

import androidx.compose.runtime.Immutable

@Immutable
enum class LibraryTabId(
    val storageKey: String,
    val title: String,
    val defaultSort: SortOption
) {
    SONGS("歌曲", "歌曲", SortOption.SongTitleAZ),
    ALBUMS("专辑", "专辑", SortOption.AlbumTitleAZ),
    ARTISTS("歌手", "歌手", SortOption.ArtistNameAZ),
    PLAYLISTS("歌单", "歌单", SortOption.PlaylistNameAZ),
    FOLDERS("文件夹", "文件夹", SortOption.FolderNameAZ),
    LIKED("收藏", "收藏", SortOption.LikedSongDateLiked);

    companion object {
        fun fromStorageKey(key: String): LibraryTabId =
            entries.firstOrNull { it.storageKey == key } ?: SONGS
    }
}

fun String.toLibraryTabIdOrNull(): LibraryTabId? =
    LibraryTabId.entries.firstOrNull { it.storageKey == this }
