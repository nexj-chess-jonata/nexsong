package br.com.nexj.nexsong.ui

import android.app.Activity
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.nexj.nexsong.model.Genre
import br.com.nexj.nexsong.model.Song

private const val MODE_VIRTUAL = 0
private const val MODE_COPY = 1
private const val MODE_MOVE = 2

/** Escolha de gênero para uma música, com opção de aplicar ao artista inteiro. */
@Composable
fun GenrePickerDialog(
    song: Song,
    genres: List<Genre>,
    onDismiss: () -> Unit,
    onPick: (Genre, Boolean) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Gênero de \"${song.title}\"", maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                var applyToArtist by remember { mutableStateOf(song.artist.isNotBlank()) }
                if (song.artist.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { applyToArtist = !applyToArtist }
                    ) {
                        Checkbox(checked = applyToArtist, onCheckedChange = { applyToArtist = it })
                        Text("Aplicar a todas de ${song.artist}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                genres.forEach { genre ->
                    TextButton(onClick = { onPick(genre, applyToArtist) }) { Text(genre.label) }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrganizeScreen(
    vm: MainViewModel,
    onBack: () -> Unit,
    onSuggestions: () -> Unit,
    onGenres: () -> Unit,
    onDuplicates: () -> Unit
) {
    val state = vm.organizeState
    val running = state != null && !state.finished
    val canMove = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
    var mode by rememberSaveable { mutableStateOf(MODE_VIRTUAL) }

    val ready = remember(vm.songs) { vm.organizeTargets() }
    val pending = remember(vm.songs) { vm.songs.count { !it.confirmed } }
    val others = remember(vm.songs) { vm.songs.count { it.genre == Genre.OUTROS } }

    val moveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) vm.confirmMove() else vm.cancelPending()
    }
    val undoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) vm.confirmUndo() else vm.cancelPending()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Organizar Agora") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("1. Resultado da análise", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            vm.genres.forEach { genre ->
                if (genre != Genre.OUTROS) {
                    val count = ready.count { it.genre == genre }
                    if (count > 0) Text("${genre.label}: $count músicas")
                }
            }
            if (ready.isEmpty()) Text("Nenhuma música classificada ainda.")
            Text(
                "Sem gênero (Outros): $others. Essas ficam onde estão.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onGenres) { Text("Gerenciar gêneros") }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            Text("2. Sugestões para revisar", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            if (pending > 0) {
                Text("$pending músicas têm um gênero sugerido e aguardam sua confirmação. Elas só vão para as pastas depois de confirmadas.")
                Spacer(Modifier.height(8.dp))
                Button(onClick = onSuggestions) { Text("Revisar sugestões") }
            } else {
                Text("Nenhuma sugestão pendente.")
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            Text("3. Duplicatas e aprendizado", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            if (vm.duplicateGroups.isNotEmpty()) {
                Text("${vm.duplicateGroups.size} grupos de músicas repetidas para você revisar.")
                Spacer(Modifier.height(8.dp))
                Button(onClick = onDuplicates) { Text("Revisar duplicatas") }
            } else {
                Text("Nenhuma duplicata encontrada.")
                if (vm.hiddenCount > 0) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = onDuplicates) { Text("Ver músicas ocultadas") }
                }
            }
            Spacer(Modifier.height(12.dp))
            if (vm.learnedCount > 0) {
                Text(
                    "O NexSong aprendeu com as suas correções (${vm.learnedCount} grupos de artista, álbum ou pasta) " +
                        "e usa isso para sugerir o gênero de outras músicas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = { vm.forgetLearning() }) { Text("Esquecer o que aprendeu") }
            } else {
                Text(
                    "Quando você corrigir o gênero de algumas músicas, o NexSong aprende e sugere o mesmo para as parecidas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            Text("4. Aplicar", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            ModeOption(
                selected = mode == MODE_VIRTUAL,
                title = "Só no app (virtual)",
                description = "Não mexe nos arquivos. As músicas já aparecem separadas por gênero nas abas.",
                onClick = { mode = MODE_VIRTUAL }
            )
            ModeOption(
                selected = mode == MODE_COPY,
                title = "Copiar para pastas",
                description = "Cria cópias em Music/NexSong Cópias/<gênero>. Os originais ficam intactos, mas ocupa espaço em dobro.",
                onClick = { mode = MODE_COPY }
            )
            ModeOption(
                selected = mode == MODE_MOVE,
                title = "Mover para pastas",
                description = if (canMove) {
                    "Move os arquivos para Music/NexSong/<gênero>. O Android pede sua confirmação, e dá para desfazer."
                } else {
                    "Disponível a partir do Android 11."
                },
                enabled = canMove,
                onClick = { mode = MODE_MOVE }
            )

            Spacer(Modifier.height(8.dp))
            Button(
                enabled = !running && mode != MODE_VIRTUAL && ready.isNotEmpty(),
                onClick = {
                    if (mode == MODE_COPY) {
                        vm.copyToFolders()
                    } else if (mode == MODE_MOVE) {
                        val sender = vm.prepareMove()
                        if (sender != null) {
                            moveLauncher.launch(IntentSenderRequest.Builder(sender).build())
                        }
                    }
                }
            ) { Text("Aplicar") }

            if (state != null) {
                Spacer(Modifier.height(12.dp))
                if (!state.finished) {
                    val fraction = if (state.total > 0) state.done.toFloat() / state.total else 0f
                    LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(4.dp))
                    Text("${state.done} de ${state.total}")
                } else {
                    Text(state.message)
                }
            }

            if (vm.undoCount > 0 && canMove) {
                Spacer(Modifier.height(16.dp))
                OutlinedButton(
                    enabled = !running,
                    onClick = {
                        val sender = vm.prepareUndo()
                        if (sender != null) {
                            undoLauncher.launch(IntentSenderRequest.Builder(sender).build())
                        }
                    }
                ) { Text("Desfazer movimentações (${vm.undoCount} músicas)") }
            }
        }
    }
}

