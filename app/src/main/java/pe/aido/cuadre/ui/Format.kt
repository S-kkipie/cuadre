package pe.aido.cuadre.ui

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val esPE: Locale = Locale.forLanguageTag("es-PE")

/** "S/ 1,284.50" — the format Peruvian wallets and receipts use. */
fun soles(amount: Double): String = String.format(Locale.US, "S/ %,.2f", amount)

/** "1,284.50" — for list rows, where "S/" on every line is noise. */
fun plainAmount(amount: Double): String = String.format(Locale.US, "%,.2f", amount)

private val timeShort = DateTimeFormatter.ofPattern("HH:mm", esPE)
private val timeSeconds = DateTimeFormatter.ofPattern("HH:mm:ss", esPE)
private val dayShort = DateTimeFormatter.ofPattern("EEE d MMM", esPE)
private val dayLong = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", esPE)

private fun Long.zoned() = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault())

fun Long.hhmm(): String = zoned().format(timeShort)
fun Long.hhmmss(): String = zoned().format(timeSeconds)

/** "vie 2 oct" (Java gives "vie. 2 oct."; drop the dots). */
fun Long.shortDay(): String = zoned().format(dayShort).replace(".", "")
fun java.time.LocalDate.shortDay(): String = format(dayShort).replace(".", "")
fun java.time.LocalDate.longDay(): String = format(dayLong).replaceFirstChar { it.uppercase(esPE) }
