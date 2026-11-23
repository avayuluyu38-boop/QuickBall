package io.github.chayanforyou.quickball.sync

import android.content.Context

/**
 * Engine start/stop, without a service of its own.
 *
 * Nothing is started until both conditions hold: sync is switched on and the
 * host's accessibility service is connected. The second one is what keeps the
 * process alive, so no foreground service and no notification are needed.
 */
object SyncBoot {

    @Volatile
    private var engine: SyncEngine? = null

    /** Sync switch flipped on, a11y already there. */
    fun start(ctx: Context) {
        if (!SyncState.enabled(ctx)) return
        if (engine != null) return
        engine = SyncEngine(ctx.applicationContext).also { it.start() }
    }

    fun stop() {
        engine?.shutdown()
        engine = null
    }

    /** Called by the provider and the wake receiver: start only if a11y is already up. */
    fun startIfReady() {
        val svc = A11y.service ?: return
        start(svc)
    }

    internal fun onA11yConnected(svc: android.accessibilityservice.AccessibilityService) {
        if (!SyncState.enabled(svc)) return
        start(svc)
    }

    internal fun onA11yGone() {
        stop()
    }
}
