package br.com.nexj.nexsong.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import br.com.nexj.nexsong.player.EqualizerManager
import kotlinx.coroutines.delay

enum class Screen { LIBRARY, PLAYER, QUEUE, EQUALIZER, LYRICS, EDITOR, ORGANIZE, SUGGESTIONS, GENRES, DUPLICATES, TRASH }

fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

/** Raiz da navegação: biblioteca, player, fila e equalizador. */
@Composable
fun NexSongRoot(vm: MainViewModel) {
    var screen by rememberSaveable { mutableStateOf(Screen.LIBRARY) }

    LaunchedEffect(vm.isPlaying) {
        while (vm.isPlaying) {
            vm.refreshPosition()
            delay(250)
        }
    }

    BackHandler(enabled = screen != Screen.LIBRARY) {
        screen = when (screen) {
            Screen.QUEUE, Screen.EQUALIZER, Screen.LYRICS -> Screen.PLAYER
            Screen.SUGGESTIONS, Screen.GENRES, Screen.DUPLICATES -> Screen.ORGANIZE
            else -> Screen.LIBRARY
        }
    }

    Crossfade(targetState = screen, label = "tela") { current ->
    when (current) {
        Screen.LIBRARY -> LibraryScreen(
            vm,
            onOpenPlayer = { screen = Screen.PLAYER },
            onOpenEditor = { screen = Screen.EDITOR },
            onOpenOrganize = { screen = Screen.ORGANIZE },
            onOpenTrash = { screen = Screen.TRASH }
        )
        Screen.TRASH -> TrashScreen(vm, onBack = { screen = Screen.LIBRARY })
        Screen.ORGANIZE -> OrganizeScreen(
            vm = vm,
            onBack = {
                vm.clearOrganize()
                screen = Screen.LIBRARY
            },
            onSuggestions = { screen = Screen.SUGGESTIONS },
            onGenres = { screen = Screen.GENRES },
            onDuplicates = { screen = Screen.DUPLICATES }
        )
        Screen.SUGGESTIONS -> SuggestionsScreen(vm, onBack = { screen = Screen.ORGANIZE })
        Screen.GENRES -> GenresScreen(vm, onBack = { screen = Screen.ORGANIZE })
        Screen.DUPLICATES -> DuplicatesScreen(vm, onBack = { screen = Screen.ORGANIZE })
        Screen.EDITOR -> EditorScreen(vm, onBack = { screen = Screen.LIBRARY })
        Screen.PLAYER -> PlayerScreen(
            vm = vm,
            onBack = { screen = Screen.LIBRARY },
            onQueue = { screen = Screen.QUEUE },
            onEqualizer = { screen = Screen.EQUALIZER },
            onLyrics = { screen = Screen.LYRICS }
        )
        Screen.LYRICS -> LyricsScreen(vm, onBack = { screen = Screen.PLAYER })
        Screen.QUEUE -> QueueScreen(vm, onBack = { screen = Screen.PLAYER })
        Screen.EQUALIZER -> EqualizerScreen(onBack = { screen = Screen.PLAYER })
    }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    vm: MainViewModel,
    onBack: () -> Unit,
    onQueue: () -> Unit,
    onEqualizer: () -> Unit,
    onLyrics: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tocando agora") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    TextButton(onClick = onLyrics) { Text("Letra") }
                    IconButton(onClick = onEqualizer) {
                        Icon(Icons.Default.Equalizer, contentDescription = "Equalizador")
                    }
                    IconButton(onClick = onQueue) {
                        Icon(Icons.Default.QueueMusic, contentDescription = "Fila")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(240.dp)
            ) {
                CoverImage(vm, vm.currentId, Modifier.fillMaxSize(), 96.dp)
            }

            Spacer(Modifier.height(32.dp))

            Text(
                vm.currentTitle.ifBlank { "Nenhuma música" },
                style = MaterialTheme.typography.titleLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                vm.currentArtist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(24.dp))

            var dragging by remember { mutableStateOf<Float?>(null) }
            val progress = if (vm.durationMs > 0) {
                (vm.positionMs.toFloat() / vm.durationMs).coerceIn(0f, 1f)
            } else {
                0f
            }
            val shown = dragging ?: progress
            Slider(
                value = shown,
                onValueChange = { dragging = it },
                onValueChangeFinished = {
                    dragging?.let { vm.seekTo((it * vm.durationMs).toLong()) }
                    dragging = null
                },
                modifier = Modifier.fillMaxWidth()
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    formatTime((shown * vm.durationMs).toLong()),
                    style = MaterialTheme.typography.labelMedium
                )
                Text(formatTime(vm.durationMs), style = MaterialTheme.typography.labelMedium)
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { vm.toggleShuffle() }) {
                    Icon(
                        Icons.Default.Shuffle,
                        contentDescription = if (vm.shuffle) "Aleatório ligado" else "Aleatório desligado",
                        tint = if (vm.shuffle) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
                IconButton(onClick = { vm.previous() }) {
                    Icon(
                        Icons.Default.SkipPrevious,
                        contentDescription = "Anterior",
                        modifier = Modifier.size(36.dp)
                    )
                }
                FilledIconButton(onClick = { vm.togglePlay() }, modifier = Modifier.size(64.dp)) {
                    Icon(
                        if (vm.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (vm.isPlaying) "Pausar" else "Tocar",
                        modifier = Modifier.size(36.dp)
                    )
                }
                IconButton(onClick = { vm.next() }) {
                    Icon(
                        Icons.Default.SkipNext,
                        contentDescription = "Próxima",
                        modifier = Modifier.size(36.dp)
                    )
                }
                IconButton(onClick = { vm.cycleRepeat() }) {
                    val description = when (vm.repeatMode) {
                        Player.REPEAT_MODE_ONE -> "Repetir uma"
                        Player.REPEAT_MODE_ALL -> "Repetir todas"
                        else -> "Repetir desligado"
                    }
                    Icon(
                        if (vm.repeatMode == Player.REPEAT_MODE_ONE) {
                            Icons.Default.RepeatOne
                        } else {
                            Icons.Default.Repeat
                        },
                        contentDescription = description,
                        tint = if (vm.repeatMode != Player.REPEAT_MODE_OFF) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(vm: MainViewModel, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fila (${vm.queue.size})") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        if (vm.queue.isEmpty()) {
            Box(
                Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("A fila está vazia")
            }
        } else {
            LazyColumn(
                Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                itemsIndexed(vm.queue) { index, entry ->
                    val current = index == vm.queueIndex
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { vm.jumpTo(index) }
                            .padding(start = 16.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                entry.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (current) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                            Text(
                                entry.artist,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { vm.moveQueueItem(index, index - 1) },
                            enabled = index > 0
                        ) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Subir")
                        }
                        IconButton(
                            onClick = { vm.moveQueueItem(index, index + 1) },
                            enabled = index < vm.queue.size - 1
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Descer")
                        }
                        IconButton(onClick = { vm.removeFromQueue(index) }) {
                            Icon(Icons.Default.Close, contentDescription = "Remover da fila")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(onBack: () -> Unit) {
    var enabled by remember { mutableStateOf(EqualizerManager.isEnabled()) }
    var levels by remember { mutableStateOf(EqualizerManager.levels()) }
    val presets = remember { EqualizerManager.presets() }
    val minLevel = remember { EqualizerManager.minLevel() }
    val maxLevel = remember { EqualizerManager.maxLevel() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Equalizador") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        if (!EqualizerManager.isAvailable() || levels.isEmpty()) {
            Box(
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Este aparelho não oferece equalizador para o app.")
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Ativar equalizador",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Switch(
                        checked = enabled,
                        onCheckedChange = {
                            enabled = it
                            EqualizerManager.setEnabled(it)
                        }
                    )
                }

                if (presets.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text("Predefinições", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(4.dp))
                    LazyRow {
                        itemsIndexed(presets) { index, name ->
                            OutlinedButton(
                                onClick = {
                                    EqualizerManager.usePreset(index)
                                    levels = EqualizerManager.levels()
                                }
                            ) {
                                Text(name)
                            }
                            Spacer(Modifier.width(8.dp))
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                levels.forEachIndexed { band, level ->
                    val hz = EqualizerManager.centerFreqHz(band)
                    val freqLabel = if (hz >= 1000) "${hz / 1000} kHz" else "$hz Hz"
                    val db = level / 100
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(freqLabel, style = MaterialTheme.typography.labelLarge)
                        Text(
                            if (db > 0) "+$db dB" else "$db dB",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Slider(
                        value = level.toFloat(),
                        onValueChange = { value ->
                            val newLevel = value.toInt()
                            EqualizerManager.setBandLevel(band, newLevel)
                            levels = levels.toMutableList().also { it[band] = newLevel }
                        },
                        valueRange = minLevel.toFloat()..maxLevel.toFloat(),
                        enabled = enabled
                    )
                }
            }
        }
    }
}
