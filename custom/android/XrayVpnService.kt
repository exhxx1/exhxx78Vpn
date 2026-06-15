package com.exhxx.xray_vpn

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import android.os.Build
import android.net.ProxyInfo
import java.io.File

class XrayVpnService : VpnService() {
    private var vpnInterface: ParcelFileDescriptor? = null
    private var xrayProcess: Process? = null

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
            // 1. حفظ الكونفيغ المدمج في ملف ليقرأه المحرك
            val configFile = File(cacheDir, "config.json")
            configFile.writeText(configJson)

            // 2. تحديد مسار المحرك الأصلي المدمج بالتطبيق
            val xrayPath = applicationInfo.nativeLibraryDir + "/libxraycore.so"
            
            // 3. تشغيل المحرك بقوة النظام (Native Execution)
            Thread {
                try {
                    val pb = ProcessBuilder(xrayPath, "-c", configFile.absolutePath)
                    pb.redirectErrorStream(true)
                    xrayProcess = pb.start()
                    Log.d("EXHXX_XRAY", "Native Xray Core Executed Successfully!")
                    
                    val reader = xrayProcess?.inputStream?.bufferedReader()
                    var line: String?
                    while (reader?.readLine().also { line = it } != null) {
                        Log.d("EXHXX_XRAY_LOG", line ?: "")
                    }
                } catch (e: Exception) {
                    Log.e("EXHXX_XRAY", "Engine Execute Error: ${e.message}")
                }
            }.start()

            // إعطاء المحرك ثانية للعمل وفتح بورت 10809
            Thread.sleep(1500)

            // 4. إنشاء وتوجيه النفق
            val builder = Builder()
            builder.setSession("Exhxx Xray VLESS")
            builder.addAddress("10.0.0.2", 32)
            builder.addRoute("0.0.0.0", 0)
            builder.addDnsServer("1.1.1.1")

            // ربط الإنترنت بالمحرك مباشرة (خدعة أندرويد 10+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                builder.setHttpProxy(ProxyInfo.buildDirectProxy("127.0.0.1", 10809))
            }

            builder.addDisallowedApplication(packageName)
            vpnInterface = builder.establish()
            Log.d("EXHXX_XRAY", "TUN Interface created and routed to Native Engine!")

        } catch (e: Exception) {
            Log.e("EXHXX_XRAY", "Error starting VPN: ${e.message}")
            stopVpn()
        }
    }

    private fun stopVpn() {
        try {
            xrayProcess?.destroy() // قتل المحرك عند إيقاف الـ VPN
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
