package com.custom.ioskeyboard

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.abs

class IOSInputMethodService : InputMethodService() {

    private var shiftState = 1 // 0: thường, 1: viết hoa chữ đầu, 2: CapsLock
    private var symbolPage = 0 // 0: ABC, 1: 123, 2: #+=
    private lateinit var mainContainer: LinearLayout
    private val handler = Handler(Looper.getMainLooper())
    private var lastSpaceTime = 0L
    private var lastShiftTime = 0L

    override fun onCreateInputView(): View {
        mainContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#D1D3D9")) // Màu xám chuẩn bàn phím sáng iOS
            setPadding(dp(3), dp(8), dp(3), dp(4))
        }
        renderKeyboard()
        return mainContainer
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        val ic = currentInputConnection
        val before = ic?.getTextBeforeCursor(2, 0)?.toString() ?: ""
        shiftState = if (before.isEmpty() || before.endsWith(". ") || before.endsWith("\n")) 1 else 0
        symbolPage = 0
        renderKeyboard()
    }

    private fun renderKeyboard() {
        mainContainer.removeAllViews()

        val row1: List<String>
        val row2: List<String>
        val row3: List<String>

        when (symbolPage) {
            0 -> {
                row1 = listOf("q","w","e","r","t","y","u","i","o","p")
                row2 = listOf("a","s","d","f","g","h","j","k","l")
                row3 = listOf("SHIFT","z","x","c","v","b","n","m","DEL")
            }
            1 -> {
                row1 = listOf("1","2","3","4","5","6","7","8","9","0")
                row2 = listOf("-","/",":",";","(",")","₫","&","@","\"")
                row3 = listOf("#+=",".",",","?","!","'","DEL")
            }
            else -> {
                row1 = listOf("[","]","{","}","#","%","^","*","+","=")
                row2 = listOf("_","\\","|","~","<",">","$","€","£","•")
                row3 = listOf("123",".",",","?","!","'","DEL")
            }
        }

        mainContainer.addView(buildStandardRow(row1, 0))
        mainContainer.addView(buildStandardRow(row2, if (symbolPage == 0) dp(18) else 0))
        mainContainer.addView(buildRow3(row3))
        mainContainer.addView(buildRow4())
        mainContainer.addView(buildIOSBottomChin()) // Khoảng đệm đáy chứa 🌐 và 🎤 giống iPhone 16 Pro Max
    }

    private fun buildStandardRow(keys: List<String>, sidePadding: Int): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(sidePadding, dp(5), sidePadding, dp(5))
            weightSum = keys.size.toFloat()
            for (k in keys) {
                val keyView = createKeyButton(k, isFunctional = false)
                val lp = LinearLayout.LayoutParams(0, dp(43), 1f).apply {
                    setMargins(dp(3), 0, dp(3), 0)
                }
                addView(keyView, lp)
            }
        }
    }

    private fun buildRow3(keys: List<String>): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(5), 0, dp(5))
            for (k in keys) {
                val isSpecial = (k == "SHIFT" || k == "DEL" || k == "#+=" || k == "123")
                val weight = if (isSpecial) 1.38f else 1f
                val displayLabel = when (k) {
                    "SHIFT" -> if (shiftState == 2) "⇪" else "⇧"
                    "DEL" -> "⌫"
                    else -> k
                }
                val btn = createKeyButton(displayLabel, isFunctional = isSpecial, rawCode = k)
                val lp = LinearLayout.LayoutParams(0, dp(43), weight).apply {
                    setMargins(dp(3), 0, dp(3), 0)
                }
                addView(btn, lp)
            }
        }
    }

    private fun buildRow4(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(5), 0, dp(4))

            val modeLabel = if (symbolPage == 0) "123" else "ABC"
            val modeBtn = createKeyButton(modeLabel, isFunctional = true, rawCode = "MODE_SWITCH")
            val emojiBtn = createKeyButton("☺", isFunctional = true, rawCode = "EMOJI")
            val spaceBtn = createSpacebarWithTrackpad()
            val enterBtn = createKeyButton("Nhập", isFunctional = true, rawCode = "ENTER")

            addView(modeBtn, LinearLayout.LayoutParams(0, dp(43), 1.35f).apply { setMargins(dp(3),0,dp(3),0) })
            addView(emojiBtn, LinearLayout.LayoutParams(0, dp(43), 1.1f).apply { setMargins(dp(3),0,dp(3),0) })
            addView(spaceBtn, LinearLayout.LayoutParams(0, dp(43), 4.8f).apply { setMargins(dp(3),0,dp(3),0) })
            addView(enterBtn, LinearLayout.LayoutParams(0, dp(43), 1.85f).apply { setMargins(dp(3),0,dp(3),0) })
        }
    }

    // Khoảng trống dưới đáy chuẩn iPhone (đẩy bàn phím lên cao + có icon Quả địa cầu & Micro)
    private fun buildIOSBottomChin(): FrameLayout {
        return FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(-1, dp(44))
            setPadding(dp(22), dp(4), dp(22), dp(6))

            val globeIcon = TextView(this@IOSInputMethodService).apply {
                text = "🌐"
                textSize = 21f
                setTextColor(Color.parseColor("#4A4A4F"))
                gravity = Gravity.CENTER
                setOnClickListener {
                    vibrateTick()
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.showInputMethodPicker()
                }
            }

            val homeIndicator = View(this@IOSInputMethodService).apply {
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#8E8E93"))
                    cornerRadius = dp(3).toFloat()
                }
            }

            val micIcon = TextView(this@IOSInputMethodService).apply {
                text = "🎤"
                textSize = 19f
                setTextColor(Color.parseColor("#4A4A4F"))
                gravity = Gravity.CENTER
                setOnClickListener {
                    vibrateTick()
                    requestHideSelf(0)
                }
            }

            addView(globeIcon, FrameLayout.LayoutParams(dp(40), dp(36), Gravity.START or Gravity.CENTER_VERTICAL))
            addView(homeIndicator, FrameLayout.LayoutParams(dp(130), dp(5), Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL))
            addView(micIcon, FrameLayout.LayoutParams(dp(40), dp(36), Gravity.END or Gravity.CENTER_VERTICAL))
        }
    }

    private fun createKeyButton(label: String, isFunctional: Boolean, rawCode: String = label): TextView {
        val isShiftActive = (rawCode == "SHIFT" && shiftState > 0)
        val bgColor = when {
            isShiftActive -> Color.WHITE
            isFunctional -> Color.parseColor("#ADB3BC")
            else -> Color.WHITE
        }

        return TextView(this).apply {
            val displayText = if (!isFunctional && symbolPage == 0 && rawCode.length == 1) {
                if (shiftState > 0) label.uppercase() else label.lowercase()
            } else label

            text = displayText
            gravity = Gravity.CENTER
            setTextColor(Color.BLACK)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, if (isFunctional) 16f else 22f)
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            background = createIOSKeyDrawable(bgColor)

            if (rawCode == "DEL") {
                // Nhấn giữ phím ⌫ để xóa liên tục như iOS
                val deleteRunnable = object : Runnable {
                    override fun run() {
                        vibrateTick()
                        performDelete()
                        handler.postDelayed(this, 65)
                    }
                }
                setOnTouchListener { v, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            v.background = createIOSKeyDrawable(Color.parseColor("#8E939E"))
                            vibrateTick()
                            performDelete()
                            handler.postDelayed(deleteRunnable, 380)
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            v.background = createIOSKeyDrawable(bgColor)
                            handler.removeCallbacks(deleteRunnable)
                        }
                    }
                    true
                }
            } else {
                setOnTouchListener { v, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            v.background = createIOSKeyDrawable(Color.parseColor("#E5E5EA"))
                            vibrateTick()
                        }
                        MotionEvent.ACTION_UP -> {
                            v.background = createIOSKeyDrawable(bgColor)
                            handleKeyClick(rawCode)
                        }
                        MotionEvent.ACTION_CANCEL -> {
                            v.background = createIOSKeyDrawable(bgColor)
                        }
                    }
                    true
                }
            }
        }
    }

    // Phím Dấu cách tích hợp vuốt trượt di chuyển con trỏ (Trackpad) và nhấn đúp ra dấu chấm
    private fun createSpacebarWithTrackpad(): TextView {
        return TextView(this).apply {
            text = "dấu cách"
            gravity = Gravity.CENTER
            setTextColor(Color.BLACK)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            background = createIOSKeyDrawable(Color.WHITE)

            var startX = 0f
            var isDraggingCursor = false

            setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = event.x
                        isDraggingCursor = false
                        v.background = createIOSKeyDrawable(Color.parseColor("#B8BCC6"))
                        vibrateTick()
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.x - startX
                        if (abs(dx) > dp(12)) {
                            isDraggingCursor = true
                            val keyCode = if (dx > 0) KeyEvent.KEYCODE_DPAD_RIGHT else KeyEvent.KEYCODE_DPAD_LEFT
                            currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
                            currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
                            startX = event.x
                            vibrateTick()
                        }
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        v.background = createIOSKeyDrawable(Color.WHITE)
                        if (!isDraggingCursor && event.action == MotionEvent.ACTION_UP) {
                            handleSpacePress()
                        }
                    }
                }
                true
            }
        }
    }

    private fun handleSpacePress() {
        val ic = currentInputConnection ?: return
        val now = System.currentTimeMillis()
        if (now - lastSpaceTime < 280) {
            val before = ic.getTextBeforeCursor(2, 0)?.toString() ?: ""
            if (before.endsWith(" ") && before.length >= 2 && before[0].isLetterOrDigit()) {
                ic.deleteSurroundingText(1, 0)
                ic.commitText(". ", 1)
                shiftState = 1
                renderKeyboard()
                lastSpaceTime = 0L
                return
            }
        }
        lastSpaceTime = now
        ic.commitText(" ", 1)
    }

    private fun performDelete() {
        val ic = currentInputConnection ?: return
        val selected = ic.getSelectedText(0)
        if (selected.isNullOrEmpty()) {
            ic.deleteSurroundingText(1, 0)
        } else {
            ic.commitText("", 1)
        }
    }

    private fun handleKeyClick(code: String) {
        val ic = currentInputConnection ?: return
        when (code) {
            "SHIFT" -> {
                val now = System.currentTimeMillis()
                shiftState = if (now - lastShiftTime < 300) 2 else if (shiftState == 0) 1 else 0
                lastShiftTime = now
                renderKeyboard()
            }
            "MODE_SWITCH" -> {
                symbolPage = if (symbolPage == 0) 1 else 0
                renderKeyboard()
            }
            "#+=" -> {
                symbolPage = 2
                renderKeyboard()
            }
            "123" -> {
                symbolPage = 1
                renderKeyboard()
            }
            "EMOJI" -> {
                ic.commitText("😊", 1)
            }
            "ENTER" -> {
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
            }
            else -> {
                val charToProcess = if (symbolPage == 0 && shiftState > 0) code.uppercase() else code.lowercase()
                if (symbolPage == 0) {
                    applyVietnameseTelex(ic, charToProcess[0])
                    if (shiftState == 1) {
                        shiftState = 0
                        renderKeyboard()
                    }
                } else {
                    ic.commitText(code, 1)
                }
            }
        }
    }

    // Xử lý gõ tiếng Việt Telex chuẩn iOS (gõ W ra thẳng Ư ngay lập tức)
    private fun applyVietnameseTelex(ic: InputConnection, inputChar: Char) {
        val before = ic.getTextBeforeCursor(20, 0)?.toString() ?: ""
        var wordStart = before.length
        while (wordStart > 0 && before[wordStart - 1].isLetter()) {
            wordStart--
        }
        val currentWord = before.substring(wordStart)

        val result = IOSTelexEngine.processKey(currentWord, inputChar)
        if (result != null) {
            ic.beginBatchEdit()
            if (currentWord.isNotEmpty()) {
                ic.deleteSurroundingText(currentWord.length, 0)
            }
            ic.commitText(result, 1)
            ic.endBatchEdit()
        } else {
            ic.commitText(inputChar.toString(), 1)
        }
    }

    // Tạo hiệu ứng phím nổi 3D có viền bóng đổ dưới đáy giống hệt iOS
    private fun createIOSKeyDrawable(faceColor: Int): LayerDrawable {
        val shadow = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(6).toFloat()
            setColor(Color.parseColor("#898C94"))
        }
        val face = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(6).toFloat()
            setColor(faceColor)
        }
        return LayerDrawable(arrayOf(shadow, face)).apply {
            setLayerInset(1, 0, 0, 0, dp(1))
        }
    }

    private fun vibrateTick() {
        val v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(7, 90))
        }
    }

    private fun dp(value: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics).toInt()
}

