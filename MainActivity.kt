package com.example.screenscanalarm

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"
    private lateinit var toggleBtn: Button
    private lateinit var pickMp3Btn: Button
    private lateinit var targetEdit: EditText
    private lateinit var statusTv: TextView

    private var mp3Uri: Uri? = null
    private var projectionPermissionData: Intent? = null
    private var projectionPermissionResultCode: Int = Activity.RESULT_CANCELED

    private val chooseMp3Launcher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            mp3Uri = uri
            statusTv.text = "Selected alarm: " + getFileName(uri)
            // save in prefs
            getSharedPreferences("ssa", Context.MODE_PRIVATE).edit().putString("alarm_uri", uri.toString()).apply()
        }
    }

    private val requestProjectionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            projectionPermissionResultCode = result.resultCode
            projectionPermissionData = result.data
            // start service with projection permission
            startCaptureService()
        } else {
            Toast.makeText(this, "Screen capture permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        createNotificationChannel()

        toggleBtn = findViewById(R.id.toggleBtn)
        pickMp3Btn = findViewById(R.id.pickMp3Btn)
        targetEdit = findViewById(R.id.targetEdit)
        statusTv = findViewById(R.id.statusTv)

        // load saved uri
        val pref = getSharedPreferences("ssa", Context.MODE_PRIVATE)
        pref.getString("alarm_uri", null)?.let {
            mp3Uri = Uri.parse(it)
            statusTv.text = "Selected alarm: ${mp3Uri?.lastPathSegment}"
        }

        toggleBtn.setOnClickListener {
            val isRunning = pref.getBoolean("service_running", false)
            if (!isRunning) {
                // request projection permission
                val mgr = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as android.media.projection.MediaProjectionManager
                val intent = mgr.createScreenCaptureIntent()
                requestProjectionLauncher.launch(intent)
            } else {
                // stop service
                stopService(Intent(this, CaptureService::class.java))
                pref.edit().putBoolean("service_running", false).apply()
                toggleBtn.text = "Start"
                statusTv.text = "Stopped"
            }
        }

        pickMp3Btn.setOnClickListener {
            chooseMp3Launcher.launch(arrayOf("audio/mpeg", "audio/*"))
        }

        // set target text saved earlier
        targetEdit.setText(pref.getString("target_text", ""))
        findViewById<Button>(R.id.saveTargetBtn).setOnClickListener {
            val txt = targetEdit.text.toString().trim()
            pref.edit().putString("target_text", txt).apply()
            Toast.makeText(this, "Target saved", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startCaptureService() {
        val pref = getSharedPreferences("ssa", Context.MODE_PRIVATE)
        val intent = Intent(this, CaptureService::class.java)
        intent.putExtra("target_text", targetEdit.text.toString())
        mp3Uri?.let { intent.putExtra("alarm_uri", it.toString()) }
        if (projectionPermissionData != null) {
            intent.putExtra("result_code", projectionPermissionResultCode)
            intent.putExtra("result_intent", projectionPermissionData)
        }
        ContextCompat.startForegroundService(this, intent)
        pref.edit().putBoolean("service_running", true).apply()
        toggleBtn.text = "Stop"
        statusTv.text = "Running"
        Toast.makeText(this, "Service started", Toast.LENGTH_SHORT).show()
    }

    private fun getFileName(uri: Uri): String {
        var name = uri.lastPathSegment ?: "alarm"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) {
                name = cursor.getString(nameIndex)
            }
        }
        return name
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel("ssa_channel", "ScreenScanAlarm", NotificationManager.IMPORTANCE_LOW)
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(ch)
        }
    }
}
