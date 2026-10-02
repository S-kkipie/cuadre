package pe.aido.cuadre.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "payments")
data class PaymentEntity(
    @PrimaryKey val id: String,          // VerificationEngine.idFor — idempotent insert
    val wallet: String,
    val amount: Double,
    val counterparty: String?,
    val postedAtMillis: Long,
    val rawText: String,
    val securityCode: String? = null,    // schema v2
)

@Dao
interface PaymentDao {
    /** IGNORE on conflict = duplicate notifications for the same payment never double-count. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(payment: PaymentEntity): Long

    @Query("SELECT * FROM payments WHERE postedAtMillis BETWEEN :from AND :to ORDER BY postedAtMillis DESC")
    suspend fun between(from: Long, to: Long): List<PaymentEntity>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM payments WHERE postedAtMillis BETWEEN :from AND :to")
    suspend fun totalBetween(from: Long, to: Long): Double

    /** Live view for the UI; re-emits whenever a payment is inserted. */
    @Query("SELECT * FROM payments WHERE postedAtMillis BETWEEN :from AND :to ORDER BY postedAtMillis DESC")
    fun observeBetween(from: Long, to: Long): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments ORDER BY postedAtMillis DESC")
    fun observeAll(): Flow<List<PaymentEntity>>

    /**
     * Only hand-entered cash can be deleted. Listener-proven Yape/Plin payments are immutable,
     * so nobody at the till can make a real payment disappear.
     */
    @Query("DELETE FROM payments WHERE id = :id AND wallet = 'EFECTIVO'")
    suspend fun deleteCash(id: String): Int
}

/** One "cuadre de caja" per business day (latest close wins). */
@Entity(tableName = "day_closes")
data class DayCloseEntity(
    @PrimaryKey val date: String,        // ISO local date, "2026-10-02"
    val openingCash: Double,
    val countedCash: Double,
    val expectedCash: Double,
    val salesTotal: Double,
    val closedAtMillis: Long,
)

@Dao
interface DayCloseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(close: DayCloseEntity)

    @Query("SELECT * FROM day_closes ORDER BY date DESC")
    fun observeAll(): Flow<List<DayCloseEntity>>

    @Query("SELECT * FROM day_closes ORDER BY date DESC LIMIT 1")
    suspend fun latest(): DayCloseEntity?
}

@Database(entities = [PaymentEntity::class, DayCloseEntity::class], version = 3, exportSchema = false)
abstract class CuadreDatabase : RoomDatabase() {
    abstract fun paymentDao(): PaymentDao
    abstract fun dayCloseDao(): DayCloseDao
}

/** v2 -> v3: daily cash closes. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS day_closes (" +
                "date TEXT NOT NULL PRIMARY KEY, openingCash REAL NOT NULL, countedCash REAL NOT NULL, " +
                "expectedCash REAL NOT NULL, salesTotal REAL NOT NULL, closedAtMillis INTEGER NOT NULL)",
        )
    }
}

/** v1 -> v2: Yape security code. Keeps payments already captured. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE payments ADD COLUMN securityCode TEXT")
    }
}
