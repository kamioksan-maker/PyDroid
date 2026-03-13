package com.pydroid.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class HistoryActivity : AppCompatActivity() {

    private lateinit var recyclerHistory: RecyclerView
    private lateinit var historyAdapter: HistoryAdapter
    private val historyItems = mutableListOf<HistoryItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        setSupportActionBar(findViewById(R.id.toolbar))
        findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
            .setNavigationOnClickListener { finish() }

        recyclerHistory = findViewById(R.id.recyclerHistory)
        historyAdapter = HistoryAdapter(historyItems)
        recyclerHistory.adapter = historyAdapter

        loadHistory()
    }

    private fun loadHistory() {
        historyItems.clear()
        val historyDir = File(filesDir, "history")
        
        if (!historyDir.exists()) {
            historyAdapter.notifyDataSetChanged()
            return
        }

        historyDir.listFiles { file -> file.extension == "json" }
            ?.sortedByDescending { it.lastModified() }
            ?.forEach { file ->
                try {
                    val json = JSONObject(file.readText())
                    historyItems.add(HistoryItem(
                        timestamp = json.getLong("timestamp"),
                        script = json.getString("script"),
                        code = json.getString("code"),
                        output = json.getString("output"),
                        success = json.getBoolean("success")
                    ))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

        historyAdapter.notifyDataSetChanged()
    }

    data class HistoryItem(
        val timestamp: Long,
        val script: String,
        val code: String,
        val output: String,
        val success: Boolean
    )

    inner class HistoryAdapter(private val items: List<HistoryItem>) : 
        RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

        private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val txtScript: TextView = view.findViewById(android.R.id.text1)
            val txtTime: TextView = view.findViewById(android.R.id.text2)
            val txtStatus: TextView = view.findViewById(R.id.txtStatus)
            val txtPreview: TextView = view.findViewById(R.id.txtPreview)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_history, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.txtScript.text = if (item.script.isNotEmpty()) item.script else "未命名脚本"
            holder.txtTime.text = dateFormat.format(Date(item.timestamp))
            holder.txtStatus.text = if (item.success) "✅ 成功" else "❌ 失败"
            holder.txtStatus.setTextColor(
                holder.itemView.context.getColor(
                    if (item.success) R.color.secondary else R.color.error
                )
            )
            
            val preview = item.output.replace("\\n", "\n")
                .take(100) + if (item.output.length > 100) "..." else ""
            holder.txtPreview.text = preview
        }

        override fun getItemCount() = items.size
    }
}
