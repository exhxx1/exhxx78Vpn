import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'dart:io';
import 'package:file_picker/file_picker.dart';

void main() {
  runApp(const ExhxxVpnApp());
}

class ExhxxVpnApp extends StatelessWidget {
  const ExhxxVpnApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      theme: ThemeData.dark().copyWith(
        scaffoldBackgroundColor: const Color(0xFF121212),
        primaryColor: Colors.tealAccent,
      ),
      home: const VpnHomeScreen(),
    );
  }
}

class VpnHomeScreen extends StatefulWidget {
  const VpnHomeScreen({super.key});

  @override
  State<VpnHomeScreen> createState() => _VpnHomeScreenState();
}

class _VpnHomeScreenState extends State<VpnHomeScreen> {
  static const platform = MethodChannel('com.exhxx.xray_vpn/channel');

  bool isConnected = false;
  String currentIp = "Unknown";
  String configJson = "";

  // الكونفيغ المخصص مالتك مع Inbound من نوع HTTP لسهولة ربطه بالنفق
  final String defaultVlessConfig = '''
  {
    "inbounds": [{
      "port": 10809,
      "listen": "127.0.0.1",
      "protocol": "http",
      "settings": {
        "allowTransparent": false
      }
    }],
    "outbounds": [
      {
        "tag": "proxy",
        "protocol": "vless",
        "settings": {
          "vnext": [
            {
              "address": "51.254.130.47",
              "port": 80,
              "users": [
                {
                  "id": "@exhxx78",
                  "encryption": "none",
                  "level": 0
                }
              ]
            }
          ]
        },
        "streamSettings": {
          "network": "tcp",
          "security": "none",
          "tcpSettings": {
            "header": {
              "type": "http",
              "request": {
                "version": "1.1",
                "method": "HTTP/78 2026",
                "path": ["/"],
                "headers": {
                  "Host": ["51.254.130.47"],
                  "User-Agent": [
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36"
                  ],
                  "Accept-Encoding": ["gzip, deflate"],
                  "Connection": ["keep-alive"],
                  "Pragma": "no-cache"
                }
              }
            }
          }
        }
      }
    ],
    "routing": { "domainStrategy": "AsIs", "rules": [] }
  }
  ''';

  @override
  void initState() {
    super.initState();
    configJson = defaultVlessConfig;
  }

  Future<void> importConfig() async {
    FilePickerResult? result = await FilePicker.platform.pickFiles(type: FileType.custom, allowedExtensions: ['json']);
    if (result != null) {
      File file = File(result.files.single.path!);
      String content = await file.readAsString();
      setState(() { configJson = content; });
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('تم استيراد الكونفيغ بنجاح! 🚀')));
    }
  }

  Future<void> toggleVpn() async {
    try {
      if (isConnected) {
        await platform.invokeMethod('stopVpn');
        setState(() { isConnected = false; currentIp = "Unknown"; });
      } else {
        final String result = await platform.invokeMethod('startVpn', {"config": configJson});
        if (result == "started") {
          setState(() { isConnected = true; currentIp = "10.0.0.2 (TUN)"; });
        }
      }
    } on PlatformException catch (e) {
      debugPrint("Error: '${e.message}'.");
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('EXHXX XRAY VPN 🛡️'), centerTitle: true, backgroundColor: Colors.black),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(
              isConnected ? Icons.shield : Icons.shield_outlined,
              size: 120,
              color: isConnected ? Colors.tealAccent : Colors.grey,
            ),
            const SizedBox(height: 20),
            Text(isConnected ? 'متصل 🟢' : 'غير متصل  🔴', style: const TextStyle(fontSize: 24, fontWeight: FontWeight.bold)),
            const SizedBox(height: 10),
            Text("IP: $currentIp", style: const TextStyle(color: Colors.grey, fontSize: 16)),
            const SizedBox(height: 50),
            ElevatedButton(
              style: ElevatedButton.styleFrom(
                backgroundColor: isConnected ? Colors.redAccent : Colors.tealAccent,
                foregroundColor: Colors.black,
                padding: const EdgeInsets.symmetric(horizontal: 50, vertical: 15),
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
              ),
              onPressed: toggleVpn,
              child: Text(isConnected ? 'إيقاف الاتصال 🛑' : 'تشغيل الـ VPN 🚀', style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
            ),
            const SizedBox(height: 20),
            OutlinedButton.icon(
              style: OutlinedButton.styleFrom(padding: const EdgeInsets.symmetric(horizontal: 30, vertical: 15)),
              onPressed: importConfig,
              icon: const Icon(Icons.file_upload, color: Colors.white),
              label: const Text('استيراد ملف JSON 📁', style: TextStyle(color: Colors.white)),
            )
          ],
        ),
      ),
    );
  }
}
