package com.example.screenscanalarm

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.IOException
import java.nio.ByteBuffer
import java.util.concurrent.Executors

class CaptureService : Service() {
    private val TAG = "CaptureService"
    private var mediaProjection: MediaProjection? = null
    private var imageReader: ImageReader? = null
    private val EXEC = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())
    private var alarmUri: Uri? = null
    private var mediaPlayer: android.media.MediaPlayer? = null
    private var targetText: String = ""
    private var playing = false

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        targetText = intent?.getStringExtra("target_text") ?: ""
        intent?.getStringExtra("alarm_uri")?.let { alarmUri = Uri.parse(it) }

        // start foreground notification
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pending = PendingIntent.getActivity(this, 1, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
        val n: Notification = NotificationCompat.Builder(this, "ssa_channel")
            .setContentTitle("ScreenScanAlarm")
            .setContentText("Scanning for: "$targetText"")
            .setSmallIcon(android.R.drawable.ic_menu_search)
            .setContentIntent(pending)
            .build()
        startForeground(1337, n)

        // start projection if permission provided
        val rc = intent?.getIntExtra("result_code", -1) ?: -1
        val data = intent?.getParcelableExtra<Intent>("result_intent")
        if (rc > 0 && data != null) {
            val mgr = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = mgr.getMediaProjection(rc, data)
            startCapture()
        } else {
            Log.w(TAG, "No projection permission provided, stopping service.")
            stopSelf()
        }

        return START_STICKY
    }

    private fun startCapture() {
        val dm = resources.displayMetrics
        val width = dm.widthPixels
        val height = dm.heightPixels
        val density = dm.densityDpi

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        val surface = imageReader!!.surface
        mediaProjection?.createVirtualDisplay("ssa-vd", width, height, density, 0, surface, null, handler)

        imageReader?.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
            val planes = image.planes
            val buffer: ByteBuffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * width
            val bmp = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888)
            bmp.copyPixelsFromBuffer(buffer)
            image.close()

            // Crop or scale to area later - for starter we just use full image
            processBitmap(bmp)
        }, handler)
    }

    private fun processBitmap(bitmap: Bitmap) {
        // Run OCR with ML Kit
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val text = visionText.text
                if (text.contains(targetText, true) && targetText.isNotEmpty()) {
                    Log.i(TAG, "Detected target: $targetText")
                    triggerAlarm()
                }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "OCR failed: ${e.message}")
            }
    }

    private fun triggerAlarm() {
        if (playing) return
        playing = true
        try {
            mediaPlayer = android.media.MediaPlayer()
            if (alarmUri != null) {
                mediaPlayer?.setDataSource(this, alarmUri!!)
            } else {
                // default tone: beep using tone generator (simple)
                mediaPlayer?.setDataSource("/system/media/audio/alarms/Alarm_Classic.ogg")
            }
            mediaPlayer?.isLooping = true
            mediaPlayer?.prepare()
            mediaPlayer?.start()

            // stop after 2 minutes
            handler.postDelayed({
                stopAlarm()
            }, 2 * 60 * 1000L)
        } catch (ex: IOException) {
            Log.w(TAG, "Alarm failed: ${ex.message}")
            playing = false
        }
    }

    private fun stopAlarm() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        playing = false
    }

    override fun onDestroy() {
        stopAlarm()
        mediaProjection?.stop()
        imageReader?.close()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
