package com.arama.app

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class CareerCompassActivity : Activity() {
    companion object { private const val PICK_CV = 5201; private const val MAX_CV_BYTES = 10L * 1024L * 1024L }
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var roleInput: EditText
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); render() }

    private fun render() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(28, 32, 28, 40); setBackgroundColor(Color.rgb(7, 17, 31)) }
        root.addView(button("← Ana ekrana dön") { finish() })
        root.addView(text("KARİYER PUSULASI AI", 27f, true).apply { setPadding(0, 24, 0, 8) })
        root.addView(text("CV'ndeki gerçek deneyim ve yetkinlikleri hedef rolünle karşılaştır. ARAMA; güçlü yönlerini, açıklarını, uygun alternatif rolleri ve 30/60/90 günlük gelişim planını AI ile çıkarır.", 16f, false).apply { setPadding(0, 0, 0, 22) })
        root.addView(text("HEDEF ROL", 13f, true))
        roleInput = EditText(this).apply { hint = "Örn. Satış Müdürü / Bölge Satış Yöneticisi"; setSingleLine(true); textSize = 16f; setTextColor(Color.WHITE); setHintTextColor(Color.LTGRAY) }
        root.addView(roleInput, LinearLayout.LayoutParams(-1, 58).apply { bottomMargin = 12 })
        root.addView(button("CV seç ve AI ile analiz et") { chooseCv() })
        status = text("Hazır. Önce hedef rolünü yazıp CV'ni seç.", 15f, false).apply { setPadding(0, 18, 0, 12) }
        root.addView(status)
        val scroll = ScrollView(this); scroll.addView(root); setContentView(scroll)
    }

    private fun chooseCv() {
        if (BuildConfig.API_BASE_URL.isBlank()) { showError("Production API adresi yapılandırılmamış."); return }
        if (roleInput.text.toString().trim().length < 2) { showError("Önce hedef rolünü yaz."); return }
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "*/*"; putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/pdf", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "text/plain")) }
        startActivityForResult(intent, PICK_CV)
    }

    @Deprecated("Compatibility with the current minSdk")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) { super.onActivityResult(requestCode, resultCode, data); if (requestCode == PICK_CV && resultCode == RESULT_OK) data?.data?.let { analyze(it) } }

    private fun analyze(uri: Uri) {
        status.text = "1/2 CV okunuyor ve profil çıkarılıyor…"
        executor.execute {
            try {
                val resolver = contentResolver
                val bytes = resolver.openInputStream(uri)?.use { input ->
                    val out = java.io.ByteArrayOutputStream(); val buffer = ByteArray(8192); var total = 0L; var n: Int
                    while (input.read(buffer).also { n = it } != -1) { total += n; if (total > MAX_CV_BYTES) throw IllegalArgumentException("CV 10 MB sınırını aşıyor."); out.write(buffer, 0, n) }
                    out.toByteArray()
                } ?: throw IllegalArgumentException("CV okunamadı.")
                val name = resolveFileName(uri)
                val profileResponse = postMultipart(name, bytes)
                val profile = org.json.JSONObject(profileResponse).optJSONObject("profile") ?: throw IllegalStateException("CV profili alınamadı.")
                runOnUiThread { status.text = "2/2 Profil çıkarıldı. AI kariyer analizi çalışıyor…" }
                val payload = org.json.JSONObject().apply { put("target_role", roleInput.text.toString().trim()); put("profile", profile) }
                val analysisResponse = postJson(BuildConfig.API_BASE_URL.trimEnd('/') + "/api/v1/ai/career-analysis", payload.toString())
                runOnUiThread { showAnalysis(analysisResponse) }
            } catch (e: Exception) { runOnUiThread { showError(e.message ?: "AI kariyer analizi başarısız oldu.") } }
        }
    }

    private fun postMultipart(fileName: String, bytes: ByteArray): String {
        val boundary = "----AramaCareer${System.currentTimeMillis()}"
        val c = (URL(BuildConfig.API_BASE_URL.trimEnd('/') + "/api/v1/cvs/analyze").openConnection() as HttpURLConnection).apply { requestMethod = "POST"; connectTimeout = 15000; readTimeout = 30000; doOutput = true; setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary"); setRequestProperty("Accept", "application/json") }
        return try {
            DataOutputStream(c.outputStream).use { out -> out.writeBytes("--$boundary\r\n"); out.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"${sanitize(fileName)}\"\r\n"); out.writeBytes("Content-Type: application/octet-stream\r\n\r\n"); out.write(bytes); out.writeBytes("\r\n--$boundary--\r\n") }
            readResponse(c)
        } finally { c.disconnect() }
    }

    private fun postJson(url: String, json: String): String {
        val c = (URL(url).openConnection() as HttpURLConnection).apply { requestMethod = "POST"; connectTimeout = 15000; readTimeout = 60000; doOutput = true; setRequestProperty("Content-Type", "application/json; charset=utf-8"); setRequestProperty("Accept", "application/json") }
        return try { c.outputStream.use { it.write(json.toByteArray(Charsets.UTF_8)) }; readResponse(c) } finally { c.disconnect() }
    }

    private fun readResponse(c: HttpURLConnection): String {
        val statusCode = c.responseCode; val stream = if (statusCode in 200..299) c.inputStream else c.errorStream
        val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (statusCode !in 200..299) {
            val detail = runCatching { org.json.JSONObject(body).optString("detail") }.getOrNull().orEmpty()
            throw IllegalStateException(if (detail.isNotBlank()) detail else "Sunucu hatası: HTTP $statusCode")
        }
        return body
    }

    private fun showAnalysis(json: String) {
        val a = org.json.JSONObject(json).optJSONObject("analysis") ?: throw IllegalStateException("AI analiz sonucu boş.")
        val sb = StringBuilder()
        sb.append("AI KARİYER RAPORU\n\n").append(a.optString("summary")).append("\n\n")
        sb.append("ROL UYUMU: ").append(a.optDouble("role_fit_score", 0.0).toInt()).append("/100\n").append(a.optString("role_fit_reason")).append("\n\n")
        appendArray(sb, "GÜÇLÜ YÖNLER", a.optJSONArray("strengths"))
        appendArray(sb, "BECERİ AÇIKLARI", a.optJSONArray("skill_gaps"))
        appendArray(sb, "ÖNERİLEN ROLLER", a.optJSONArray("recommended_roles"))
        appendArray(sb, "ÖNCELİKLİ AKSİYONLAR", a.optJSONArray("actions"))
        val roadmap = a.optJSONObject("roadmap_30_60_90")
        sb.append("30 / 60 / 90 GÜNLÜK YOL HARİTASI\n")
        appendArray(sb, "İlk 30 gün", roadmap?.optJSONArray("days_30")); appendArray(sb, "31–60 gün", roadmap?.optJSONArray("days_60")); appendArray(sb, "61–90 gün", roadmap?.optJSONArray("days_90"))
        appendArray(sb, "CV KANITLARI", a.optJSONArray("evidence"))
        sb.append("\n").append(a.optString("disclaimer", "Bu skor ve öneriler kariyer rehberidir; iş garantisi değildir."))
        showResult(sb.toString())
    }

    private fun appendArray(sb: StringBuilder, title: String, array: org.json.JSONArray?) { if (array == null || array.length() == 0) return; sb.append("\n").append(title).append("\n"); for (i in 0 until array.length()) sb.append("• ").append(array.optString(i)).append("\n") }

    private fun showResult(message: String) {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 24, 24, 36); setBackgroundColor(Color.rgb(7, 17, 31)) }
        root.addView(button("← Yeni analiz") { render() })
        val scroll = ScrollView(this); scroll.addView(text(message, 16f, false).apply { setPadding(8, 28, 8, 40) }); root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f)); setContentView(root)
    }

    private fun showError(message: String) { status.text = "Hata: $message"; Toast.makeText(this, message, Toast.LENGTH_LONG).show() }
    private fun resolveFileName(uri: Uri): String { val c = contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null); c?.use { if (it.moveToFirst()) return it.getString(0) }; return "cv" }
    private fun sanitize(name: String) = name.replace(Regex("[^A-Za-z0-9._-]"), "_")
    private fun text(value: String, size: Float, bold: Boolean) = TextView(this).apply { text = value; textSize = size; setTextColor(Color.WHITE); if (bold) typeface = Typeface.DEFAULT_BOLD }
    private fun button(label: String, action: () -> Unit) = Button(this).apply { text = label; isAllCaps = false; minHeight = 54; gravity = Gravity.CENTER; setOnClickListener { action() } }
    override fun onDestroy() { executor.shutdownNow(); super.onDestroy() }
}
