package dev.danielgilchrist.knockoff.fakechat

import android.app.Activity
import android.os.Bundle

class OpenActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        finish()
    }
}
