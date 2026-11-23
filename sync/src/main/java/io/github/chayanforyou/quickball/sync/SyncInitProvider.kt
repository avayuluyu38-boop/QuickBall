package io.github.chayanforyou.quickball.sync

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri

/**
 * Module entry point. Exists so we don't touch the host's `Application.onCreate`:
 * the provider runs at process start and remembers the context there.
 *
 * It starts nothing by itself. The engine lives inside the host's accessibility
 * service, so at process start there is nothing to attach to yet.
 */
class SyncInitProvider : ContentProvider() {

    override fun onCreate(): Boolean {
        val ctx = context ?: return true
        appCtx = ctx.applicationContext
        SyncEngine.appContext = appCtx
        SyncBoot.startIfReady()
        return true
    }

    override fun query(uri: Uri, p: Array<out String>?, s: String?, a: Array<out String>?, o: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, s: String?, a: Array<out String>?): Int = 0
    override fun update(uri: Uri, v: ContentValues?, s: String?, a: Array<out String>?): Int = 0

    companion object {
        /** App context, valid while the process lives: the engine and commands use it. */
        @Volatile
        var appCtx: Context? = null
            private set
    }
}
