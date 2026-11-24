package com.example.vinylog.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.example.vinylog.objects.Album
import com.example.vinylog.util.ReleaseLookup

@Composable
fun AlbumDetailDialog(
    album: Album,
    onDismiss: () -> Unit,
) {
    val songs = album.songsOnRecord.orEmpty().lines().map { it.trim() }.filter { it.isNotEmpty() }
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 640.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
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
                DetailLine("Artist", album.artistName)
                DetailLine("Year", album.yearPublished)
                DetailLine("Label", album.recordLabel)
                DetailLine("Genre", album.genre)
                DetailLine("Format", album.mediaType)
                DetailLine("Barcode", album.barcode)
                if (songs.isNotEmpty()) {
                    Text(
                        text = "Songs",
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
