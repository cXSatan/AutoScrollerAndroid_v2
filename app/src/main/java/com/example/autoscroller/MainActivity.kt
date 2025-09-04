package com.example.autoscroller

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.example.autoscroller.ui.setup.SetupActivity

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, SetupActivity::class.java))
        finish()
    }
}