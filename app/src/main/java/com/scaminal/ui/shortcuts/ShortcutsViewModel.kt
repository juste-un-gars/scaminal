/**
 * @file ShortcutsViewModel.kt
 * @description ViewModel pour la gestion des raccourcis de commandes SSH.
 */
package com.scaminal.ui.shortcuts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scaminal.data.entity.CommandShortcut
import com.scaminal.data.repository.ShortcutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShortcutsViewModel @Inject constructor(
    private val repository: ShortcutRepository
) : ViewModel() {

    val shortcuts: StateFlow<List<CommandShortcut>> = repository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun add(label: String, command: String) {
        viewModelScope.launch { repository.add(label, command) }
    }

    fun update(shortcut: CommandShortcut) {
        viewModelScope.launch { repository.update(shortcut) }
    }

    fun delete(shortcut: CommandShortcut) {
        viewModelScope.launch { repository.delete(shortcut) }
    }
}
