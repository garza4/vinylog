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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
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
import com.example.vinylog.games.isGameSystem
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
) {
    var mode by remember(album.id) { mutableStateOf("view") }
    var draft by remember(album.id) { mutableStateOf(album.toDraft()) }
    var titleQuery by remember(album.id) {
        mutableStateOf(
            ReleaseLookup.cleanCommerceTitle(album.albumName.orEmpty()).ifBlank { album.albumName.orEmpty() },
        )
    }
    var artistQuery by remember(album.id) { mutableStateOf(album.artistName.orEmpty()) }
    var searchToken by remember(album.id) { mutableIntStateOf(0) }
    var searching by remember(album.id) { mutableStateOf(false) }
    var hits by remember(album.id) { mutableStateOf<List<ScannedRelease>>(emptyList()) }
    var note by remember(album.id) { mutableStateOf("") }
    var busy by remember(album.id) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()
    val songs = album.songsOnRecord.orEmpty().lines().map { it.trim() }.filter { it.isNotEmpty() }
    val mediaType = album.mediaType.orEmpty()

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
                                if (draft.album.isBlank()) {
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
                            DetailLine(personLabel(mediaType), album.artistName)
                            DetailLine("Year", album.yearPublished)
                            DetailLine(publisherLabel(mediaType), album.recordLabel)
                            DetailLine("Genre", album.genre)
                            DetailLine("Format", album.mediaType)
                            DetailLine("Barcode", album.barcode)
                            if (songs.isNotEmpty()) {
                                Text(
                                    text = worksLabel(mediaType),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                                songs.forEachIndexed { index, title ->
                                    Text(text = "${index + 1}. $title")
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
                        }
                    }
                }
            }
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
            title = draft.album,
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
        )
    }
    if (note.isNotBlank()) {
        Text(text = note, color = MaterialTheme.colorScheme.error)
    }
    DraftField("Title", draft.album) { onDraftChange(draft.copy(album = it)) }
    DraftField(personLabel(mediaType), draft.artist) { onDraftChange(draft.copy(artist = it)) }
    DraftField("Year", draft.year) { onDraftChange(draft.copy(year = it)) }
    DraftField(publisherLabel(mediaType), draft.label) { onDraftChange(draft.copy(label = it)) }
    DraftField("Genre", draft.genre) { onDraftChange(draft.copy(genre = it)) }
    DraftField(worksLabel(mediaType), draft.songs, singleLine = false) {
        onDraftChange(draft.copy(songs = it))
    }
    DraftField("Barcode", draft.barcode) { onDraftChange(draft.copy(barcode = it)) }
    DraftField("Cover URL", draft.cover) { onDraftChange(draft.copy(cover = it)) }
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
    DraftField(personLabel(mediaType), artist, onValue = onArtist)
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

private fun personLabel(mediaType: String): String = when {
    mediaType == "Books" -> "Author"
    mediaType == "Movies" -> "Director"
    isGameSystem(mediaType) -> "Developer"
    else -> "Artist"
}

private fun publisherLabel(mediaType: String): String = when {
    mediaType == "Books" || isGameSystem(mediaType) -> "Publisher"
    mediaType == "Movies" -> "Studio"
    else -> "Label"
}

private fun worksLabel(mediaType: String): String = when {
    mediaType == "Books" || mediaType == "Movies" || isGameSystem(mediaType) -> "Notes"
    else -> "Songs"
}
