package pe.aido.cuadre.core

import pe.aido.cuadre.domain.Wallet

/** Wallets send names in caps ("ROSA HUAMAN"); show and speak them in name case. */
fun displayName(raw: String): String =
    raw.trim().lowercase().split(Regex("\\s+")).joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } }

/** "Plin (BBVA)" → "Plin": what a person says out loud or reads in a compact row. */
val Wallet.shortName: String get() = displayName.substringBefore(" (")
