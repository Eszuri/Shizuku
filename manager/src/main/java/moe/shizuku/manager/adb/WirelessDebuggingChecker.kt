package moe.shizuku.manager.adb

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.lifecycle.Observer
import moe.shizuku.manager.R
import moe.shizuku.manager.utils.EnvironmentUtils
import rikka.shizuku.Shizuku

object WirelessDebuggingChecker {

    fun checkStatus(context: Context, onStatus: ((Boolean, String) -> Unit)? = null) {
        // 1. If Shizuku service is already running, Wireless ADB is working and active
        if (Shizuku.pingBinder()) {
            val msg = context.getString(R.string.wadb_status_connected_service_running)
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            onStatus?.invoke(true, msg)
            return
        }

        // 2. Check direct ADB TCP port property
        val tcpPort = EnvironmentUtils.getAdbTcpPort()
        if (tcpPort > 0) {
            val msg = context.getString(R.string.wadb_status_port_detected, tcpPort)
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            onStatus?.invoke(true, msg)
            return
        }

        // 3. On Android 11+, probe mDNS service discovery for active Wireless Debugging
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            var resolved = false
            var adbMdns: AdbMdns? = null

            val handler = Handler(Looper.getMainLooper())
            val timeoutRunnable = Runnable {
                if (!resolved) {
                    try {
                        adbMdns?.stop()
                    } catch (_: Throwable) {
                    }
                    val msg = context.getString(R.string.wadb_status_not_connected)
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    onStatus?.invoke(false, msg)
                }
            }

            val observer = object : Observer<Int> {
                override fun onChanged(port: Int) {
                    if (port in 1..65535 && !resolved) {
                        resolved = true
                        handler.removeCallbacks(timeoutRunnable)
                        try {
                            adbMdns?.stop()
                        } catch (_: Throwable) {
                        }
                        val msg = context.getString(R.string.wadb_status_port_detected, port)
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        onStatus?.invoke(true, msg)
                    }
                }
            }

            try {
                adbMdns = AdbMdns(context, AdbMdns.TLS_CONNECT, observer)
                adbMdns.start()
                handler.postDelayed(timeoutRunnable, 2500)
            } catch (e: Throwable) {
                e.printStackTrace()
                val msg = context.getString(R.string.wadb_status_not_connected)
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                onStatus?.invoke(false, msg)
            }
        } else {
            val msg = context.getString(R.string.wadb_status_not_connected)
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            onStatus?.invoke(false, msg)
        }
    }
}
