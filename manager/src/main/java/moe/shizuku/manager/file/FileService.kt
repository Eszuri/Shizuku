package moe.shizuku.manager.file

import android.os.Process
import androidx.annotation.Keep
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

@Keep
class FileService : IFileService.Stub() {

    override fun destroy() {
        Process.killProcess(Process.myPid())
    }

    override fun exists(path: String?): Boolean {
        if (path.isNullOrEmpty()) return false
        return File(path).exists()
    }

    override fun isDirectory(path: String?): Boolean {
        if (path.isNullOrEmpty()) return false
        return File(path).isDirectory
    }

    override fun listFiles(path: String?): List<String> {
        if (path.isNullOrEmpty()) return emptyList()
        val file = File(path)
        val files = file.listFiles() ?: return emptyList()
        return files.map { it.name }
    }

    override fun getDetailedFilesJson(path: String?): String {
        if (path.isNullOrEmpty()) return "[]"
        val dir = File(path)
        val files = dir.listFiles() ?: return "[]"
        val array = JSONArray()
        for (f in files) {
            val obj = JSONObject().apply {
                put("name", f.name)
                put("path", f.absolutePath)
                put("isDirectory", f.isDirectory)
                put("size", if (f.isDirectory) (f.listFiles()?.size?.toLong() ?: 0L) else f.length())
                put("lastModified", f.lastModified())
                put("canRead", f.canRead())
                put("canWrite", f.canWrite())
            }
            array.put(obj)
        }
        return array.toString()
    }

    override fun readFile(path: String?): ByteArray {
        if (path.isNullOrEmpty()) return ByteArray(0)
        val file = File(path)
        if (!file.exists() || file.isDirectory) return ByteArray(0)
        return try {
            file.readBytes()
        } catch (e: Throwable) {
            e.printStackTrace()
            ByteArray(0)
        }
    }

    override fun writeFile(path: String?, data: ByteArray?): Boolean {
        if (path.isNullOrEmpty() || data == null) return false
        return try {
            val file = File(path)
            file.parentFile?.mkdirs()
            file.writeBytes(data)
            true
        } catch (e: Throwable) {
            e.printStackTrace()
            false
        }
    }

    override fun copy(srcPath: String?, destPath: String?): Boolean {
        if (srcPath.isNullOrEmpty() || destPath.isNullOrEmpty()) return false
        return try {
            val src = File(srcPath)
            val dest = File(destPath)
            if (!src.exists()) return false
            if (src.isDirectory) {
                src.copyRecursively(dest, overwrite = true)
            } else {
                dest.parentFile?.mkdirs()
                src.copyTo(dest, overwrite = true)
            }
            true
        } catch (e: Throwable) {
            e.printStackTrace()
            false
        }
    }

    override fun move(srcPath: String?, destPath: String?): Boolean {
        if (srcPath.isNullOrEmpty() || destPath.isNullOrEmpty()) return false
        return try {
            val src = File(srcPath)
            val dest = File(destPath)
            if (!src.exists()) return false
            dest.parentFile?.mkdirs()
            if (src.renameTo(dest)) {
                true
            } else {
                if (copy(srcPath, destPath)) {
                    delete(srcPath)
                } else {
                    false
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            false
        }
    }

    override fun delete(path: String?): Boolean {
        if (path.isNullOrEmpty()) return false
        return try {
            val file = File(path)
            if (file.isDirectory) {
                file.deleteRecursively()
            } else {
                file.delete()
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            false
        }
    }

    override fun mkdirs(path: String?): Boolean {
        if (path.isNullOrEmpty()) return false
        return try {
            File(path).mkdirs()
        } catch (e: Throwable) {
            e.printStackTrace()
            false
        }
    }

    override fun getFileSize(path: String?): Long {
        if (path.isNullOrEmpty()) return 0L
        return File(path).length()
    }
}
