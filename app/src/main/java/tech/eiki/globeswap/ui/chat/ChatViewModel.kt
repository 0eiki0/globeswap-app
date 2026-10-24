package tech.eiki.globeswap.ui.chat

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import tech.eiki.globeswap.data.ConversationDetail
import tech.eiki.globeswap.data.ReportReason
import tech.eiki.globeswap.data.ServiceLocator
import tech.eiki.globeswap.data.userMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    private val repo = ServiceLocator.repository
    private val conversationId: String = checkNotNull(savedStateHandle["conversationId"])

    /** Loading = noch nichts geladen, Missing = gelöscht oder nicht (mehr) zugänglich. */
    sealed interface DetailState {
        data object Loading : DetailState
        data object Missing : DetailState
        data class Loaded(val detail: ConversationDetail) : DetailState
    }

    val detail: StateFlow<DetailState> = repo.observeConversation(conversationId)
        .map { if (it == null) DetailState.Missing else DetailState.Loaded(it) }
        .catch { e -> Log.e(TAG, "Unterhaltung", e) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailState.Loading)

    fun photoUrl(photoId: String) = repo.photoUrl(photoId)

    private val _sending = MutableStateFlow(false)
    val sending: StateFlow<Boolean> = _sending.asStateFlow()

    private val _toasts = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val toasts: SharedFlow<String> = _toasts.asSharedFlow()

    fun reply(jpeg: ByteArray) = launchSafely("Could not send") {
        _sending.value = true
        try {
            repo.reply(conversationId, jpeg)
        } finally {
            _sending.value = false
        }
    }

    fun markSeen() = launchSafely(null) { repo.markSeen(conversationId) }

    fun report(reason: ReportReason, messageId: String?, alsoBlock: Boolean, onBlocked: () -> Unit) =
        launchSafely("Could not report") {
            repo.report(conversationId, messageId, reason)
            if (alsoBlock) {
                repo.block(conversationId)
                onBlocked()
            }
            _toasts.emit("Thanks! Your report has been sent.")
        }

    fun block(onBlocked: () -> Unit) = launchSafely("Could not block") {
        repo.block(conversationId)
        _toasts.emit("Person blocked.")
        onBlocked()
    }

    fun deleteChat(onDeleted: () -> Unit) = launchSafely("Could not delete chat") {
        repo.deleteConversation(conversationId)
        _toasts.emit("Chat deleted.")
        onDeleted()
    }

    fun deletePhoto(messageId: String) = launchSafely("Could not delete photo") {
        repo.deleteMessage(conversationId, messageId)
        _toasts.emit("Photo deleted.")
    }

    /** Reaktion auf ein Foto des Gegenübers setzen; dasselbe Emoji noch einmal entfernt sie. */
    fun react(messageId: String, emoji: String) = launchSafely("Could not react") {
        repo.react(conversationId, messageId, emoji)
    }

    fun endChat() = launchSafely("Could not end chat") {
        repo.endConversation(conversationId)
        _toasts.emit("Chat ended.")
    }

    private fun launchSafely(errorMessage: String?, block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, errorMessage ?: "Fehler", e)
                if (errorMessage != null) _toasts.emit("$errorMessage: ${e.userMessage()}")
            }
        }
    }

    private companion object {
        const val TAG = "ChatViewModel"
    }
}
