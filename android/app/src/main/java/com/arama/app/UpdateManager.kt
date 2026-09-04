package com.arama.app

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.Executors
import org.json.JSONObject

class UpdateManager(private val activity: Activity) {
    companion object {
        private const val TAG = "AramaOTA"
        private const val RAW_MANIFEST_URL = "https://raw.githubusercontent.com/kanunal99-jpg/-Arama/main/release/latest.json"
        private const val API_MANIFEST_URL = "https://api.github.com/repos/kanunal99-jpg/-Arama/contents/release/latest.json?ref=main"
    }

    fun checkForUpdate() {
        Executors.newSingleThreadExecutor().execute {
            try {
                val json = fetchManifest()
                val manifest = JSONObject(json)
                val remoteVersion = manifest.getLong("versionCode")
                val currentVersion = activity.packageManager.getPackageInfo(activity.packageName, 0).longVersionCode
                if (remoteVersion > currentVersion) {
                    val versionName = manifest.optString("versionName", remoteVersion.toString())
                    val apkUrl = manifest.getString("apkUrl")
                    val sha256 = manifest.optString("sha256", "")
                    activity.runOnUiThread {
                        UpdateDialog.show(activity, versionName) { download(apkUrl, sha256) }
                    }
                } else {
                    activity.runOnUiThread {
                        Toast.makeText(activity, "Arama güncel. Sürüm: ${manifest.optString("versionName", remoteVersion.toString())}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                activity.runOnUiThread {
                    Toast.makeText(activity, "Güncelleme kontrolü başarısız: ${e.javaClass.simpleName}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun fetchManifest(): String {
        return try {
            httpGet(RAW_MANIFEST_URL)
        } catch (_: Exception) {
            val apiJson = httpGet(API_MANIFEST_URL)
            val apiObject = JSONObject(apiJson)
            val encoded = apiObject.getString("content").replace("\\s".toRegex(), "")
            String(android.util.Base64.decode(encoded, android.util.Base64.DEFAULT), Charsets.UTF_8)
        }
    }

    private fun httpGet(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "Arama-Android-OTA")
            connection.connect()
            if (connection.responseCode !in 200..299) throw IllegalStateException("HTTP ${connection.responseCode}")
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun download(apkUrl: String, expectedSha256: String) {
        if (android.os.Build.VERSION.SDK_INT >= 26 && !activity.packageManager.canRequestPackageInstalls()) {
            activity.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${activity.packageName}")))
            Toast.makeText(activity, "Güncellemeyi kurmak için Arama'ya izin verin. İzin verdikten sonra tekrar kontrol edin.", Toast.LENGTH_LONG).show()
            return
        }

        activity.runOnUiThread {
            Toast.makeText(activity, "Arama güncellemesi indiriliyor…", Toast.LENGTH_SHORT).show()
        }

        Executors.newSingleThreadExecutor().execute {
            val updateDir = File(activity.cacheDir, "updates")
            val apkFile = File(updateDir, "arama-update.apk")
            try {
                updateDir.mkdirs()
                if (!updateDir.isDirectory) throw IllegalStateException("OTA_CACHE_DIRECTORY_UNAVAILABLE")
                if (apkFile.exists()) apkFile.delete()

                val connection = URL(apkUrl).openConnection() as HttpURLConnection
                try {
                    connection.connectTimeout = 15000
                    connection.readTimeout = 30000
                    connection.instanceFollowRedirects = true
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("Accept", "application/vnd.android.package-archive")
                    connection.setRequestProperty("User-Agent", "Arama-Android-OTA")
                    connection.connect()
                    if (connection.responseCode !in 200..299) {
                        throw IllegalStateException("OTA_HTTP_${connection.responseCode}")
                    }
                    connection.inputStream.use { input ->
                        FileOutputStream(apkFile).use { output ->
                            input.copyTo(output, 8192)
                            output.fd.sync()
                        }
                    }
                } finally {
                    connection.disconnect()
                }

                if (!apkFile.isFile || apkFile.length() == 0L) throw IllegalStateException("OTA_APK_EMPTY")
                if (expectedSha256.isNotBlank() && !verifySha256(apkFile, expectedSha256)) {
                    apkFile.delete()
                    throw IllegalStateException("OTA_SHA256_MISMATCH")
                }

                activity.runOnUiThread { installWithPackageInstaller(apkFile) }
            } catch (e: Exception) {
                apkFile.delete()
                activity.runOnUiThread {
                    Toast.makeText(activity, "Güncelleme indirilemedi: ${e.message ?: e.javaClass.simpleName}", Toast.LENGTH_LONG).show()
                    openApkUrl(apkUrl)
                }
            }
        }
    }

    private fun verifySha256(file: File, expected: String): Boolean {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(8192)
                var count: Int
                while (input.read(buffer).also { count = it } != -1) digest.update(buffer, 0, count)
            }
            digest.digest().joinToString("") { "%02x".format(it) }.equals(expected.trim(), ignoreCase = true)
        } catch (_: Exception) { false }
    }

    private fun installWithPackageInstaller(apkFile: File) {
        var sessionId = -1
        var committed = false
        try {
            val packageInstaller = activity.packageManager.packageInstaller
            val params = android.content.pm.PackageInstaller.SessionParams(
                android.content.pm.PackageInstaller.SessionParams.MODE_FULL_INSTALL
            ).apply {
                setAppPackageName(activity.packageName)
            }
            sessionId = packageInstaller.createSession(params)
            packageInstaller.openSession(sessionId).use { session ->
                apkFile.inputStream().use { input ->
                    session.openWrite("base.apk", 0, apkFile.length()).use { output ->
                        input.copyTo(output, 8192)
                        session.fsync(output)
                    }
                }

                val callbackIntent = Intent(activity, OtaInstallReceiver::class.java)
                    .setPackage(activity.packageName)
                    .addFlags(Intent.FLAG_RECEIVER_FOREGROUND)

                // PackageInstaller fills the callback Intent with EXTRA_STATUS and
                // EXTRA_INTENT when user approval is required. Android/AOSP uses a
                // mutable PendingIntent for this callback on modern Android.
                val pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT or
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        PendingIntent.FLAG_MUTABLE
                    } else {
                        0
                    }

                val pendingIntent = PendingIntent.getBroadcast(
                    activity,
                    sessionId,
                    callbackIntent,
                    pendingIntentFlags
                )

                session.commit(pendingIntent.intentSender)
                committed = true
            }

            Toast.makeText(activity, "Güncelleme kurulumu başlatıldı. Android onayı bekleniyor…", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e(TAG, "PackageInstaller kurulumu başlatamadı. sessionId=$sessionId", e)
            if (sessionId != -1 && !committed) {
                try {
                    packageInstallerForCleanup().abandonSession(sessionId)
                } catch (_: Exception) {
                    // Best effort cleanup only.
                }
            }
            Toast.makeText(
                activity,
                "Güncelleme kurulumu başlatılamadı: ${e.javaClass.simpleName}: ${e.message ?: "ayrıntı yok"}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun packageInstallerForCleanup(): android.content.pm.PackageInstaller =
        activity.packageManager.packageInstaller

    private fun openApkUrl(apkUrl: String) {
        try {
            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(apkUrl)))
        } catch (_: Exception) {
            Toast.makeText(activity, "APK bağlantısı açılamadı.", Toast.LENGTH_LONG).show()
        }
    }
}
