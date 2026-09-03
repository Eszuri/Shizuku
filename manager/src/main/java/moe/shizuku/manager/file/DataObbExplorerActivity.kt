package moe.shizuku.manager.file

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import moe.shizuku.manager.R
import moe.shizuku.manager.app.AppActivity
import moe.shizuku.manager.databinding.ActivityDataObbExplorerBinding
import moe.shizuku.manager.databinding.ItemDataObbFileBinding
import rikka.shizuku.Shizuku
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Stack

class DataObbExplorerActivity : AppActivity() {

    private lateinit var binding: ActivityDataObbExplorerBinding
    private val pathStack = Stack<String>()
    private var currentPath: String = ShizukuFileManager.ANDROID_DATA_PATH
    private val filesList = mutableListOf<FileItem>()
    private lateinit var adapter: FileListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDataObbExplorerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupTabs()
        setupRecyclerView()
        setupButtons()

        ShizukuFileManager.addConnectionListener { connected ->
            runOnUiThread {
                if (connected) {
                    loadDirectory(currentPath)
                } else {
                    binding.tvEmptyMessage.text = getString(R.string.msg_service_not_connected)
                    binding.layoutEmpty.visibility = View.VISIBLE
                }
            }
        }

        if (Shizuku.pingBinder()) {
            ShizukuFileManager.connect()
        } else {
            Toast.makeText(this, getString(R.string.home_status_service_not_running, getString(R.string.app_name)), Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ShizukuFileManager.removeConnectionListener { }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        binding.btnNavUp.setOnClickListener {
            navigateUp()
        }
    }

    private fun setupTabs() {
        binding.chipGroupLocations.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            pathStack.clear()
            when (checkedIds.first()) {
                R.id.chip_data -> currentPath = ShizukuFileManager.ANDROID_DATA_PATH
                R.id.chip_obb -> currentPath = ShizukuFileManager.ANDROID_OBB_PATH
                R.id.chip_media -> currentPath = ShizukuFileManager.ANDROID_MEDIA_PATH
            }
            loadDirectory(currentPath)
        }
    }

    private fun setupRecyclerView() {
        adapter = FileListAdapter(
            files = filesList,
            onItemClick = { item ->
                if (item.isDirectory) {
                    pathStack.push(currentPath)
                    currentPath = item.path
                    loadDirectory(currentPath)
                } else {
                    showFileDetailsDialog(item)
                }
            },
            onDeleteClick = { item ->
                confirmDelete(item)
            }
        )
        binding.recyclerFiles.layoutManager = LinearLayoutManager(this)
        binding.recyclerFiles.adapter = adapter
    }

    private fun setupButtons() {
        binding.fabRefresh.setOnClickListener {
            loadDirectory(currentPath)
        }
        binding.fabNewFolder.setOnClickListener {
            showCreateFolderDialog()
        }
    }

    private fun loadDirectory(path: String) {
        binding.tvCurrentPath.text = path
        binding.progressLoading.visibility = View.VISIBLE
        binding.layoutEmpty.visibility = View.GONE

        lifecycleScope.launch {
            val result = ShizukuFileManager.listFilesDetailed(path)
            binding.progressLoading.visibility = View.GONE
            filesList.clear()
            filesList.addAll(result)
            adapter.notifyDataSetChanged()

            if (filesList.isEmpty()) {
                binding.tvEmptyMessage.text = getString(R.string.msg_empty_folder)
                binding.layoutEmpty.visibility = View.VISIBLE
            } else {
                binding.layoutEmpty.visibility = View.GONE
            }
        }
    }

