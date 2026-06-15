package com.exhxx.xray_vpn

import android.content.Intent
import android.net.VpnService
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity: FlutterActivity() {
    private val CHANNEL = "com.exhxx.xray_vpn/channel"
    private var pendingConfig: String? = null

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            when (call.method) {
                "startVpn" -> {
                    pendingConfig = call.argument<String>("config")
                    val intent = VpnService.prepare(this)
                    if (intent != null) {
                        startActivityForResult(intent, 100)
                    } else {
                        startXrayService()
                    }
                    result.success("started")
                }
                "stopVpn" -> {
                    val stopIntent = Intent(this, XrayVpnService::class.java)
                    stopIntent.action = "STOP"
                    startService(stopIntent)
                    result.success("stopped")
                }
                else -> result.notImplemented()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == RESULT_OK) {
            startXrayService()
        }
    }

    private fun startXrayService() {
        val startIntent = Intent(this, XrayVpnService::class.java)
        startIntent.action = "START"
        startIntent.putExtra("config", pendingConfig)
        startService(startIntent)
    }
}
