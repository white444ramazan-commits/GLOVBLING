package com.glovbling.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class OverlayService : Service() {
    companion object {
        const val EXTRA_ROOT_VERIFIED = "root_verified"
        private const val CHANNEL_ID = "glovbling_overlay"
        private const val NOTIFICATION_ID = 4701
    }

    private lateinit var wm: WindowManager
    private var rootView: LinearLayout? = null
    private var body: LinearLayout? = null
    private var content: LinearLayout? = null
    private var params: WindowManager.LayoutParams? = null
    private var expanded = true
    private var currentTab = 0
    private var rootBadge: TextView? = null
    private var globalBadge: TextView? = null
    private var preview: CirclePreview? = null
    private var handle: TextView? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.getBooleanExtra(EXTRA_ROOT_VERIFIED, false) != true) {
            stopSelf()
            return START_NOT_STICKY
        }
        createChannel()
        val note = notification()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, note, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, note)
        }
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (rootView == null) showPanel()
        return START_STICKY
    }

    private fun showPanel() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(10), dp(10), dp(7))
            background = gradient(intArrayOf(0xFF151831.toInt(), 0xFF12283A.toInt(), 0xFF1F1A43.toInt()), 24)
            elevation = dp(18).toFloat()
        }
        val panelBody = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        body = panelBody

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(5), dp(4), dp(5), dp(12))
        }
        val titles = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        titles.addView(label("GLOVBLING", 18f, Color.WHITE, true))
        rootBadge = label("ROOT ACCESS  •  VERIFIED", 9f, 0xFF75EFB7.toInt(), true)
        titles.addView(rootBadge, wrap().apply { topMargin = dp(3) })
        globalBadge = label("GLOBAL  •  com.tencent.ig", 9f, 0xFF9AA5CF.toInt(), true)
        titles.addView(globalBadge, wrap().apply { topMargin = dp(2) })
        header.addView(titles, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(label("–", 24f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            background = solid(0xFF393D63.toInt(), 12)
            setOnClickListener { togglePanel() }
        }, LinearLayout.LayoutParams(dp(38), dp(34)))
        panelBody.addView(header)

        val tabs = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, dp(10))
        }
        tabs.addView(tabButton("ПАНЕЛЬ", 0), LinearLayout.LayoutParams(0, dp(40), 1f).apply { marginEnd = dp(6) })
        tabs.addView(tabButton("ТРЕНИРОВКА", 1), LinearLayout.LayoutParams(0, dp(40), 1f))
        panelBody.addView(tabs)

        val scroll = ScrollView(this)
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(1), dp(1), dp(1), dp(8))
        }
        scroll.addView(content)
        panelBody.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(panelBody, LinearLayout.LayoutParams(-1, 0, 1f))

        val footer = FrameLayout(this)
        handle = label("━━━━━━", 11f, 0xFFC0C7E7.toInt(), true).apply {
            gravity = Gravity.CENTER
            background = solid(0xFF363B5B.toInt(), 18)
            setOnClickListener { togglePanel() }
        }
        footer.addView(handle, FrameLayout.LayoutParams(dp(94), dp(20), Gravity.CENTER))
        root.addView(footer, LinearLayout.LayoutParams(-1, dp(25)))

        rootView = root
        params = WindowManager.LayoutParams(
            dp(340), dp(510),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.CENTER }

        try {
            wm.addView(root, params)
            renderTab()
            refreshTabs(tabs.getChildAt(0) as TextView, tabs.getChildAt(1) as TextView)
        } catch (e: Exception) {
            Toast.makeText(this, "GLOVBLING: ${e.message}", Toast.LENGTH_LONG).show()
            stopSelf()
        }
    }

    private fun togglePanel() {
        val root = rootView ?: return
        val panelBody = body ?: return
        val window = params ?: return
        if (expanded) {
            root.animate().alpha(0.15f).scaleY(0.88f).setDuration(130)
                .setInterpolator(DecelerateInterpolator()).withEndAction {
                    panelBody.visibility = View.GONE
                    window.width = dp(116)
                    window.height = dp(40)
                    window.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                    runCatching { wm.updateViewLayout(root, window) }
                    root.alpha = 1f
                    root.scaleY = 1f
                    expanded = false
                    handle?.text = "━━━━"
                }.start()
        } else {
            panelBody.visibility = View.VISIBLE
            window.width = dp(340)
            window.height = dp(510)
            window.gravity = Gravity.CENTER
            runCatching { wm.updateViewLayout(root, window) }
            root.alpha = 0.2f
            root.scaleY = 0.9f
            root.animate().alpha(1f).scaleY(1f).setDuration(220)
                .setInterpolator(DecelerateInterpolator()).start()
            expanded = true
            handle?.text = "━━━━━━"
        }
    }

    private fun tabButton(name: String, index: Int) = label(name, 10f, Color.WHITE, true).apply {
        gravity = Gravity.CENTER
        background = solid(if (currentTab == index) 0xFF6553E1.toInt() else 0xFF282D4B.toInt(), 12)
        setOnClickListener {
            currentTab = index
            renderTab()
            val tabs = body?.getChildAt(1) as? LinearLayout ?: return@setOnClickListener
            refreshTabs(tabs.getChildAt(0) as TextView, tabs.getChildAt(1) as TextView)
        }
    }

    private fun refreshTabs(first: TextView, second: TextView) {
        first.background = solid(if (currentTab == 0) 0xFF6553E1.toInt() else 0xFF282D4B.toInt(), 12)
        second.background = solid(if (currentTab == 1) 0xFF6553E1.toInt() else 0xFF282D4B.toInt(), 12)
    }

    private fun renderTab() {
        val target = content ?: return
        target.removeAllViews()
        if (currentTab == 0) buildPanelTab(target) else buildTrainingTab(target)
    }

    private fun buildPanelTab(target: LinearLayout) {
        target.addView(section("СИСТЕМНЫЙ РЕЖИМ"))
        addSwitch(target, "Показывать ROOT-статус", "Метка состояния в панели", true) {
            rootBadge?.visibility = if (it) View.VISIBLE else View.GONE
        }
        addSwitch(target, "Показывать Global", "Подпись целевого пакета", true) {
            globalBadge?.visibility = if (it) View.VISIBLE else View.GONE
        }
        target.addView(infoCard(
            "ПОДКЛЮЧЕНИЕ",
            "Целевой пакет: com.tencent.ig\nПанель запускает установленную Global-версию. Она не подключается к памяти игры."
        ))
        target.addView(infoCard(
            "СТАТУС",
            "Плавающее окно активно. Нижняя полоска сворачивает и раскрывает панель."
        ))
    }

    private fun buildTrainingTab(target: LinearLayout) {
        target.addView(section("ТРЕНИРОВОЧНЫЙ ПРОФИЛЬ"))
        target.addView(infoCard(
            "ЛОКАЛЬНЫЙ ПРЕДПРОСМОТР",
            "Значения сохраняются только в GLOVBLING. Они не управляют игровым прицелом, не ищут игроков и не меняют память PUBG."
        ))
        val prefs = getSharedPreferences("training_preview", MODE_PRIVATE)
        val speed = prefs.getInt("tempo", 50).coerceIn(1, 100)
        val radius = prefs.getInt("radius", 60).coerceIn(20, 120)

        val card = verticalCard()
        card.addView(label("КРУГ УЧЕБНОГО ПРЕДПРОСМОТРА", 10f, 0xFFB1BBE1.toInt(), true))
        preview = CirclePreview(this).apply { radiusScale = radius / 120f }
        card.addView(preview, LinearLayout.LayoutParams(-1, dp(126)).apply { topMargin = dp(8) })
        target.addView(card, wrap().apply { bottomMargin = dp(10) })

        addSlider(target, "Темп упражнения", speed, 1, 100, "Настройка только для локального профиля.") {
            prefs.edit().putInt("tempo", it).apply()
        }
        addSlider(target, "Радиус круга (FOV-превью)", radius, 20, 120, "Меняется только круг в этом окне.") {
            prefs.edit().putInt("radius", it).apply()
            preview?.radiusScale = it / 120f
            preview?.invalidate()
        }
    }

    private fun addSwitch(
        target: LinearLayout,
        title: String,
        subtitle: String,
        defaultValue: Boolean,
        changed: (Boolean) -> Unit
    ) {
        val row = horizontalCard()
        val texts = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        texts.addView(label(title, 12f, Color.WHITE, true))
        texts.addView(label(subtitle, 10f, 0xFF96A2C2.toInt()).apply { setPadding(0, dp(4), 0, 0) })
        row.addView(texts, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(Switch(this).apply {
            isChecked = defaultValue
            setOnCheckedChangeListener { _, checked -> changed(checked) }
        })
        target.addView(row, wrap().apply { bottomMargin = dp(9) })
    }

    private fun addSlider(
        target: LinearLayout,
        title: String,
        initial: Int,
        minValue: Int,
        maxValue: Int,
        note: String,
        changed: (Int) -> Unit
    ) {
        val card = verticalCard()
        val heading = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val valueText = label(initial.toString(), 13f, 0xFF94E0FF.toInt(), true)
        heading.addView(label(title, 12f, Color.WHITE, true), LinearLayout.LayoutParams(0, -2, 1f))
        heading.addView(valueText)
        card.addView(heading)
        val seek = SeekBar(this).apply {
            max = maxValue - minValue
            progress = initial - minValue
        }
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = minValue + progress
                valueText.text = value.toString()
                if (fromUser) changed(value)
                if (title.startsWith("Радиус")) {
                    preview?.radiusScale = value / 120f
                    preview?.invalidate()
                }
            }
            override fun onStartTrackingTouch(bar: SeekBar?) = Unit
            override fun onStopTrackingTouch(bar: SeekBar?) = Unit
        })
        card.addView(seek, wrap().apply { topMargin = dp(4) })
        card.addView(label(note, 9f, 0xFF919DBC.toInt()))
        target.addView(card, wrap().apply { bottomMargin = dp(9) })
    }

    private fun section(value: String) = label(value, 10f, 0xFFA9B5E1.toInt(), true).apply {
        setPadding(dp(3), dp(4), 0, dp(10))
    }

    private fun infoCard(title: String, value: String): LinearLayout {
        val card = verticalCard()
        card.addView(label(title, 9f, 0xFF97A9E1.toInt(), true))
        card.addView(label(value, 11f, 0xFFE0E6F9.toInt()).apply { setPadding(0, dp(7), 0, 0) })
        return card
    }

    private fun verticalCard() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(13), dp(12), dp(13), dp(12))
        background = solid(0xFF232846.toInt(), 15)
    }

    private fun horizontalCard() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(12), dp(10), dp(8), dp(10))
        background = solid(0xFF232846.toInt(), 15)
    }

    private fun label(value: String, size: Float, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

    private fun solid(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }

    private fun gradient(colors: IntArray, radius: Int) =
        GradientDrawable(GradientDrawable.Orientation.TL_BR, colors).apply {
            cornerRadius = dp(radius).toFloat()
        }

    private fun wrap() = LinearLayout.LayoutParams(-1, -2)
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "GLOVBLING floating panel", NotificationManager.IMPORTANCE_LOW
            ).apply { description = "User-started floating companion panel." }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun notification(): Notification {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .setContentTitle("GLOVBLING активна")
                .setContentText("Плавающая панель запущена")
                .setOngoing(true)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .setContentTitle("GLOVBLING активна")
                .setContentText("Плавающая панель запущена")
                .setOngoing(true)
                .build()
        }
    }

    override fun onDestroy() {
        rootView?.let { runCatching { wm.removeView(it) } }
        rootView = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private inner class CirclePreview(context: Context) : View(context) {
        var radiusScale: Float = 0.5f
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val cx = width / 2f
            val cy = height / 2f
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = dp(1).toFloat()
            paint.color = 0xFF495B88.toInt()
            canvas.drawRoundRect(
                dp(2).toFloat(), dp(2).toFloat(),
                width - dp(2).toFloat(), height - dp(2).toFloat(),
                dp(12).toFloat(), dp(12).toFloat(), paint
            )
            paint.color = 0xFF5CD6E2.toInt()
            paint.strokeWidth = dp(2).toFloat()
            val maxRadius = minOf(width, height) * 0.43f
            canvas.drawCircle(cx, cy, maxRadius * radiusScale.coerceIn(0.15f, 1f), paint)
            paint.style = Paint.Style.FILL
            paint.color = 0xFFA9B3E3.toInt()
            canvas.drawCircle(cx, cy, dp(2).toFloat(), paint)
        }
    }
}
