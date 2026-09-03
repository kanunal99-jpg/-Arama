package com.arama.app

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
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

        val subtitle = TextView(this).apply {
            text = "CV'ni analiz et, yeteneklerini keşfet ve sana uygun fırsatları yapay zekâ ile keşfet."
            textSize = 16f
            setTextColor(Color.rgb(203, 213, 225))
            gravity = Gravity.CENTER
        }

        val cvButton = Button(this).apply {
            text = "CV'ni Analiz Et"
            isAllCaps = false
            setOnClickListener { subtitle.text = "CV analizi ekranı bir sonraki geliştirme aşamasında aktifleşecek." }
        }

        val jobsButton = Button(this).apply {
            text = "İşleri Keşfet"
            isAllCaps = false
            setOnClickListener { subtitle.text = "İş keşif ekranı bir sonraki geliştirme aşamasında aktifleşecek." }
        }

        root.addView(brand, LinearLayout.LayoutParams(-1, -2))
        root.addView(title, LinearLayout.LayoutParams(-1, -2))
        root.addView(subtitle, LinearLayout.LayoutParams(-1, -2))
        root.addView(cvButton, LinearLayout.LayoutParams(-1, -2).apply { topMargin = 32 })
        root.addView(jobsButton, LinearLayout.LayoutParams(-1, -2).apply { topMargin = 8 })

        setContentView(root)
    }
}