    private fun navigateUp() {
        if (pathStack.isNotEmpty()) {
            currentPath = pathStack.pop()
            loadDirectory(currentPath)
        } else {
            // Check if current path has parent within Android
            val current = java.io.File(currentPath)
            val parent = current.parent
            if (parent != null && parent.contains("/Android")) {
                currentPath = parent
                loadDirectory(currentPath)
            } else {
                finish()
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (pathStack.isNotEmpty()) {
            navigateUp()
        } else {
            super.onBackPressed()
        }
    }

    private fun showCreateFolderDialog() {
        val input = EditText(this).apply {
            hint = getString(R.string.hint_folder_name)
            setSingleLine()
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.btn_create_folder)
            .setView(input)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    val targetPath = "$currentPath/$name"
                    lifecycleScope.launch {
                        val success = ShizukuFileManager.mkdirs(targetPath)
                        if (success) {
                            Toast.makeText(this@DataObbExplorerActivity, R.string.msg_folder_created, Toast.LENGTH_SHORT).show()
                            loadDirectory(currentPath)
                        } else {
                            Toast.makeText(this@DataObbExplorerActivity, R.string.msg_folder_create_failed, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun confirmDelete(item: FileItem) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.btn_delete)
            .setMessage(getString(R.string.msg_confirm_delete, item.name))
            .setPositiveButton(R.string.btn_delete) { _, _ ->
                lifecycleScope.launch {
                    val success = ShizukuFileManager.delete(item.path)
                    if (success) {
                        Toast.makeText(this@DataObbExplorerActivity, R.string.msg_file_deleted, Toast.LENGTH_SHORT).show()
                        loadDirectory(currentPath)
                    } else {
                        Toast.makeText(this@DataObbExplorerActivity, R.string.msg_delete_failed, Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showFileDetailsDialog(item: FileItem) {
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(item.lastModified))
        val sizeStr = formatFileSize(item.size)

        val message = StringBuilder().apply {
            appendLine(getString(R.string.file_details_name, item.name))
            appendLine(getString(R.string.file_details_path, item.path))
            appendLine(getString(R.string.file_details_size, sizeStr))
            appendLine(getString(R.string.file_details_modified, dateStr))
            appendLine(getString(R.string.file_details_readable, item.canRead.toString()))
            append(getString(R.string.file_details_writable, item.canWrite.toString()))
        }.toString()

        MaterialAlertDialogBuilder(this)
            .setTitle(item.name)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .setNeutralButton(R.string.btn_read_preview) { _, _ ->
                lifecycleScope.launch {
                    val bytes = ShizukuFileManager.readFile(item.path)
                    val preview = if (bytes.size > 2048) {
                        String(bytes.copyOfRange(0, 2048)) + "\n... [Truncated]"
                    } else {
                        String(bytes)
                    }
                    MaterialAlertDialogBuilder(this@DataObbExplorerActivity)
                        .setTitle(getString(R.string.title_file_preview, item.name))
                        .setMessage(preview)
                        .setPositiveButton(android.R.string.ok, null)
                        .show()
                }
            }
            .show()
    }

    private fun formatFileSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        val index = digitGroups.coerceIn(0, units.size - 1)
        return String.format(Locale.getDefault(), "%.2f %s", size / Math.pow(1024.0, index.toDouble()), units[index])
    }

    private class FileListAdapter(
        private val files: List<FileItem>,
        private val onItemClick: (FileItem) -> Unit,
        private val onDeleteClick: (FileItem) -> Unit
    ) : RecyclerView.Adapter<FileListAdapter.ViewHolder>() {

        class ViewHolder(val binding: ItemDataObbFileBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemDataObbFileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = files[position]
            holder.binding.fileName.text = item.name

            if (item.isDirectory) {
                holder.binding.fileIcon.setImageResource(R.drawable.ic_folder_24)
                holder.binding.fileDetails.text = "${item.size} items"
            } else {
                holder.binding.fileIcon.setImageResource(R.drawable.ic_file_24)
                val sizeStr = formatSize(item.size)
                val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(item.lastModified))
                holder.binding.fileDetails.text = "$sizeStr • $dateStr"
            }

            holder.binding.root.setOnClickListener { onItemClick(item) }
            holder.binding.btnDeleteFile.setOnClickListener { onDeleteClick(item) }
        }

        override fun getItemCount(): Int = files.size

        private fun formatSize(size: Long): String {
            if (size <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
            val index = digitGroups.coerceIn(0, units.size - 1)
            return String.format(Locale.getDefault(), "%.1f %s", size / Math.pow(1024.0, index.toDouble()), units[index])
        }
    }
}
