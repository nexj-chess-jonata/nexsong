package br.com.nexj.nexsong.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.nexj.nexsong.model.Genre
import br.com.nexj.nexsong.model.Song

@Composable
fun PermissionScreen(onRequest: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text("NexSong", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Para encontrar e organizar suas músicas, o app precisa de permissão para ler os áudios do celular. Nada sai do aparelho.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRequest) { Text("Permitir acesso às músicas") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    vm: MainViewModel,
    onOpenPlayer: () -> Unit,
    onOpenEditor: () -> Unit,
    onOpenOrganize: () -> Unit,
    onOpenTrash: () -> Unit
) {
    var editing by remember { mutableStateOf<Song?>(null) }
    var selected by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var showFilters by remember { mutableStateOf(false) }
    var bulkGenre by remember { mutableStateOf(false) }
    var selecting by remember { mutableStateOf(false) }
    var mainMenu by remember { mutableStateOf(false) }
    val trashLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) vm.confirmTrash() else vm.cancelTrash()
    }
    BackHandler(enabled = selecting) {
        selected = emptySet()
        selecting = false
    }
    val sendToTrash: (List<Song>) -> Unit = { list ->
        val sender = vm.prepareTrash(list)
        if (sender != null) trashLauncher.launch(IntentSenderRequest.Builder(sender).build())
    }
    val moveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) vm.confirmMove() else vm.cancelPending()
    }
    val tabs = remember(vm.songs, vm.genres) {
        listOf<Genre?>(null) + vm.genres.filter { genre -> vm.songs.any { it.genre == genre } }
    }

    val visible = remember(
        vm.songs, vm.selectedGenre, vm.query,
        vm.formatFilter, vm.durationFilter, vm.onlyNoArtist, vm.sortBy,
        vm.lyricsFilter, vm.lyricsIds
    ) {
        vm.filtered()
    }

    Scaffold(
        topBar = {
            if (selecting) {
                TopAppBar(
                    title = { Text("${selected.size} selecionadas") },
                    navigationIcon = {
                        IconButton(onClick = {
                            selected = emptySet()
                            selecting = false
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancelar seleção")
                        }
                    },
                    actions = {
                        TextButton(onClick = { selected = visible.map { it.id }.toSet() }) { Text("Todas") }
                        TextButton(
                            enabled = selected.isNotEmpty(),
                            onClick = {
                                vm.editingIds = selected
                                selected = emptySet()
                                selecting = false
                                onOpenEditor()
                            }
                        ) { Text("Editar") }
                    }
                )
            } else {
                TopAppBar(
                    title = { Text("NexSong") },
                    actions = {
                        IconButton(onClick = { showFilters = true }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filtros e ordem")
                        }
                        FilledTonalButton(onClick = onOpenOrganize) { Text("Organizar") }
                        Box {
                            IconButton(onClick = { mainMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Mais opções")
                            }
                            DropdownMenu(expanded = mainMenu, onDismissRequest = { mainMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("Selecionar várias") },
                                    onClick = {
                                        mainMenu = false
                                        selecting = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Atualizar biblioteca") },
                                    onClick = {
                                        mainMenu = false
                                        vm.loadSongs()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Lixeira") },
                                    onClick = {
                                        mainMenu = false
                                        onOpenTrash()
                                    }
                                )
                            }
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (selecting) {
                SelectionBar(
                    count = selected.size,
                    canTrash = vm.canTrash,
                    onGenre = { bulkGenre = true },
                    onDelete = {
                        val chosen = vm.songs.filter { it.id in selected }
                        selected = emptySet()
                        selecting = false
                        sendToTrash(chosen)
                    }
                )
            } else if (vm.currentId != null) {
                MiniPlayer(vm, onOpenPlayer)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = vm.query,
                onValueChange = { vm.query = it },
                singleLine = true,
                placeholder = { Text("Buscar música, artista ou álbum") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            )

            ScrollableTabRow(
                selectedTabIndex = tabs.indexOf(vm.selectedGenre).coerceAtLeast(0),
                edgePadding = 8.dp
            ) {
                tabs.forEach { genre ->
                    val count = if (genre == null) vm.songs.size else vm.songs.count { it.genre == genre }
                    Tab(
                        selected = vm.selectedGenre == genre,
                        onClick = { vm.selectedGenre = genre },
                        text = { Text("${genre?.label ?: "Todas"} ($count)") }
                    )
                }
            }

            when {
                vm.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                visible.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nenhuma música encontrada", style = MaterialTheme.typography.bodyMedium)
                }
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    itemsIndexed(visible, key = { _, song -> song.id }) { index, song ->
                        SongRow(
                            song = song,
                            playing = song.id == vm.currentId,
                            selected = song.id in selected,
                            onClick = {
                                if (selecting) {
                                    selected = if (song.id in selected) selected - song.id else selected + song.id
                                } else {
                                    vm.play(visible, index)
                                }
                            },
                            onLongClick = {
                                selecting = true
                                selected = selected + song.id
                            },
                            canTrash = vm.canTrash,
                            onSelect = {
                                selecting = true
                                selected = selected + song.id
                            },
                            onDelete = { sendToTrash(listOf(song)) },
                            onPlayNext = { vm.playNext(song) },
                            onAddToQueue = { vm.addToQueue(song) },
                            onEdit = {
                                vm.editingIds = setOf(song.id)
                                onOpenEditor()
                            },
                            onChangeGenre = { editing = song }
                        )
                    }
                }
            }
        }
    }

    editing?.let { song ->
        GenrePickerDialog(
            song = song,
            genres = vm.genres,
            onDismiss = { editing = null },
            onPick = { genre, applyToArtist ->
                vm.setGenre(song, genre, applyToArtist)
                editing = null
            }
        )
    }

    if (bulkGenre) {
        BulkGenreDialog(
            count = selected.size,
            genres = vm.genres,
            onDismiss = { bulkGenre = false },
            onConfirm = { genre, move ->
                val chosen = vm.songs.filter { it.id in selected }
                bulkGenre = false
                selected = emptySet()
                selecting = false
                vm.setGenreForSongs(chosen, genre)
                if (move) {
                    val sender = vm.prepareMoveSongs(chosen, genre)
                    if (sender != null) {
                        moveLauncher.launch(IntentSenderRequest.Builder(sender).build())
                    }
                }
            }
        )
    }

    if (showFilters) {
        FilterDialog(vm = vm, onClose = { showFilters = false })
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SongRow(
    song: Song,
    playing: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    canTrash: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onEdit: () -> Unit,
    onChangeGenre: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (selected) Icons.Default.CheckCircle else Icons.Default.MusicNote,
            contentDescription = null,
            tint = if (playing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                song.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
                color = if (playing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Text(
                song.artist.ifBlank { "Artista desconhecido" },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(song.genre.label, style = MaterialTheme.typography.labelMedium)
            if (song.source.isNotBlank()) {
                Text(
                    if (song.confirmed) "por ${song.source}" else "sugestão",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Mais opções")
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Tocar a seguir") },
                    onClick = {
                        menuOpen = false
                        onPlayNext()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Adicionar à fila") },
                    onClick = {
                        menuOpen = false
                        onAddToQueue()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Editar informações") },
                    onClick = {
                        menuOpen = false
                        onEdit()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Mudar gênero") },
                    onClick = {
                        menuOpen = false
                        onChangeGenre()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Selecionar várias") },
                    onClick = {
                        menuOpen = false
                        onSelect()
                    }
                )
                if (canTrash) {
                    DropdownMenuItem(
                        text = { Text("Apagar (lixeira)") },
                        onClick = {
                            menuOpen = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

/** Barra de ações que aparece embaixo enquanto se selecionam várias músicas. */
@Composable
private fun SelectionBar(
    count: Int,
    canTrash: Boolean,
    onGenre: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                if (count == 0) "Toque nas músicas para marcar" else "$count músicas marcadas",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    enabled = count > 0,
                    onClick = onGenre,
                    modifier = Modifier.weight(1f)
                ) { Text("Enviar para um gênero") }
                if (canTrash) {
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(enabled = count > 0, onClick = onDelete) { Text("Apagar") }
                }
            }
        }
    }
}

@Composable
private fun MiniPlayer(vm: MainViewModel, onOpen: () -> Unit) {
    Surface(
        tonalElevation = 3.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
    ) {
        Column(Modifier.navigationBarsPadding()) {
            val progress = if (vm.durationMs > 0) {
                (vm.positionMs.toFloat() / vm.durationMs).coerceIn(0f, 1f)
            } else {
                0f
            }
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        vm.currentTitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        vm.currentArtist,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { vm.previous() }) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Anterior")
                }
                IconButton(onClick = { vm.togglePlay() }) {
                    Icon(
                        if (vm.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (vm.isPlaying) "Pausar" else "Tocar"
                    )
                }
                IconButton(onClick = { vm.next() }) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Próxima")
                }
            }
        }
    }
}
