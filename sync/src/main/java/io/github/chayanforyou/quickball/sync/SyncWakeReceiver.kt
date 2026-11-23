package io.github.chayanforyou.quickball.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Nudges the engine after a package update and after unlock.
 *
 * Nothing is started here: a receiver can't start a service in the background on
 * newer Android without a foreground notification, so the engine only reconnects
 * if the accessibility service is already up. BOOT_COMPLETED is deliberately not
 * handled either — after a reboot nothing works until the first unlock.
 */
class SyncWakeReceiver : BroadcastReceiver() {

    override fun onReceive(ctx: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_USER_PRESENT, Intent.ACTION_MY_PACKAGE_REPLACED ->
                runCatching { SyncBoot.startIfReady() }
        }
    }
}
