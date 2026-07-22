package com.androidprj.fuzic.player

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutionException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Process-scoped [MediaController] facade.
 *
 * Exposes only the controller surface the rest of the app needs — never the
 * underlying [androidx.media3.exoplayer.ExoPlayer] or
 * [androidx.media3.session.MediaSession]. All MediaController interactions
 * dispatch to [Dispatchers.Main] because Media3's controller API is
 * main-thread only.
 */
@Singleton
class PlayerController @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    // `lazy` defaults to SYNCHRONIZED, so the future is built exactly once
    // even under concurrent first-call contention.
    private val controllerFuture: Lazy<ListenableFuture<MediaController>> = lazy {
        val sessionToken = SessionToken(context, ComponentName(context, FuzicPlaybackService::class.java))
        MediaController.Builder(context, sessionToken)
            .setListener(NoOpListener)
            .buildAsync()
    }

    @Volatile
    private var released: Boolean = false

    /**
     * Awaits the connected [MediaController] and returns it.
     *
     * @throws IllegalStateException if [release] has already been called.
     */
    suspend fun controller(): MediaController = withContext(Dispatchers.Main) {
        check(!released) { "PlayerController has been released" }
        val deferred = CompletableDeferred<MediaController>()
        Futures.addCallback(
            controllerFuture.value,
            object : FutureCallback<MediaController> {
                override fun onSuccess(result: MediaController) {
                    deferred.complete(result)
                }

                override fun onFailure(t: Throwable) {
                    // Surface the underlying cause instead of Guava's
                    // ExecutionException wrapper.
                    val cause = (t as? ExecutionException)?.cause ?: t
                    deferred.completeExceptionally(cause)
                }
            },
            MoreExecutors.directExecutor(),
        )
        deferred.await()
    }

    /**
     * Cancels the pending connection future (if any). Idempotent; safe to
     * call multiple times.
     */
    suspend fun release() {
        withContext(Dispatchers.Main) {
            released = true
            if (controllerFuture.isInitialized()) {
                controllerFuture.value?.let { future ->
                    if (!future.isDone) {
                        MediaController.releaseFuture(future)
                    }
                }
            }
        }
    }

    suspend fun addListener(listener: Player.Listener): Unit = withContext(Dispatchers.Main) {
        controller().addListener(listener)
    }

    suspend fun removeListener(listener: Player.Listener): Unit = withContext(Dispatchers.Main) {
        controller().removeListener(listener)
    }

    private object NoOpListener : MediaController.Listener
}