package pe.aido.cuadre.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.android.gms.tasks.Tasks
import com.google.firebase.messaging.FirebaseMessaging
import pe.aido.cuadre.cuadre
import java.util.concurrent.TimeUnit

/**
 * Tells the store which FCM token reaches this phone, so payments confirmed on the other phones
 * arrive here. Runs after pairing, on token rotation and on app start while linked; retries offline.
 */
class PushRegisterWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext.cuadre
        val link = app.storeLink.link.value ?: return Result.success()
        val api = CuadreApi()
        if (!api.configured) return Result.success()

        val fcmToken = try {
            Tasks.await(FirebaseMessaging.getInstance().token, 30, TimeUnit.SECONDS)
        } catch (_: Exception) {
            return Result.retry()
        }
        return when (val r = api.registerPush(link.token, fcmToken)) {
            is CuadreApi.Result.Ok -> Result.success()
            is CuadreApi.Result.Rejected -> {
                // 401: the owner unlinked this phone from the panel.
                if (r.status == 401) app.storeLink.clear()
                Result.success()
            }
            is CuadreApi.Result.Failed -> Result.retry()
        }
    }

    companion object {
        private const val NAME = "cuadre-push-register"

        fun enqueue(context: Context) {
            // Firebase stays dormant until a store is linked: no token exists before this.
            FirebaseMessaging.getInstance().isAutoInitEnabled = true
            val request = OneTimeWorkRequestBuilder<PushRegisterWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(NAME, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
