package com.azati.nfc_url_tag

import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.cardemulation.HostApduService
import android.os.Bundle

/**
 * Emulates an NFC Forum Type 4 Tag that returns a single NDEF URI record.
 *
 * Protocol based on NFC Forum Type 4 Tag Operation Specification (Appendix E
 * "Example of Mapping Version 2.0 Command Flow").
 */
class HostCardEmulationService : HostApduService() {

    private var ndefUriBytes: ByteArray? = null
    private var ndefUriLen: ByteArray? = null
    private var totalLen: Int = 0
    private var readCapabilityContainerCheck = false
    private var tagReadComplete = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isRunning = true
        intent?.getStringExtra(EXTRA_URL)?.let { url ->
            val ndef = NdefMessage(NdefRecord.createUri(url))
            val bytes = ndef.toByteArray()
            ndefUriBytes = bytes
            ndefUriLen = byteArrayOf(
                ((bytes.size ushr 8) and 0xFF).toByte(),
                (bytes.size and 0xFF).toByte()
            )
            totalLen = bytes.size + 2
        }
        return START_NOT_STICKY
    }

    override fun processCommandApdu(commandApdu: ByteArray, extras: Bundle?): ByteArray {
        val uriBytes = ndefUriBytes ?: return A_ERROR
        val uriLen = ndefUriLen ?: return A_ERROR

        if (APDU_SELECT.contentEquals(commandApdu)) return A_OKAY
        if (CAPABILITY_CONTAINER_OK.contentEquals(commandApdu)) return A_OKAY
        if (READ_CAPABILITY_CONTAINER.contentEquals(commandApdu) &&
            !readCapabilityContainerCheck
        ) {
            readCapabilityContainerCheck = true
            return READ_CAPABILITY_CONTAINER_RESPONSE
        }
        if (NDEF_SELECT_OK.contentEquals(commandApdu)) return A_OKAY
        if (NDEF_READ_BINARY_NLEN.contentEquals(commandApdu)) {
            val response = ByteArray(uriLen.size + A_OKAY.size)
            System.arraycopy(uriLen, 0, response, 0, uriLen.size)
            System.arraycopy(A_OKAY, 0, response, uriLen.size, A_OKAY.size)
            readCapabilityContainerCheck = false
            return response
        }
        if (commandApdu.size >= 5 &&
            commandApdu[0] == 0x00.toByte() &&
            commandApdu[1] == 0xB0.toByte()
        ) {
            val offset = ((commandApdu[2].toInt() and 0xFF) shl 8) or
                (commandApdu[3].toInt() and 0xFF)
            val length = commandApdu[4].toInt() and 0xFF

            val fullSize = uriLen.size + uriBytes.size
            if (offset > fullSize) return A_ERROR

            val full = ByteArray(fullSize)
            System.arraycopy(uriLen, 0, full, 0, uriLen.size)
            System.arraycopy(uriBytes, 0, full, uriLen.size, uriBytes.size)

            val available = fullSize - offset
            val realLength = if (available <= length) available else length
            val response = ByteArray(realLength + A_OKAY.size)
            System.arraycopy(full, offset, response, 0, realLength)
            System.arraycopy(A_OKAY, 0, response, realLength, A_OKAY.size)

            readCapabilityContainerCheck = false
            if (offset + realLength >= totalLen) tagReadComplete = true
            return response
        }
        return A_ERROR
    }

    override fun onDeactivated(reason: Int) {
        if (tagReadComplete) {
            val broadcast = Intent(TagReadReceiver.ACTION_TAG_READ).setPackage(packageName)
            sendBroadcast(broadcast)
        }
        tagReadComplete = false
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        NfcUrlTagPlugin.setComponentEnabled(
            applicationContext,
            HostCardEmulationService::class.java,
            false
        )
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        isRunning = false
        super.onDestroy()
    }

    companion object {
        const val EXTRA_URL = "url"

        @Volatile
        var isRunning = false
            private set

        // SELECT AID = D2760000850101 (NFC Forum NDEF Tag Application).
        private val APDU_SELECT = byteArrayOf(
            0x00, 0xA4.toByte(), 0x04, 0x00, 0x07,
            0xD2.toByte(), 0x76, 0x00, 0x00, 0x85.toByte(), 0x01, 0x01,
            0x00
        )
        private val CAPABILITY_CONTAINER_OK = byteArrayOf(
            0x00, 0xA4.toByte(), 0x00, 0x0C, 0x02, 0xE1.toByte(), 0x03
        )
        private val READ_CAPABILITY_CONTAINER = byteArrayOf(
            0x00, 0xB0.toByte(), 0x00, 0x00, 0x0F
        )
        private val READ_CAPABILITY_CONTAINER_RESPONSE = byteArrayOf(
            0x00, 0x11, 0x20,
            0xFF.toByte(), 0xFF.toByte(),
            0xFF.toByte(), 0xFF.toByte(),
            0x04, 0x06, 0xE1.toByte(), 0x04,
            0xFF.toByte(), 0xFE.toByte(),
            0x00, 0xFF.toByte(),
            0x90.toByte(), 0x00
        )
        private val NDEF_SELECT_OK = byteArrayOf(
            0x00, 0xA4.toByte(), 0x00, 0x0C, 0x02, 0xE1.toByte(), 0x04
        )
        private val NDEF_READ_BINARY_NLEN = byteArrayOf(
            0x00, 0xB0.toByte(), 0x00, 0x00, 0x02
        )
        private val A_OKAY = byteArrayOf(0x90.toByte(), 0x00)
        private val A_ERROR = byteArrayOf(0x6A, 0x82.toByte())
    }
}
