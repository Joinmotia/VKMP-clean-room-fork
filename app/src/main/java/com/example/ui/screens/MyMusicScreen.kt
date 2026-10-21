package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AuthSession
import com.example.model.VkAudioTrack
import com.example.ui.components.TrackItemRow
import com.example.ui.theme.VkBluePrimary
import com.example.ui.theme.VkGreenCached

@Composable
fun MyMusicScreen(
    tracks: List<VkAudioTrack>,
    currentPlayingTrack: VkAudioTrack?,
    session: AuthSession,
    searchQuery: String,
    downloadProgressMap: Map<String, Float>,
    isBatchDownloading: Boolean,
    batchProgress: Pair<Int, Int>,
    batchStatusText: String,
    onSearchChange: (String) -> Unit,
    onTrackClick: (VkAudioTrack) -> Unit,
    onPlayAllClick: () -> Unit,
    onDownloadAllClick: () -> Unit,
    onCancelBatchClick: () -> Unit,
    onDownloadTrack: (VkAudioTrack) -> Unit,
    onDeleteCacheTrack: (String) -> Unit,
    onToggleFavorite: (VkAudioTrack) -> Unit,
    onOpenAuthDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("my_music_screen")
    ) {
        // Top App Bar with Title & MAX / Account Status
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Моя музыка",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (session.isLoggedIn) {
                        if (session.isMaxVerified) "${session.userName} • MAX ID ✓" else session.userName
                    } else "Не авторизован",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (session.isMaxVerified) VkBluePrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Auth / Profile Button
            Button(
                onClick = onOpenAuthDialog,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (session.isMaxVerified) VkBluePrimary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (session.isMaxVerified) Color.White else MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.testTag("profile_auth_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (session.isLoggedIn) "Профиль" else "Войти",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Search Box
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Поиск по моим трекам...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Очистить",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .testTag("my_music_search_field")
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Batch Download Banner if active
        AnimatedVisibility(visible = isBatchDownloading) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("batch_download_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Скачивание аудиозаписей...",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        IconButton(
                            onClick = onCancelBatchClick,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cancel,
                                contentDescription = "Отмена",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = batchStatusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    val progressFraction = if (batchProgress.second > 0) {
                        (batchProgress.first.toFloat() / batchProgress.second.toFloat()).coerceIn(0f, 1f)
                    } else 0f

                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Action Buttons Row: "Слушать всё" and "📥 Скачать все"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Play All
            Button(
                onClick = onPlayAllClick,
                modifier = Modifier
                    .weight(1f)
                    .testTag("play_all_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Слушать всё", fontWeight = FontWeight.SemiBold)
            }

            // Download All ("Скачать все")
            OutlinedButton(
                onClick = onDownloadAllClick,
                enabled = !isBatchDownloading,
                modifier = Modifier
                    .weight(1f)
                    .testTag("download_all_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = VkBluePrimary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Скачать все", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            }
        }

        // Track count header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${tracks.size} треков",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val cachedCount = tracks.count { it.isCached }
            if (cachedCount > 0) {
                Text(
                    text = "$cachedCount в офлайн-кеше",
                    style = MaterialTheme.typography.bodySmall,
                    color = VkGreenCached
                )
            }
        }

        // Track List
        if (tracks.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (searchQuery.isNotEmpty()) "Ничего не найдено" else "Список треков пуст",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Попробуйте изменить поисковый запрос",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("my_music_track_list")
            ) {
                items(tracks, key = { it.id }) { track ->
                    val isPlaying = currentPlayingTrack?.id == track.id
                    val downloadProgress = downloadProgressMap[track.id]
                    TrackItemRow(
                        track = track,
                        isCurrentPlaying = isPlaying,
                        downloadProgress = downloadProgress,
                        onClick = { onTrackClick(track) },
                        onDownloadClick = { onDownloadTrack(track) },
                        onDeleteCacheClick = { onDeleteCacheTrack(track.id) },
                        onToggleFavorite = { onToggleFavorite(track) }
                    )
                }
            }
        }
    }
}
