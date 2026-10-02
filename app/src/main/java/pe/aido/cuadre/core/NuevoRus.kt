package pe.aido.cuadre.core

/**
 * Nuevo RUS (SUNAT) income thresholds, 2026: category 1 up to S/ 5,000 a month (quota S/ 20),
 * category 2 up to S/ 8,000 (quota S/ 50), and at most S/ 96,000 a year.
 * Source: emprender.sunat.gob.pe — Nuevo Régimen Único Simplificado. Reference only; the
 * merchant's accountant has the last word (purchases and assets also count for the regime).
 */
object NuevoRus {
    const val CATEGORY_1_MONTHLY = 5_000.0
    const val CATEGORY_2_MONTHLY = 8_000.0
    const val ANNUAL_LIMIT = 96_000.0
    const val CATEGORY_1_QUOTA = 20.0
    const val CATEGORY_2_QUOTA = 50.0
    private const val NEAR = 0.8

    data class Status(
        val category: Int,              // 1 or 2; 0 when over the regime's monthly limit
        val categoryLimit: Double,
        val quota: Double,
        val monthIncome: Double,
        val yearIncome: Double,
        val nearLimit: Boolean,         // ≥ 80% of the current category's monthly limit
        val overMonthly: Boolean,
        val nearAnnual: Boolean,
        val overAnnual: Boolean,
    )

    fun status(monthIncome: Double, yearIncome: Double): Status {
        val category = when {
            monthIncome <= CATEGORY_1_MONTHLY -> 1
            monthIncome <= CATEGORY_2_MONTHLY -> 2
            else -> 0
        }
        val limit = if (category == 1) CATEGORY_1_MONTHLY else CATEGORY_2_MONTHLY
        return Status(
            category = category,
            categoryLimit = limit,
            quota = if (category == 1) CATEGORY_1_QUOTA else CATEGORY_2_QUOTA,
            monthIncome = monthIncome,
            yearIncome = yearIncome,
            nearLimit = category != 0 && monthIncome >= limit * NEAR,
            overMonthly = monthIncome > CATEGORY_2_MONTHLY,
            nearAnnual = yearIncome >= ANNUAL_LIMIT * NEAR,
            overAnnual = yearIncome > ANNUAL_LIMIT,
        )
    }
}
