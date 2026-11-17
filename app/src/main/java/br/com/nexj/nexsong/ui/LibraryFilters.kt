package br.com.nexj.nexsong.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.nexj.nexsong.model.DurationFilter
import br.com.nexj.nexsong.model.FormatFilter
import br.com.nexj.nexsong.model.LyricsFilter
import br.com.nexj.nexsong.model.SortBy

@Composable
fun FilterDialog(vm: MainViewModel, onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Filtros e ordem") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OptionGroup(
                    title = "Ordenar por",
                    options = SortBy.values().toList(),
                    selected = vm.sortBy,
                    label = { it.label },
                    onSelect = { vm.sortBy = it }
                )
                OptionGroup(
                    title = "Formato",
                    options = FormatFilter.values().toList(),
                    selected = vm.formatFilter,
                    label = { it.label },
                    onSelect = { vm.formatFilter = it }
                )
                OptionGroup(
                    title = "Duração",
                    options = DurationFilter.values().toList(),
                    selected = vm.durationFilter,
                    label = { it.label },
                    onSelect = { vm.durationFilter = it }
                )
                OptionGroup(
                    title = "Letra",
                    options = LyricsFilter.values().toList(),
                    selected = vm.lyricsFilter,
                    label = { it.label },
                    onSelect = { vm.lyricsFilter = it }
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { vm.onlyNoArtist = !vm.onlyNoArtist },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = vm.onlyNoArtist, onCheckedChange = { vm.onlyNoArtist = it })
                    Text("Somente músicas sem artista")
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("Fechar") } },
        dismissButton = {
            TextButton(onClick = {
                vm.sortBy = SortBy.TITLE
                vm.formatFilter = FormatFilter.ALL
                vm.durationFilter = DurationFilter.ALL
                vm.lyricsFilter = LyricsFilter.ALL
                vm.onlyNoArtist = false
            }) { Text("Limpar") }
        }
    )
}

@Composable
private fun <T> OptionGroup(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit
) {
    Text(title, style = MaterialTheme.typography.labelLarge)
    options.forEach { option ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelect(option) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = option == selected, onClick = { onSelect(option) })
            Text(label(option))
        }
    }
    Spacer(Modifier.height(12.dp))
}
