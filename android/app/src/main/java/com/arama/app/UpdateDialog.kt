package com.arama.app

import android.app.AlertDialog
import android.content.Context

object UpdateDialog {
    fun show(context: Context, versionName: String, onUpdate: () -> Unit) {
        AlertDialog.Builder(context)
            .setTitle("Yeni Arama sürümü hazır")
            .setMessage("Arama $versionName sürümü kullanılabilir. Şimdi güncellemek ister misiniz?")
            .setPositiveButton("Şimdi Güncelle") { _, _ -> onUpdate() }
            .setNegativeButton("Sonra", null)
            .show()
    }
}
