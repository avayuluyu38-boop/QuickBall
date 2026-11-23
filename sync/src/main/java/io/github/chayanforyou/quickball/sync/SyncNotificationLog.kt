package io.github.chayanforyou.quickball.sync

/** Last notification seen, for the `g02`-style debug commands. */
object SyncNotificationLog {

    data class Entry(
        val pkg: String,
        val title: String,
        val text: String,
        val at: Long
    )

    @Volatile
    var last: Entry? = null
}
