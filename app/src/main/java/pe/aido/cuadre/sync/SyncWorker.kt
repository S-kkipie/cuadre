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
import pe.aido.cuadre.cuadre
import java.util.concurrent.TimeUnit

/**
 * Shares payments captured on this phone with the store, then catches up on the payments the
 * store's other phones confirmed (missed pushes, or from before this phone joined). Runs when there's a connection, retries
 * with backoff, and survives restarts — a payment confirmed with no signal still reaches the owner.
 */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext.cuadre
        val link = app.storeLink.link.value ?: return Result.success()
        val api = CuadreApi()
        if (!api.configured) return Result.success()

        for (payment in app.repository.pendingUpload(since = link.linkedAt)) {
            when (val r = api.upload(link.token, payment)) {
                is CuadreApi.Result.Ok -> app.repository.markUploaded(payment.id)
                is CuadreApi.Result.Rejected -> {
                    // 401: the owner revoked this phone. Stop sharing instead of retrying forever.
                    if (r.status == 401) app.storeLink.clear()
                    return Result.success()
                }
                is CuadreApi.Result.Failed -> return Result.retry()
            }
        }

        // Catch-up is quiet: these are old by definition, so no sound and no confirmation screen.
        val since = System.currentTimeMillis() - CATCH_UP_MILLIS
        when (val r = api.storePayments(link.token, since)) {
            is CuadreApi.Result.Ok -> r.value.forEach { app.repository.insertRemote(it) }
            is CuadreApi.Result.Rejected -> if (r.status == 401) app.storeLink.clear()
            is CuadreApi.Result.Failed -> return Result.retry()
        }
        return Result.success()
    }

    companion object {
        private const val NAME = "cuadre-sync"
        /** History is shown month by month; a phone that joins mid-month gets the whole month. */
        private const val CATCH_UP_MILLIS = 35L * 24 * 60 * 60 * 1000

        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
                .build()
            // APPEND_OR_REPLACE: a payment arriving mid-run still gets its own pass.
            WorkManager.getInstance(context).enqueueUniqueWork(NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
        }
    }
}
