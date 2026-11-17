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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.nexj.nexsong.classify.DuplicateFinder
import br.com.nexj.nexsong.model.FormatFilter
import br.com.nexj.nexsong.model.Song

private class DuplicateAction(val others: List<Song>, val keep: Song, val delete: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuplicatesScreen(vm: MainViewModel, onBack: () -> Unit) {
    val groups = vm.duplicateGroups
    val keepChoice = remember { mutableStateMapOf<String, Long>() }
    var action by remember { mutableStateOf<DuplicateAction?>(null) }
    val canDelete = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) vm.confirmDelete() else vm.cancelDelete()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Duplicatas (${groups.size})") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "Músicas com o mesmo título e artista e duração parecida. " +
                        "Nada é apagado sem a sua confirmação.",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (vm.duplicateMessage.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(vm.duplicateMessage, color = MaterialTheme.colorScheme.primary)
                }
            }

            if (groups.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                        Text("Nenhuma duplicata encontrada")
                    }
                }
            }

            items(groups, key = { it.key }) { group ->
                val suggested = vm.suggestedKeep(group)
                val keepId = keepChoice[group.key] ?: suggested.id
                val keepSong = group.songs.firstOrNull { it.id == keepId } ?: suggested
                val others = group.songs.filter { it.id != keepSong.id }
                val first = group.songs.first()

                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            first.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            first.artist.ifBlank { "Artista desconhecido" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Text("Escolha qual manter:", style = MaterialTheme.typography.labelMedium)

                        group.songs.forEach { song ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { keepChoice[group.key] = song.id }
                            ) {
                                RadioButton(
                                    selected = song.id == keepSong.id,
                                    onClick = { keepChoice[group.key] = song.id }
                                )
                                Column {
                                    Text(
                                        song.displayName + if (song.id == suggested.id) "  (sugerida)" else "",
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        "${FormatFilter.of(song).label} · ${formatTime(song.durationMs)}" +
                                            if (song.album.isNotBlank()) " · ${song.album}" else "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        song.relativePath.ifBlank { "pasta desconhecida" },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        val identical = vm.contentCheck[group.key]
                        if (vm.checkingGroup == group.key) {
                            Text("Comparando os arquivos...", style = MaterialTheme.typography.bodySmall)
                        } else if (identical == true) {
                            Text(
                                "Arquivos idênticos (mesmo conteúdo).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else if (identical == false) {
                            Text(
                                "Arquivos diferentes: podem ser versões ou qualidades distintas.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }

                        Row {
                            TextButton(
                                enabled = vm.checkingGroup == null,
                                onClick = { vm.checkContent(group) }
                            ) { Text("Comparar conteúdo") }
                            TextButton(onClick = { vm.ignoreGroup(group) }) { Text("Não são duplicatas") }
                        }
                        Row {
                            TextButton(onClick = { action = DuplicateAction(others, keepSong, false) }) {
                                Text("Ocultar as outras")
                            }
                            if (canDelete) {
                                TextButton(onClick = { action = DuplicateAction(others, keepSong, true) }) {
                                    Text("Apagar as outras")
                                }
                            }
                        }
                    }
                }
            }

            item {
                Column {
                    if (vm.hiddenCount > 0) {
                        TextButton(onClick = { vm.showHiddenSongs() }) {
                            Text("Mostrar ${vm.hiddenCount} músicas ocultadas")
                        }
                    }
                    TextButton(onClick = { vm.restoreIgnoredGroups() }) {
                        Text("Rever grupos marcados como \"não são duplicatas\"")
                    }
                    if (!canDelete) {
                        Text(
                            "No Android 10 o app só consegue ocultar. Para apagar, use o app Arquivos.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    action?.let { pending ->
        val count = pending.others.size
        AlertDialog(
            onDismissRequest = { action = null },
            title = { Text(if (pending.delete) "Apagar $count música(s)?" else "Ocultar $count música(s)?") },
            text = {
                Text(
                    if (pending.delete) {
                        "Fica: ${pending.keep.displayName}\n\n" +
                            "As outras serão apagadas do aparelho e isso não pode ser desfeito. " +
                            "O Android vai pedir mais uma confirmação."
                    } else {
                        "Fica: ${pending.keep.displayName}\n\n" +
                            "As outras somem da biblioteca do NexSong, mas os arquivos continuam no aparelho."
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val ids = pending.others.map { it.id }
                    action = null
                    if (pending.delete) {
                        val sender = vm.prepareDelete(ids)
                        if (sender != null) {
                            deleteLauncher.launch(IntentSenderRequest.Builder(sender).build())
                        }
                    } else {
                        vm.hideSongs(ids)
                    }
                }) { Text(if (pending.delete) "Apagar" else "Ocultar") }
            },
            dismissButton = { TextButton(onClick = { action = null }) { Text("Cancelar") } }
        )
    }
}