// Bộ máy xử lý dấu Tiếng Việt chuẩn iOS: W -> Ư tức thì, WW -> W, bỏ dấu tự do
object IOSTelexEngine {

    private val toneRows = mapOf(
        'a' to charArrayOf('a','à','á','ả','ã','ạ'),
        'ă' to charArrayOf('ă','ằ','ắ','ẳ','ẵ','ặ'),
        'â' to charArrayOf('â','ầ','ấ','ẩ','ẫ','ậ'),
        'e' to charArrayOf('e','è','é','ẻ','ẽ','ẹ'),
        'ê' to charArrayOf('ê','ề','ế','ể','ễ','ệ'),
        'i' to charArrayOf('i','ì','í','ỉ','ĩ','ị'),
        'o' to charArrayOf('o','ò','ó','ỏ','õ','ọ'),
        'ô' to charArrayOf('ô','ồ','ố','ổ','ỗ','ộ'),
        'ơ' to charArrayOf('ơ','ờ','ớ','ở','ỡ','ợ'),
        'u' to charArrayOf('u','ù','ú','ủ','ũ','ụ'),
        'ư' to charArrayOf('ư','ừ','ứ','ử','ữ','ự'),
        'y' to charArrayOf('y','ỳ','ý','ỷ','ỹ','ỵ')
    )

