package tech.eiki.globeswap.data

import java.io.IOException

/** Verständliche Fehlermeldung für die Oberfläche. */
fun Throwable.userMessage(): String = when (this) {
    is ApiException -> when (code) {
        "banned" -> "Your account has been blocked because of reports."
        "update_required" -> "Please update GlobeSwap to continue."
        "too_many_waiting" -> "You already have several photos waiting for a swap. Please wait until they have been swapped."
        "rate_limited" -> "Not so fast – please wait a moment."
        "invalid_image" -> "The photo could not be processed."
        "blocked" -> "This chat is blocked."
        "not_yours" -> "You can only delete your own photos."
        "invalid_name" -> "This name isn't allowed. Use up to 24 letters or numbers – no links, emails or phone numbers."
        "invalid_code" -> "This backup code is not valid. Please check it and try again."
        "ended" -> "This chat has ended."
        "not_your_turn" -> "Wait for the stranger's photo before you reply."
        "not_found" -> "No longer available."
        else -> "Server error ($status)."
    }
    is IOException -> "No connection to the server."
    else -> localizedMessage ?: "Unknown error"
}
