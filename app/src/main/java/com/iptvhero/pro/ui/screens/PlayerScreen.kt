package com.iptvhero.pro.ui.screens

import android.app.Activity
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.iptvhero.pro.ui.components.heroFocusable
import kotlinx.coroutines.delay

@Composable
fun PlayerScreen(
    streamUrl: String?,
    channelTitle: String,
    onStepChannel: (Int) -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val playerFocusRequester = remember { FocusRequester() }
    var controlsVisible by remember { mutableStateOf(true) }
    var interactionTick by remember { mutableIntStateOf(0) }

    fun revealControlsTemporarily() {
        controlsVisible = true
        interactionTick += 1
    }

    val exoPlayer = remember {
        ExoPlayer.Builder(context.applicationContext).build()
    }

    // Oculta barra de status e barra de navegação do Android/TV para tela cheia real
    DisposableEffect(context) {
        val activity = context as? Activity
        val window = activity?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        controller?.let {
            it.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            it.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    LaunchedEffect(streamUrl) {
        if (!streamUrl.isNullOrBlank()) {
            exoPlayer.stop()
            exoPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(streamUrl.trim())))
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
            revealControlsTemporarily()
        }
        runCatching { playerFocusRequester.requestFocus() }
    }

    // Auto-oculta a barra superior (< CATALOGO, CH -, CH +, título) após 4 segundos sem interação
    LaunchedEffect(controlsVisible, interactionTick) {
        if (controlsVisible) {
            delay(4000L)
            controlsVisible = false
            runCatching { playerFocusRequester.requestFocus() }
        }
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
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                controlsVisible = !controlsVisible
                if (controlsVisible) interactionTick += 1
            }
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.ChannelUp, Key.MediaNext -> {
                            onStepChannel(1)
                            revealControlsTemporarily()
                            true
                        }
                        Key.ChannelDown, Key.MediaPrevious -> {
                            onStepChannel(-1)
                            revealControlsTemporarily()
                            true
                        }
                        Key.DirectionCenter, Key.Enter, Key.Menu, Key.Info -> {
                            controlsVisible = !controlsVisible
                            if (controlsVisible) interactionTick += 1
                            true
                        }
                        else -> {
                            revealControlsTemporarily()
                            false
                        }
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
                    useController = false
                }
            },
            update = { view ->
                view.player = exoPlayer
                view.useController = false
            },
            modifier = Modifier.fillMaxSize()
        )

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.72f))
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
                        .heroFocusable(RoundedCornerShape(10.dp)) {
                            onStepChannel(-1)
                            revealControlsTemporarily()
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text("CH -", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                        .heroFocusable(RoundedCornerShape(10.dp)) {
                            onStepChannel(1)
                            revealControlsTemporarily()
                        }
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
}
