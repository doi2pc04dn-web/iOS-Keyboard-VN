package com.custom.ioskeyboard

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

class SetupActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 120, 60, 60)
            setBackgroundColor(Color.parseColor("#F2F2F7"))
            gravity = Gravity.CENTER_HORIZONTAL
        }

        val title = TextView(this).apply {
            text = "iOS Keyboard VN"
            textSize = 26f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.BLACK)
            setPadding(0, 0, 0, 20)
        }

        val desc = TextView(this).apply {
            text = "Bàn phím chuẩn giao diện iPhone 16 Pro Max\n(Hỗ trợ gõ W ra Ư tức thì & trượt dấu cách di chuyển con trỏ)"
            textSize = 15f
            setTextColor(Color.DKGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 70)
        }

        val btnEnable = createIOSButton("Bước 1: Bật iOS Keyboard trong Cài đặt", "#007AFF") {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }

        val btnSelect = createIOSButton("Bước 2: Chọn làm bàn phím mặc định", "#34C759") {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showInputMethodPicker()
        }

        val testInput = EditText(this).apply {
            hint = "Bước 3: Chạm vào đây gõ thử chữ W, Ư..."
            textSize = 16f
            setPadding(40, 40, 40, 40)
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = 28f
            }
        }

        root.addView(title)
        root.addView(desc)
        root.addView(btnEnable, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 35 })
        root.addView(btnSelect, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 60 })
        root.addView(testInput, LinearLayout.LayoutParams(-1, -2))

        setContentView(root)
    }

    private fun createIOSButton(label: String, hexColor: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            text = label
            setTextColor(Color.WHITE)
            textSize = 15f
            isAllCaps = false
            setPadding(30, 35, 30, 35)
            background = GradientDrawable().apply {
                setColor(Color.parseColor(hexColor))
                cornerRadius = 30f
            }
            setOnClickListener { onClick() }
        }
    }
}