    private val toneKeyMap = mapOf('f' to 1, 's' to 2, 'r' to 3, 'x' to 4, 'j' to 5, 'z' to 0)

    fun processKey(word: String, ch: Char): String? {
        val lowCh = ch.lowercaseChar()
        val isUpperCh = ch.isUpperCase()

        // 1. Xử lý phím W chuẩn iOS: Gõ W đứng một mình hoặc đầu từ -> biến ngay thành Ư
        if (lowCh == 'w') {
            if (word.isEmpty()) {
                return if (isUpperCh) "Ư" else "ư"
            }
            // Nếu ký tự cuối đang là 'ư' (do vừa bấm W), bấm W lần nữa sẽ trả về 'w' (WW -> W)
            val last = word.last()
            if (last == 'ư' || last == 'Ư') {
                return word.dropLast(1) + (if (last.isUpperCase()) 'W' else 'w')
            }
            // Chuyển uo + w -> ươ
            val lowWord = word.lowercase()
            if (lowWord.endsWith("uo") || lowWord.endsWith("ưo")) {
                val uChar = if (word[word.length - 2].isUpperCase()) 'Ư' else 'ư'
                val oChar = if (word.last().isUpperCase()) 'Ơ' else 'ơ'
                return word.dropLast(2) + uChar + oChar
            }
            // Chuyển a/o/u ở trong từ sang ă/ơ/ư
            val chars = word.toCharArray()
            for (i in chars.indices.reversed()) {
                val (base, tone) = extractBaseAndTone(chars[i])
                val targetBase = when (base) {
                    'a' -> 'ă'
                    'o' -> 'ơ'
                    'u' -> 'ư'
                    else -> null
                }
                if (targetBase != null) {
                    val newChar = applyToneToBase(targetBase, tone, chars[i].isUpperCase())
                    chars[i] = newChar
                    return String(chars)
                }
            }
            // Nếu đầu từ là phụ âm (như 'nh', 'th', 'tr', 'ch', 'ng', 'kh', 'ph', 'g', 'c', 't', 'd', 'l', 'm', 'n', 'r', 's', 'v', 'x') + w -> thêm 'ư'
            return word + (if (isUpperCh) "Ư" else "ư")
        }

        // 2. Xử lý gõ đúp aa->â, ee->ê, oo->ô, dd->đ
        if (word.isNotEmpty() && lowCh in charArrayOf('a', 'e', 'o', 'd')) {
            val last = word.last()
            if (lowCh == 'd' && (last == 'd' || last == 'D')) {
                return word.dropLast(1) + (if (last.isUpperCase()) 'Đ' else 'đ')
            }
            if (lowCh == 'd' && (last == 'đ' || last == 'Đ')) {
                return word.dropLast(1) + (if (last.isUpperCase()) 'D' else 'd') + ch
            }
            val chars = word.toCharArray()
            for (i in chars.indices.reversed()) {
                val (base, tone) = extractBaseAndTone(chars[i])
                if (base == lowCh) {
                    val hatBase = when (lowCh) {
                        'a' -> 'â'
                        'e' -> 'ê'
                        'o' -> 'ô'
                        else -> null
                    }
                    if (hatBase != null) {
                        chars[i] = applyToneToBase(hatBase, tone, chars[i].isUpperCase())
                        return String(chars)
                    }
                }
            }
        }

        // 3. Xử lý bỏ dấu thanh (s, f, r, x, j, z) tự do trên nguyên âm chính
        val toneIdx = toneKeyMap[lowCh]
        if (toneIdx != null && word.isNotEmpty()) {
            val chars = word.toCharArray()
            val targetIndex = findTargetVowelIndex(chars)
            if (targetIndex != -1) {
                val (base, currentTone) = extractBaseAndTone(chars[targetIndex])
                // Nếu bấm trùng dấu đang có -> hủy dấu và trả lại chữ cái vừa bấm (ví dụ: 'á' + 's' -> 'as')
                if (currentTone == toneIdx && toneIdx != 0) {
                    chars[targetIndex] = applyToneToBase(base, 0, chars[targetIndex].isUpperCase())
                    return String(chars) + ch
                }
                if (toneIdx == 0 && currentTone == 0) return null
                chars[targetIndex] = applyToneToBase(base, toneIdx, chars[targetIndex].isUpperCase())
                return String(chars)
            }
        }

        return null
    }

