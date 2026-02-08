/**
 * @file FavoritesViewModel.kt
 * @description ViewModel pour l'écran des favoris. Expose la liste des hôtes favoris depuis Room.
 */
package com.scaminal.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scaminal.data.repository.HostRepository
import com.scaminal.network.model.Host
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val hostRepository: HostRepository
) : ViewModel() {

    /** Flow réactif des hôtes marqués comme favoris. */
    val favorites: StateFlow<List<Host>> = hostRepository.getFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Retire un hôte des favoris (toggle isFavorite → false). */
    fun removeFavorite(ipAddress: String) {
        viewModelScope.launch {
            hostRepository.toggleFavorite(ipAddress)
            Timber.d("Favorite removed: %s", ipAddress)
        }
    }

    /** Supprime un hôte de la base de données. */
    fun deleteHost(ipAddress: String) {
        viewModelScope.launch {
            hostRepository.deleteByIp(ipAddress)
            Timber.d("Host deleted from favorites: %s", ipAddress)
        }
    }
}
