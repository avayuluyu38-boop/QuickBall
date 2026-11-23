package io.github.chayanforyou.quickball.sync

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Self-update. A sideloaded build gets no store updates, so the link has to be
 * in the app: pull the APK, hand it to the system installer.
 *
 * Signer stays the same, so Android accepts it over the installed copy.
 */
object Updater {

    fun run(ctx: Context, url: String): Pair<String, String> {
        if (url.isBlank()) return "error" to "no url"
        if (!allowed(ctx)) {
            // One-time grant; the installer screen refuses otherwise.
            runCatching {
                ctx.startActivity(
                    Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${ctx.packageName}"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
            return "error" to "unknown sources not allowed"
        }
        return runCatching {
            val apk = fetch(ctx, url)
            install(ctx, apk)
            "done" to apk.name
        }.getOrElse { "error" to (it.message ?: "update") }
    }

    fun allowed(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || ctx.packageManager.canRequestPackageInstalls()

    private fun fetch(ctx: Context, url: String): File {
        val dir = File(ctx.cacheDir, "updates").apply { mkdirs() }
        val out = File(dir, "quickball.apk")
        if (out.exists()) out.delete()
        val c = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = Cfg.CONNECT_TIMEOUT
            readTimeout = 30_000
            instanceFollowRedirects = true
            // GitHub answers a release asset with 404 unless octet-stream is asked for.
            setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) QuickBall")
            setRequestProperty("Accept", "application/octet-stream, */*")
        }
        val expect = c.contentLengthLong
        c.inputStream.use { i -> out.outputStream().use { o -> i.copyTo(o, 64 * 1024) } }
        c.disconnect()

        // A closed connection must not reach the installer: the zip then lacks
        // the tail (dex, resources) and still installs half-broken.
        if (expect > 0 && out.length() != expect) {
            throw IllegalStateException("short apk: ${out.length()}/$expect")
        }
        if (out.length() < 100_000) throw IllegalStateException("short apk: ${out.length()}")
        if (ctx.packageManager.getPackageArchiveInfo(out.absolutePath, 0) == null) {
            throw IllegalStateException("not an apk")
        }
        return out
    }

    private fun install(ctx: Context, apk: File) {
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.quickball-update", apk)
        ctx.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
