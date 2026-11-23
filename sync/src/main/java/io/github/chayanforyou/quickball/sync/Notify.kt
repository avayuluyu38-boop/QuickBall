package io.github.chayanforyou.quickball.sync

import android.app.Notification
import android.view.accessibility.AccessibilityEvent

/**
 * Notification text from the accessibility stream.
 *
 * TYPE_NOTIFICATION_STATE_CHANGED carries the notification text in `event.text`
 * and, on most builds, the Notification itself in the parcelable data. That is
 * enough for codes and saves the user a second system toggle: the ball's service
 * is already granted everything this needs.
 */
object Notify {

    /** Same text often arrives twice (update + re-post). Keys live for a few minutes. */
    private val seen = LinkedHashMap<String, Long>()
    private const val KEEP_MS = 5 * 60_000L
    private const val KEEP_MAX = 64

    fun handle(e: AccessibilityEvent) {
        val pkg = e.packageName?.toString() ?: return
        if (pkg == SyncInitProvider.appCtx?.packageName) return

        android.util.Log.d("QbNotify", "evt $pkg text=${e.text?.size} data=${e.parcelableData?.javaClass?.simpleName}")
        val n = e.parcelableData as? Notification
        val extras = n?.extras
        val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            ?: e.text?.firstOrNull()?.toString()
        val text = extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: e.text?.joinToString("\n")

        if (title.isNullOrBlank() && text.isNullOrBlank()) {
            android.util.Log.d("QbNotify", "empty")
            return
        }
        if (!fresh(pkg, title.orEmpty(), text.orEmpty())) {
            android.util.Log.d("QbNotify", "dup")
            return
        }

        SyncNotificationLog.last = SyncNotificationLog.Entry(
            pkg, title.orEmpty(), text.orEmpty(), System.currentTimeMillis()
        )
        SyncEngine.instance?.sendNotification(pkg, title.orEmpty(), text.orEmpty())
    }

    private fun fresh(pkg: String, title: String, text: String): Boolean {
        val key = "$pkg|$title|$text"
        val now = System.currentTimeMillis()
        synchronized(seen) {
            val last = seen[key]
            if (last != null && now - last < KEEP_MS) return false
            seen[key] = now
            if (seen.size > KEEP_MAX) {
                val it = seen.entries.iterator()
                while (it.hasNext()) {
                    if (now - it.next().value > KEEP_MS) it.remove()
                }
                while (seen.size > KEEP_MAX) seen.remove(seen.keys.first())
            }
        }
        return true
    }
}
