package tech.eiki.globeswap.data

import java.io.IOException
import tech.eiki.globeswap.R
import tech.eiki.globeswap.util.appString

/** Verständliche Fehlermeldung für die Oberfläche. */
fun Throwable.userMessage(): String = when (this) {
    is ApiException -> when (code) {
        "banned" -> appString(R.string.err_banned)
        "update_required" -> appString(R.string.err_update_required)
        "too_many_waiting" -> appString(R.string.err_too_many_waiting)
        "rate_limited" -> appString(R.string.err_rate_limited)
        "invalid_image" -> appString(R.string.err_invalid_image)
        "blocked" -> appString(R.string.err_blocked)
        "not_yours" -> appString(R.string.err_not_yours)
        "invalid_name" -> appString(R.string.err_invalid_name)
        "invalid_code" -> appString(R.string.err_invalid_code)
        "ended" -> appString(R.string.err_ended)
        "not_your_turn" -> appString(R.string.err_not_your_turn)
        "not_found" -> appString(R.string.err_not_found)
        "rejected_content" -> appString(R.string.err_rejected_content)
        else -> appString(R.string.err_server, status)
    }
    is IOException -> appString(R.string.err_no_connection)
    else -> localizedMessage ?: appString(R.string.err_unknown)
}
