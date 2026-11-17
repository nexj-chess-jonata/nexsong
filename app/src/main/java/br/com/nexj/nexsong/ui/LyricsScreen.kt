package br.com.nexj.nexsong.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.nexj.nexsong.data.ParsedLyrics
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsScreen(vm: MainViewModel, onBack: () -> Unit) {
    var editing by rememberSaveable { mutableStateOf(false) }
    var showBulk by remember { mutableStateOf(false) }

    if (editing) {
        LyricsEditor(vm, onClose = { editing = false })
        return
    }

    val status = vm.lyricsStatus
    val parsed = vm.lyrics

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Letra")
                        Text(
                            vm.currentTitle,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    if (status == MainViewModel.LyricsStatus.READY) {
                        TextButton(onClick = { editing = true }) { Text("Editar") }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when {
                    status == MainViewModel.LyricsStatus.READY && parsed != null && parsed.synced ->
                        SyncedLyrics(vm, parsed)

                    status == MainViewModel.LyricsStatus.READY && parsed != null ->
                        PlainLyrics(parsed)

                    status == MainViewModel.LyricsStatus.LOADING ->
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(12.dp))
                            Text("Buscando letra…")
                        }

                    else ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                when (status) {
                                    MainViewModel.LyricsStatus.NOT_FOUND ->
                                        "Não encontrei a letra desta música na internet."
                                    MainViewModel.LyricsStatus.ERROR ->
                                        "Não foi possível buscar a letra. Verifique a conexão."
                                    else -> "Esta música ainda não tem letra salva."
                                },
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(
                                enabled = vm.currentId != null,
                                onClick = { vm.searchLyricsNow() }
                            ) { Text("Buscar na internet") }
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                enabled = vm.currentId != null,
                                onClick = { editing = true }
                            ) { Text("Escrever a letra") }
                        }
                }
            }

            if (status == MainViewModel.LyricsStatus.READY && parsed != null && parsed.synced) {
                OffsetRow(vm)
            }

            HorizontalDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { vm.updateAutoLyrics(!vm.autoLyrics) }
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = vm.autoLyrics, onCheckedChange = { vm.updateAutoLyrics(it) })
                Text("Buscar a letra automaticamente ao tocar", style = MaterialTheme.typography.bodyMedium)
            }
            TextButton(
                onClick = { showBulk = true },
                modifier = Modifier.padding(horizontal = 8.dp)
            ) { Text("Baixar letras de toda a biblioteca") }
        }
    }

    if (showBulk) {
        BulkLyricsDialog(vm, onClose = { showBulk = false })
    }
}

@Composable
private fun SyncedLyrics(vm: MainViewModel, parsed: ParsedLyrics) {
    val position = vm.positionMs
    val offset = vm.lyricsOffsetMs
    val index = remember(position, parsed, offset) {
        parsed.lines.indexOfLast { it.timeMs + offset <= position }
    }
    val listState = rememberLazyListState()

    LaunchedEffect(index) {
        if (index >= 0) listState.animateScrollToItem(maxOf(0, index - 3))
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp)
    ) {
        itemsIndexed(parsed.lines) { i, line ->
            val current = i == index
            Text(
                text = line.text.ifBlank { "♪" },
                style = if (current) {
                    MaterialTheme.typography.titleLarge
                } else {
                    MaterialTheme.typography.bodyLarge
                },
                color = if (current) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { vm.seekTo(maxOf(0L, line.timeMs + offset)) }
                    .padding(vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun PlainLyrics(parsed: ParsedLyrics) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Text(
            "Letra sem marcação de tempo: role para acompanhar.",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        Text(
            parsed.lines.joinToString("\n") { it.text },
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun OffsetRow(vm: MainViewModel) {
    val tenths = vm.lyricsOffsetMs / 100
    val absolute = abs(tenths)
    val sign = when {
        tenths > 0 -> "+"
        tenths < 0 -> "−"
        else -> ""
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Ajuste da letra: $sign${absolute / 10},${absolute % 10} s",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = { vm.nudgeLyricsOffset(-500L) }) { Text("Adiantar") }
        Spacer(Modifier.width(4.dp))
        TextButton(onClick = { vm.nudgeLyricsOffset(500L) }) { Text("Atrasar") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LyricsEditor(vm: MainViewModel, onClose: () -> Unit) {
    var text by rememberSaveable { mutableStateOf(vm.lyricsRaw) }
    val hadLyrics = vm.lyricsRaw.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editar letra") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        vm.saveLyricsText(text)
                        onClose()
                    }) { Text("Salvar") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                "Para a letra acompanhar a música, comece cada linha com o tempo, por exemplo [01:23.45]. Sem os tempos, ela aparece como texto simples.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
            if (hadLyrics) {
                TextButton(onClick = {
                    vm.deleteLyrics()
                    onClose()
                }) { Text("Apagar a letra desta música") }
            }
        }
    }
}

@Composable
private fun BulkLyricsDialog(vm: MainViewModel, onClose: () -> Unit) {
    val state = vm.lyricsBulk
    var wifiOnly by remember { mutableStateOf(true) }
    val running = state != null && !state.finished

    AlertDialog(
        onDismissRequest = { if (!running) onClose() },
        title = { Text("Baixar letras") },
        text = {
            Column {
                if (state == null) {
                    Text("Busca a letra de todas as músicas que ainda não têm e guarda no aparelho para uso sem internet. Mantenha o app aberto até terminar.")
                    Spacer(Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { wifiOnly = !wifiOnly }
                    ) {
                        Checkbox(checked = wifiOnly, onCheckedChange = { wifiOnly = it })
                        Text("Somente no Wi-Fi")
                    }
                } else if (!state.finished) {
                    val fraction = if (state.total > 0) state.done.toFloat() / state.total else 0f
                    LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Text("${state.done} de ${state.total}")
                } else {
                    Text(state.message)
                }
            }
        },
        confirmButton = {
            when {
                state == null -> TextButton(onClick = { vm.downloadAllLyrics(wifiOnly) }) { Text("Começar") }
                state.finished -> TextButton(onClick = {
                    vm.clearBulkLyrics()
                    onClose()
                }) { Text("Fechar") }
                else -> TextButton(onClick = { vm.cancelBulkLyrics() }) { Text("Parar") }
            }
        },
        dismissButton = {
            if (state == null) TextButton(onClick = onClose) { Text("Cancelar") }
        }
    )
}
