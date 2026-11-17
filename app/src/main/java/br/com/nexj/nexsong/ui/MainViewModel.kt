package br.com.nexj.nexsong.ui

import android.app.Application
import android.content.ComponentName
import android.content.IntentSender
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import br.com.nexj.nexsong.classify.DuplicateFinder
import br.com.nexj.nexsong.classify.GenreClassifier
import br.com.nexj.nexsong.data.CoverStore
import br.com.nexj.nexsong.data.DuplicateStore
import br.com.nexj.nexsong.data.Edits
import br.com.nexj.nexsong.data.FolderOrganizer
import br.com.nexj.nexsong.data.GenreStore
import br.com.nexj.nexsong.data.Learned
import br.com.nexj.nexsong.data.LrcParser
import br.com.nexj.nexsong.data.LyricsClient
import br.com.nexj.nexsong.data.LyricsStore
import br.com.nexj.nexsong.data.MediaScanner
import br.com.nexj.nexsong.data.MoveLog
import br.com.nexj.nexsong.data.Overrides
import br.com.nexj.nexsong.data.ParsedLyrics
import br.com.nexj.nexsong.model.DurationFilter
import br.com.nexj.nexsong.model.FormatFilter
import br.com.nexj.nexsong.model.Genre
import br.com.nexj.nexsong.model.LyricsFilter
import br.com.nexj.nexsong.model.Song
import br.com.nexj.nexsong.model.SortBy
import br.com.nexj.nexsong.player.PlaybackService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(app: Application) : AndroidViewModel(app) {

    data class OrganizeState(
        val done: Int,
        val total: Int,
        val finished: Boolean,
        val message: String
    )

    data class QueueEntry(val title: String, val artist: String)

    enum class LyricsStatus { NONE, LOADING, READY, NOT_FOUND, ERROR }

    private val scanner = MediaScanner(app)
    private val organizer = FolderOrganizer(app)
    private val overrides = Overrides(app)
    private val edits = Edits(app)
    private val covers = CoverStore(app)
    private val genreStore = GenreStore(app)
    private val moveLog = MoveLog(app)
    private val lyricsStore = LyricsStore(app)
    private val lyricsClient = LyricsClient()
    private val learned = Learned(app)
    private val duplicateStore = DuplicateStore(app)

    // ---- Biblioteca ----
    var songs by mutableStateOf<List<Song>>(emptyList())
        private set
    var loading by mutableStateOf(false)
        private set
    var genres by mutableStateOf(genreStore.all())
        private set
    var selectedGenre by mutableStateOf<Genre?>(null)
    var query by mutableStateOf("")
    var formatFilter by mutableStateOf(FormatFilter.ALL)
    var durationFilter by mutableStateOf(DurationFilter.ALL)
    var onlyNoArtist by mutableStateOf(false)
    var sortBy by mutableStateOf(SortBy.TITLE)

    // ---- Edição ----
    var editingIds by mutableStateOf<Set<Long>>(emptySet())
    var coverVersion by mutableStateOf(0)
        private set

    // ---- Organização ----
    var organizeState by mutableStateOf<OrganizeState?>(null)
        private set
    var undoCount by mutableStateOf(moveLog.count())
        private set
    private var pendingMove: List<Song> = emptyList()
    private var pendingUndo: List<MoveLog.Entry> = emptyList()

    // ---- Aprendizado e duplicatas ----
    var learnedCount by mutableStateOf(learned.count())
        private set
    var duplicateGroups by mutableStateOf<List<DuplicateFinder.Group>>(emptyList())
        private set
    var hiddenCount by mutableStateOf(duplicateStore.hiddenIds().size)
        private set
    /** Resultado da comparação de conteúdo por grupo: true = arquivos idênticos. */
    var contentCheck by mutableStateOf<Map<String, Boolean>>(emptyMap())
        private set
    var checkingGroup by mutableStateOf<String?>(null)
        private set
    var duplicateMessage by mutableStateOf("")
        private set
    private var pendingDelete: List<Long> = emptyList()

    // ---- Letras ----
    var lyrics by mutableStateOf<ParsedLyrics?>(null)
        private set
    var lyricsRaw by mutableStateOf("")
        private set
    var lyricsStatus by mutableStateOf(LyricsStatus.NONE)
        private set
    var lyricsOffsetMs by mutableStateOf(0L)
        private set
    var lyricsIds by mutableStateOf<Set<Long>>(emptySet())
        private set
    var lyricsFilter by mutableStateOf(LyricsFilter.ALL)
    var lyricsBulk by mutableStateOf<OrganizeState?>(null)
        private set
    var autoLyrics by mutableStateOf(lyricsStore.autoFetch)
        private set
    private var lyricsJob: Job? = null
    private var bulkJob: Job? = null

    // ---- Player ----
    private var controller: MediaController? = null
    var isPlaying by mutableStateOf(false)
        private set
    var currentId by mutableStateOf<Long?>(null)
        private set
    var currentTitle by mutableStateOf("")
        private set
    var currentArtist by mutableStateOf("")
        private set
    var positionMs by mutableStateOf(0L)
        private set
    var durationMs by mutableStateOf(0L)
        private set
    var shuffle by mutableStateOf(false)
        private set
    var repeatMode by mutableStateOf(Player.REPEAT_MODE_OFF)
        private set
    var queue by mutableStateOf<List<QueueEntry>>(emptyList())
        private set
    var queueIndex by mutableStateOf(0)
        private set

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            syncPlayerState()
        }
    }

    // =====================================================================
    // Biblioteca e classificação
    // =====================================================================

    fun loadSongs() {
        if (loading) return
        viewModelScope.launch {
            loading = true
            val map = genres.associateBy { it.id }
            val hidden = duplicateStore.hiddenIds()
            songs = withContext(Dispatchers.IO) {
                val scanned = scanner.scan().filter { it.id !in hidden }
                inferFromSameArtist(applyLearned(scanned.map { classify(applyEdits(it), map) }))
            }
            refreshDuplicates()
            if (selectedGenre != null && genres.none { it == selectedGenre }) selectedGenre = null
            lyricsIds = withContext(Dispatchers.IO) { lyricsStore.ids() }
            loading = false
        }
    }

    private fun classify(song: Song, map: Map<String, Genre>): Song {
        val artistKey = GenreClassifier.normalize(song.artist)
        val result = GenreClassifier.classify(
            song,
            overrides.forSong(song.id)?.let { map[it] },
            overrides.forArtist(artistKey)?.let { map[it] },
            map
        )
        return song.copy(
            genre = result.genre,
            source = result.source,
            confirmed = result.confirmed,
            searchKey = GenreClassifier.normalize("${song.title} ${song.artist} ${song.album}")
        )
    }

    /**
     * Usa o que o app aprendeu com as suas correções: músicas ainda sem gênero (ou só com uma
     * sugestão fraca) de um álbum, artista ou pasta que você já corrigiu viram sugestões.
     */
    private fun applyLearned(list: List<Song>): List<Song> {
        val data = learned.load()
        if (data.isEmpty) return list
        val map = genres.associateBy { it.id }
        return list.map { song ->
            val open = !song.confirmed || (song.genre == Genre.OUTROS && song.source.isEmpty())
            val hit = if (open) learned.suggest(song, data) else null
            val genre = hit?.let { map[it.genreId] }
            if (hit != null && genre != null) {
                song.copy(genre = genre, source = hit.source, confirmed = false)
            } else {
                song
            }
        }
    }

    private fun learnFrom(list: List<Song>, genre: Genre) {
        if (genre == Genre.OUTROS) return
        for (song in list) learned.record(song, genre.id)
        learnedCount = learned.count()
        songs = applyLearned(songs)
    }

    fun forgetLearning() {
        learned.clear()
        learnedCount = 0
        loadSongs()
    }

    /**
     * Música sem gênero, mas de um artista que já tem outras músicas classificadas:
     * sugere o gênero mais comum desse artista. Fica aguardando confirmação.
     */
    private fun inferFromSameArtist(list: List<Song>): List<Song> {
        val keys = list.map { GenreClassifier.normalize(it.artist) }
        val votes = HashMap<String, HashMap<Genre, Int>>()
        list.forEachIndexed { i, song ->
            val key = keys[i]
            if (key.isNotEmpty() && song.confirmed && song.genre != Genre.OUTROS) {
                val counts = votes.getOrPut(key) { HashMap() }
                counts[song.genre] = (counts[song.genre] ?: 0) + 1
            }
        }
        return list.mapIndexed { i, song ->
            val counts = votes[keys[i]]
            if (song.genre == Genre.OUTROS && song.source.isEmpty() && counts != null) {
                val best = counts.maxByOrNull { it.value }?.key
                if (best != null) {
                    song.copy(genre = best, source = "outras músicas do artista", confirmed = false)
                } else {
                    song
                }
            } else {
                song
            }
        }
    }

    private fun applyEdits(song: Song): Song {
        if (!edits.has(song.id)) return song
        return song.copy(
            title = edits.title(song.id) ?: song.title,
            artist = edits.artist(song.id) ?: song.artist,
            album = edits.album(song.id) ?: song.album
        )
    }

    /** Lista da biblioteca com busca, filtros e ordenação aplicados. */
    fun filtered(): List<Song> {
        val tokens = GenreClassifier.normalize(query).split(' ').filter { it.isNotEmpty() }
        val list = songs.filter { song ->
            (selectedGenre == null || song.genre == selectedGenre) &&
                (formatFilter == FormatFilter.ALL || FormatFilter.of(song) == formatFilter) &&
                durationFilter.matches(song.durationMs) &&
                (!onlyNoArtist || song.artist.isBlank()) &&
                (lyricsFilter == LyricsFilter.ALL ||
                    (song.id in lyricsIds) == (lyricsFilter == LyricsFilter.WITH)) &&
                tokens.all { song.searchKey.contains(it) }
        }
        return when (sortBy) {
            SortBy.TITLE -> list.sortedBy { it.title.lowercase() }
            SortBy.ARTIST -> list.sortedBy { it.artist.lowercase() }
            SortBy.ALBUM -> list.sortedBy { it.album.lowercase() }
            SortBy.DURATION -> list.sortedBy { it.durationMs }
        }
    }

    // =====================================================================
    // Gênero: correções, sugestões e gêneros personalizados
    // =====================================================================

    /** Define o gênero de uma música (ou de todas do mesmo artista). Conta como confirmação. */
    fun setGenre(song: Song, genre: Genre, applyToArtist: Boolean) {
        val key = GenreClassifier.normalize(song.artist)
        if (applyToArtist && key.isNotEmpty()) {
            overrides.setArtist(key, genre.id)
            songs = songs.map {
                if (GenreClassifier.normalize(it.artist) == key) {
                    it.copy(genre = genre, source = "você", confirmed = true)
                } else {
                    it
                }
            }
        } else {
            overrides.setSong(song.id, genre.id)
            songs = songs.map {
                if (it.id == song.id) it.copy(genre = genre, source = "você", confirmed = true) else it
            }
        }
        learnFrom(listOf(song), genre)
    }

    fun confirmAllSuggestions() {
        val pending = songs.filter { !it.confirmed }
        if (pending.isEmpty()) return
        overrides.setSongs(pending.map { it.id }, pending.map { it.genre.id })
        songs = songs.map { if (it.confirmed) it else it.copy(source = "você", confirmed = true) }
    }

    /** Cria um gênero. Devolve false se o nome for vazio ou repetido. */
    fun addGenre(label: String): Boolean {
        if (!genreStore.add(label)) return false
        genres = genreStore.all()
        loadSongs()
        return true
    }

    fun renameGenre(genre: Genre, label: String): Boolean {
        if (!genreStore.rename(genre.id, label)) return false
        genres = genreStore.all()
        loadSongs()
        return true
    }

    fun deleteGenre(genre: Genre) {
        genreStore.delete(genre.id)
        genres = genreStore.all()
        loadSongs()
    }

    // =====================================================================
    // Edição de informações e capas
    // =====================================================================

    /** Salva edições. Valores nulos mantêm o que já existe em cada música. */
    fun saveEdits(ids: Set<Long>, title: String?, artist: String?, album: String?, genre: Genre?) {
        val map = genres.associateBy { it.id }
        songs = songs.map { song ->
            if (song.id !in ids) {
                song
            } else {
                if (title != null || artist != null || album != null) {
                    edits.set(song.id, title, artist, album)
                }
                if (genre != null) overrides.setSong(song.id, genre.id)
                classify(
                    song.copy(
                        title = title ?: song.title,
                        artist = artist ?: song.artist,
                        album = album ?: song.album
                    ),
                    map
                )
            }
        }
        if (genre != null) learnFrom(songs.filter { it.id in ids }, genre)
    }

    fun hasEdits(id: Long): Boolean = edits.has(id)

    fun resetEdits(id: Long) {
        edits.clear(id)
        loadSongs()
    }

    suspend fun loadCover(id: Long): Bitmap? = withContext(Dispatchers.IO) { covers.load(id) }

    fun hasCustomCover(id: Long): Boolean {
        val version = coverVersion // lido para a tela se atualizar quando a capa mudar
        return version >= 0 && covers.hasCustom(id)
    }

    fun setCover(id: Long, source: Uri) {
        viewModelScope.launch {
            val saved = withContext(Dispatchers.IO) { covers.save(id, source) }
            if (saved) coverVersion++
        }
    }

    fun removeCover(id: Long) {
        covers.remove(id)
        coverVersion++
    }

    // =====================================================================
    // Organização em pastas
    // =====================================================================

    /** Músicas prontas para ir para pastas: com gênero definido e confirmado. */
    fun organizeTargets(): List<Song> =
        songs.filter { it.genre != Genre.OUTROS && it.confirmed }

    private fun needsMove(): List<Song> =
        organizeTargets().filter { it.relativePath != organizer.targetPath(it.genre) }

    fun clearOrganize() {
        organizeState = null
    }

    fun copyToFolders() {
        val targets = organizeTargets()
        viewModelScope.launch {
            organizeState = OrganizeState(0, targets.size, false, "")
            val report = organizer.copy(targets) { done, total ->
                organizeState = OrganizeState(done, total, false, "")
            }
            organizeState = OrganizeState(
                targets.size, targets.size, true,
                "Copiadas: ${report.copied}. Já estavam nas pastas: ${report.alreadyThere}. Falhas: ${report.failed}."
            )
        }
    }

    /** Prepara um lote para mover e devolve o pedido de permissão do Android (ou null). */
    fun prepareMove(): IntentSender? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        val plan = needsMove().take(MOVE_BATCH)
        if (plan.isEmpty()) {
            organizeState = OrganizeState(0, 0, true, "Nada para mover: as músicas já estão nas pastas.")
            return null
        }
        return try {
            val sender = organizer.writeRequest(plan.map { it.uri })
            pendingMove = plan
            sender
        } catch (e: Exception) {
            organizeState = OrganizeState(0, 0, true, "Não foi possível pedir a permissão ao Android.")
            null
        }
    }

    fun confirmMove() {
        val plan = pendingMove
        pendingMove = emptyList()
        if (plan.isEmpty()) return
        val remaining = needsMove().size - plan.size
        viewModelScope.launch {
            organizeState = OrganizeState(0, plan.size, false, "")
            val report = organizer.move(plan) { done, total ->
                organizeState = OrganizeState(done, total, false, "")
            }
            moveLog.add(report.entries)
            undoCount = moveLog.count()
            val rest = if (remaining > 0) " Restam $remaining: toque em Aplicar de novo." else ""
            organizeState = OrganizeState(
                plan.size, plan.size, true,
                "Movidas: ${report.moved}. Falhas: ${report.failed}.$rest"
            )
            loadSongs()
        }
    }

    fun prepareUndo(): IntentSender? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        val plan = moveLog.entries(MOVE_BATCH)
        if (plan.isEmpty()) return null
        return try {
            val sender = organizer.writeRequest(plan.map { organizer.audioUri(it.id) })
            pendingUndo = plan
            sender
        } catch (e: Exception) {
            organizeState = OrganizeState(0, 0, true, "Não foi possível pedir a permissão ao Android.")
            null
        }
    }

    fun confirmUndo() {
        val plan = pendingUndo
        pendingUndo = emptyList()
        if (plan.isEmpty()) return
        viewModelScope.launch {
            organizeState = OrganizeState(0, plan.size, false, "")
            val report = organizer.restore(plan) { done, total ->
                organizeState = OrganizeState(done, total, false, "")
            }
            moveLog.remove(report.doneIds)
            undoCount = moveLog.count()
            val rest = if (undoCount > 0) " Restam $undoCount: toque em Desfazer de novo." else ""
            organizeState = OrganizeState(
                plan.size, plan.size, true,
                "Devolvidas ao local original: ${report.restored}. Falhas: ${report.failed}.$rest"
            )
            loadSongs()
        }
    }

    fun cancelPending() {
        pendingMove = emptyList()
        pendingUndo = emptyList()
    }

    // =====================================================================
    // Duplicatas
    // =====================================================================

    private fun refreshDuplicates() {
        val snapshot = songs
        val ignored = duplicateStore.ignoredGroups()
        hiddenCount = duplicateStore.hiddenIds().size
        viewModelScope.launch {
            duplicateGroups = withContext(Dispatchers.Default) {
                DuplicateFinder.find(snapshot, ignored)
            }
        }
    }

    fun suggestedKeep(group: DuplicateFinder.Group): Song =
        DuplicateFinder.suggestedKeep(group, lyricsIds)

    /** "Não são duplicatas": o grupo some da lista. */
    fun ignoreGroup(group: DuplicateFinder.Group) {
        duplicateStore.ignore(group.key)
        duplicateMessage = ""
        refreshDuplicates()
    }

    fun restoreIgnoredGroups() {
        duplicateStore.clearIgnored()
        duplicateMessage = ""
        refreshDuplicates()
    }

    /** Oculta as músicas da biblioteca do NexSong. Os arquivos continuam no aparelho. */
    fun hideSongs(ids: Collection<Long>) {
        if (ids.isEmpty()) return
        duplicateStore.hide(ids)
        val hidden = ids.toSet()
        songs = songs.filter { it.id !in hidden }
        duplicateMessage = "${hidden.size} músicas ocultadas. Os arquivos continuam no aparelho."
        refreshDuplicates()
    }

    fun showHiddenSongs() {
        duplicateStore.unhideAll()
        duplicateMessage = ""
        loadSongs()
    }

    /** Compara o conteúdo dos arquivos do grupo para dizer se são idênticos. */
    fun checkContent(group: DuplicateFinder.Group) {
        if (checkingGroup != null) return
        checkingGroup = group.key
        viewModelScope.launch {
            val resolver = getApplication<Application>().contentResolver
            val prints = withContext(Dispatchers.IO) {
                group.songs.map { DuplicateFinder.fingerprint(resolver, it.uri) }
            }
            if (prints.none { it == null }) {
                contentCheck = contentCheck + (group.key to (prints.toSet().size == 1))
            }
            checkingGroup = null
        }
    }

    /** Pede ao Android a confirmação para apagar. Só existe a partir do Android 11. */
    fun prepareDelete(ids: List<Long>): IntentSender? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || ids.isEmpty()) return null
        return try {
            val sender = organizer.deleteRequest(ids.map { organizer.audioUri(it) })
            pendingDelete = ids
            sender
        } catch (e: Exception) {
            duplicateMessage = "Não foi possível pedir a confirmação ao Android."
            null
        }
    }

    fun confirmDelete() {
        val ids = pendingDelete
        pendingDelete = emptyList()
        if (ids.isEmpty()) return
        moveLog.remove(ids)
        undoCount = moveLog.count()
        for (id in ids) lyricsStore.delete(id)
        lyricsIds = lyricsIds - ids.toSet()
        duplicateMessage = "${ids.size} músicas apagadas do aparelho."
        loadSongs()
    }

    fun cancelDelete() {
        pendingDelete = emptyList()
    }

    // =====================================================================
    // Letras
    // =====================================================================

    private fun showLyrics(id: Long, text: String) {
        lyricsRaw = text
        lyrics = LrcParser.parse(text)
        lyricsOffsetMs = lyricsStore.offset(id)
        lyricsStatus = LyricsStatus.READY
    }

    /** Carrega a letra da música atual: primeiro a salva no aparelho, depois a internet. */
    private fun loadLyricsForCurrent() {
        lyricsJob?.cancel()
        val id = currentId
        lyrics = null
        lyricsRaw = ""
        lyricsOffsetMs = 0L
        if (id == null) {
            lyricsStatus = LyricsStatus.NONE
            return
        }
        lyricsJob = viewModelScope.launch {
            val local = withContext(Dispatchers.IO) { lyricsStore.load(id) }
            if (local != null) {
                showLyrics(id, local)
            } else if (lyricsStore.notFound(id)) {
                lyricsStatus = LyricsStatus.NOT_FOUND
            } else if (!autoLyrics) {
                lyricsStatus = LyricsStatus.NONE
            } else {
                fetchLyricsInto(id)
            }
        }
    }

    private suspend fun fetchLyricsInto(id: Long) {
        lyricsStatus = LyricsStatus.LOADING
        val result = downloadLyrics(id)
        if (currentId != id) return
        when (result) {
            is LyricsClient.Result.Found -> showLyrics(id, result.text)
            LyricsClient.Result.NotFound -> lyricsStatus = LyricsStatus.NOT_FOUND
            LyricsClient.Result.Error -> lyricsStatus = LyricsStatus.ERROR
        }
    }

    /** Busca a letra na internet e, se achar, guarda no aparelho. */
    private suspend fun downloadLyrics(id: Long): LyricsClient.Result {
        val song = songs.firstOrNull { it.id == id } ?: return LyricsClient.Result.NotFound
        val result = withContext(Dispatchers.IO) {
            lyricsClient.fetch(song.title, song.artist, song.album, song.durationMs)
        }
        when (result) {
            is LyricsClient.Result.Found -> {
                val saved = withContext(Dispatchers.IO) { lyricsStore.save(id, result.text) }
                if (saved) lyricsIds = lyricsIds + id
            }
            LyricsClient.Result.NotFound -> lyricsStore.markNotFound(id)
            LyricsClient.Result.Error -> Unit
        }
        return result
    }

    fun searchLyricsNow() {
        val id = currentId ?: return
        lyricsStore.clearNotFound(id)
        lyricsJob?.cancel()
        lyricsJob = viewModelScope.launch { fetchLyricsInto(id) }
    }

    fun saveLyricsText(text: String) {
        val id = currentId ?: return
        if (text.isBlank()) {
            deleteLyrics()
            return
        }
        if (lyricsStore.save(id, text)) {
            lyricsIds = lyricsIds + id
            showLyrics(id, text)
        }
    }

    fun deleteLyrics() {
        val id = currentId ?: return
        lyricsStore.delete(id)
        lyricsIds = lyricsIds - id
        lyrics = null
        lyricsRaw = ""
        lyricsOffsetMs = 0L
        lyricsStatus = LyricsStatus.NONE
    }

    fun nudgeLyricsOffset(deltaMs: Long) {
        val id = currentId ?: return
        lyricsOffsetMs += deltaMs
        lyricsStore.setOffset(id, lyricsOffsetMs)
    }

    fun updateAutoLyrics(enabled: Boolean) {
        autoLyrics = enabled
        lyricsStore.autoFetch = enabled
    }

    private fun isOnWifi(): Boolean {
        return try {
            val manager = getApplication<Application>().getSystemService(ConnectivityManager::class.java)
                ?: return false
            val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        } catch (e: Exception) {
            false
        }
    }

    /** Baixa a letra de todas as músicas que ainda não têm. O app precisa ficar aberto. */
    fun downloadAllLyrics(wifiOnly: Boolean) {
        if (bulkJob?.isActive == true) return
        if (wifiOnly && !isOnWifi()) {
            lyricsBulk = OrganizeState(0, 0, true, "Sem Wi-Fi no momento. Conecte-se ou desmarque a opção.")
            return
        }
        val targets = songs.filter { it.id !in lyricsIds && !lyricsStore.notFound(it.id) }
        bulkJob = viewModelScope.launch {
            var found = 0
            var missing = 0
            var errors = 0
            var errorsInARow = 0
            var done = 0
            var stopped = ""
            lyricsBulk = OrganizeState(0, targets.size, false, "")
            for (song in targets) {
                if (wifiOnly && !isOnWifi()) {
                    stopped = " Parei porque o Wi-Fi caiu."
                    break
                }
                when (downloadLyrics(song.id)) {
                    is LyricsClient.Result.Found -> {
                        found++
                        errorsInARow = 0
                    }
                    LyricsClient.Result.NotFound -> {
                        missing++
                        errorsInARow = 0
                    }
                    LyricsClient.Result.Error -> {
                        errors++
                        errorsInARow++
                    }
                }
                done++
                lyricsBulk = OrganizeState(done, targets.size, false, "")
                if (errorsInARow >= 5) {
                    stopped = " Parei porque a conexão falhou várias vezes seguidas."
                    break
                }
                delay(300)
            }
            lyricsBulk = OrganizeState(
                done, targets.size, true,
                "Letras baixadas: $found. Não encontradas: $missing. Falhas de conexão: $errors.$stopped"
            )
            if (lyrics == null && currentId != null) loadLyricsForCurrent()
        }
    }

    fun cancelBulkLyrics() {
        bulkJob?.cancel()
        lyricsBulk = null
    }

    fun clearBulkLyrics() {
        lyricsBulk = null
    }

    // =====================================================================
    // Player
    // =====================================================================

    fun connectPlayer() {
        if (controller != null) return
        val app = getApplication<Application>()
        val token = SessionToken(app, ComponentName(app, PlaybackService::class.java))
        val future = MediaController.Builder(app, token).buildAsync()
        future.addListener({
            val c = future.get()
            controller = c
            c.addListener(playerListener)
            syncPlayerState()
        }, ContextCompat.getMainExecutor(app))
    }

    fun play(list: List<Song>, index: Int) {
        val c = controller ?: return
        // Fila limitada ao redor da música escolhida, para não sobrecarregar o serviço.
        val start = maxOf(0, index - 100)
        val end = minOf(list.size, index + 100)
        val window = list.subList(start, end)
        c.setMediaItems(window.map { it.toMediaItem() }, index - start, 0L)
        c.prepare()
        c.play()
    }

    fun togglePlay() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun next() {
        controller?.seekToNextMediaItem()
    }

    fun previous() {
        controller?.seekToPreviousMediaItem()
    }

    fun seekTo(ms: Long) {
        controller?.seekTo(ms)
        positionMs = ms
    }

    fun toggleShuffle() {
        val c = controller ?: return
        c.shuffleModeEnabled = !c.shuffleModeEnabled
    }

    fun cycleRepeat() {
        val c = controller ?: return
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun playNext(song: Song) {
        val c = controller ?: return
        if (c.mediaItemCount == 0) {
            c.setMediaItem(song.toMediaItem())
            c.prepare()
            c.play()
        } else {
            c.addMediaItem(c.currentMediaItemIndex + 1, song.toMediaItem())
        }
    }

    fun addToQueue(song: Song) {
        val c = controller ?: return
        if (c.mediaItemCount == 0) {
            c.setMediaItem(song.toMediaItem())
            c.prepare()
            c.play()
        } else {
            c.addMediaItem(song.toMediaItem())
        }
    }

    fun jumpTo(index: Int) {
        val c = controller ?: return
        if (index !in 0 until c.mediaItemCount) return
        c.seekTo(index, 0L)
        c.play()
    }

    fun removeFromQueue(index: Int) {
        val c = controller ?: return
        if (index !in 0 until c.mediaItemCount) return
        c.removeMediaItem(index)
    }

    fun moveQueueItem(from: Int, to: Int) {
        val c = controller ?: return
        val count = c.mediaItemCount
        if (from !in 0 until count || to !in 0 until count) return
        c.moveMediaItem(from, to)
    }

    fun refreshPosition() {
        val c = controller ?: return
        positionMs = c.currentPosition
        durationMs = c.duration.takeIf { it != C.TIME_UNSET } ?: 0L
    }

    private fun syncPlayerState() {
        val c = controller ?: return
        isPlaying = c.isPlaying
        val newId = c.currentMediaItem?.mediaId?.toLongOrNull()
        val songChanged = newId != currentId
        currentId = newId
        currentTitle = c.mediaMetadata.title?.toString().orEmpty()
        currentArtist = c.mediaMetadata.artist?.toString().orEmpty()
        shuffle = c.shuffleModeEnabled
        repeatMode = c.repeatMode
        val entries = ArrayList<QueueEntry>(c.mediaItemCount)
        for (i in 0 until c.mediaItemCount) {
            val metadata = c.getMediaItemAt(i).mediaMetadata
            entries.add(
                QueueEntry(
                    metadata.title?.toString().orEmpty(),
                    metadata.artist?.toString().orEmpty()
                )
            )
        }
        queue = entries
        queueIndex = c.currentMediaItemIndex
        refreshPosition()
        if (songChanged) loadLyricsForCurrent()
    }

    private fun Song.toMediaItem(): MediaItem =
        MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri(uri)
            .setRequestMetadata(MediaItem.RequestMetadata.Builder().setMediaUri(uri).build())
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist.ifBlank { "Artista desconhecido" })
                    .setAlbumTitle(album)
                    .build()
            )
            .build()

    override fun onCleared() {
        controller?.removeListener(playerListener)
        controller?.release()
        controller = null
        super.onCleared()
    }

    companion object {
        /** Quantas músicas são movidas por vez (cada lote pede uma confirmação ao Android). */
        const val MOVE_BATCH = 500
    }
}