@Composable
private fun ModeOption(
    selected: Boolean,
    title: String,
    description: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick, enabled = enabled)
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuggestionsScreen(vm: MainViewModel, onBack: () -> Unit) {
    val pending = remember(vm.songs) { vm.songs.filter { !it.confirmed } }
    var changing by remember { mutableStateOf<Song?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sugestões (${pending.size})") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    if (pending.isNotEmpty()) {
                        TextButton(onClick = { vm.confirmAllSuggestions() }) { Text("Confirmar todas") }
                    }
                }
            )
        }
    ) { padding ->
        if (pending.isEmpty()) {
            Box(
                Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Nenhuma sugestão pendente")
            }
        } else {
            LazyColumn(
                Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                items(pending, key = { it.id }) { song ->
                    Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp)) {
                        Text(
                            song.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            song.artist.ifBlank { "Artista desconhecido" },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Sugestão: ${song.genre.label} (por ${song.source})",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row {
                            TextButton(onClick = { vm.setGenre(song, song.genre, false) }) {
                                Text("Confirmar")
                            }
                            TextButton(onClick = { changing = song }) { Text("Trocar") }
                            TextButton(onClick = { vm.setGenre(song, Genre.OUTROS, false) }) {
                                Text("Recusar")
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    changing?.let { song ->
        GenrePickerDialog(
            song = song,
            genres = vm.genres,
            onDismiss = { changing = null },
            onPick = { genre, applyToArtist ->
                vm.setGenre(song, genre, applyToArtist)
                changing = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenresScreen(vm: MainViewModel, onBack: () -> Unit) {
    var adding by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<Genre?>(null) }
    var deleting by remember { mutableStateOf<Genre?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gêneros") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { adding = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Novo gênero")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            items(vm.genres, key = { it.id }) { genre ->
                val count = vm.songs.count { it.genre == genre }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(genre.label, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "$count músicas",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (genre != Genre.OUTROS) {
                        IconButton(onClick = { renaming = genre }) {
                            Icon(Icons.Default.Edit, contentDescription = "Renomear")
                        }
                        IconButton(onClick = { deleting = genre }) {
                            Icon(Icons.Default.Delete, contentDescription = "Apagar")
                        }
                    }
                }
            }
        }
    }

    if (adding) {
        GenreNameDialog(
            title = "Novo gênero",
            initial = "",
            onDismiss = { adding = false },
            onConfirm = { name ->
                val ok = vm.addGenre(name)
                if (ok) adding = false
                ok
            }
        )
    }

    renaming?.let { genre ->
        GenreNameDialog(
            title = "Renomear gênero",
            initial = genre.label,
            onDismiss = { renaming = null },
            onConfirm = { name ->
                val ok = vm.renameGenre(genre, name)
                if (ok) renaming = null
                ok
            }
        )
    }

    deleting?.let { genre ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Apagar \"${genre.label}\"?") },
            text = {
                Text("As músicas não são apagadas. Elas serão classificadas de novo e podem ir para outro gênero ou para Outros.")
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteGenre(genre)
                    deleting = null
                }) { Text("Apagar") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun GenreNameDialog(
    title: String,
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Boolean
) {
    var name by remember { mutableStateOf(initial) }
    var error by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        error = false
                    },
                    label = { Text("Nome") },
                    singleLine = true,
                    isError = error
                )
                if (error) {
                    Text(
                        "Nome vazio ou já usado por outro gênero.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (!onConfirm(name)) error = true }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
