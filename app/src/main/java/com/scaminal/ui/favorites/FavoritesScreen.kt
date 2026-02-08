/**
 * @file FavoritesScreen.kt
 * @description Écran des hôtes favoris sauvegardés en base de données.
 */
package com.scaminal.ui.favorites

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.scaminal.network.model.Host

@Composable
fun FavoritesScreen(
    onNavigateToTerminal: (String) -> Unit = {},
    viewModel: FavoritesViewModel = hiltViewModel()
) {
    val favorites by viewModel.favorites.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Favoris (${favorites.size})",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (favorites.isEmpty()) {
            FavoritesEmptyState()
        } else {
            FavoritesList(
                favorites = favorites,
                onRemoveFavorite = viewModel::removeFavorite,
                onDelete = viewModel::deleteHost,
                onTap = onNavigateToTerminal
            )
        }
    }
}

@Composable
private fun FavoritesEmptyState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "Aucun favori\nMarquez des hôtes depuis le scanner",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun FavoritesList(
    favorites: List<Host>,
    onRemoveFavorite: (String) -> Unit,
    onDelete: (String) -> Unit,
    onTap: (String) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(favorites, key = { it.ipAddress }) { host ->
            FavoriteItem(
                host = host,
                onRemoveFavorite = { onRemoveFavorite(host.ipAddress) },
                onDelete = { onDelete(host.ipAddress) },
                onTap = { onTap(host.ipAddress) }
            )
        }
    }
}

@Composable
private fun FavoriteItem(
    host: Host,
    onRemoveFavorite: () -> Unit,
    onDelete: () -> Unit,
    onTap: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onTap)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = host.ipAddress,
                    style = MaterialTheme.typography.titleMedium
                )
                host.hostname?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (host.openPorts.isNotEmpty()) {
                    Text(
                        text = host.openPorts.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            IconButton(onClick = onRemoveFavorite) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Retirer des favoris",
                    tint = MaterialTheme.colorScheme.error
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Supprimer",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
