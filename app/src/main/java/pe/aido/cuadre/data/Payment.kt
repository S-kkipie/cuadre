package pe.aido.cuadre.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase

@Entity(tableName = "payments")
data class PaymentEntity(
    @PrimaryKey val id: String,          // VerificationEngine.idFor — idempotent insert
    val wallet: String,
    val amount: Double,
    val counterparty: String?,
    val postedAtMillis: Long,
    val rawText: String,
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
}

@Database(entities = [PaymentEntity::class], version = 1, exportSchema = false)
abstract class CuadreDatabase : RoomDatabase() {
    abstract fun paymentDao(): PaymentDao
}
