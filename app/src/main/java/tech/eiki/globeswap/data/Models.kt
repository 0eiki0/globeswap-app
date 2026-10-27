package tech.eiki.globeswap.data

import androidx.annotation.StringRes
import kotlinx.serialization.Serializable
import tech.eiki.globeswap.R

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
    /** Land der anderen Person (ISO-Code), wenn sie es teilt. */
    val country: String = "",
    /** Freiwilliger Name der anderen Person ("" = keiner). */
    val name: String = "",
    /** Beendet: keine neuen Fotos mehr. */
    val ended: Boolean = false,
    val endedByMe: Boolean = false,
)

/** Name der anderen Person zum Anzeigen: ihr freiwilliger Name oder "Stranger". */
/** Name der anderen Person oder [stranger] („Stranger“ in der Sprache der App), wenn sie keinen gesetzt hat. */
fun Conversation.displayName(stranger: String): String = name.ifBlank { stranger }

@Serializable
data class Message(
    val id: String,
    val mine: Boolean,
    val photoId: String,
    val createdAt: Long,
    /** Das Foto wurde gelöscht – an seiner Stelle steht nur ein Hinweis. */
    val deleted: Boolean = false,
    /** Herkunftsland (ISO-Code), wenn die Person es teilt. */
    val country: String = "",
    /** Emoji-Reaktion des Gegenübers auf dieses Foto. */
    val reaction: String = "",
)

/** Einstellungen der Person auf dem Server. */
@Serializable
data class UserSettings(
    val shareCountry: Boolean = true,
    val matchPref: String = MatchPref.ANY,
    /** Das gerade erkannte eigene Land (nur wenn geteilt). */
    val country: String = "",
    /** Der Server kennt überhaupt Länder. */
    val geoEnabled: Boolean = false,
    /** Es gibt einen Wiederherstellungs-Code für dieses Konto. */
    val hasRecoveryCode: Boolean = false,
    /** Eigener freiwilliger Name ("" = keiner). */
    val displayName: String = "",
)

object MatchPref {
    const val ANY = "any"
    const val NEAR = "near"
    const val FAR = "far"
}

@Serializable
data class WorldCountry(val country: String, val continent: String = "", val photos: Int = 0, val firstAt: Long = 0)

@Serializable
data class World(
    val countries: List<WorldCountry> = emptyList(),
    val continents: Int = 0,
    /** Anonymer Vergleich: so viel Prozent der aktiven Nutzer haben weniger Länder (null = zu wenige zum Vergleichen). */
    val percentile: Int? = null,
    /** Werte für die Erfolge (Serie, Tagesthema, Zähler, Merkmale). */
    val achievements: AchievementData = AchievementData(),
)

@Serializable
data class AchievementData(
    /** Eigenes Land (aus der IP), für „Nachbarschaft“ und „Antipode“. */
    val home: String = "",
    val createdAt: Long = 0,
    val streak: Int = 0,
    val bestStreak: Int = 0,
    val themeDays: Int = 0,
    /** z. B. night, sunrise, otherday, chat_photos, chat_days, reactions, edited, mix */
    val stats: Map<String, Long> = emptyMap(),
    /** z. B. "tz:60", "filter:VIVID", "wide" */
    val flags: List<String> = emptyList(),
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
internal data class RecoveryCodeResponse(val code: String)

@Serializable
internal data class RestoreResponse(val userId: String, val token: String, val publicId: String = "", val code: String)

@Serializable
internal data class MeResponse(val publicId: String)

@Serializable
internal data class ServerEvent(val type: String, val conversationId: String? = null, val photoId: String? = null)

enum class ReportReason(@StringRes val label: Int) {
    NUDITY(R.string.reason_nudity),
    VIOLENCE(R.string.reason_violence),
    HARASSMENT(R.string.reason_harassment),
    SPAM(R.string.reason_spam),
    OTHER(R.string.reason_other),
}
