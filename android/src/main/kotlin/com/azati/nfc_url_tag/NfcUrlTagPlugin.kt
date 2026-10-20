package com.azati.nfc_url_tag

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.nfc.NfcManager
import android.nfc.cardemulation.CardEmulation
import android.os.Build
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.embedding.engine.plugins.lifecycle.HiddenLifecycleReference
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result

class NfcUrlTagPlugin : FlutterPlugin, ActivityAware, MethodCallHandler {

    private lateinit var appContext: Context
    private lateinit var methodChannel: MethodChannel
    private lateinit var tagReadEventChannel: EventChannel
    private lateinit var nfcStatusEventChannel: EventChannel

    private var activity: Activity? = null
    private var activityBinding: ActivityPluginBinding? = null
    private val lifecycleObserver = LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_RESUME -> onActivityResume()
            Lifecycle.Event.ON_PAUSE -> onActivityPause()
            else -> Unit
        }
    }

    private val tagReadStreamHandler = TagReadStreamHandler()
    private val nfcStatusStreamHandler = NfcStatusStreamHandler()

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        appContext = binding.applicationContext
        methodChannel = MethodChannel(binding.binaryMessenger, CHANNEL_METHODS)
        methodChannel.setMethodCallHandler(this)
        tagReadEventChannel = EventChannel(binding.binaryMessenger, CHANNEL_TAG_READ)
        tagReadEventChannel.setStreamHandler(tagReadStreamHandler)
        nfcStatusEventChannel = EventChannel(binding.binaryMessenger, CHANNEL_NFC_STATUS)
        nfcStatusEventChannel.setStreamHandler(nfcStatusStreamHandler)
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        if (HostCardEmulationService.isRunning) {
            activity?.let { unsetPreferredService(it) }
            stopHceService()
            setComponentEnabled(appContext, HostCardEmulationService::class.java, false)
        }
        methodChannel.setMethodCallHandler(null)
        tagReadEventChannel.setStreamHandler(null)
        nfcStatusEventChannel.setStreamHandler(null)
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {
            "isNfcAvailable" -> result.success(getNfcAdapter() != null)
            "isHceSupported" -> result.success(
                getNfcAdapter() != null &&
                    appContext.packageManager.hasSystemFeature(
                        PackageManager.FEATURE_NFC_HOST_CARD_EMULATION
                    )
            )
            "isNfcEnabled" -> result.success(getNfcAdapter()?.isEnabled ?: false)
            "openNfcSettings" -> {
                val intent = Intent(android.provider.Settings.ACTION_NFC_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    appContext.startActivity(intent)
                    result.success(null)
                } catch (e: android.content.ActivityNotFoundException) {
                    result.error("NO_NFC_SETTINGS", "NFC settings activity not found", null)
                }
            }
            "startSharing" -> {
                val url = call.argument<String>("url")
                val currentActivity = activity
                if (url == null) {
                    result.error("ARG_URL_MISSING", "url argument is required", null)
                    return
                }
                if (!isValidUri(url)) {
                    result.error(
                        "ARG_URL_INVALID",
                        "url must be an absolute URI with a valid scheme",
                        null
                    )
                    return
                }
                if (currentActivity == null) {
                    result.error(
                        "NO_ACTIVITY",
                        "startSharing requires a foreground activity",
                        null
                    )
                    return
                }
                startSharing(currentActivity, url)
                result.success(null)
            }
            "stopSharing" -> {
                stopSharing(activity)
                result.success(null)
            }
            else -> result.notImplemented()
        }
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        bindActivity(binding)
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        bindActivity(binding)
    }

    override fun onDetachedFromActivityForConfigChanges() {
        unbindActivity()
    }

    override fun onDetachedFromActivity() {
        unbindActivity()
    }

    private fun bindActivity(binding: ActivityPluginBinding) {
        activityBinding = binding
        activity = binding.activity
        (binding.lifecycle as HiddenLifecycleReference).lifecycle.addObserver(lifecycleObserver)
    }

    private fun unbindActivity() {
        activity?.takeIf { HostCardEmulationService.isRunning }
            ?.let { unsetPreferredService(it) }
        activityBinding?.let { binding ->
            (binding.lifecycle as HiddenLifecycleReference).lifecycle.removeObserver(
                lifecycleObserver
            )
        }
        activityBinding = null
        activity = null
    }

    private fun onActivityResume() {
        val act = activity ?: return
        if (!HostCardEmulationService.isRunning) return
        setComponentEnabled(appContext, HostCardEmulationService::class.java, true)
        setPreferredService(act)
    }

    private fun onActivityPause() {
        val act = activity ?: return
        if (!HostCardEmulationService.isRunning) return
        unsetPreferredService(act)
    }

    private fun startSharing(activity: Activity, url: String) {
        stopHceService()
        setComponentEnabled(appContext, HostCardEmulationService::class.java, true)
        startHceService(url)
        setPreferredService(activity)
    }

    private fun stopSharing(activity: Activity?) {
        activity?.let { unsetPreferredService(it) }
        stopHceService()
        setComponentEnabled(appContext, HostCardEmulationService::class.java, false)
    }

    private fun startHceService(url: String) {
        val intent = Intent(appContext, HostCardEmulationService::class.java)
            .putExtra(HostCardEmulationService.EXTRA_URL, url)
        appContext.startService(intent)
    }

    private fun stopHceService() {
        appContext.stopService(Intent(appContext, HostCardEmulationService::class.java))
    }

    private fun getNfcAdapter(): NfcAdapter? {
        val manager = appContext.getSystemService(Context.NFC_SERVICE) as? NfcManager
        return manager?.defaultAdapter
    }

    private fun setPreferredService(activity: Activity) {
        val adapter = getNfcAdapter() ?: return
        CardEmulation.getInstance(adapter).setPreferredService(
            activity,
            ComponentName(activity, HostCardEmulationService::class.java)
        )
    }

    private fun unsetPreferredService(activity: Activity) {
        val adapter = getNfcAdapter() ?: return
        CardEmulation.getInstance(adapter).unsetPreferredService(activity)
    }

    private fun isValidUri(url: String): Boolean {
        if (url.isBlank() || url.any { it.isWhitespace() }) return false
        val parsed = runCatching { android.net.Uri.parse(url) }.getOrNull() ?: return false
        val scheme = parsed.scheme ?: return false
        return SCHEME_REGEX.matches(scheme)
    }

    private inner class TagReadStreamHandler : EventChannel.StreamHandler {
        private var receiver: TagReadReceiver? = null

        override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
            val sink = events ?: return
            val r = TagReadReceiver { sink.success(null) }
            receiver = r
            val filter = IntentFilter(TagReadReceiver.ACTION_TAG_READ)
            registerInternalReceiver(appContext, r, filter)
        }

        override fun onCancel(arguments: Any?) {
            receiver?.let { runCatching { appContext.unregisterReceiver(it) } }
            receiver = null
        }
    }

    private inner class NfcStatusStreamHandler : EventChannel.StreamHandler {
        private var receiver: NfcAdapterStateReceiver? = null

        override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
            val sink = events ?: return
            val r = NfcAdapterStateReceiver { enabled -> sink.success(enabled) }
            receiver = r
            val filter = IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED)
            registerInternalReceiver(appContext, r, filter)
        }

        override fun onCancel(arguments: Any?) {
            receiver?.let { runCatching { appContext.unregisterReceiver(it) } }
            receiver = null
        }
    }

    companion object {
        private const val CHANNEL_METHODS = "com.azati.nfc_url_tag/methods"
        private const val CHANNEL_TAG_READ = "com.azati.nfc_url_tag/events/tag_read"
        private const val CHANNEL_NFC_STATUS = "com.azati.nfc_url_tag/events/nfc_status"

        private val SCHEME_REGEX = Regex("^[a-zA-Z][a-zA-Z0-9+.\\-]*$")

        internal fun setComponentEnabled(
            context: Context,
            component: Class<*>,
            enabled: Boolean
        ) {
            val state =
                if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            context.packageManager.setComponentEnabledSetting(
                ComponentName(context, component),
                state,
                PackageManager.DONT_KILL_APP
            )
        }

        internal fun registerInternalReceiver(
            context: Context,
            receiver: android.content.BroadcastReceiver,
            filter: IntentFilter
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(receiver, filter)
            }
        }
    }
}
