package com.example.vinylog.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.vinylog.games.GameSystem
import com.example.vinylog.games.GameSystems
import com.example.vinylog.ui.GameCartridge
import com.example.vinylog.ui.theme.VinyLogTheme

class GameLibraryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VinyLogTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .systemBarsPadding(),
                    ) {
                        Text(
                            text = "Video Games",
                            style = MaterialTheme.typography.headlineLarge,
                            modifier = Modifier.padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 4.dp),
                        )
                        Text(
                            text = "Select a system",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp),
                        )
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(GameSystems, key = { it.name }) { system ->
                                SystemRow(system) {
                                    startActivity(
                                        Intent(this@GameLibraryActivity, AlbumView::class.java)
                                            .putExtra("mt", system.name),
                                    )
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SystemRow(system: GameSystem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(64.dp), contentAlignment = Alignment.Center) {
            GameCartridge(
                modifier = Modifier.fillMaxSize(),
                labelColor = Color(system.labelColor),
            )
        }
        Text(
            text = system.name,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}