    private fun findTargetVowelIndex(chars: CharArray): Int {
        // Ưu tiên nguyên âm đã có mũ/móc (ă, â, ê, ô, ơ, ư) trước
        for (i in chars.indices.reversed()) {
            val (base, _) = extractBaseAndTone(chars[i])
            if (base in charArrayOf('ă', 'â', 'ê', 'ô', 'ơ', 'ư')) return i
        }
        // Tìm cụm nguyên âm để đặt dấu đúng vị trí
        val vowelIndices = mutableListOf<Int>()
        for (i in chars.indices) {
            val (base, _) = extractBaseAndTone(chars[i])
            if (toneRows.containsKey(base)) {
                // Không tính chữ 'u' trong 'qu' hoặc 'i' trong 'gi' là nguyên âm chính nếu phía sau còn nguyên âm khác
                val prev = if (i > 0) chars[i - 1].lowercaseChar() else ' '
                if ((base == 'u' && prev == 'q') || (base == 'i' && prev == 'g')) continue
                vowelIndices.add(i)
            }
        }
        if (vowelIndices.isEmpty()) return -1
        if (vowelIndices.size == 1) return vowelIndices[0]
        // Nếu có phụ âm cuối (như 'oanh', 'toan') -> đặt dấu ở nguyên âm thứ 2; nếu mở (như 'hoa', 'kia') -> đặt ở nguyên âm đầu
        val hasEndingConsonant = vowelIndices.last() < chars.size - 1
        return if (hasEndingConsonant) vowelIndices[vowelIndices.size - 1] else vowelIndices[0]
    }

    private fun extractBaseAndTone(c: Char): Pair<Char, Int> {
        val low = c.lowercaseChar()
        for ((base, arr) in toneRows) {
            val idx = arr.indexOf(low)
            if (idx != -1) return Pair(base, idx)
        }
        return Pair(low, 0)
    }

    private fun applyToneToBase(base: Char, toneIdx: Int, isUpper: Boolean): Char {
        val arr = toneRows[base] ?: return base
        val res = arr[toneIdx]
        return if (isUpper) res.uppercaseChar() else res
    }
}
