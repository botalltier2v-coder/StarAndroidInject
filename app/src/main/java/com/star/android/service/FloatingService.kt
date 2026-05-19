package com.star.android.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.star.android.utils.ShellUtils
import com.star.android.utils.SocketClient
import java.io.File

class FloatingService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var menuView: LinearLayout
    private lateinit var collapsedView: ImageView
    private lateinit var container: LinearLayout
    private lateinit var params: WindowManager.LayoutParams
    private val targetPackage = "com.kiloo.subwaysurf"
    private var isInjected = false
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    
    private val autoInjectRunnable = object : Runnable {
        override fun run() {
            Thread {
                val isRunning = ShellUtils.runAsRoot("pidof $targetPackage")
                handler.post {
                    if (isRunning) {
                        if (!isInjected) {
                            if (injectLibrary()) {
                                isInjected = true
                                Toast.makeText(this@FloatingService, "Injeksi Otomatis Berhasil!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        isInjected = false
                    }
                }
            }.start()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                1,
                createNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(1, createNotification())
        }
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        setupFloatingMenu()
        handler.post(autoInjectRunnable)
    }

    private fun createNotification(): Notification {
        val channelId = "star_menu_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Star Menu Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Star Menu Aktif")
            .setContentText("Menu sedang berjalan di atas game.")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .build()
    }

    private fun setupFloatingMenu() {
        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 100
        }

        collapsedView = ImageView(this).apply {
            setImageResource(com.star.android.R.drawable.logo)
            layoutParams = LinearLayout.LayoutParams(120, 120)
            setOnClickListener {
                visibility = android.view.View.GONE
                menuView.visibility = android.view.View.VISIBLE
            }
            visibility = android.view.View.VISIBLE
            makeDraggable(this)
        }

        menuView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#CC000000"))
            setPadding(40, 40, 40, 40)
            visibility = android.view.View.GONE
            
            val header = TextView(context).apply {
                text = "STAR MENU INJECTOR"
                setTextColor(Color.YELLOW)
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 30)
                makeDraggable(this)
            }
            addView(header)

            addView(CheckBox(context).apply {
                text = "Super Jump"
                setTextColor(Color.WHITE)
                setOnCheckedChangeListener { _, isChecked ->
                    SocketClient.setJump(isChecked)
                }
            })

            addView(CheckBox(context).apply {
                text = "Multi Score"
                setTextColor(Color.WHITE)
                setOnCheckedChangeListener { _, isChecked ->
                    SocketClient.setScore(isChecked)
                }
            })

            addView(CheckBox(context).apply {
                text = "Get Coins (Patch)"
                setTextColor(Color.WHITE)
                setOnCheckedChangeListener { _, isChecked ->
                    SocketClient.setGetCoin(isChecked)
                }
            })

            addView(CheckBox(context).apply {
                text = "No Clip (Patch)"
                setTextColor(Color.WHITE)
                setOnCheckedChangeListener { _, isChecked ->
                    SocketClient.setNoClip(isChecked)
                }
            })

            addView(TextView(context).apply {
                text = "Speed Multiplier"
                setTextColor(Color.WHITE)
                setPadding(0, 20, 0, 0)
            })

            addView(android.widget.SeekBar(context).apply {
                max = 50
                progress = 1
                setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                        val value = if (progress < 1) 1.0f else progress.toFloat()
                        SocketClient.setSpeedValue(value)
                    }
                    override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
                    override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
                })
            })

            addView(Button(context).apply {
                text = "HIDE MENU"
                setOnClickListener {
                    menuView.visibility = android.view.View.GONE
                    collapsedView.visibility = android.view.View.VISIBLE
                }
            })

            addView(Button(context).apply {
                text = "EXIT"
                setOnClickListener { stopSelf() }
            })
        }

        container = LinearLayout(this).apply {
            addView(collapsedView)
            addView(menuView)
        }

        windowManager.addView(container, params)
    }

    private fun makeDraggable(view: View) {
        view.setOnTouchListener(object : View.OnTouchListener {
            private var initialX: Int = 0
            private var initialY: Int = 0
            private var initialTouchX: Float = 0f
            private var initialTouchY: Float = 0f

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(container, params)
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        val diffX = Math.abs(event.rawX - initialTouchX)
                        val diffY = Math.abs(event.rawY - initialTouchY)
                        if (diffX < 10 && diffY < 10) {
                            v.performClick()
                        }
                        return true
                    }
                }
                return false
            }
        })
    }

    private fun injectLibrary(): Boolean {
        val libPath = File(applicationInfo.nativeLibraryDir, "libStarcool.so").absolutePath
        return ShellUtils.deployAndRunInjector(this, targetPackage, libPath)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(autoInjectRunnable)
        if (::container.isInitialized) windowManager.removeView(container)
    }
}
