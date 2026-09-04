package com.arama.app

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class MainActivity : Activity() {
    companion object {
        private const val PICK_CV = 4101
        private const val MAX_CV_BYTES = 10L * 1024L * 1024L
    }

    private lateinit var subtitle: TextView
    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 48, 32, 48)
            setBackgroundColor(Color.rgb(7, 17, 31))
        }

        val brand = TextView(this).apply {
            text = "ARAMA"
            textSize = 18f
            setTextColor(Color.rgb(34, 211, 238))
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }

        val title = TextView(this).apply {
            text = "Kariyerinin bir sonraki adımını bul."
            textSize = 30f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 16)
        }

        subtitle = TextView(this).apply {
            text = "CV'ni analiz et, yeteneklerini keşfet ve sana uygun fırsatları yapay zekâ ile keşfet."
            textSize = 16f
            setTextColor(Color.rgb(203, 213, 225))
            gravity = Gravity.CENTER
        }

        val cvButton = Button(this).apply {
            text = "CV'ni Analiz Et"
            isAllCaps = false
            setOnClickListener { openCvPicker() }
        }

        val jobsButton = Button(this).apply {
            text = "İşleri Keşfet"
            isAllCaps = false
            setOnClickListener { subtitle.text = "İş keşfi sonraki ürün aşamasında aktifleşecek." }
        }

        val updateButton = Button(this).apply {
            text = "Güncellemeleri Kontrol Et"
            isAllCaps = false
            setOnClickListener { UpdateManager(this@MainActivity).checkForUpdate() }
        }

        root.addView(brand, LinearLayout.LayoutParams(-1, -2))
        root.addView(title, LinearLayout.LayoutParams(-1, -2))
        root.addView(subtitle, LinearLayout.LayoutParams(-1, -2))
        root.addView(cvButton, LinearLayout.LayoutParams(-1, -2).apply { topMargin = 32 })
        root.addView(jobsButton, LinearLayout.LayoutParams(-1, -2).apply { topMargin = 8 })
        root.addView(updateButton, LinearLayout.LayoutParams(-1, -2).apply { topMargin = 8 })

        setContentView(root)
        UpdateManager(this).checkForUpdate()
    }

    private fun openCvPicker() {
        if (BuildConfig.API_BASE_URL.isBlank()) {
            showError("Production CV API adresi henüz yapılandırılmadı.")
            return
        }
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                "application/pdf",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "text/plain"
            ))
        }
        startActivityForResult(intent, PICK_CV)
    }

    @Deprecated("Activity Result API migration can be done separately; this keeps minSdk compatibility simple.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_CV && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            analyzeCv(uri)
        }
    }

    private fun analyzeCv(uri: Uri) {
        subtitle.text = "CV okunuyor ve analiz ediliyor…"
        executor.execute {
            try {
                val resolver = contentResolver
                val size = resolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
                if (size > MAX_CV_BYTES) throw IllegalArgumentException("CV dosyası 10 MB sınırını aşıyor.")

                val fileName = resolveFileName(uri)
                val bytes = resolver.openInputStream(uri)?.use { input ->
                    val output = java.io.ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    var total = 0L
                    var count: Int
                    while (input.read(buffer).also { count = it } != -1) {
                        total += count
                        if (total > MAX_CV_BYTES) throw IllegalArgumentException("CV dosyası 10 MB sınırını aşıyor.")
                        output.write(buffer, 0, count)
                    }
                    output.toByteArray()
                } ?: throw IllegalArgumentException("CV dosyası okunamadı.")

                val response = postMultipart(fileName, bytes)
                runOnUiThread { showResult(response) }
            } catch (e: Exception) {
                runOnUiThread { showError(e.message ?: "CV analizi başarısız oldu.") }
            }
        }
    }

    private fun postMultipart(fileName: String, bytes: ByteArray): String {
        val boundary = "----AramaCV${System.currentTimeMillis()}"
        val connection = (URL(BuildConfig.API_BASE_URL.trimEnd('/') + "/api/v1/cvs/analyze").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15000
            readTimeout = 30000
            doOutput = true
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            setRequestProperty("Accept", "application/json")
        }

        return try {
            DataOutputStream(connection.outputStream).use { out ->
                out.writeBytes("--$boundary\r\n")
                out.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"${sanitizeFilename(fileName)}\"\r\n")
                out.writeBytes("Content-Type: application/octet-stream\r\n\r\n")
                out.write(bytes)
                out.writeBytes("\r\n--$boundary--\r\n")
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                val detail = Regex("\"detail\"\\s*:\\s*\"([^\"]*)\"").find(body)?.groupValues?.get(1)
                throw IllegalStateException(detail ?: "CV analizi başarısız oldu. HTTP $status")
            }
            body
        } finally {
            connection.disconnect()
        }
    }

    private fun showResult(json: String) {
        val profile = org.json.JSONObject(json).optJSONObject("profile")
        val name = profile?.optString("name", "-") ?: "-"
        val email = profile?.optString("email", "-") ?: "-"
        val phone = profile?.optString("phone", "-") ?: "-"
        val skills = profile?.optJSONArray("skills")?.let { array ->
            (0 until array.length()).joinToString(", ") { array.optString(it) }
        }.orEmpty().ifBlank { "-" }
        val sections = profile?.optJSONObject("sections")
        val experience = sectionText(sections, "experience")
        val education = sectionText(sections, "education")

        val message = "Ad: $name\nE-posta: $email\nTelefon: $phone\n\nYetenekler: $skills\n\nDeneyim: $experience\n\nEğitim: $education"
        runOnUiThread { showResultScreen(message) }
    }

    private fun sectionText(sections: org.json.JSONObject?, key: String): String {
        val array = sections?.optJSONArray(key) ?: return "-"
        return (0 until array.length()).joinToString("\n") { "• ${array.optString(it)}" }.ifBlank { "-" }
    }

    private fun showResultScreen(message: String) {
        val scroll = ScrollView(this).apply { setBackgroundColor(Color.rgb(7, 17, 31)) }
        val text = TextView(this).apply {
            text = "CV ANALİZ SONUCU\n\n$message"
            textSize = 16f
            setTextColor(Color.WHITE)
            setPadding(32, 48, 32, 48)
        }
        scroll.addView(text)
        setContentView(scroll)
        subtitle = text
    }

    private fun showError(message: String) {
        subtitle.text = message
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun resolveFileName(uri: Uri): String {
        val cursor = contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
        cursor?.use { if (it.moveToFirst()) return it.getString(0) }
        return "cv"
    }

    private fun sanitizeFilename(name: String): String = name.replace(Regex("[^A-Za-z0-9._-]"), "_")

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }
}
