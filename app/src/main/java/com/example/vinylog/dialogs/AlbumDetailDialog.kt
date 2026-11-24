package com.example.vinylog.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.vinylog.media.MediaKind
import com.example.vinylog.media.creditLabel
import com.example.vinylog.media.entryFields
import com.example.vinylog.media.mediaKind
import com.example.vinylog.media.valueOf
import com.example.vinylog.objects.Album
import com.example.vinylog.util.ReleaseLookup
import com.example.vinylog.util.ScannedRelease
import com.example.vinylog.util.SearchResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

@Composable
fun AlbumDetailDialog(
    album: Album,
    onDismiss: () -> Unit,
    onSave: suspend (Album) -> String?,
    onSearch: suspend (title: String, artist: String) -> SearchResult,
    onChoose: suspend (ScannedRelease) -> String?,
    onDelete: suspend (Album) -> String?,
) {
    val mediaType = album.mediaType.orEmpty()
    var mode by remember(album.id) { mutableStateOf("view") }
    var draft by remember(album.id) { mutableStateOf(album.toDraft()) }
    var titleQuery by remember(album.id) {
        mutableStateOf(
            ReleaseLookup.catalogQueryTitle(
                album.albumName.orEmpty(),
                album.artistName.orEmpty(),
                mediaType,
            ),
        )
    }
    var artistQuery by remember(album.id) {
        mutableStateOf(
            ReleaseLookup.catalogQueryCredit(
                album.albumName.orEmpty(),
                album.artistName.orEmpty(),
                mediaType,
            ),
        )
    }
    var searchToken by remember(album.id) { mutableIntStateOf(0) }
    var searching by remember(album.id) { mutableStateOf(false) }
    var hits by remember(album.id) { mutableStateOf<List<ScannedRelease>>(emptyList()) }
    var note by remember(album.id) { mutableStateOf("") }
    var busy by remember(album.id) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()

    LaunchedEffect(mode) {
        scroll.scrollTo(0)
    }

    LaunchedEffect(mode, searchToken) {
        if (mode != "search" || searchToken == 0) return@LaunchedEffect
        searching = true
        note = ""
        hits = emptyList()
        val result = try {
            onSearch(titleQuery, artistQuery)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (exception: Exception) {
            SearchResult.Failed("Couldn't reach the catalog.")
        }
        ensureActive()
        searching = false
        when (result) {
            is SearchResult.Hits -> hits = result.releases
            SearchResult.None -> note = "No matches. Try a different title or name."
            is SearchResult.Failed -> note = result.message
        }
    }

    Dialog(onDismissRequest = { if (!busy) onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 640.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Box {
                Column(
                    modifier = Modifier
                        .verticalScroll(scroll)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    when (mode) {
                        "edit" -> EditDetails(
                            mediaType = mediaType,
                            draft = draft,
                            note = note,
                            busy = busy,
                            onDraftChange = { draft = it },
                            onSave = {
                                if (busy) return@EditDetails
                                if (draft.title.isBlank()) {
                                    note = "Enter a title before saving."
                                    return@EditDetails
                                }
                                busy = true
                                note = ""
                                scope.launch {
                                    val failure = try {
                                        onSave(album.edited(draft))
                                    } catch (cancelled: CancellationException) {
                                        throw cancelled
                                    } catch (exception: Exception) {
                                        "Couldn't save these details. Try again."
                                    }
                                    ensureActive()
                                    busy = false
                                    if (failure == null) mode = "view" else note = failure
                                }
                            },
                            onCancel = {
                                if (!busy) {
                                    note = ""
                                    mode = "view"
                                }
                            },
                        )

                        "delete" -> DeleteConfirm(
                            title = album.albumName.orEmpty().ifBlank { "Untitled" },
                            note = note,
                            busy = busy,
                            onDelete = {
                                if (busy) return@DeleteConfirm
                                busy = true
                                note = ""
                                scope.launch {
                                    val failure = try {
                                        onDelete(album)
                                    } catch (cancelled: CancellationException) {
                                        throw cancelled
                                    } catch (exception: Exception) {
                                        "Couldn't delete this entry. Try again."
                                    }
                                    ensureActive()
                                    busy = false
                                    if (failure == null) onDismiss() else note = failure
                                }
                            },
                            onCancel = {
                                if (!busy) {
                                    note = ""
                                    mode = "view"
                                }
                            },
                        )

                        "search" -> SearchCatalog(
                            mediaType = mediaType,
                            title = titleQuery,
                            artist = artistQuery,
                            hits = hits,
                            note = note,
                            searching = searching,
                            busy = busy,
                            onTitle = { titleQuery = it },
                            onArtist = { artistQuery = it },
                            onSearch = {
                                if (titleQuery.isBlank() && artistQuery.isBlank()) {
                                    note = "Enter a title to search."
                                    hits = emptyList()
                                    return@SearchCatalog
                                }
                                searchToken += 1
                            },
                            onChoose = { hit ->
                                if (busy || searching) return@SearchCatalog
                                busy = true
                                note = ""
                                scope.launch {
                                    val failure = try {
                                        onChoose(hit)
                                    } catch (cancelled: CancellationException) {
                                        throw cancelled
                                    } catch (exception: Exception) {
                                        "Couldn't save this match. Try again."
                                    }
                                    ensureActive()
                                    busy = false
                                    if (failure == null) mode = "view" else note = failure
                                }
                            },
                            onCancel = {
                                if (!busy) {
                                    note = ""
                                    mode = "view"
                                }
                            },
                        )

                        else -> {
                            CoverImage(
                                url = album.coverUrl.orEmpty(),
                                title = album.albumName.orEmpty(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp),
                            )
                            Text(
                                text = album.albumName.orEmpty().ifBlank { "Untitled" },
                                style = MaterialTheme.typography.headlineSmall,
                            )
                            entryFields(mediaType).forEach { field ->
                                if (field.key == "title" || field.key == "cover" || field.multiline) return@forEach
                                DetailLine(field.label, album.valueOf(field.key))
                            }
                            when (mediaKind(mediaType)) {
                                MediaKind.Music -> DetailLine("Format", mediaType)
                                MediaKind.Game -> DetailLine("Platform", mediaType)
                                MediaKind.Movie, MediaKind.Book, MediaKind.Show -> Unit
                            }
                            entryFields(mediaType).filter { it.multiline }.forEach { field ->
                                val lines = album.valueOf(field.key).lines().map { it.trim() }.filter { it.isNotEmpty() }
                                if (lines.isEmpty()) return@forEach
                                Text(
                                    text = field.label,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                                lines.forEachIndexed { index, line ->
                                    val prefix = if (field.key == "tracks") "${index + 1}. " else ""
                                    Text(text = prefix + line)
                                }
                            }
                            TextButton(
                                onClick = onDismiss,
                                modifier = Modifier.align(Alignment.End),
                            ) {
                                Text("Close")
                            }
                        }
                    }
                }
                if (mode == "view") {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                        shadowElevation = 2.dp,
                    ) {
                        Row {
                            IconButton(onClick = {
                                note = ""
                                mode = "search"
                                if (titleQuery.isNotBlank() || artistQuery.isNotBlank()) {
                                    searchToken += 1
                                }
                            }) {
                                Icon(Icons.Filled.Search, contentDescription = "Search catalog")
                            }
                            IconButton(onClick = {
                                draft = album.toDraft()
                                note = ""
                                mode = "edit"
                            }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit details")
                            }
                            IconButton(onClick = {
                                note = ""
                                mode = "delete"
                            }) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = "Delete entry",
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeleteConfirm(
    title: String,
    note: String,
    busy: Boolean,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
) {
    Text(text = "Delete this entry?", style = MaterialTheme.typography.titleLarge)
    Text(text = "$title will be removed from this shelf.")
    if (note.isNotBlank()) {
        Text(text = note, color = MaterialTheme.colorScheme.error)
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        TextButton(
            enabled = !busy,
            onClick = onDelete,
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) {
            Text(if (busy) "Deleting…" else "Delete")
        }
        TextButton(enabled = !busy, onClick = onCancel) {
            Text("Cancel")
        }
    }
}

@Composable
private fun EditDetails(
    mediaType: String,
    draft: MediaDraft,
    note: String,
    busy: Boolean,
    onDraftChange: (MediaDraft) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    Text(text = "Edit details", style = MaterialTheme.typography.titleLarge)
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
        Text(text = note, color = MaterialTheme.colorScheme.error)
    }
    MediaDraftFields(mediaType, draft, onDraftChange)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        TextButton(enabled = !busy, onClick = onSave) {
            Text(if (busy) "Saving…" else "Save")
        }
        TextButton(enabled = !busy, onClick = onCancel) {
            Text("Cancel")
        }
    }
}

@Composable
private fun SearchCatalog(
    mediaType: String,
    title: String,
    artist: String,
    hits: List<ScannedRelease>,
    note: String,
    searching: Boolean,
    busy: Boolean,
    onTitle: (String) -> Unit,
    onArtist: (String) -> Unit,
    onSearch: () -> Unit,
    onChoose: (ScannedRelease) -> Unit,
    onCancel: () -> Unit,
) {
    Text(text = "Search catalog", style = MaterialTheme.typography.titleLarge)
    Text(
        text = "Pick a match to use for this entry.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    DraftField("Title", title, onValue = onTitle)
    DraftField(creditLabel(mediaType), artist, onValue = onArtist)
    if (note.isNotBlank()) {
        Text(text = note, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (searching) {
        Text(text = "Searching…")
    }
    hits.forEach { hit ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !busy && !searching) { onChoose(hit) }
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
        TextButton(enabled = !searching && !busy, onClick = onSearch) {
            Text(if (busy) "Saving…" else "Search")
        }
        TextButton(enabled = !busy, onClick = onCancel) {
            Text("Cancel")
        }
    }
}

@Composable
fun CoverImage(
    url: String,
    title: String,
    modifier: Modifier = Modifier,
) {
    val initial = title.trim().take(1).uppercase().ifBlank { "?" }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (url.isNotBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(url)
                    .addHeader("User-Agent", ReleaseLookup.USER_AGENT)
                    .crossfade(true)
                    .build(),
                contentDescription = "$title cover",
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String?) {
    val text = value?.trim().orEmpty()
    if (text.isEmpty()) return
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}

