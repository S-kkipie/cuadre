package pe.aido.cuadre

import android.app.Application
import android.content.Context
import androidx.room.Room
import pe.aido.cuadre.data.CuadreDatabase
import pe.aido.cuadre.data.PaymentRepository

class CuadreApp : Application() {
    val database: CuadreDatabase by lazy {
        Room.databaseBuilder(this, CuadreDatabase::class.java, "cuadre.db").build()
    }
    val repository: PaymentRepository by lazy { PaymentRepository(database.paymentDao()) }
}

val Context.repository: PaymentRepository
    get() = (applicationContext as CuadreApp).repository
