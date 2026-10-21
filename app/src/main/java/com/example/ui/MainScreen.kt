package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.DownloadDone
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.AuthDialog
import com.example.ui.components.EqualizerDialog
import com.example.ui.components.FullPlayerSheet
import com.example.ui.components.MiniPlayerBar
import com.example.ui.components.SleepTimerDialog
import com.example.ui.screens.DiscoverScreen
import com.example.ui.screens.MyMusicScreen
import com.example.ui.screens.OfflineScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.VkBluePrimary
import com.example.ui.viewmodel.MusicPlayerViewModel

@Composable
fun MainScreen(
    viewModel: MusicPlayerViewModel = viewModel()
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    val session by viewModel.session.collectAsState()
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentPositionMs by viewModel.currentPositionMs.collectAsState()
    val durationMs by viewModel.durationMs.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val isShuffle by viewModel.isShuffle.collectAsState()
    val queue by viewModel.queue.collectAsState()
    val sleepTimerMinutes by viewModel.sleepTimerMinutes.collectAsState()
    val equalizerPreset by viewModel.equalizerPreset.collectAsState()

    val filteredLibrary by viewModel.filteredLibrary.collectAsState()
    val vkCharts by viewModel.vkCharts.collectAsState()
    val recommendations by viewModel.recommendations.collectAsState()
    val cachedTracks by viewModel.cachedTracks.collectAsState()
    val totalCacheSizeBytes by viewModel.totalCacheSizeBytes.collectAsState()

    val downloadProgressMap by viewModel.downloadProgressMap.collectAsState()
    val isBatchDownloading by viewModel.isBatchDownloading.collectAsState()
    val batchProgress by viewModel.batchProgress.collectAsState()
    val batchStatusText by viewModel.batchStatusText.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val isFullPlayerVisible by viewModel.isFullPlayerVisible.collectAsState()
    val isSleepTimerDialogVisible by viewModel.isSleepTimerDialogVisible.collectAsState()
    val isEqualizerDialogVisible by viewModel.isEqualizerDialogVisible.collectAsState()
    val isAuthDialogVisible by viewModel.isAuthDialogVisible.collectAsState()

    val authPhoneInput by viewModel.authPhoneInput.collectAsState()
    val authTokenInput by viewModel.authTokenInput.collectAsState()
    val isAuthLoading by viewModel.isAuthLoading.collectAsState()
    val authStatusMessage by viewModel.authStatusMessage.collectAsState()

    // Back handling
    BackHandler(enabled = isFullPlayerVisible || selectedTab != 0) {
        if (isFullPlayerVisible) {
            viewModel.isFullPlayerVisible.value = false
        } else {
            selectedTab = 0
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                Column(
                    modifier = Modifier
                        .navigationBarsPadding()
                ) {
                    // Mini Player above NavigationBar
                    if (currentTrack != null && !isFullPlayerVisible) {
                        MiniPlayerBar(
                            track = currentTrack,
                            isPlaying = isPlaying,
                            currentPositionMs = currentPositionMs,
                            durationMs = durationMs,
                            onBarClick = { viewModel.isFullPlayerVisible.value = true },
                            onPlayPauseClick = { viewModel.togglePlayPause() },
                            onNextClick = { viewModel.next() }
                        )
                    }

                    // Navigation Bar
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp,
                        modifier = Modifier.testTag("bottom_nav_bar")
                    ) {
                        // Tab 0: Моя музыка
                        NavigationBarItem(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == 0) Icons.Default.LibraryMusic else Icons.Outlined.LibraryMusic,
                                    contentDescription = "Моя музыка"
                                )
                            },
                            label = { Text("Моя музыка") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                indicatorColor = VkBluePrimary
                            ),
                            modifier = Modifier.testTag("nav_tab_my_music")
                        )

                        // Tab 1: Рекомендации
                        NavigationBarItem(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == 1) Icons.Default.Explore else Icons.Outlined.Explore,
                                    contentDescription = "Рекомендации"
                                )
                            },
                            label = { Text("Чарты") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                indicatorColor = VkBluePrimary
                            ),
                            modifier = Modifier.testTag("nav_tab_discover")
                        )

                        // Tab 2: Офлайн
                        NavigationBarItem(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == 2) Icons.Default.DownloadDone else Icons.Outlined.DownloadDone,
                                    contentDescription = "Офлайн"
                                )
                            },
                            label = { Text("Офлайн") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                indicatorColor = VkBluePrimary
                            ),
                            modifier = Modifier.testTag("nav_tab_offline")
                        )

                        // Tab 3: Настройки
                        NavigationBarItem(
                            selected = selectedTab == 3,
                            onClick = { selectedTab = 3 },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == 3) Icons.Default.Settings else Icons.Outlined.Settings,
                                    contentDescription = "Настройки"
                                )
                            },
                            label = { Text("Настройки") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                indicatorColor = VkBluePrimary
                            ),
                            modifier = Modifier.testTag("nav_tab_settings")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .statusBarsPadding()
            ) {
                when (selectedTab) {
                    0 -> MyMusicScreen(
                        tracks = filteredLibrary,
                        currentPlayingTrack = currentTrack,
                        session = session,
                        searchQuery = searchQuery,
                        downloadProgressMap = downloadProgressMap,
                        isBatchDownloading = isBatchDownloading,
                        batchProgress = batchProgress,
                        batchStatusText = batchStatusText,
                        onSearchChange = { viewModel.searchQuery.value = it },
                        onTrackClick = { track -> viewModel.playTrack(track, filteredLibrary) },
                        onPlayAllClick = { viewModel.playAll(filteredLibrary) },
                        onDownloadAllClick = { viewModel.downloadAllMyMusic() },
                        onCancelBatchClick = { viewModel.cancelBatchDownload() },
                        onDownloadTrack = { track -> viewModel.downloadTrack(track) },
                        onDeleteCacheTrack = { trackId -> viewModel.removeCachedTrack(trackId) },
                        onToggleFavorite = { track -> viewModel.toggleFavorite(track) },
                        onOpenAuthDialog = { viewModel.isAuthDialogVisible.value = true }
                    )

                    1 -> DiscoverScreen(
                        charts = vkCharts,
                        recommendations = recommendations,
                        currentPlayingTrack = currentTrack,
                        downloadProgressMap = downloadProgressMap,
                        onTrackClick = { track -> viewModel.playTrack(track, vkCharts) },
                        onDownloadTrack = { track -> viewModel.downloadTrack(track) },
                        onDeleteCacheTrack = { trackId -> viewModel.removeCachedTrack(trackId) },
                        onToggleFavorite = { track -> viewModel.toggleFavorite(track) }
                    )

                    2 -> OfflineScreen(
                        cachedTracks = cachedTracks,
                        currentPlayingTrack = currentTrack,
                        totalSizeBytes = totalCacheSizeBytes,
                        onTrackClick = { track -> viewModel.playTrack(track, cachedTracks) },
                        onPlayAllClick = { viewModel.playAll(cachedTracks) },
                        onDeleteCacheTrack = { trackId -> viewModel.removeCachedTrack(trackId) },
                        onClearAllCache = { viewModel.clearAllCache() }
                    )

                    3 -> SettingsScreen(
                        session = session,
                        equalizerPreset = equalizerPreset,
                        sleepTimerMinutes = sleepTimerMinutes,
                        onOpenAuthDialog = { viewModel.isAuthDialogVisible.value = true },
                        onLogout = { viewModel.logout() },
                        onOpenEqualizer = { viewModel.isEqualizerDialogVisible.value = true },
                        onOpenSleepTimer = { viewModel.isSleepTimerDialogVisible.value = true },
                        onClearCache = { viewModel.clearAllCache() }
                    )
                }
            }
        }

        // Full Screen Player Animated Overlay
        AnimatedVisibility(
            visible = isFullPlayerVisible,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.fillMaxSize()
        ) {
            FullPlayerSheet(
                track = currentTrack,
                isPlaying = isPlaying,
                currentPositionMs = currentPositionMs,
                durationMs = durationMs,
                repeatMode = repeatMode,
                isShuffle = isShuffle,
                sleepTimerMinutes = sleepTimerMinutes,
                queue = queue,
                isFavorite = filteredLibrary.any { it.id == currentTrack?.id },
                onClose = { viewModel.isFullPlayerVisible.value = false },
                onPlayPause = { viewModel.togglePlayPause() },
                onNext = { viewModel.next() },
                onPrevious = { viewModel.previous() },
                onSeek = { pos -> viewModel.seekTo(pos) },
                onToggleRepeat = { viewModel.toggleRepeat() },
                onToggleShuffle = { viewModel.toggleShuffle() },
                onToggleFavorite = { currentTrack?.let { viewModel.toggleFavorite(it) } },
                onDownloadClick = { currentTrack?.let { viewModel.downloadTrack(it) } },
                onSleepTimerClick = { viewModel.isSleepTimerDialogVisible.value = true },
                onEqualizerClick = { viewModel.isEqualizerDialogVisible.value = true },
                onSelectQueueTrack = { queueTrack -> viewModel.playTrack(queueTrack) }
            )
        }

        // Sleep Timer Dialog
        if (isSleepTimerDialogVisible) {
            SleepTimerDialog(
                currentMinutes = sleepTimerMinutes,
                onSelectMinutes = { mins -> viewModel.setSleepTimer(mins) },
                onDismiss = { viewModel.isSleepTimerDialogVisible.value = false }
            )
        }

        // Equalizer Dialog
        if (isEqualizerDialogVisible) {
            EqualizerDialog(
                currentPreset = equalizerPreset,
                onSelectPreset = { preset -> viewModel.setEqualizerPreset(preset) },
                onDismiss = { viewModel.isEqualizerDialogVisible.value = false }
            )
        }

        // Auth Dialog (MAX & Token)
        if (isAuthDialogVisible) {
            AuthDialog(
                phoneInput = authPhoneInput,
                tokenInput = authTokenInput,
                isLoading = isAuthLoading,
                statusMessage = authStatusMessage,
                onPhoneChange = { viewModel.authPhoneInput.value = it },
                onTokenChange = { viewModel.authTokenInput.value = it },
                onLoginMax = { viewModel.loginWithMax() },
                onLoginToken = { viewModel.loginWithToken() },
                onContinueGuest = { viewModel.continueAsGuest() },
                onDismiss = { viewModel.isAuthDialogVisible.value = false }
            )
        }
    }
}
