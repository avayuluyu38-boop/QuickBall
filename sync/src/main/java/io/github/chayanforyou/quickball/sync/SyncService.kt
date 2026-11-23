package io.github.chayanforyou.quickball.sync

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder

/**
 * Keeps the channel alive on its own.
 *
 * The bridge variant could rely on the ball's accessibility service holding the
 * process, but that ties sync to the ball being switched on and to the system
 * keeping a bound service. A foreground service is the boring, dependable
 * version: it survives the ball being toggled, the screen going off and the
 * process being trimmed.
 *
 * Type is strictly dataSync: other types pull in extra permissions and must
 * match the manifest.
 */
class SyncService : Service() {

    private val channelId = "quickball-sync"

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
        startForegroundCompat()
        SyncBoot.ensureEngine(this)
    }

    override fun onDestroy() {
        SyncBoot.releaseEngine(this)
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    private fun label(): String =
        runCatching { applicationInfo.loadLabel(packageManager).toString() }.getOrDefault("Quick Ball")

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (nm.getNotificationChannel(channelId) != null) return
        val ch = NotificationChannel(channelId, "Sync", NotificationManager.IMPORTANCE_MIN)
        ch.setShowBadge(false)
        ch.setSound(null, null)
        nm.createNotificationChannel(ch)
    }

    private fun startForegroundCompat() {
        val n: Notification =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                Notification.Builder(this, channelId)
                    .setContentTitle(label())
                    .setContentText("Sync is on")
                    .setSmallIcon(android.R.drawable.stat_notify_sync)
                    .setOngoing(true)
                    .setShowWhen(false)
                    .build()
            else
                @Suppress("DEPRECATION")
                Notification.Builder(this)
                    .setContentTitle(label())
                    .setContentText("Sync is on")
                    .setSmallIcon(android.R.drawable.stat_notify_sync)
                    .setOngoing(true)
                    .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIF_ID, n)
        }
    }

    companion object {
        private const val NOTIF_ID = 0x5157
    }
}
