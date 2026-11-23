package io.github.chayanforyou.quickball.sync

import android.content.Context
import android.provider.Settings
import java.util.UUID

/**
 * Relay settings. Baked in at build time (`-PsyncHost`, `-PsyncPort`, `-PsyncPath`,
 * `-Ptls`), so a fork can point the feature at its own server without touching code.
 */
object Cfg {

    val host: String get() = BuildConfig.RELAY_HOST
    val port: String get() = BuildConfig.RELAY_PORT

    /** Server prefix, e.g. /x9k2m. Empty means root. */
    val path: String
        get() {
            val p = BuildConfig.RELAY_PATH.trim().trim('/')
            return if (p.isEmpty()) "" else "/$p"
        }
    val accountId: String get() = BuildConfig.ACCOUNT_ID
    val tls: Boolean get() = BuildConfig.USE_TLS
    val encrypted: Boolean get() = BuildConfig.ENCRYPTED

    private const val PREFS = "qb_sync"
    private const val K_DEVICE = "did"
    private const val K_TOKEN = "tok"

    const val HEARTBEAT_MS = 30_000L
    const val POLL_MS = 15_000L
    const val RECONNECT_MIN_MS = 5_000L
    const val RECONNECT_MAX_MS = 60_000L
    const val CONNECT_TIMEOUT = 5_000
    const val READ_TIMEOUT = 10_000

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Stable device identifier: prefs, otherwise a fresh UUID. */
    @Synchronized
    fun deviceId(ctx: Context): String {
        val p = prefs(ctx)
        p.getString(K_DEVICE, null)?.let { if (it.isNotEmpty()) return it }
        val id = UUID.randomUUID().toString().replace("-", "").substring(0, 16)
        p.edit().putString(K_DEVICE, id).apply()
        return id
    }

    /** Resets the local identifier: the server will issue a new token for a new row. */
    fun rotateDeviceId(ctx: Context) {
        prefs(ctx).edit().remove(K_DEVICE).remove(K_TOKEN).apply()
    }

    fun token(ctx: Context): String = prefs(ctx).getString(K_TOKEN, null).orEmpty()

    fun setToken(ctx: Context, value: String) {
        prefs(ctx).edit().putString(K_TOKEN, value).apply()
    }

    fun androidId(ctx: Context): String =
        runCatching {
            Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ANDROID_ID)
        }.getOrNull().orEmpty()
}