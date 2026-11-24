package com.example.vinylog.dialogs

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.vinylog.activities.BarcodeScan
import com.example.vinylog.objects.Album
import com.example.vinylog.objects.database.AppDb
import com.example.vinylog.util.BarcodeScanner
import com.example.vinylog.util.LookupResult
import com.example.vinylog.util.ReleaseLookup
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class MediaDraft(
    val album: String = "",
    val artist: String = "",
    val songs: String = "",
    val year: String = "",
    val genre: String = "",
    val barcode: String = "",
    val media: String = "",
    val cover: String = "",
    val label: String = "",
)

@Composable
fun MediaSaveDialog(
    mediaType: String,
    onDismiss: () -> Unit,
    db: AppDb,
    barcodeScanner: BarcodeScanner,
    scannedBarcode: BarcodeScan?,
    onBarcodeHandled: () -> Unit,
    catalogLookup: Boolean = true,
) {
    var draft by remember(mediaType) { mutableStateOf(MediaDraft(media = mediaType)) }
    var choice by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(scannedBarcode?.token) {
        val code = scannedBarcode?.barcode?.trim().orEmpty()
        if (code.isEmpty()) return@LaunchedEffect
        if (!catalogLookup) {
            draft = draft.copy(barcode = code, media = draft.media.ifBlank { mediaType })
            note = "Enter the game details, then save."
            choice = "manual"
            onBarcodeHandled()
            return@LaunchedEffect
        }
        choice = "lookup"
        note = ""
        val result = try {
            withContext(Dispatchers.IO) { ReleaseLookup().lookup(code) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (exception: Exception) {
            Log.e("MediaSaveDialog", "Barcode lookup failed", exception)
            LookupResult.Failed("Couldn't reach the catalog.")
        }
        ensureActive()
        draft = when (result) {
            is LookupResult.Found -> draft.copy(
                album = result.release.album.ifBlank { draft.album },
                artist = result.release.artist,
                songs = result.release.songs,
                year = result.release.year,
                genre = result.release.genre,
                barcode = code,
                media = draft.media.ifBlank { mediaType },
                cover = result.release.coverUrl,
                label = result.release.label,
            )
            is LookupResult.NotFound, is LookupResult.Failed -> draft.copy(barcode = code)
        }
        note = when (result) {
            is LookupResult.Found -> "Check the details, then save."
            is LookupResult.NotFound -> "No catalog match for this barcode. Enter the details yourself."
            is LookupResult.Failed -> "Couldn't reach the catalog. Enter the details yourself."
        }
        choice = "manual"
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

        "manual" -> EntryForm(
            draft = draft,
            note = note,
            saving = saving,
            onDraftChange = { draft = it },
            onDismiss = { if (!saving) onDismiss() },
            onSave = {
                if (saving) return@EntryForm
                if (draft.album.isBlank()) {
                    note = "Enter an album or title before saving."
                    return@EntryForm
                }
                val album = Album(
                    draft.album.trim(),
                    draft.artist.trim(),
                    draft.songs.trim(),
                    draft.year.trim(),
                    draft.genre.trim(),
                    draft.media.trim().ifBlank { mediaType },
                    draft.barcode.trim(),
                    draft.cover.trim(),
                    draft.label.trim(),
                )
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
                        title = draft.album,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                    )
                }
                if (note.isNotBlank()) {
                    Text(text = note, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DraftField("Album", draft.album) { onDraftChange(draft.copy(album = it)) }
                DraftField("Artist", draft.artist) { onDraftChange(draft.copy(artist = it)) }
                DraftField("Year", draft.year) { onDraftChange(draft.copy(year = it)) }
                DraftField("Genre", draft.genre) { onDraftChange(draft.copy(genre = it)) }
                DraftField("Songs", draft.songs, singleLine = false) { onDraftChange(draft.copy(songs = it)) }
                DraftField("Barcode", draft.barcode) { onDraftChange(draft.copy(barcode = it)) }
                DraftField("Media", draft.media) { onDraftChange(draft.copy(media = it)) }
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

@Composable
private fun DraftField(
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
