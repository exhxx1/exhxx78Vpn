package com.exhxx.xray_vpn

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log

class XrayVpnService : VpnService() {
    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START" -> {
                val config = intent.getStringExtra("config") ?: return START_NOT_STICKY
                startVpn(config)
            }
            "STOP" -> stopVpn()
        }
        return START_NOT_STICKY
    }

    private fun startVpn(configJson: String) {
        if (vpnInterface != null) stopVpn()

        try {
            val builder = Builder()
            builder.setSession("Exhxx Xray VLESS")
            builder.addAddress("10.0.0.2", 32)
            builder.addDnsServer("1.1.1.1") // توجيه الـ DNS كما طلبت
            builder.addRoute("0.0.0.0", 0)
            
            builder.addDisallowedApplication(packageName) // منع التطبيق نفسه من الدخول للنفق

            vpnInterface = builder.establish()
            Log.d("EXHXX_XRAY", "TUN Interface created! Routing all traffic. Config: \$configJson")
            
            // هنا يتم استدعاء مكتبة libXray.aar الفعيلة
            // XrayCore.start(configJson, vpnInterface.fd)

        } catch (e: Exception) {
            Log.e("EXHXX_XRAY", "Error starting VPN: \${e.message}")
            stopVpn()
        }
    }

    private fun stopVpn() {
        try {
            // XrayCore.stop()
            vpnInterface?.close()
            vpnInterface = null
            Log.d("EXHXX_XRAY", "VPN Stopped")
        } catch (e: Exception) {
            e.printStackTrace()
        }
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopVpn()
    }
}
