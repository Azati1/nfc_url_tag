package com.azati.nfc_url_tag

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TagReadReceiver(private val onTagRead: () -> Unit) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == ACTION_TAG_READ) onTagRead()
    }

    companion object {
        const val ACTION_TAG_READ = "com.azati.nfc_url_tag.ACTION_TAG_READ"
    }
}
