package io.github.chayanforyou.quickball.sync

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Engine start/stop.
 *
 * The engine no longer hangs off the host's accessibility service: it runs in a
 * foreground service of its own, so the ball can be toggled and the screen can
 * go off without dropping the channel. The accessibility service is still what
 * supplies node trees, taps and screenshots, but it is no longer the lifetime.
 */
object SyncBoot {

    @Volatile
    private var engine: SyncEngine? = null

    /** Sync switch flipped on. */
    fun start(ctx: Context) {
        if (!SyncState.enabled(ctx)) return
        val app = ctx.applicationContext
        runCatching { ContextCompat.startForegroundService(app, Intent(app, SyncService::class.java)) }
            .onFailure { t ->
                // Android 12+ refuses a foreground start from the background in some
                // states. The process is alive either way, so run in it instead.
                Log.w(TAG, "fgs start refused: ${t.message}")
                ensureEngine(app)
            }
    }

    fun stop(ctx: Context?) {
        val app = ctx?.applicationContext
        if (app != null) runCatching { app.stopService(Intent(app, SyncService::class.java)) }
        releaseEngine(app)
    }

    /** Called by SyncService and as a fallback when a foreground start is refused. */
    @Synchronized
    fun ensureEngine(ctx: Context) {
        if (engine != null) return
        engine = SyncEngine(ctx.applicationContext).also { it.start() }
    }

    @Synchronized
    fun releaseEngine(@Suppress("UNUSED_PARAMETER") ctx: Context?) {
        engine?.shutdown()
        engine = null
    }

    /** Called by the provider and the wake receiver. */
    fun startIfReady() {
        val app = SyncInitProvider.appCtx ?: return
        start(app)
    }

    /** The ball was switched on: make sure the channel is up even if the switch was off. */
    internal fun onA11yConnected(svc: android.accessibilityservice.AccessibilityService) {
        if (!SyncState.enabled(svc)) return
        start(svc)
    }

    /** The ball went off. The foreground service keeps the channel, so nothing to do. */
    internal fun onA11yGone() = Unit

    private const val TAG = "QbBoot"
}
