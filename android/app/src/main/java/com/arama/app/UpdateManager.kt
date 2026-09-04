package com.arama.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Base64
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.Executors
import org.json.JSONObject

class UpdateManager(private val activity: Activity) {
    companion object {
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
            String(Base64.decode(encoded, Base64.DEFAULT), Charsets.UTF_8)
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

                val uri = FileProvider.getUriForFile(activity, "${activity.packageName}.fileprovider", apkFile)
                activity.runOnUiThread { install(uri) }
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

    private fun install(uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            activity.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(activity, "APK yükleyicisi açılamadı. Ayarlardan Arama'nın bilinmeyen uygulama yükleme iznini kontrol edin.", Toast.LENGTH_LONG).show()
        }
    }

    private fun openApkUrl(apkUrl: String) {
        try {
            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(apkUrl)))
        } catch (_: Exception) {
            Toast.makeText(activity, "APK bağlantısı açılamadı.", Toast.LENGTH_LONG).show()
        }
    }
}
