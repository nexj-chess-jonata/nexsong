package br.com.nexj.nexsong.ui

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Lixeira: músicas apagadas pelo NexSong. O Android guarda por 30 dias antes de apagar de vez. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(vm: MainViewModel, onBack: () -> Unit) {
    val trashed = vm.trashItems
    val now = remember(trashed) { System.currentTimeMillis() }
    var purge by remember { mutableStateOf<List<Long>?>(null) }

    LaunchedEffect(Unit) { vm.refreshTrash() }

    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) vm.confirmRestore() else vm.cancelTrash()
    }
    val purgeLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) vm.confirmPurge() else vm.cancelTrash()
    }
    val restore: (List<Long>) -> Unit = { ids ->
        val sender = vm.prepareRestore(ids)
        if (sender != null) restoreLauncher.launch(IntentSenderRequest.Builder(sender).build())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lixeira (${trashed.size})") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    if (trashed.isNotEmpty()) {
                        TextButton(onClick = { restore(trashed.map { it.id }) }) { Text("Restaurar tudo") }
                        TextButton(onClick = { purge = trashed.map { it.id } }) { Text("Esvaziar") }
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
            item {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        if (vm.canTrash) {
                            "As músicas apagadas ficam aqui por 30 dias. Depois disso o Android apaga de vez."
                        } else {
                            "A lixeira precisa do Android 11 ou superior."
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (vm.trashMessage.isNotEmpty()) {
                        Text(vm.trashMessage, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            if (trashed.isEmpty()) {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("A lixeira está vazia")
                    }
                }
            }
            items(trashed, key = { it.id }) { entry ->
                Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp)) {
                    Text(
                        entry.title.ifBlank { "Música sem título" },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        entry.artist.ifBlank { "Artista desconhecido" } +
                            " · apagada de vez em ${entry.daysLeft(now)} dias",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row {
                        TextButton(onClick = { restore(listOf(entry.id)) }) { Text("Restaurar") }
                        TextButton(onClick = { purge = listOf(entry.id) }) { Text("Apagar de vez") }
                        TextButton(onClick = { vm.forgetTrashItems(listOf(entry.id)) }) { Text("Tirar da lista") }
                    }
                    HorizontalDivider()
                }
            }
        }
    }

    purge?.let { ids ->
        AlertDialog(
            onDismissRequest = { purge = null },
            title = { Text("Apagar de vez ${ids.size} música(s)?") },
            text = { Text("Isso não pode ser desfeito. O Android vai pedir mais uma confirmação.") },
            confirmButton = {
                TextButton(onClick = {
                    purge = null
                    val sender = vm.preparePurge(ids)
                    if (sender != null) purgeLauncher.launch(IntentSenderRequest.Builder(sender).build())
                }) { Text("Apagar de vez") }
            },
            dismissButton = { TextButton(onClick = { purge = null }) { Text("Cancelar") } }
        )
    }
}
