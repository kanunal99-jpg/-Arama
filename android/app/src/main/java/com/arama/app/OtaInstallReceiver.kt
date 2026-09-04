package com.arama.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.widget.Toast

class OtaInstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirmIntent = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                if (confirmIntent != null) {
                    confirmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(confirmIntent)
                }
            }
            PackageInstaller.STATUS_SUCCESS -> {
                Toast.makeText(context, "Arama güncellemesi başarıyla kuruldu.", Toast.LENGTH_LONG).show()
            }
            else -> {
                val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                    ?: "Bilinmeyen yükleme hatası"
                Toast.makeText(context, "Arama güncellemesi kurulamadı: $message", Toast.LENGTH_LONG).show()
            }
        }
    }
}
