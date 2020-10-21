package com.hgnis.reader.views.activity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.hgnis.reader.R
import com.hgnis.reader.utility.AppSharePreference


class splashScreen : AppCompatActivity() {
    var appSharePreference: AppSharePreference? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)
        appSharePreference = AppSharePreference(this)
        if (appSharePreference!!.name.isNotEmpty()) {
            Handler(Looper.getMainLooper()).postDelayed({
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }, 1500)
        } else {
            val intent = Intent(this, login::class.java)
            startActivity(intent)
            finish()
        }

    }
}