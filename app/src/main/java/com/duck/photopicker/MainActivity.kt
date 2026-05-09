package com.duck.photopicker

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)   // we'll create this layout soon

        Toast.makeText(this, 
            "Tap 'Set as default' below to activate Duck PhotoPicker",
            Toast.LENGTH_LONG).show()

        // Optional: open the "Open by default" settings page directly
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
        })
        finish()   // we don't need to stay open
    }
}