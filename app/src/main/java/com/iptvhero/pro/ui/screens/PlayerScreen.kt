package com.iptvhero.pro.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.iptvhero.pro.ui.components.heroFocusable

@Composable
fun PlayerScreen(
    streamUrl: String?,
    channelTitle: String,
    onStepChannel: (Int) -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val playerFocusRequester = remember { FocusRequester() }

    val exoPlayer = remember {
        ExoPlayer.Builder(context.applicationContext).build()
    }

    LaunchedEffect(streamUrl) {
        if (!streamUrl.isNullOrBlank()) {
            exoPlayer.stop()
            exoPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(streamUrl.trim())))
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        }
        runCatching { playerFocusRequester.requestFocus() }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(playerFocusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.ChannelUp, Key.MediaNext -> {
                            onStepChannel(1)
                            true
                        }
                        Key.ChannelDown, Key.MediaPrevious -> {
                            onStepChannel(-1)
                            true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                }
            },
            update = { view ->
                view.player = exoPlayer
            },
            modifier = Modifier.fillMaxSize()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.65f))
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.14f))
                    .heroFocusable(RoundedCornerShape(10.dp)) { onBack() }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text("< CATALOGO", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.12f))
                    .heroFocusable(RoundedCornerShape(10.dp)) { onStepChannel(-1) }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text("CH -", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.12f))
                    .heroFocusable(RoundedCornerShape(10.dp)) { onStepChannel(1) }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text("CH +", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            Text(
                text = channelTitle.ifBlank { streamUrl ?: "Sem stream selecionado" },
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
