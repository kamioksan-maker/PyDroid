package com.pydroid.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class FileBrowserActivity : AppCompatActivity() {

    private lateinit var recyclerFiles: RecyclerView
    private lateinit var txtPath: TextView
    private lateinit var fileAdapter: FileAdapter
    private val files = mutableListOf<FileItem>()
    private var currentPath: File = File(filesDir)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_browser)

        setSupportActionBar(findViewById(R.id.toolbar))
        findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
            .setNavigationOnClickListener { finish() }

        initViews()
        loadFiles(currentPath)
    }

    private fun initViews() {
        recyclerFiles = findViewById(R.id.recyclerFiles)
        txtPath = findViewById(R.id.txtPath)

        fileAdapter = FileAdapter(files) { file ->
            if (file.isDirectory) {
                currentPath = file
                loadFiles(currentPath)
            } else {
                showFileOptions(file)
            }
        }
        recyclerFiles.adapter = fileAdapter
    }

    private fun loadFiles(path: File) {
        files.clear()
        txtPath.text = path.absolutePath

        if (path != filesDir) {
            files.add(FileItem(path.parentFile, true))
        }

        val scriptsDir = File(filesDir, MainActivity.SCRIPTS_DIR)
        if (scriptsDir.exists() && path != scriptsDir) {
            files.add(FileItem(scriptsDir, true))
        }

        path.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name }))?.forEach { file ->
            files.add(FileItem(file, file.isDirectory))
        }

        fileAdapter.notifyDataSetChanged()
    }

    private fun showFileOptions(file: File) {
        val options = arrayOf("查看", "重命名", "删除", "分享")
        AlertDialog.Builder(this)
            .setTitle(file.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> viewFile(file)
                    1 -> renameFile(file)
                    2 -> deleteFile(file)
                    3 -> shareFile(file)
                }
            }
            .show()
    }

    private fun viewFile(file: File) {
        val content = file.readText()
        AlertDialog.Builder(this)
            .setTitle(file.name)
            .setMessage(content)
            .setPositiveButton("关闭", null)
            .show()
    }

    private fun renameFile(file: File) {
        val input = android.widget.EditText(this)
        input.setText(file.name)
        
        AlertDialog.Builder(this)
            .setTitle("重命名")
            .setView(input)
            .setPositiveButton("确定") { _, _ ->
                val newName = input.text.toString()
                if (newName.isNotEmpty() && newName != file.name) {
                    file.renameTo(File(file.parent, newName))
                    loadFiles(currentPath)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun deleteFile(file: File) {
        AlertDialog.Builder(this)
            .setMessage("确定要删除 ${file.name} 吗？")
            .setPositiveButton("删除") { _, _ ->
                if (file.isDirectory) {
                    file.deleteRecursively()
                } else {
                    file.delete()
                }
                loadFiles(currentPath)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    @Suppress("UNUSED_PARAMETER")
    private fun shareFile(file: File) {
        // TODO: 实现文件分享功能
    }

    data class FileItem(val file: File, val isDirectory: Boolean)

    inner class FileAdapter(
        private val items: List<FileItem>,
        private val onItemClick: (File) -> Unit
    ) : RecyclerView.Adapter<FileAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val txtName: TextView = view.findViewById(android.R.id.text1)
            val txtInfo: TextView = view.findViewById(android.R.id.text2)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_2, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            val icon = if (item.isDirectory) "📁" else "📄"
            holder.txtName.text = "$icon ${item.file.name}"
            
            val size = if (item.isDirectory) {
                "${item.file.listFiles()?.size ?: 0} 个项目"
            } else {
                formatSize(item.file.length())
            }
            holder.txtInfo.text = size
            holder.itemView.setOnClickListener { onItemClick(item.file) }
        }

        override fun getItemCount() = items.size

        private fun formatSize(bytes: Long): String {
            return when {
                bytes < 1024 -> "$bytes B"
                bytes < 1024 * 1024 -> "${bytes / 1024} KB"
                bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
                else -> "${bytes / (1024 * 1024 * 1024)} GB"
            }
        }
    }
}
