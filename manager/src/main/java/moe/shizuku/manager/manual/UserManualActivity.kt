package moe.shizuku.manager.manual

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.MenuItem
import moe.shizuku.manager.R
import moe.shizuku.manager.app.AppBarActivity
import moe.shizuku.manager.databinding.ActivityUserManualBinding
import moe.shizuku.manager.ktx.toHtml

class UserManualActivity : AppBarActivity() {

    private lateinit var binding: ActivityUserManualBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserManualBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setTitle(R.string.user_manual_title)
        }

        // Action buttons
        binding.btnOpenDevOptions.setOnClickListener {
            val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra(":settings:fragment_args_key", "toggle_adb_wireless")
            }
            try {
                startActivity(intent)
            } catch (_: ActivityNotFoundException) {
            }
        }

        binding.btnOpenNotificationSettings.setOnClickListener {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            }
            try {
                startActivity(intent)
            } catch (_: ActivityNotFoundException) {
            }
        }

        // Formatted section contents
        binding.textSec1.text = getString(R.string.manual_sec1_content).toHtml()
        binding.textAlertOppo.text = getString(R.string.manual_alert_oppo_content).toHtml()
        binding.textSec2.text = getString(R.string.manual_sec2_content).toHtml()
        binding.textSec3.text = getString(R.string.manual_sec3_content).toHtml()
        binding.textSec4.text = getString(R.string.manual_sec4_content).toHtml()
        binding.textSec5.text = getString(R.string.manual_sec5_content).toHtml()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
