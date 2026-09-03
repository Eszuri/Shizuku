package moe.shizuku.manager.file

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import rikka.shizuku.Shizuku

data class FileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long,
    val canRead: Boolean,
    val canWrite: Boolean
)

object ShizukuFileManager {

    const val ANDROID_DATA_PATH = "/storage/emulated/0/Android/data"
    const val ANDROID_OBB_PATH = "/storage/emulated/0/Android/obb"
    const val ANDROID_MEDIA_PATH = "/storage/emulated/0/Android/media"

    private var fileService: IFileService? = null
    private val connectionListeners = mutableListOf<(Boolean) -> Unit>()

    private val userServiceArgs = Shizuku.UserServiceArgs(
        ComponentName("moe.shizuku.privileged.api", FileService::class.java.name)
    )
        .daemon(false)
        .processNameSuffix("file_service")
        .debuggable(true)
        .version(1)

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            fileService = IFileService.Stub.asInterface(service)
            notifyListeners(true)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            fileService = null
            notifyListeners(false)
        }
    }

    fun isConnected(): Boolean = fileService != null && fileService?.asBinder()?.isBinderAlive == true

    fun addConnectionListener(listener: (Boolean) -> Unit) {
        if (!connectionListeners.contains(listener)) {
            connectionListeners.add(listener)
        }
        listener(isConnected())
    }

    fun removeConnectionListener(listener: (Boolean) -> Unit) {
        connectionListeners.remove(listener)
    }

    private fun notifyListeners(connected: Boolean) {
        connectionListeners.forEach { it(connected) }
    }

    fun connect(callback: ((Boolean) -> Unit)? = null) {
        if (isConnected()) {
            callback?.invoke(true)
            return
        }

        if (!Shizuku.pingBinder()) {
            callback?.invoke(false)
            return
        }

        try {
            Shizuku.bindUserService(userServiceArgs, serviceConnection)
            callback?.invoke(true)
        } catch (e: Throwable) {
            e.printStackTrace()
            callback?.invoke(false)
        }
    }

    fun disconnect() {
        try {
            if (isConnected()) {
                Shizuku.unbindUserService(userServiceArgs, serviceConnection, true)
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        } finally {
            fileService = null
            notifyListeners(false)
        }
    }

    suspend fun listFilesDetailed(path: String): List<FileItem> = withContext(Dispatchers.IO) {
        val service = fileService ?: return@withContext emptyList()
        val jsonStr = try {
            service.getDetailedFilesJson(path)
        } catch (e: Throwable) {
            e.printStackTrace()
            return@withContext emptyList()
        }

        val list = mutableListOf<FileItem>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    FileItem(
                        name = obj.optString("name", ""),
                        path = obj.optString("path", ""),
                        isDirectory = obj.optBoolean("isDirectory", false),
                        size = obj.optLong("size", 0L),
                        lastModified = obj.optLong("lastModified", 0L),
                        canRead = obj.optBoolean("canRead", true),
                        canWrite = obj.optBoolean("canWrite", true)
                    )
                )
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        list.sortedWith(compareByDescending<FileItem> { it.isDirectory }.thenBy { it.name.lowercase() })
    }

    suspend fun readFile(path: String): ByteArray = withContext(Dispatchers.IO) {
        try {
            fileService?.readFile(path) ?: ByteArray(0)
        } catch (e: Throwable) {
            e.printStackTrace()
            ByteArray(0)
        }
    }

    suspend fun writeFile(path: String, data: ByteArray): Boolean = withContext(Dispatchers.IO) {
        try {
            fileService?.writeFile(path, data) ?: false
        } catch (e: Throwable) {
            e.printStackTrace()
            false
        }
    }

    suspend fun copy(src: String, dst: String): Boolean = withContext(Dispatchers.IO) {
        try {
            fileService?.copy(src, dst) ?: false
        } catch (e: Throwable) {
            e.printStackTrace()
            false
        }
    }

    suspend fun move(src: String, dst: String): Boolean = withContext(Dispatchers.IO) {
        try {
            fileService?.move(src, dst) ?: false
        } catch (e: Throwable) {
            e.printStackTrace()
            false
        }
    }

    suspend fun delete(path: String): Boolean = withContext(Dispatchers.IO) {
        try {
            fileService?.delete(path) ?: false
        } catch (e: Throwable) {
            e.printStackTrace()
            false
        }
    }

    suspend fun mkdirs(path: String): Boolean = withContext(Dispatchers.IO) {
        try {
            fileService?.mkdirs(path) ?: false
        } catch (e: Throwable) {
            e.printStackTrace()
            false
        }
    }

    suspend fun exists(path: String): Boolean = withContext(Dispatchers.IO) {
        try {
            fileService?.exists(path) ?: false
        } catch (e: Throwable) {
            e.printStackTrace()
            false
        }
    }
}
