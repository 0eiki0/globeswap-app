package tech.eiki.globeswap.data

/** Merkmale eines gesendeten Fotos für die Erfolge; gehen als Header an den Server. */
data class PhotoInfo(
    /** Beim Tagesthema mitgemacht. */
    val theme: Boolean = false,
    /** Mit Text, Zeichnung oder Sticker. */
    val edited: Boolean = false,
    /** Benutzter Filter (Name aus PhotoFilter), null ohne Filter. */
    val filter: String? = null,
    /** Mit der Ultraweitwinkel-Kamera aufgenommen. */
    val wide: Boolean = false,
) {
    fun headers(): Map<String, String> = buildMap {
        if (theme) put("X-Photo-Theme", "1")
        if (edited) put("X-Photo-Edited", "1")
        filter?.let { put("X-Photo-Filter", it) }
        if (wide) put("X-Photo-Wide", "1")
    }
}
