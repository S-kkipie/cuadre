package pe.aido.cuadre.core

/** What a cashier types: "2.50", "2,50", "S/ 15", "1250". Null if it isn't a positive amount. */
fun parseSoles(input: String): Double? {
    val t = input.trim().removePrefix("S/").trim().replace(" ", "")
    if (t.isEmpty()) return null
    val normalized = when {
        // "1,250.50" → thousands comma; "1.250,50" → thousands dot.
        t.contains(',') && t.contains('.') ->
            if (t.lastIndexOf('.') > t.lastIndexOf(',')) t.replace(",", "") else t.replace(".", "").replace(",", ".")
        t.contains(',') -> t.replace(",", ".")
        else -> t
    }
    val v = normalized.toDoubleOrNull() ?: return null
    return if (v > 0 && v < 1_000_000) Math.round(v * 100) / 100.0 else null
}
