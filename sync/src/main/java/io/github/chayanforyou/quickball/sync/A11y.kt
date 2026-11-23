package io.github.chayanforyou.quickball.sync

import android.accessibilityservice.AccessibilityService

/**
 * Bridge to the host's accessibility service.
 *
 * The module ships no service of its own: QuickBallService is already an
 * AccessibilityService, and it is the only place screen, node tree and input
 * can be done from anyway. The host calls [attach] when it connects and
 * [detach] when it goes away; the lifetime of sync follows the ball.
 */
object A11y {

    @Volatile
    var service: AccessibilityService? = null
        private set

    @Volatile
    var currentPackage: String? = null
        private set

    fun attach(svc: AccessibilityService) {
        service = svc
        SyncBoot.onA11yConnected(svc)
    }

    fun detach(svc: AccessibilityService) {
        if (service === svc) service = null
        SyncBoot.onA11yGone()
    }

    fun note(pkg: String?) {
        currentPackage = pkg
    }
}
