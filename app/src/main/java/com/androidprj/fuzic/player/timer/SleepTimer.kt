package com.androidprj.fuzic.player.timer

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Pure-Kotlin coroutine helper that delays for a configurable number of
 * minutes, then invokes [onExpire] on the supplied [scope].
 *
 * ## Why a dedicated helper?
 *
 * Sleep timers have one quirky requirement that the rest of the
 * repository does not: they must be *replaceable* without leaking
 * coroutines, but a normal `delay()` + `cancel()` pair is awkward to
 * expose as a one-shot API. [SleepTimer] wraps that pair with
 *
 * - a single mutable [Job] slot so [start] always supersedes any prior
 *   timer,
 * - an optional [onExpireResult] callback the repository uses to report
 *   success/failure of the expiration callback, and
 * - an [isActive] flag for UI surfaces that need to render a countdown.
 *
 * ## Thread safety
 *
 * All state lives in a single mutable [Job] reference. Reads and writes
 * happen on the caller's thread, but cancellation is mediated through
 * Job's existing single-thread-safe machinery. Concurrent [start] /
 * [cancel] calls cannot leak coroutines; whichever call lands last
 * wins.
 *
 * ## Lifecycle
 *
 * The supplied [scope] should outlive every [start] invocation. When
 * that scope is cancelled, the active timer is cancelled automatically
 * — the repository passes its process-scoped
 * `CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)` here.
 *
 * ## Imports
 *
 * Intentionally Android-free and Media3-free. This class is unit-testable
 * on the plain JVM and reused by tests via [kotlinx.coroutines.test].
 */
class SleepTimer(
    private val scope: CoroutineScope,
    private val onExpire: suspend () -> Unit,
) {

    // Single mutable slot — replacing it cancels the previous job. Job is
    // a reference type, so reads do not need any extra synchronization
    // beyond the JVM's own happens-before guarantees for the caller's
    // thread: SleepTimer is only ever touched from Dispatchers.Main
    // because every caller in this repo is a transport command or the
    // repository init block, both of which hop to Main first.
    private var currentJob: Job? = null

    /**
     * Schedules a timer that fires after [minutes] minutes. Replaces any
     * timer previously started via this instance.
     *
     * Binary outcomes:
     *
     * - `minutes == null` or `<= 0`: any prior timer is cancelled and
     *   the [onExpire] callback is **not** invoked. [onExpireResult] is
     *   still called with [Result.success] so the caller can clear its
     *   own "timer armed" UI affordance.
     * - `minutes > 0`: any prior timer is cancelled and a new job is
     *   launched on [scope] that delays `minutes * 60_000L` then
     *   invokes [onExpire]. The result of that invocation is reported
     *   through [onExpireResult].
     *
     * @param minutes Target duration in minutes. `null` or non-positive
     *  values cancel without firing.
     * @param onExpireResult Receives the outcome of the [onExpire]
     *  invocation. Only invoked on success paths or on
     *  expiration-triggered failures; cancellation throws are swallowed
     *  and not reported because the caller already knows the timer was
     *  cancelled.
     */
    fun start(
        minutes: Int?,
        onExpireResult: (Result<Unit>) -> Unit = {},
    ) {
        // Cancel any prior timer before we even look at the new minutes
        // value — a 0/null input is the canonical "off" path.
        currentJob?.cancel()
        currentJob = null

        if (minutes == null || minutes <= 0) {
            // Clear state. Do NOT invoke onExpire; this is the "cancel"
            // path. We still report success through onExpireResult so the
            // caller's UI affordance clears without an extra signal.
            onExpireResult(Result.success(Unit))
            return
        }

        val delayMillis = minutes.toLong() * 60_000L
        currentJob = scope.launch {
            // delay() suspension alone is the cancellation point: when
            // this job is cancelled (by [cancel] or by a [start] that
            // supersedes it), the delay throws CancellationException
            // and we re-throw it below without ever invoking
            // [onExpire]. No racing withTimeoutOrNull needed.
            try {
                delay(delayMillis)
                onExpire()
                onExpireResult(Result.success(Unit))
            } catch (cancellation: CancellationException) {
                // Honour structured cancellation — never swallow it.
                throw cancellation
            } catch (t: Throwable) {
                onExpireResult(Result.failure(t))
            }
        }
    }

    /**
     * Cancels the active timer, if any. Safe to call when no timer is
     * armed (acts as a no-op).
     */
    fun cancel() {
        currentJob?.cancel()
        currentJob = null
    }

    /**
     * `true` when a timer is currently armed (i.e. a non-null [Job] is
     * stored). The value may lag a fraction behind [cancel] in
     * concurrent settings; the repository never relies on this getter
     * for control flow.
     */
    val isActive: Boolean
        get() = currentJob?.isActive == true
}
