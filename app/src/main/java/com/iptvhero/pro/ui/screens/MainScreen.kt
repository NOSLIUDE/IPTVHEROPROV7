package com.iptvhero.pro.ui.screens

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.iptvhero.pro.ui.components.ProfileManagerDialog
import com.iptvhero.pro.ui.components.heroFocusable
import com.iptvhero.pro.ui.theme.DcCyan
import com.iptvhero.pro.ui.theme.GoldFocus
import com.iptvhero.pro.ui.theme.HeroThemeType
import com.iptvhero.pro.ui.theme.MarvelYellow
import com.iptvhero.pro.ui.theme.TextDim
import com.iptvhero.pro.ui.theme.TextPrimary
import com.iptvhero.pro.ui.viewmodel.MainViewModel

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    currentTheme: HeroThemeType = HeroThemeType.DUEL,
    onCycleTheme: () -> Unit = {},
    onBackToProfiles: () -> Unit = {},
    onNavigateToFullPlayer: () -> Unit = {},
    exitHintText: String? = null,
    onTriggerBack: () -> Boolean = { false },
    onExitApp: () -> Unit = {}
) {
    val context = LocalContext.current
    val profiles by viewModel.profiles.collectAsState(initial = emptyList())
    val channels by viewModel.channels.collectAsState(initial = emptyList())
    val uiState by viewModel.uiState.collectAsState()
    val activeProfile = viewModel.activeProfile

    var selectedTab by remember { mutableStateOf("CANAIS") }
    var selectedCategory by remember { mutableStateOf("") }
    val categoryHistory = remember { mutableStateListOf<String>() }
    var searchQuery by remember { mutableStateOf("") }
    var showProfileDialog by remember { mutableStateOf(false) }

    val previewPlayer = remember {
        ExoPlayer.Builder(context.applicationContext).build()
    }

    DisposableEffect(Unit) {
        onDispose {
            previewPlayer.release()
        }
    }

    fun selectCategoryTracked(newCategory: String) {
        if (newCategory != selectedCategory) {
            categoryHistory.add(selectedCategory)
            selectedCategory = newCategory
        }
    }

    fun stopPlayback() {
        previewPlayer.stop()
        previewPlayer.clearMediaItems()
        viewModel.setStreamUrl(null, "")
    }

    fun playChannelInPreview(title: String, url: String?) {
        if (url.isNullOrBlank()) return
        viewModel.setStreamUrl(url, title)
        previewPlayer.setMediaItem(MediaItem.fromUri(url.trim()))
        previewPlayer.prepare()
        previewPlayer.playWhenReady = true
    }

    val tabs = listOf("CANAIS", "FILMES", "SERIES")
    val tabChannels = remember(channels, selectedTab) {
        when (selectedTab) {
            "FILMES" -> channels.filter { it.contentType.equals("VOD", ignoreCase = true) }
            "SERIES" -> channels.filter { it.contentType.equals("SERIES", ignoreCase = true) }
            else -> channels.filter { it.contentType.equals("LIVE", ignoreCase = true) }
        }
    }

    val availableCategories = remember(tabChannels) {
        tabChannels.groupingBy { it.groupName.ifBlank { "Geral" } }.eachCount().toSortedMap()
    }

    val filteredChannels = remember(tabChannels, selectedCategory, searchQuery) {
        val byCat = if (selectedCategory.isBlank()) tabChannels
        else tabChannels.filter { it.groupName.equals(selectedCategory, ignoreCase = true) }
        if (searchQuery.isBlank()) byCat
        else byCat.filter { it.title.contains(searchQuery.trim(), ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(currentTheme.backgroundBrush)
            .padding(14.dp)
    ) {
        // 1. TOP BAR V7
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(currentTheme.headerBrush)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "IPTV HERO PRO • V6/7",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 17.sp
                )
                Text(
                    text = if (uiState.currentChannelTitle.isNotBlank()) {
                        "Ativo: ${uiState.currentChannelTitle}"
                    } else {
                        activeProfile?.let { "Perfil: ${it.name} • ${filteredChannels.size} itens" }
                            ?: "Sem perfil ativo • Abra Listas / M3U / Xtream"
                    },
                    color = Color.White.copy(alpha = 0.88f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0x8C0A0A16))
                        .heroFocusable(CircleShape) { stopPlayback() }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text("PARAR", color = MarvelYellow, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                }

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0x8C0A0A16))
                        .heroFocusable(CircleShape) {
                            previewPlayer.playWhenReady = false
                            onNavigateToFullPlayer()
                        }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text("TELA CHEIA", color = DcCyan, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                }

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0x8C0A0A16))
                        .heroFocusable(CircleShape) { showProfileDialog = true }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text("LISTAS / M3U / XTREAM", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                }

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0x8C0A0A16))
                        .heroFocusable(CircleShape) { onCycleTheme() }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Text("TEMA", color = MarvelYellow, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x8C0A0A16))
                        .heroFocusable(RoundedCornerShape(10.dp)) { onBackToProfiles() }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = activeProfile?.name ?: "PERFIS",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. CORPO PRINCIPAL: SIDEBAR (ABAS + CATEGORIAS) + ÁREA CENTRAL (PREVIEW + BUSCA + GRADE)
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // SIDEBAR ESQUERDA
            Column(
                modifier = Modifier
                    .width(220.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xCC0D111C))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    tabs.forEach { tab ->
                        val selected = selectedTab == tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) GoldFocus.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.05f))
                                .heroFocusable(RoundedCornerShape(8.dp)) {
                                    selectedTab = tab
                                    selectedCategory = ""
                                    categoryHistory.clear()
                                    searchQuery = ""
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tab,
                                color = if (selected) GoldFocus else TextDim,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedCategory.isBlank()) GoldFocus.copy(alpha = 0.20f) else Color.Transparent)
                                .heroFocusable(RoundedCornerShape(8.dp)) { selectCategoryTracked("") }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Todas", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("${tabChannels.size}", color = GoldFocus, fontSize = 12.sp)
                        }
                    }
                    items(availableCategories.entries.toList()) { (catName, count) ->
                        val isCatSelected = selectedCategory.equals(catName, ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isCatSelected) GoldFocus.copy(alpha = 0.20f) else Color.Transparent)
                                .heroFocusable(RoundedCornerShape(8.dp)) { selectCategoryTracked(catName) }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = catName,
                                color = if (isCatSelected) GoldFocus else TextPrimary,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text("$count", color = TextDim, fontSize = 11.sp)
                        }
                    }
                }
            }

            // ÁREA CENTRAL: MINI PREVIEW + BUSCA + GRADE
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(260.dp)
                            .height(135.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black)
                    ) {
                        if (!uiState.currentStreamUrl.isNullOrBlank()) {
                            AndroidView(
                                factory = { ctx ->
                                    PlayerView(ctx).apply {
                                        player = previewPlayer
                                        useController = false
                                        layoutParams = FrameLayout.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.MATCH_PARENT
                                        )
                                    }
                                },
                                update = { it.player = previewPlayer },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Preview em espera", color = TextDim, fontSize = 12.sp)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Buscar canal, filme ou série...", color = TextDim, fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldFocus,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }

                if (filteredChannels.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x990D111C)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.errorMessage
                                ?: if (uiState.isLoading) (uiState.successMessage ?: "Processando lista...")
                                else "Nenhum item encontrado nesta aba/categoria.",
                            color = if (uiState.errorMessage != null) MarvelYellow else TextDim,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(filteredChannels, key = { _, ch -> ch.id }) { _, channel ->
                            val isPlaying = uiState.currentStreamUrl == channel.streamUrl && !channel.streamUrl.isNullOrBlank()
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isPlaying) Color(0xFF1D2942) else Color(0xCC121726))
                                    .heroFocusable(RoundedCornerShape(10.dp)) {
                                        playChannelInPreview(channel.title, channel.streamUrl)
                                    }
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = channel.groupName.ifBlank { selectedTab },
                                    color = DcCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = channel.title,
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. FOOTER D-PAD COM HISTÓRICO E 3 TOQUES EM VOLTAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xCC0D111C))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = exitHintText ?: "D-Pad: Navegue • OK: Reproduzir • Voltar: Categoria/Tela anterior (3x para sair)",
                color = if (exitHintText != null) MarvelYellow else TextDim,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .heroFocusable(RoundedCornerShape(8.dp)) {
                            if (categoryHistory.isNotEmpty()) {
                                selectedCategory = categoryHistory.removeAt(categoryHistory.lastIndex)
                            } else {
                                onTriggerBack()
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("VOLTAR", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .heroFocusable(RoundedCornerShape(8.dp)) { onExitApp() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("SAIR", color = MarvelYellow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showProfileDialog) {
        ProfileManagerDialog(
            profiles = profiles,
            activeProfileId = activeProfile?.id,
            onDismiss = { showProfileDialog = false },
            onActivateProfile = { viewModel.selectProfile(it) },
            onDeleteProfile = { viewModel.deleteProfile(it) },
            onImportContent = { txtContent ->
                viewModel.importBatchTxt(txtContent)
                showProfileDialog = false
            },
            onImportGithubHero = { url ->
                viewModel.loadHeroListFromGithub(url)
                showProfileDialog = false
            },
            onImportXtream = { server, user, pass ->
                viewModel.importXtream(server, user, pass)
                showProfileDialog = false
            }
        )
    }

    if (uiState.pendingHeroMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissPendingHeroList() },
            title = { Text("Atualização de Lista") },
            text = { Text(uiState.pendingHeroMessage ?: "") },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmPendingHeroList() }) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissPendingHeroList() }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
