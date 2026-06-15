package com.exhxx.xray_vpn

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import android.os.Build
import android.net.ProxyInfo
import libv2ray.Libv2ray

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
            // 1. تشغيل المحرك بالخلفية لفتح بورت 10809
            Thread {
                try {
                    Libv2ray.startV2Ray(configJson)
                    Log.d("EXHXX_XRAY", "Xray Engine Started on HTTP 10809")
                } catch (e: Exception) {
                    Log.e("EXHXX_XRAY", "Engine Crash: ${e.message}")
                }
            }.start()

            // إعطاء المحرك ثانية للتحميل قبل فتح النفق
            Thread.sleep(1000)

            // 2. إنشاء نفق الـ VPN وتوجيهه
            val builder = Builder()
            builder.setSession("Exhxx Xray VLESS")
            builder.addAddress("10.0.0.2", 32)
            builder.addRoute("0.0.0.0", 0)
            builder.addDnsServer("1.1.1.1")

            // السر الهندسي: إجبار نظام الأندرويد على إرسال بيانات الـ VPN إلى بورت المحرك
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                builder.setHttpProxy(ProxyInfo.buildDirectProxy("127.0.0.1", 10809))
            }

            builder.addDisallowedApplication(packageName)
            vpnInterface = builder.establish()
            Log.d("EXHXX_XRAY", "TUN Interface created and routed to HTTP Proxy!")

        } catch (e: Exception) {
            Log.e("EXHXX_XRAY", "Error starting VPN: ${e.message}")
            stopVpn()
        }
    }

    private fun stopVpn() {
        try {
            Libv2ray.stopV2Ray()
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
