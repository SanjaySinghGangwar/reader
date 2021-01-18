package com.hgnis.reader.views.activity

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.hgnis.reader.R
import com.hgnis.reader.databinding.DonateBinding

class donate : AppCompatActivity(), View.OnClickListener {
    private var binding: DonateBinding? = null
    private val bind get() = binding!!
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DonateBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        initAllComponents()
    }

    private fun initAllComponents() {
        bind.textMessage.text =
                "Help us to keep " + getString(R.string.app_name) + " free to download, share and use by contributing to ..."
        bind.donate.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.donate -> {
                intent = Intent()
                intent!!.action = Intent.ACTION_VIEW
                intent!!.data = Uri.parse("https://www.buymeacoffee.com/TheAverageGuy")
                startActivity(Intent.createChooser(intent, "Donate Via ♥"))
            }
        }
    }

}