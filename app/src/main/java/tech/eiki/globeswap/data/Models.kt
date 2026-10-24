package tech.eiki.globeswap.data

import kotlinx.serialization.Serializable

@Serializable
data class Conversation(
    val id: String,
    /** Eindeutige öffentliche UUID der anderen Person, z. B. "3f2a1c9e-7b4d-4e0a-9c1f-2d8e6b5a4c3f". */
    val label: String,
    val lastMessageAt: Long,
    val lastFromMe: Boolean,
    val lastPhotoId: String,
    val unread: Boolean,
    val blocked: Boolean,
)

@Serializable
data class Message(
    val id: String,
    val mine: Boolean,
    val photoId: String,
    val createdAt: Long,
)

@Serializable
data class ConversationDetail(
    val conversation: Conversation,
    val messages: List<Message>,
    /** So viele neueste Fotos behält der Server pro Unterhaltung (0 = alle). */
    val maxPhotos: Int = 0,
)

sealed interface SwapResult {
    /** Ein wartendes Foto eines Fremden wurde gefunden – die Unterhaltung existiert bereits. */
    data class Matched(val conversationId: String) : SwapResult

    /** Niemand wartet gerade – das eigene Foto liegt im Pool, bis jemand anderes swappt. */
    data class Waiting(val photoId: String) : SwapResult
}

@Serializable
internal data class SwapResponse(val status: String, val photoId: String, val conversationId: String? = null)

@Serializable
internal data class RegisterResponse(val userId: String, val token: String, val publicId: String = "")

@Serializable
internal data class MeResponse(val publicId: String)

@Serializable
internal data class ServerEvent(val type: String, val conversationId: String? = null, val photoId: String? = null)

enum class ReportReason(val label: String) {
    NUDITY("Nudity / sexual content"),
    VIOLENCE("Violence / disturbing content"),
    HARASSMENT("Harassment / hate"),
    SPAM("Spam / advertising"),
    OTHER("Something else"),
}
