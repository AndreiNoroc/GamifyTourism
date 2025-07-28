package com.example.myfindgo.presentation.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*
import com.example.myfindgo.data.models.LeaderboardEntry
import com.example.myfindgo.data.repositories.LeaderboardRepository
import kotlinx.coroutines.launch

@Composable
fun LeaderboardScreen(
    onBackClick: () -> Unit
) {
    var leaderboard by remember { mutableStateOf<List<LeaderboardEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val leaderboardRepository = remember { LeaderboardRepository() }

    LaunchedEffect(Unit) {
        scope.launch {
            leaderboard = leaderboardRepository.getLeaderboard()
            isLoading = false
        }
    }

    Scaffold(
        positionIndicator = {
            PositionIndicator(
                scalingLazyListState = rememberScalingLazyListState()
            )
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                ScalingLazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    item {
                        Button(
                            onClick = onBackClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text("Back")
                        }
                    }

                    itemsIndexed(leaderboard) { index, entry ->
                        LeaderboardItem(
                            entry = entry,
                            position = index + 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LeaderboardItem(
    entry: LeaderboardEntry,
    position: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                when (position) {
                    1 -> Color(0xFFFFD700) // Gold
                    2 -> Color(0xFFC0C0C0) // Silver
                    3 -> Color(0xFFCD7F32) // Bronze
                    else -> MaterialTheme.colors.surface
                }
            )
            .clip(RoundedCornerShape(8.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colors.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = position.toString(),
                        style = MaterialTheme.typography.body2,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.onPrimary
                    )
                }

                Column {
                    Text(
                        text = entry.username,
                        style = MaterialTheme.typography.body2,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${entry.score} points",
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurfaceVariant
                    )
                }
            }

            if (position <= 3) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Top ${position}",
                    tint = MaterialTheme.colors.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
} 