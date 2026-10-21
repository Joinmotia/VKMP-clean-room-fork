package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.VkAudioTrack
import com.example.ui.components.TrackItemRow
import com.example.ui.theme.VkBluePrimary

@Composable
fun DiscoverScreen(
    charts: List<VkAudioTrack>,
    recommendations: List<VkAudioTrack>,
    currentPlayingTrack: VkAudioTrack?,
    downloadProgressMap: Map<String, Float>,
    onTrackClick: (VkAudioTrack) -> Unit,
    onDownloadTrack: (VkAudioTrack) -> Unit,
    onDeleteCacheTrack: (String) -> Unit,
    onToggleFavorite: (VkAudioTrack) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedGenre by remember { mutableStateOf("Все") }
    val genres = listOf("Все", "Хиты", "Хип-хоп", "Рок", "Поп", "Электроника", "Фонк")

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("discover_screen")
    ) {
        // Title
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Обзор и Чарты",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Популярное ВКонтакте прямо сейчас",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Genre filter chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                genres.forEach { genre ->
                    val isSelected = selectedGenre == genre
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedGenre = genre },
                        label = { Text(genre) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = VkBluePrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        // Section: "Чарт ВКонтакте"
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint = Color(0xFFFF5722),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Чарт ВКонтакте: Топ-10",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        itemsIndexed(charts.take(10), key = { _, track -> "chart_${track.id}" }) { index, track ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Number badge (1, 2, 3...)
                Text(
                    text = "${index + 1}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    color = if (index < 3) VkBluePrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .width(22.dp)
                )

                Box(modifier = Modifier.weight(1f)) {
                    TrackItemRow(
                        track = track,
                        isCurrentPlaying = currentPlayingTrack?.id == track.id,
                        downloadProgress = downloadProgressMap[track.id],
                        onClick = { onTrackClick(track) },
                        onDownloadClick = { onDownloadTrack(track) },
                        onDeleteCacheClick = { onDeleteCacheTrack(track.id) },
                        onToggleFavorite = { onToggleFavorite(track) }
                    )
                }
            }
        }

        // Section: "Рекомендации для вас"
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Рекомендации для вас",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        itemsIndexed(recommendations, key = { _, track -> "rec_${track.id}" }) { _, track ->
            TrackItemRow(
                track = track,
                isCurrentPlaying = currentPlayingTrack?.id == track.id,
                downloadProgress = downloadProgressMap[track.id],
                onClick = { onTrackClick(track) },
                onDownloadClick = { onDownloadTrack(track) },
                onDeleteCacheClick = { onDeleteCacheTrack(track.id) },
                onToggleFavorite = { onToggleFavorite(track) }
            )
        }
    }
}
