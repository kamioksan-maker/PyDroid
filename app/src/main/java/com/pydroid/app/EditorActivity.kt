package com.pydroid.app

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import java.io.File
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class EditorActivity : AppCompatActivity() {

    private lateinit var edtScriptName: EditText
    private lateinit var edtCode: EditText
    private lateinit var txtConsole: TextView
    private lateinit var btnRun: Button
    private lateinit var btnStop: Button
    private lateinit var btnSave: Button

    private var scriptName: String = ""
    private var isRunning = false
    private var pythonThread: Thread? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editor)

        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }

        initViews()
        loadScript()
        setupClickListeners()
    }

    private fun initViews() {
        edtScriptName = findViewById(R.id.edtScriptName)
        edtCode = findViewById(R.id.edtCode)
        txtConsole = findViewById(R.id.txtConsole)
        btnRun = findViewById(R.id.btnRun)
        btnStop = findViewById(R.id.btnStop)
        btnSave = findViewById(R.id.btnSave)

        findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
            .setNavigationOnClickListener { finish() }
    }

    private fun loadScript() {
        scriptName = intent.getStringExtra("script_name") ?: ""
        if (scriptName.isNotEmpty()) {
            edtScriptName.setText(scriptName)
            val scriptFile = File(filesDir, "${MainActivity.SCRIPTS_DIR}/$scriptName.py")
            if (scriptFile.exists()) {
                edtCode.setText(scriptFile.readText())
            }
        }
    }

    private fun setupClickListeners() {
        btnSave.setOnClickListener {
            saveScript()
        }

        btnRun.setOnClickListener {
            runScript()
        }

        btnStop.setOnClickListener {
            stopScript()
        }
    }

    private fun saveScript() {
        val name = edtScriptName.text.toString().trim()
        val code = edtCode.text.toString()

        if (name.isEmpty()) {
            Toast.makeText(this, "请输入脚本名称", Toast.LENGTH_SHORT).show()
            return
        }

        val scriptsDir = File(filesDir, MainActivity.SCRIPTS_DIR)
        val scriptFile = File(scriptsDir, "$name.py")
        
        scriptFile.writeText(code)
        Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show()

        if (scriptName != name && scriptName.isNotEmpty()) {
            File(scriptsDir, "$scriptName.py").delete()
        }
        scriptName = name
    }

    private fun runScript() {
        val code = edtCode.text.toString()
        if (code.isEmpty()) {
            Toast.makeText(this, "代码不能为空", Toast.LENGTH_SHORT).show()
            return
        }

        if (isRunning) {
            Toast.makeText(this, "脚本正在运行中", Toast.LENGTH_SHORT).show()
            return
        }

        isRunning = true
        updateButtons()
        txtConsole.text = "正在执行...\n"

        pythonThread = Thread {
            try {
                val outputStream = ByteArrayOutputStream()
                val printStream = PrintStream(outputStream)
                val originalOut = System.out
                val originalErr = System.err
                
                try {
                    System.setOut(printStream)
                    System.setErr(printStream)

                    val py = Python.getInstance()
                    py.exec(code)

                    val output = outputStream.toString()
                    Handler(Looper.getMainLooper()).post {
                        txtConsole.text = output.ifEmpty { "执行完成（无输出）" }
                    }
                } finally {
                    System.setOut(originalOut)
                    System.setErr(originalErr)
                }

                saveHistory(code, outputStream.toString(), true)

            } catch (e: Exception) {
                val errorMsg = "错误：${e.message}"
                Handler(Looper.getMainLooper()).post {
                    txtConsole.text = errorMsg
                }
                saveHistory(code, errorMsg, false)
            } finally {
                isRunning = false
                Handler(Looper.getMainLooper()).post {
                    updateButtons()
                }
            }
        }
        pythonThread?.start()
    }

    private fun stopScript() {
        pythonThread?.interrupt()
        isRunning = false
        updateButtons()
        txtConsole.append("\n已停止执行")
        Toast.makeText(this, "已停止", Toast.LENGTH_SHORT).show()
    }

    private fun updateButtons() {
        btnRun.isEnabled = !isRunning
        btnStop.visibility = if (isRunning) View.VISIBLE else View.GONE
        btnSave.isEnabled = !isRunning
    }

    private fun saveHistory(code: String, output: String, success: Boolean) {
        val historyDir = File(filesDir, "history")
        historyDir.mkdirs()
        
        val timestamp = System.currentTimeMillis()
        val historyFile = File(historyDir, "$timestamp.json")
        
        val json = """{
            "timestamp": $timestamp,
            "script": "$scriptName",
            "code": "${code.replace("\n", "\\n").replace("\"", "\\\"")}",
            "output": "${output.replace("\n", "\\n").replace("\"", "\\\"")}",
            "success": $success
        }"""
        
        historyFile.writeText(json)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isRunning) {
            stopScript()
        }
    }
}
