package com.glovbling.app

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private var rootReady = false
    private lateinit var rootStatus: TextView
    private lateinit var launchButton: TextView
    private val gamePackage = "com.tencent.ig"
    private val overlayRequestCode = 4107

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(9, 12, 22)
        window.navigationBarColor = Color.rgb(9, 12, 22)

        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(30), dp(22), dp(24))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(9, 12, 22), Color.rgb(29, 24, 61), Color.rgb(10, 33, 48))
            )
        }
        setContentView(ScrollView(this).apply {
            isFillViewport = true
            addView(page)
        })

        page.addView(label("GLOVBLING", 30f, Color.WHITE, true), lp().apply { bottomMargin = dp(5) })
        page.addView(label("GLOBAL COMPANION  /  ROOT SESSION", 11f, 0xFF9EAAC8.toInt(), true), lp().apply { bottomMargin = dp(28) })

        val statusCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(20), dp(18), dp(20))
            background = gradient(intArrayOf(0xFF272653.toInt(), 0xFF18364B.toInt()), 22)
        }
        statusCard.addView(label("СИСТЕМА ГОТОВНОСТИ", 11f, 0xFFB6BEFF.toInt(), true))
        rootStatus = label("Проверка ROOT-доступа…", 17f, Color.WHITE, true)
        statusCard.addView(rootStatus, lp().apply { topMargin = dp(10) })
        statusCard.addView(
            label("Нужно разрешение KernelSU и доступ к плавающему окну Android.", 12f, 0xFFBEC6DA.toInt()),
            lp().apply { topMargin = dp(9) }
        )
        page.addView(statusCard, lp().apply { bottomMargin = dp(24) })

        page.addView(label("ЗАПУСК", 11f, 0xFF9EAAC8.toInt(), true), lp().apply { bottomMargin = dp(10) })
        launchButton = label("ЗАПУСТИТЬ GL", 16f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            minHeight = dp(58)
            isEnabled = false
            alpha = 0.45f
            background = gradient(intArrayOf(0xFF6D5BFF.toInt(), 0xFF2AAFCF.toInt()), 18)
            setOnClickListener { onLaunchPressed() }
        }
        page.addView(launchButton, lp().apply { bottomMargin = dp(22) })

        page.addView(label("TARGET PACKAGE", 10f, 0xFF8C99B8.toInt(), true), lp().apply { bottomMargin = dp(5) })
        page.addView(label(gamePackage, 14f, Color.WHITE, true), lp())
        page.addView(
            label(
                "Тренировочные параметры являются только визуальным предпросмотром и не управляют игрой.",
                11f,
                0xFF8794B2.toInt()
            ),
            lp().apply { topMargin = dp(30) }
        )
        checkRoot()
    }

    private fun checkRoot() {
        rootStatus.text = "Запрос ROOT-разрешения…"
        launchButton.isEnabled = false
        launchButton.alpha = 0.45f
        Thread {
            val granted = RootAccess.hasRootAccess()
            runOnUiThread {
                rootReady = granted
                rootStatus.text = if (granted) "ROOT ДОСТУП ПОДТВЕРЖДЁН" else "ROOT НЕ ПОЛУЧЕН"
                rootStatus.setTextColor(if (granted) 0xFF75EFB7.toInt() else 0xFFFF8B97.toInt())
                launchButton.isEnabled = granted
                launchButton.alpha = if (granted) 1f else 0.45f
            }
        }.start()
    }

    private fun onLaunchPressed() {
        if (!rootReady) {
            checkRoot()
            return
        }
        if (!Settings.canDrawOverlays(this)) {
            startActivityForResult(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")),
                overlayRequestCode
            )
            return
        }
        launchGlobal()
    }

    @Deprecated("System permission screen result")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == overlayRequestCode) {
            if (Settings.canDrawOverlays(this) && rootReady) launchGlobal()
            else Toast.makeText(this, "Разреши плавающее окно и ROOT-доступ.", Toast.LENGTH_LONG).show()
        }
    }

    private fun launchGlobal() {
        val gameIntent = packageManager.getLaunchIntentForPackage(gamePackage)
        if (gameIntent == null) {
            Toast.makeText(this, "Не найден PUBG Mobile Global ($gamePackage).", Toast.LENGTH_LONG).show()
            return
        }
        val panelIntent = Intent(this, OverlayService::class.java)
            .putExtra(OverlayService.EXTRA_ROOT_VERIFIED, true)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(panelIntent)
            else startService(panelIntent)
            startActivity(gameIntent)
        } catch (error: Exception) {
            Toast.makeText(this, "Ошибка запуска панели: ${error.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun label(value: String, size: Float, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

    private fun gradient(colors: IntArray, radius: Int) =
        GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, colors).apply {
            cornerRadius = dp(radius).toFloat()
        }

    private fun lp() = LinearLayout.LayoutParams(-1, -2)
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
