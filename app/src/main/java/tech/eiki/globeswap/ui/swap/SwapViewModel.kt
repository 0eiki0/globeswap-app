package tech.eiki.globeswap.ui.swap

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import tech.eiki.globeswap.data.PhotoInfo
import tech.eiki.globeswap.data.ServiceLocator
import tech.eiki.globeswap.data.SwapResult
import tech.eiki.globeswap.data.userMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface SwapUiState {
    data object Capturing : SwapUiState
    data object Sending : SwapUiState
    data class Waiting(val photoId: String) : SwapUiState
    data class Matched(val conversationId: String) : SwapUiState
    class Failed(val message: String, val jpeg: ByteArray, val info: PhotoInfo) : SwapUiState
}

class SwapViewModel : ViewModel() {
    private val repo = ServiceLocator.repository
    private val _state = MutableStateFlow<SwapUiState>(SwapUiState.Capturing)
    val state: StateFlow<SwapUiState> = _state.asStateFlow()
    private var waitJob: Job? = null

    fun swap(jpeg: ByteArray, info: PhotoInfo = PhotoInfo()) {
        _state.value = SwapUiState.Sending
        viewModelScope.launch {
            try {
                when (val result = repo.swap(jpeg, info)) {
                    is SwapResult.Matched -> _state.value = SwapUiState.Matched(result.conversationId)
                    is SwapResult.Waiting -> {
                        _state.value = SwapUiState.Waiting(result.photoId)
                        awaitMatch(result.photoId)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Swap fehlgeschlagen", e)
                _state.value = SwapUiState.Failed(e.userMessage(), jpeg, info)
            }
        }
    }

    private fun awaitMatch(photoId: String) {
        waitJob?.cancel()
        waitJob = viewModelScope.launch {
            try {
                _state.value = SwapUiState.Matched(repo.observeMatch(photoId).filterNotNull().first())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Foto abgelaufen oder entfernt: wieder von vorn.
                Log.e(TAG, "Warten fehlgeschlagen", e)
                _state.value = SwapUiState.Capturing
            }
        }
    }

    /** Zieht das wartende Foto zurück; wurde es gerade getauscht, landet die Unterhaltung trotzdem in der Liste. */
    fun cancelWaiting() {
        val waiting = _state.value as? SwapUiState.Waiting ?: return
        waitJob?.cancel()
        _state.value = SwapUiState.Capturing
        viewModelScope.launch { runCatching { repo.cancelWaiting(waiting.photoId) } }
    }

    fun reset() {
        waitJob?.cancel()
        _state.value = SwapUiState.Capturing
    }

    private companion object {
        const val TAG = "SwapViewModel"
    }
}
