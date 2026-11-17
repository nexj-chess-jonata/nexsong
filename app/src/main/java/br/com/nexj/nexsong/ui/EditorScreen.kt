package br.com.nexj.nexsong.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import br.com.nexj.nexsong.model.Genre

/** Mostra a capa da música (escolhida pelo usuário ou embutida no arquivo) ou um ícone. */
@Composable
fun CoverImage(
    vm: MainViewModel,
    songId: Long?,
    modifier: Modifier = Modifier,
    iconSize: Dp = 48.dp
) {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, songId, vm.coverVersion) {
        value = null
        value = if (songId == null) null else vm.loadCover(songId)?.asImageBitmap()
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        val image = bitmap
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = "Capa",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                Icons.Default.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(vm: MainViewModel, onBack: () -> Unit) {
    val ids = vm.editingIds
    val targets = remember(ids, vm.songs) { vm.songs.filter { it.id in ids } }
    val single = targets.singleOrNull()

    if (targets.isEmpty()) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    var title by remember(single?.id) { mutableStateOf(single?.title.orEmpty()) }
    var artist by remember(single?.id) { mutableStateOf(single?.artist.orEmpty()) }
    var album by remember(single?.id) { mutableStateOf(single?.album.orEmpty()) }
    var genre by remember(single?.id) { mutableStateOf<Genre?>(single?.genre) }

    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null && single != null) vm.setCover(single.id, uri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (single != null) "Editar música" else "Editar ${targets.size} músicas")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        if (single != null) {
                            vm.saveEdits(
                                ids = ids,
                                title = title.trim().ifBlank { null },
                                artist = artist.trim(),
                                album = album.trim(),
                                genre = genre.takeIf { it != single.genre }
                            )
                        } else {
                            vm.saveEdits(
                                ids = ids,
                                title = null,
                                artist = artist.trim().ifBlank { null },
                                album = album.trim().ifBlank { null },
                                genre = genre
                            )
                        }
                        onBack()
                    }) { Text("Salvar") }
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
            if (single != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(120.dp)
                    ) {
                        CoverImage(vm, single.id, Modifier.fillMaxSize())
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        OutlinedButton(onClick = { coverPicker.launch("image/*") }) {
                            Text("Trocar capa")
                        }
                        if (vm.hasCustomCover(single.id)) {
                            TextButton(onClick = { vm.removeCover(single.id) }) {
                                Text("Remover capa escolhida")
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
            } else {
                Text(
                    "Preencha só o que quer mudar. Campos vazios ficam como estão em cada música.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(12.dp))
            }

            OutlinedTextField(
                value = artist,
                onValueChange = { artist = it },
                label = { Text("Artista") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = album,
                onValueChange = { album = it },
                label = { Text("Álbum") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
            Text("Gênero", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            val options: List<Genre?> =
                if (single != null) vm.genres else listOf<Genre?>(null) + vm.genres
            LazyRow {
                items(options) { option ->
                    val text = option?.label ?: "Manter"
                    if (option == genre) {
                        Button(onClick = { genre = option }) { Text(text) }
                    } else {
                        OutlinedButton(onClick = { genre = option }) { Text(text) }
                    }
                    Spacer(Modifier.width(8.dp))
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "As alterações ficam salvas no NexSong. O arquivo de música não é modificado.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (single != null && vm.hasEdits(single.id)) {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = {
                    vm.resetEdits(single.id)
                    onBack()
                }) { Text("Restaurar informações originais") }
            }
        }
    }
}
