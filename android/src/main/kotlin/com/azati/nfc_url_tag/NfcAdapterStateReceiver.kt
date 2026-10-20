package com.azati.nfc_url_tag

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter

class NfcAdapterStateReceiver(
    private val onStateChanged: (Boolean) -> Unit,
) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action != NfcAdapter.ACTION_ADAPTER_STATE_CHANGED) return
        val state = intent.getIntExtra(NfcAdapter.EXTRA_ADAPTER_STATE, NfcAdapter.STATE_OFF)
        when (state) {
            NfcAdapter.STATE_ON -> onStateChanged(true)
            NfcAdapter.STATE_OFF -> onStateChanged(false)
        }
    }
}
