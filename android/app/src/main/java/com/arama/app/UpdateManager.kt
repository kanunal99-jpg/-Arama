package com.arama.app

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.Executors
import org.json.JSONObject

class UpdateManager(private val activity: Activity) {
    companion object {
        private const val MANIFEST_URL = "https://raw.githubusercontent.com/kanunal99-jpg/-Arama/main/release/latest.json"
    }

    fun checkForUpdate() {
        Executors.newSingleThreadExecutor().execute {
            try {
                val connection = URL(MANIFEST_URL).openConnection() as HttpURLConnection
                connection.connectTimeout = 8000
                connection.readTimeout = 8000
                connection.requestMethod = "GET"
                connection.connect()
                if (connection.responseCode !in 200..299) return@execute
                val json = connection.inputStream.bufferedReader().use { it.readText() }
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
                }
            } catch (_: Exception) { }
        }
    }

    private fun download(apkUrl: String, expectedSha256: String) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= 26 && !activity.packageManager.canRequestPackageInstalls()) {
                activity.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${activity.packageName}")))
                Toast.makeText(activity, "Güncellemeyi kurmak için bu kaynak için izin verin.", Toast.LENGTH_LONG).show()
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
            val observer = Executors.newSingleThreadExecutor()
            observer.execute {
                var downloading = true
                while (downloading) {
                    Thread.sleep(700)
                    val cursor = manager.query(DownloadManager.Query().setFilterById(id))
                    if (cursor == null) {
                        continue
                    }
                    cursor.use {
                        if (!it.moveToFirst()) {
                            continue
                        }
                        val status = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                        when (status) {
                            DownloadManager.STATUS_SUCCESSFUL -> {
                                downloading = false
                                val uri = manager.getUriForDownloadedFile(id)
                                if (uri == null) {
                                    activity.runOnUiThread { Toast.makeText(activity, "Güncelleme dosyası bulunamadı.", Toast.LENGTH_LONG).show() }
                                } else if (expectedSha256.isNotBlank() && !verifySha256(uri, expectedSha256)) {
                                    manager.remove(id)
                                    activity.runOnUiThread { Toast.makeText(activity, "Güncelleme doğrulanamadı.", Toast.LENGTH_LONG).show() }
                                } else {
                                    activity.runOnUiThread { install(uri) }
                                }
                            }
                            DownloadManager.STATUS_FAILED -> {
                                downloading = false
                                activity.runOnUiThread { Toast.makeText(activity, "Güncelleme indirilemedi.", Toast.LENGTH_LONG).show() }
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
            Toast.makeText(activity, "Güncelleme başlatılamadı.", Toast.LENGTH_LONG).show()
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
