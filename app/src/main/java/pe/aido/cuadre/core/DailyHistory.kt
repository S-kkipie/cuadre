package pe.aido.cuadre.core

import pe.aido.cuadre.core.VerificationEngine.ConfirmedPayment
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Groups confirmed payments into business days (local calendar days), newest first. */
object DailyHistory {

    data class Day(val date: LocalDate, val total: Double, val count: Int)

    fun byDay(payments: List<ConfirmedPayment>, zone: ZoneId): List<Day> =
        payments
            .groupBy { Instant.ofEpochMilli(it.event.postedAtMillis).atZone(zone).toLocalDate() }
            .map { (date, list) -> Day(date, VerificationEngine.total(list), list.size) }
            .sortedByDescending { it.date }
}
