package moe.shizuku.manager.starter

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import moe.shizuku.manager.AppConstants.EXTRA
import moe.shizuku.manager.R
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.adb.AdbClient
import moe.shizuku.manager.adb.AdbKey
import moe.shizuku.manager.adb.AdbKeyException
import moe.shizuku.manager.adb.PreferenceAdbKeyStore
import moe.shizuku.manager.app.AppBarActivity
import moe.shizuku.manager.databinding.StarterActivityBinding
import rikka.lifecycle.Resource
import rikka.lifecycle.Status
import rikka.lifecycle.viewModels
import rikka.shizuku.Shizuku
import java.net.ConnectException
import javax.net.ssl.SSLProtocolException

class StarterActivity : AppBarActivity() {

    private val viewModel by viewModels {
        ViewModel(
            this,
            intent.getStringExtra(EXTRA_HOST),
            intent.getIntExtra(EXTRA_PORT, 0)
        )
    }

    private var serviceStartedHandled = false

    private val binderListener = object : Shizuku.OnBinderReceivedListener {
        override fun onBinderReceived() {
            Shizuku.removeBinderReceivedListener(this)
            handleServiceStarted()
        }
    }

    private fun handleServiceStarted() {
        if (serviceStartedHandled) return
        serviceStartedHandled = true
        viewModel.cancelPolling()

        viewModel.appendOutput(getString(R.string.starter_service_started))
        window?.decorView?.postDelayed({
            if (!isFinishing) finish()
        }, 3000)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_close_24)

        val binding = StarterActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel.output.observe(this) {
            val output = it.data!!.trim()
            if (output.endsWith("info: shizuku_starter exit with 0") && !serviceStartedHandled) {
                viewModel.appendOutput("")
                viewModel.appendOutput(getString(R.string.starter_waiting_service))

                if (Shizuku.pingBinder()) {
                    handleServiceStarted()
                    return@observe
                }

                Shizuku.addBinderReceivedListenerSticky(binderListener)

                viewModel.startPollingService {
                    handleServiceStarted()
                }
            } else if (it.status == Status.ERROR) {
                var message = 0
                when (it.error) {
                    is AdbKeyException -> {
                        message = R.string.adb_error_key_store
                    }
                    is ConnectException -> {
                        message = R.string.cannot_connect_port
                    }
                    is SSLProtocolException -> {
                        message = R.string.adb_pair_required
                    }
                }

                if (message != 0) {
                    MaterialAlertDialogBuilder(this)
                        .setMessage(message)
                        .setPositiveButton(android.R.string.ok, null)
                        .show()
                }
            }
            binding.text1.text = output
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.cancelPolling()
        Shizuku.removeBinderReceivedListener(binderListener)
    }

    companion object {
        const val EXTRA_HOST = "$EXTRA.HOST"
        const val EXTRA_PORT = "$EXTRA.PORT"
    }
}

private class ViewModel(private val context: Context, host: String?, port: Int) : androidx.lifecycle.ViewModel() {

    private val sb = StringBuilder()
    private val _output = MutableLiveData<Resource<StringBuilder>>()
    private var pollingJob: kotlinx.coroutines.Job? = null

    val output = _output as LiveData<Resource<StringBuilder>>

    init {
        try {
            startAdb(host ?: "127.0.0.1", port)
        } catch (e: Throwable) {
            postResult(e)
        }
    }

    fun startPollingService(onSuccess: () -> Unit) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch(Dispatchers.Main) {
            var elapsed = 0
            while (elapsed < 8000) {
                kotlinx.coroutines.delay(500)
                elapsed += 500
                if (Shizuku.pingBinder()) {
                    onSuccess()
                    return@launch
                }
            }

            if (!Shizuku.pingBinder()) {
                appendOutput("")
                appendOutput(context.getString(R.string.starter_warn_timeout))
                appendOutput(context.getString(R.string.starter_info_device_warning))
                appendOutput(context.getString(R.string.starter_info_dev_options_guide))
            }
        }
    }

    fun cancelPolling() {
        pollingJob?.cancel()
    }

    fun appendOutput(line: String) {
        sb.appendLine(line)
        postResult()
    }

    private fun postResult(throwable: Throwable? = null) {
        if (throwable == null)
            _output.postValue(Resource.success(sb))
        else
            _output.postValue(Resource.error(throwable, sb))
    }

    private fun startAdb(host: String, port: Int) {
        sb.append(context.getString(R.string.starter_starting_adb, port)).append('\n').append('\n')
        postResult()

        GlobalScope.launch(Dispatchers.IO) {
            val key = try {
                AdbKey(PreferenceAdbKeyStore(ShizukuSettings.getPreferences()), "shizuku")
            } catch (e: Throwable) {
                e.printStackTrace()
                sb.append('\n').append(Log.getStackTraceString(e))

                postResult(AdbKeyException(e))
                return@launch
            }

            AdbClient(host, port, key).runCatching {
                connect()
                shellCommand(Starter.internalCommand) {
                    sb.append(String(it))
                    postResult()
                }
                close()
            }.onFailure {
                it.printStackTrace()

                sb.append('\n').append(Log.getStackTraceString(it))
                postResult(it)
            }
        }
    }
}
