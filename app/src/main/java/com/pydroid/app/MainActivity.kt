package com.pydroid.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerScripts: RecyclerView
    private lateinit var txtEmpty: TextView
    private lateinit var fabNew: FloatingActionButton
    private lateinit var scriptAdapter: ScriptAdapter
    private val scripts = mutableListOf<ScriptItem>()

    companion object {
        const val SCRIPTS_DIR = "scripts"
        const val PERMISSION_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(findViewById(R.id.toolbar))
        
        initViews()
        checkPermissions()
        loadScripts()
        setupClickListeners()
    }

    private fun initViews() {
        recyclerScripts = findViewById(R.id.recyclerScripts)
        txtEmpty = findViewById(R.id.txtEmpty)
        fabNew = findViewById(R.id.fabNew)

        scriptAdapter = ScriptAdapter(scripts) { script ->
            openEditor(script)
        }
        recyclerScripts.adapter = scriptAdapter
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS), PERMISSION_CODE)
            }
        }
    }

    private fun loadScripts() {
        scripts.clear()
        val scriptsDir = File(filesDir, SCRIPTS_DIR)
        
        if (!scriptsDir.exists()) {
            scriptsDir.mkdirs()
            createSampleScript(scriptsDir)
        }

        scriptsDir.listFiles { file -> file.extension == "py" }?.forEach { file ->
            scripts.add(ScriptItem(file.nameWithoutExtension, file.lastModified()))
        }

        scripts.sortByDescending { it.lastModified }
        scriptAdapter.notifyDataSetChanged()
        updateEmptyState()
    }

    private fun createSampleScript(dir: File) {
        val sampleCode = """# PyDroid 示例脚本
print("Hello from PyDroid!")
print("Python 版本:")
import sys
print(sys.version)

print("\n测试 numpy:")
import numpy as np
arr = np.array([1, 2, 3, 4, 5])
print(f"数组：{arr}")
print(f"平均值：{np.mean(arr)}")
""".trimIndent()
        
        File(dir, "hello_world.py").writeText(sampleCode)
    }

    private fun updateEmptyState() {
        txtEmpty.visibility = if (scripts.isEmpty()) View.VISIBLE else View.GONE
        recyclerScripts.visibility = if (scripts.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun setupClickListeners() {
        fabNew.setOnClickListener {
            showNewScriptDialog()
        }

        findViewById<Button>(R.id.btnFileBrowser).setOnClickListener {
            startActivity(Intent(this, FileBrowserActivity::class.java))
        }

        findViewById<Button>(R.id.btnHistory).setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        findViewById<Button>(R.id.btnPackages).setOnClickListener {
            showPackageDialog()
        }

        findViewById<Button>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun showNewScriptDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("新建脚本")
        
        val input = android.widget.EditText(this)
        input.hint = "脚本名称（不含.py）"
        builder.setView(input)
        
        builder.setPositiveButton("创建") { _, _ ->
            val name = input.text.toString().trim()
            if (name.isNotEmpty()) {
                createScript(name)
            }
        }
        builder.setNegativeButton("取消", null)
        builder.show()
    }

    private fun createScript(name: String) {
        val scriptsDir = File(filesDir, SCRIPTS_DIR)
        val scriptFile = File(scriptsDir, "$name.py")
        
        if (scriptFile.exists()) {
            Snackbar.make(fabNew, "脚本已存在", Snackbar.LENGTH_SHORT).show()
            return
        }

        scriptFile.writeText("# $name\n# 创建时间：${System.currentTimeMillis()}\n\n")
        Snackbar.make(fabNew, "脚本创建成功", Snackbar.LENGTH_SHORT).show()
        loadScripts()
    }

    private fun openEditor(script: ScriptItem) {
        val intent = Intent(this, EditorActivity::class.java)
        intent.putExtra("script_name", script.name)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        loadScripts()
    }

    data class ScriptItem(val name: String, val lastModified: Long)

    inner class ScriptAdapter(
        private val items: List<ScriptItem>,
        private val onItemClick: (ScriptItem) -> Unit
    ) : RecyclerView.Adapter<ScriptAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val txtName: TextView = view.findViewById(android.R.id.text1)
            val txtTime: TextView = view.findViewById(android.R.id.text2)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_2, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.txtName.text = item.name + ".py"
            holder.txtTime.text = android.text.format.DateFormat.format("yyyy-MM-dd HH:mm", item.lastModified)
            holder.itemView.setOnClickListener { onItemClick(item) }
            
            holder.itemView.setOnLongClickListener {
                showDeleteDialog(item, position)
                true
            }
        }

        override fun getItemCount() = items.size

        private fun showDeleteDialog(item: ScriptItem, position: Int) {
            AlertDialog.Builder(this@MainActivity)
                .setMessage("确定要删除 ${item.name}.py 吗？")
                .setPositiveButton("删除") { _, _ ->
                    File(filesDir, SCRIPTS_DIR).resolve("${item.name}.py").delete()
                    scripts.removeAt(position)
                    notifyItemRemoved(position)
                    updateEmptyState()
                    Snackbar.make(fabNew, "已删除", Snackbar.LENGTH_SHORT).show()
                }
                .setNegativeButton("取消", null)
                .show()
        }
    }
}
