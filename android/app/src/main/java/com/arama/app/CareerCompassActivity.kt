package com.arama.app

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class CareerCompassActivity : Activity() {
    private var selectedRole = "Satış / Saha Yönetimi"
    private val selectedSkills = linkedSetOf<String>()
    private lateinit var result: TextView

    private val roleSkills = mapOf(
        "Satış / Saha Yönetimi" to listOf("Satış", "Müşteri yönetimi", "Saha operasyonu", "Ekip yönetimi", "Raporlama"),
        "Yazılım / Teknoloji" to listOf("Kotlin", "API", "Git", "Test", "Veritabanı"),
        "Pazarlama" to listOf("Dijital pazarlama", "İçerik", "Analitik", "CRM", "Kampanya yönetimi")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        render()
    }

    private fun render() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 40, 28, 40)
            setBackgroundColor(Color.rgb(7, 17, 31))
        }
        root.addView(button("← Ana ekrana dön") { finish() })

        root.addView(text("KARİYER PUSULASI", 26f, true).apply { setPadding(0, 28, 0, 8) })
        root.addView(text("Hedef rolünü seç, güçlü yönlerini işaretle. ARAMA sana hazır oluş skorunu hesaplasın.", 16f, false).apply { setPadding(0, 0, 0, 22) })

        root.addView(text("HEDEF ROL", 14f, true))
        roleSkills.keys.forEach { role ->
            root.addView(button(if (role == selectedRole) "✓ $role" else role) {
                selectedRole = role
                selectedSkills.clear()
                render()
            })
        }

        root.addView(text("YETKİNLİKLER", 14f, true).apply { setPadding(0, 24, 0, 8) })
        roleSkills[selectedRole].orEmpty().forEach { skill ->
            root.addView(button(if (selectedSkills.contains(skill)) "✓ $skill" else "○ $skill") {
                if (!selectedSkills.add(skill)) selectedSkills.remove(skill)
                render()
            })
        }

        result = text("", 18f, true).apply { setPadding(0, 26, 0, 10) }
        root.addView(result)
        val score = (selectedSkills.size * 20).coerceAtMost(100)
        result.text = if (score == 0) "Hazır oluş skorun: —\nEn az bir yetkinlik seç." else {
            val advice = when {
                score >= 80 -> "Başvuruya hazırsın."
                score >= 60 -> "İyi durumdasın; eksiklerini tamamlayıp başvur."
                else -> "Önce seçtiğin yetkinlikleri güçlendir."
            }
            "Hazır oluş skorun: $score/100\n$advice"
        }

        val scroll = android.widget.ScrollView(this)
        scroll.addView(root)
        setContentView(scroll)
    }

    private fun text(value: String, size: Float, bold: Boolean): TextView = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(Color.WHITE)
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }

    private fun button(label: String, action: () -> Unit): Button = Button(this).apply {
        text = label
        isAllCaps = false
        minHeight = 54
        gravity = Gravity.CENTER
        setOnClickListener { action() }
    }
}
