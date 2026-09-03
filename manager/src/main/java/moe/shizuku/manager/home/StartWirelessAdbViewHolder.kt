package moe.shizuku.manager.home

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Build
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import moe.shizuku.manager.R
import moe.shizuku.manager.adb.AdbPairingTutorialActivity
import moe.shizuku.manager.databinding.HomeItemContainerBinding
import moe.shizuku.manager.databinding.HomeStartWirelessAdbBinding
import moe.shizuku.manager.ktx.toHtml
import moe.shizuku.manager.model.ServiceStatus
import moe.shizuku.manager.starter.StarterActivity
import moe.shizuku.manager.utils.EnvironmentUtils
import rikka.core.content.asActivity
import rikka.html.text.HtmlCompat
import rikka.recyclerview.BaseViewHolder
import rikka.recyclerview.BaseViewHolder.Creator
import rikka.shizuku.Shizuku

class StartWirelessAdbViewHolder(private val binding: HomeStartWirelessAdbBinding, root: View) :
    BaseViewHolder<ServiceStatus>(root) {

    companion object {
        val CREATOR = Creator<ServiceStatus> { inflater: LayoutInflater, parent: ViewGroup? ->
            val outer = HomeItemContainerBinding.inflate(inflater, parent, false)
            val inner = HomeStartWirelessAdbBinding.inflate(inflater, outer.root, true)
            StartWirelessAdbViewHolder(inner, outer.root)
        }
    }

    init {
        binding.button1.setOnClickListener { v: View ->
            if (data.isRunning) {
                onStopClicked(v.context)
            } else {
                onAdbClicked(v.context)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            binding.button3.setOnClickListener { v: View ->
                val intent = Intent(v.context, moe.shizuku.manager.manual.UserManualActivity::class.java)
                v.context.startActivity(intent)
            }
            binding.button2.setOnClickListener { v: View ->
                onPairClicked(v.context)
            }
            binding.text1.movementMethod = LinkMovementMethod.getInstance()
        } else {
            binding.button2.isVisible = false
            binding.button3.isVisible = false
        }
    }

    override fun onBind() {
        val context = itemView.context
        val isRunning = data.isRunning

        if (isRunning) {
            binding.button1.text = context.getString(R.string.home_button_stop)
            binding.button1.setIconResource(R.drawable.ic_close_24)
            val colorError = MaterialColors.getColor(binding.button1, com.google.android.material.R.attr.colorError)
            val colorOnError = MaterialColors.getColor(binding.button1, com.google.android.material.R.attr.colorOnError)
            binding.button1.backgroundTintList = ColorStateList.valueOf(colorError)
            binding.button1.setTextColor(colorOnError)
            binding.button1.iconTint = ColorStateList.valueOf(colorOnError)
        } else {
            binding.button1.text = context.getString(R.string.home_root_button_start)
            binding.button1.setIconResource(R.drawable.ic_server_start_24dp)
            val colorPrimary = MaterialColors.getColor(binding.button1, com.google.android.material.R.attr.colorPrimary)
            val colorOnPrimary = MaterialColors.getColor(binding.button1, com.google.android.material.R.attr.colorOnPrimary)
            binding.button1.backgroundTintList = ColorStateList.valueOf(colorPrimary)
            binding.button1.setTextColor(colorOnPrimary)
            binding.button1.iconTint = ColorStateList.valueOf(colorOnPrimary)
        }

        binding.button2.text = context.getString(R.string.adb_pairing)
        binding.button3.text = context.getString(R.string.home_wireless_adb_view_guide_button)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            binding.text1.text = context.getString(R.string.home_wireless_adb_description)
                .toHtml(HtmlCompat.FROM_HTML_OPTION_TRIM_WHITESPACE)
        } else {
            binding.text1.text = context.getString(R.string.home_wireless_adb_description_pre_11)
                .toHtml(HtmlCompat.FROM_HTML_OPTION_TRIM_WHITESPACE)
        }
    }

    private fun onStopClicked(context: Context) {
        if (!Shizuku.pingBinder()) {
            return
        }
        MaterialAlertDialogBuilder(context)
            .setMessage(R.string.dialog_stop_message)
            .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
                try {
                    Shizuku.exit()
                } catch (e: Throwable) {
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun onAdbClicked(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            AdbDialogFragment().show(context.asActivity<FragmentActivity>().supportFragmentManager)
            return
        }

        val port = EnvironmentUtils.getAdbTcpPort()
        if (port > 0) {
            val host = "127.0.0.1"
            val intent = Intent(context, StarterActivity::class.java).apply {
                putExtra(StarterActivity.EXTRA_HOST, host)
                putExtra(StarterActivity.EXTRA_PORT, port)
            }
            context.startActivity(intent)
        } else {
            WadbNotEnabledDialogFragment().show(context.asActivity<FragmentActivity>().supportFragmentManager)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun onPairClicked(context: Context) {
        if ((context.display?.displayId ?: -1) > 0) {
            // Running in a multi-display environment (e.g., Windows Subsystem for Android),
            // pairing dialog can be displayed simultaneously with Shizuku.
            // Input from notification is harder to use under this situation.
            AdbPairDialogFragment().show(context.asActivity<FragmentActivity>().supportFragmentManager)
        } else {
            context.startActivity(Intent(context, AdbPairingTutorialActivity::class.java))
        }
    }
}
