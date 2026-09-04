package com.arama.app

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import android.util.Base64
import android.widget.Toast
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
            } catch (_: Exception) {
                activity.runOnUiThread {
                    Toast.makeText(activity, "Güncelleme kontrolü başarısız oldu. Tekrar deneyin.", Toast.LENGTH_LONG).show()
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
        try {
            if (android.os.Build.VERSION.SDK_INT >= 26 && !activity.packageManager.canRequestPackageInstalls()) {
                activity.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${activity.packageName}")))
                Toast.makeText(activity, "Güncellemeyi kurmak için Arama'ya izin verin. İzin verdikten sonra Güncellemeleri Kontrol Et'e tekrar basın.", Toast.LENGTH_LONG).show()
                return
            }

            val request = DownloadManager.Request(Uri.parse(apkUrl))
                .setTitle("Arama güncellemesi")
                .setDescription("Yeni sürüm indiriliyor…")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalFilesDir(activity, Environment.DIRECTORY_DOWNLOADS, "arama-update.apk")
                .setMimeType("application/vnd.android.package-archive")

            val manager = activity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val id = manager.enqueue(request)
            Toast.makeText(activity, "Arama güncellemesi indiriliyor…", Toast.LENGTH_SHORT).show()

            val observer = Executors.newSingleThreadExecutor()
            observer.execute {
                var downloading = true
                while (downloading) {
                    Thread.sleep(700)
                    val cursor = manager.query(DownloadManager.Query().setFilterById(id))
                    if (cursor != null) {
                        var hasRow = false
                        var status = DownloadManager.STATUS_PENDING
                        if (cursor.moveToFirst()) {
                            hasRow = true
                            status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                        }
                        cursor.close()
                        if (hasRow) {
                            when (status) {
                                DownloadManager.STATUS_SUCCESSFUL -> {
                                    downloading = false
                                    val uri = manager.getUriForDownloadedFile(id)
                                    if (uri == null) {
                                        activity.runOnUiThread { Toast.makeText(activity, "Güncelleme dosyası bulunamadı.", Toast.LENGTH_LONG).show() }
                                    } else if (expectedSha256.isNotBlank() && !verifySha256(uri, expectedSha256)) {
                                        manager.remove(id)
                                        activity.runOnUiThread { Toast.makeText(activity, "Güncelleme doğrulanamadı; dosya silindi.", Toast.LENGTH_LONG).show() }
                                    } else {
                                        activity.runOnUiThread { install(uri) }
                                    }
                                }
                                DownloadManager.STATUS_FAILED -> {
                                    downloading = false
                                    activity.runOnUiThread {
                                        Toast.makeText(activity, "Güncelleme indirilemedi. APK sayfası açılıyor…", Toast.LENGTH_LONG).show()
                                        openApkUrl(apkUrl)
                                    }
                                }
                            }
                        }
                    }
                }
                observer.shutdown()
            }
        } catch (e: Exception) {
            activity.runOnUiThread {
                Toast.makeText(activity, "Güncelleme başlatılamadı: ${e.javaClass.simpleName}", Toast.LENGTH_LONG).show()
                openApkUrl(apkUrl)
            }
        }
    }

    private fun openApkUrl(apkUrl: String) {
        try {
            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(apkUrl)))
        } catch (_: Exception) {
            Toast.makeText(activity, "APK bağlantısı açılamadı.", Toast.LENGTH_LONG).show()
        }
    }

    private fun verifySha256(uri: Uri, expected: String): Boolean {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            activity.contentResolver.openInputStream(uri)?.use { input ->
                val buffer = ByteArray(8192)
                var count: Int
                while (input.read(buffer).also { count = it } != -1) digest.update(buffer, 0, count)
            } ?: return false
            digest.digest().joinToString("") { "%02x".format(it) }.equals(expected.trim(), ignoreCase = true)
        } catch (_: Exception) { false }
    }

    private fun install(uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        activity.startActivity(intent)
    }
}
