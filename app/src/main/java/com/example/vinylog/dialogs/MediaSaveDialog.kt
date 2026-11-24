package com.example.vinylog.dialogs

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.vinylog.activities.BarcodeScan
import com.example.vinylog.media.MediaKind
import com.example.vinylog.media.entryFields
import com.example.vinylog.media.mediaKind
import com.example.vinylog.objects.Album
import com.example.vinylog.objects.database.AppDb
import com.example.vinylog.util.BarcodeScanner
import com.example.vinylog.util.ReleaseLookup
import com.example.vinylog.util.ScannedRelease
import com.example.vinylog.util.SearchResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal data class MediaDraft(
    val title: String = "",
    val person: String = "",
    val company: String = "",
    val year: String = "",
    val genre: String = "",
    val tracks: String = "",
    val cast: String = "",
    val runtime: String = "",
    val pages: String = "",
    val format: String = "",
    val rating: String = "",
    val players: String = "",
    val barcode: String = "",
    val cover: String = "",
    val media: String = "",
)

@Composable
fun MediaSaveDialog(
    mediaType: String,
    onDismiss: () -> Unit,
    db: AppDb,
    barcodeScanner: BarcodeScanner,
    scannedBarcode: BarcodeScan?,
    onBarcodeHandled: () -> Unit,
) {
    var draft by remember(mediaType) { mutableStateOf(MediaDraft(media = mediaType)) }
    var choice by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var choosing by remember { mutableStateOf(false) }
    var hits by remember { mutableStateOf<List<ScannedRelease>>(emptyList()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(scannedBarcode?.token) {
        val code = scannedBarcode?.barcode?.trim().orEmpty()
        if (code.isEmpty()) return@LaunchedEffect
        choice = "lookup"
        note = ""
        hits = emptyList()
        choosing = false
        draft = draft.copy(barcode = code, media = draft.media.ifBlank { mediaType })
        val result = try {
            withContext(Dispatchers.IO) { ReleaseLookup().lookupChoices(code, mediaType) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (exception: Exception) {
            Log.e("MediaSaveDialog", "Barcode lookup failed", exception)
            SearchResult.Failed("Couldn't reach the catalog.")
        }
        ensureActive()
        when (result) {
            is SearchResult.Hits -> {
                hits = result.releases
                note = ""
                choice = "pick"
            }
            SearchResult.None -> {
                note = "No catalog match for this barcode. Enter the details yourself."
                choice = "manual"
            }
            is SearchResult.Failed -> {
                note = result.message
                choice = "manual"
            }
        }
        onBarcodeHandled()
    }

    when (choice) {
        "" -> AlertDialog(
            title = { Text(text = "Media Saver") },
            text = { Text(text = "Scan the barcode on the case, or type the entry yourself.") },
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = { choice = "manual" }) {
                    Text("Enter manually")
                }
            },
            dismissButton = {
                TextButton(onClick = { barcodeScanner.scanBarcode() }) {
                    Text("Scan")
                }
            },
        )

        "lookup" -> AlertDialog(
            title = { Text("Looking up barcode") },
            text = { Text("Checking the catalog for this release.") },
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            },
        )

        "pick" -> MatchPicker(
            barcode = draft.barcode,
            hits = hits,
            note = note,
            choosing = choosing,
            onChoose = { hit ->
                if (choosing) return@MatchPicker
                choosing = true
                note = ""
                scope.launch {
                    val full = try {
                        withContext(Dispatchers.IO) { ReleaseLookup().loadChoice(hit) }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (exception: Exception) {
                        hit
                    }
                    ensureActive()
                    draft = draft.withChoice(full, mediaType)
                    choosing = false
                    note = "Check the details, then save."
                    choice = "manual"
                }
            },
            onManual = {
                if (!choosing) {
                    note = "Enter the details, then save."
                    choice = "manual"
                }
            },
            onDismiss = { if (!choosing) onDismiss() },
        )

        "manual" -> EntryForm(
            draft = draft,
            note = note,
            saving = saving,
            onDraftChange = { draft = it },
            onDismiss = { if (!saving) onDismiss() },
            onSave = {
                if (saving) return@EntryForm
                if (draft.title.isBlank()) {
                    note = "Enter a title before saving."
                    return@EntryForm
                }
                val album = draft.toAlbum(id = 0, mediaType = draft.media.ifBlank { mediaType })
                saving = true
                scope.launch {
                    val failure = withContext(Dispatchers.IO) {
                        try {
                            db.libraryDao().insert(album)
                            null
                        } catch (exception: Exception) {
                            Log.e("MediaSaveDialog", "Could not save entry", exception)
                            "Couldn't save this entry. Try again."
                        }
                    }
                    saving = false
                    if (failure == null) onDismiss() else note = failure
                }
            },
        )
    }
}

@Composable
private fun MatchPicker(
    barcode: String,
    hits: List<ScannedRelease>,
    note: String,
    choosing: Boolean,
    onChoose: (ScannedRelease) -> Unit,
    onManual: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 640.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(text = "Choose a match", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "Pick the entry to use for this barcode.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (barcode.isNotBlank()) {
                    Text(
                        text = barcode,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (choosing) {
                    Text(text = "Loading this match…")
                }
                if (note.isNotBlank()) {
                    Text(text = note, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                hits.forEach { hit ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !choosing) { onChoose(hit) }
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CoverImage(
                            url = hit.coverUrl,
                            title = hit.album,
                            modifier = Modifier.size(56.dp),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = hit.album.ifBlank { "Untitled" },
                                style = MaterialTheme.typography.titleMedium,
                            )
                            val meta = listOf(hit.artist, hit.year, hit.label)
                                .map { it.trim() }
                                .filter { it.isNotBlank() }
                                .joinToString(" · ")
                            if (meta.isNotBlank()) {
                                Text(
                                    text = meta,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (hit.hint.isNotBlank()) {
                                Text(
                                    text = hit.hint,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    TextButton(enabled = !choosing, onClick = onManual) {
                        Text("Enter manually")
                    }
                    TextButton(enabled = !choosing, onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

@Composable
private fun EntryForm(
    draft: MediaDraft,
    note: String,
    saving: Boolean,
    onDraftChange: (MediaDraft) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(text = "Save entry", style = MaterialTheme.typography.titleLarge)
                if (draft.cover.isNotBlank()) {
                    CoverImage(
                        url = draft.cover,
                        title = draft.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                    )
                }
                if (note.isNotBlank()) {
                    Text(text = note, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                MediaDraftFields(draft.media, draft, onDraftChange)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    TextButton(
                        modifier = Modifier.padding(8.dp),
                        enabled = !saving,
                        onClick = onSave,
                    ) {
                        Text(if (saving) "Saving…" else "Save")
                    }
                    TextButton(
                        modifier = Modifier.padding(8.dp),
                        enabled = !saving,
                        onClick = onDismiss,
                    ) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

internal fun Album.toDraft(): MediaDraft = MediaDraft(
    title = albumName.orEmpty(),
    person = artistName.orEmpty(),
    company = recordLabel.orEmpty(),
    year = yearPublished.orEmpty(),
    genre = genre.orEmpty(),
    tracks = songsOnRecord.orEmpty(),
    cast = castNames.orEmpty(),
    runtime = runtime.orEmpty(),
    pages = pageCount.orEmpty(),
    format = formatName.orEmpty(),
    rating = contentRating.orEmpty(),
    players = playerCount.orEmpty(),
    barcode = barcode.orEmpty(),
    cover = coverUrl.orEmpty(),
    media = mediaType.orEmpty(),
)

internal fun MediaDraft.toAlbum(id: Long, mediaType: String): Album {
    val updated = Album(
        title.trim(),
        person.trim(),
        tracks.trim(),
        year.trim(),
        genre.trim(),
        mediaType,
        barcode.trim(),
        cover.trim(),
        company.trim(),
        runtime.trim(),
        cast.trim(),
        pages.trim(),
        format.trim(),
        rating.trim(),
        players.trim(),
    )
    updated.id = id
    return updated
}

internal fun Album.edited(draft: MediaDraft): Album = draft.toAlbum(id, mediaType.orEmpty())

private fun MediaDraft.withChoice(release: ScannedRelease, mediaType: String): MediaDraft {
    return copy(
        title = release.album.ifBlank { title },
        person = release.artist,
        company = release.label,
        year = release.year,
        genre = release.genre,
        tracks = if (mediaKind(mediaType) == MediaKind.Music) release.songs else "",
        cast = release.cast,
        runtime = release.runtime,
        pages = release.pages,
        format = release.format,
        rating = release.rating,
        players = release.players,
        barcode = barcode,
        cover = release.coverUrl.ifBlank { cover },
        media = media.ifBlank { mediaType },
    )
}

internal fun MediaDraft.value(key: String): String = when (key) {
    "title" -> title
    "artist", "director", "author", "developer", "creator" -> person
    "label", "studio", "publisher", "network" -> company
    "year" -> year
    "genre" -> genre
    "tracks" -> tracks
    "cast" -> cast
    "runtime" -> runtime
    "pages", "seasons" -> pages
    "format" -> format
    "rating" -> rating
    "players", "episodes" -> players
    "barcode" -> barcode
    "cover" -> cover
    else -> ""
}

internal fun MediaDraft.withValue(key: String, value: String): MediaDraft = when (key) {
    "title" -> copy(title = value)
    "artist", "director", "author", "developer", "creator" -> copy(person = value)
    "label", "studio", "publisher", "network" -> copy(company = value)
    "year" -> copy(year = value)
    "genre" -> copy(genre = value)
    "tracks" -> copy(tracks = value)
    "cast" -> copy(cast = value)
    "runtime" -> copy(runtime = value)
    "pages", "seasons" -> copy(pages = value)
    "format" -> copy(format = value)
    "rating" -> copy(rating = value)
    "players", "episodes" -> copy(players = value)
    "barcode" -> copy(barcode = value)
    "cover" -> copy(cover = value)
    else -> this
}

@Composable
internal fun MediaDraftFields(
    mediaType: String,
    draft: MediaDraft,
    onDraftChange: (MediaDraft) -> Unit,
) {
    entryFields(mediaType).forEach { field ->
        DraftField(
            label = field.label,
            value = draft.value(field.key),
            singleLine = !field.multiline,
        ) { onDraftChange(draft.withValue(field.key, it)) }
    }
}

@Composable
internal fun DraftField(
    label: String,
    value: String,
    singleLine: Boolean = true,
    onValue: (String) -> Unit,
) {
    TextField(
        modifier = Modifier.fillMaxWidth(),
        value = value,
        onValueChange = onValue,
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        label = { Text(label) },
    )
}
