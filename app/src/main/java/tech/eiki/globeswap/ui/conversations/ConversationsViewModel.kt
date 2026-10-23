package tech.eiki.globeswap.ui.conversations

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import tech.eiki.globeswap.data.Conversation
import tech.eiki.globeswap.data.ServiceLocator
import tech.eiki.globeswap.data.userMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ConversationsViewModel : ViewModel() {
    private val repo = ServiceLocator.repository

    val conversations: StateFlow<List<Conversation>> = repo.observeConversations()
        .map { list -> list.filterNot { it.blocked } }
        .catch { e -> Log.e("ConversationsViewModel", "Laden fehlgeschlagen", e); emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun photoUrl(photoId: String) = repo.photoUrl(photoId)

    private val _publicId = MutableStateFlow<String?>(null)
    val publicId: StateFlow<String?> = _publicId.asStateFlow()

    fun loadPublicId() {
        if (_publicId.value != null) return
        viewModelScope.launch {
            runCatching { repo.publicId() }
                .onSuccess { _publicId.value = it }
                .onFailure { Log.w("ConversationsViewModel", "UUID nicht geladen", it) }
        }
    }

    fun deleteAccount(onResult: (error: String?) -> Unit) {
        viewModelScope.launch {
            val error = runCatching { repo.deleteAccount() }.exceptionOrNull()
            if (error == null) _publicId.value = null
            onResult(error?.userMessage())
        }
    }
}
