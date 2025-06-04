package com.example.cash

import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.bumptech.glide.Glide
import com.itextpdf.text.*
import com.itextpdf.text.pdf.*
import com.opencsv.CSVWriter
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var buttonBalance: Button
    private lateinit var databasehelper: databasehelper
    private var isEnglish = false
    private var reminderHour = 21
    private var reminderMinute = 0
    private val REQUEST_CODE_EXPORT_CSV = 2001
    private val REQUEST_CODE_EXPORT_XLSX = 2002
    private val REQUEST_CODE_EXPORT_PDF = 2003
    private val REQUEST_CODE_LOGIN = 1000
    private var isLoggedIn = false
    private var userName: String? = null
    private var userAvatar: String? = null

    private var tempExportData: ByteArray? = null
    private var userEmail: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 一定要先設 layout！
        setContentView(R.layout.home_page)

        // 自動抓已登入的 userEmail
        val userPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        userEmail = userPrefs.getString("user_email", "") ?: ""
        isLoggedIn = userEmail.isNotEmpty()

        // 設定區 panel 跟設定齒輪
        val settingPanel = findViewById<LinearLayout>(R.id.settingPanel)
        val imageButtonSetting = findViewById<ImageButton>(R.id.imageButton_setting)
        settingPanel.visibility = View.GONE
        imageButtonSetting.setOnClickListener {
            settingPanel.visibility =
                if (settingPanel.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        // 語言偏好
        val langPrefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        isEnglish = langPrefs.getBoolean("language_english", false)
        applyLocale(isEnglish)

        val prefs = getSharedPreferences("reminder_prefs", Context.MODE_PRIVATE)
        reminderHour = prefs.getInt("hour", 21)
        reminderMinute = prefs.getInt("minute", 0)

        val switchLang = findViewById<Switch>(R.id.switch_language)
        val dateTextView = findViewById<TextView>(R.id.dateTextView)
        buttonBalance = findViewById(R.id.button_balance)
        databasehelper = databasehelper(this)
        val switchReminder = findViewById<Switch>(R.id.switch_reminder)
        val btnSelectTime = findViewById<Button>(R.id.btn_select_reminder_time)

        updateDateText(dateTextView)
        updateLanguage()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intent)
                Toast.makeText(this, "⚠️ 請開啟「精準提醒權限」以啟用每日提醒", Toast.LENGTH_LONG).show()
            }
        }

        // === ✨ 登入/登出區塊邏輯（最重要） ===
        val loginPanel = findViewById<LinearLayout>(R.id.loginPanel)
        val tvLogin = findViewById<TextView>(R.id.tv_login)
        val tvLoginTip = findViewById<TextView>(R.id.tv_login_tip)
        val ivUser = findViewById<ImageView>(R.id.iv_user)

        loginPanel.setOnClickListener {
            if (!isLoggedIn) {
                // 未登入時，去登入
                val intent = Intent(this, LoginActivity::class.java)
                startActivityForResult(intent, REQUEST_CODE_LOGIN)
            } else {
                // 已登入時，執行登出
                isLoggedIn = false
                userName = null
                userAvatar = null
                userEmail = ""
                // 清空本地帳號
                userPrefs.edit().remove("user_email").apply()
                tvLogin.text = getString(R.string.login_click)
                tvLoginTip.text = getString(R.string.login_tip)
                ivUser.setImageResource(R.drawable.user) // 預設頭貼
                Toast.makeText(this, getString(R.string.logged_out_toast), Toast.LENGTH_SHORT).show()
                displayIncomeExpenseDifference()
            }
        }

        findViewById<Button>(R.id.export_button).setOnClickListener {
            if (!isLoggedIn) {
                Toast.makeText(this, getString(R.string.toast_login_first), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val options = arrayOf("CSV", "Excel", "PDF")
            val builder = android.app.AlertDialog.Builder(this)
            builder.setTitle(getString(R.string.select_export_format))
            builder.setItems(options) { _, which ->
                when (which) {
                    0 -> startExportCSVWithSAF(createCSVBytes())
                    1 -> startExportXLSXWithSAF(createXLSXBytes())
                    2 -> startExportPDFWithSAF(createPDFBytes())
                }
            }
            builder.show()
        }

        switchLang.isChecked = isEnglish
        switchLang.setOnCheckedChangeListener { _, checked ->
            isEnglish = checked
            val langPrefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
            langPrefs.edit().putBoolean("language_english", isEnglish).apply()
            applyLocale(isEnglish)
            updateDateText(dateTextView)
            updateLanguage()
        }

        displayIncomeExpenseDifference()

        findViewById<ImageButton>(R.id.imageButton_calendar).setOnClickListener {
            showDatePickerDialog(dateTextView)
        }

        setupButtonNavigation()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }

        btnSelectTime.setOnClickListener {
            if (!isLoggedIn) {
                Toast.makeText(this, getString(R.string.toast_login_first), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            TimePickerDialog(this, { _, hour, minute ->
                reminderHour = hour
                reminderMinute = minute
                if (switchReminder.isChecked) {
                    setDailyReminder(reminderHour, reminderMinute)
                }
            }, reminderHour, reminderMinute, true).show()
        }

        findViewById<Button>(R.id.btn_set_budget).setOnClickListener {
            if (!isLoggedIn) {
                Toast.makeText(this, getString(R.string.toast_login_first), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val intent = Intent(this, BudgetActivity::class.java)
            intent.putExtra("user_email", userEmail) // 你的全局登入 email
            startActivity(intent)
        }

        switchReminder.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                setDailyReminder(reminderHour, reminderMinute)
            } else {
                cancelReminder()
            }
        }

        if (prefs.contains("hour") && prefs.contains("minute")) {
            setDailyReminder(reminderHour, reminderMinute)
            switchReminder.isChecked = true
        }
    }

    override fun onResume() {
        super.onResume()
        displayIncomeExpenseDifference()
    }

    private fun setDailyReminder(hour: Int, minute: Int) {
        val prefs = getSharedPreferences("reminder_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("hour", hour).putInt("minute", minute).apply()

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) add(Calendar.DATE, 1)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } else {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }

        Toast.makeText(this, getString(R.string.reminder_enabled), Toast.LENGTH_SHORT).show()
    }

    private fun cancelReminder() {
        val intent = Intent(this, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)

        val prefs = getSharedPreferences("reminder_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()

        Toast.makeText(this, getString(R.string.reminder_disabled), Toast.LENGTH_SHORT).show()
    }

    private fun applyLocale(toEnglish: Boolean) {
        val locale = if (toEnglish) Locale.ENGLISH else Locale("zh")
        Locale.setDefault(locale)

        val config = resources.configuration
        config.setLocale(locale)

        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics) // 允許 warning
    }

    private fun updateDateText(tv: TextView) {
        val pattern = getString(R.string.date_format)
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        tv.text = sdf.format(Date())
    }

    private fun updateLanguage() {
        findViewById<TextView>(R.id.logoName).text = getString(R.string.Logoname)
        findViewById<Button>(R.id.expand_button).text = getString(R.string.button_expense)
        findViewById<Button>(R.id.income_button).text = getString(R.string.button_income)
        findViewById<TextView>(R.id.hintTextView).text = getString(R.string.click_to_check)
        findViewById<Button>(R.id.export_button).text = getString(R.string.export)
        findViewById<TextView>(R.id.text_reminder).text = getString(R.string.daily_reminder)
        findViewById<Button>(R.id.btn_select_reminder_time).text = getString(R.string.reminder_time_picker)
        findViewById<TextView>(R.id.text_language).text = getString(R.string.language_switch)
        findViewById<TextView>(R.id.btn_set_budget).text = getString(R.string.button_set_budget)
        val tvLogin = findViewById<TextView>(R.id.tv_login)
        val tvLoginTip = findViewById<TextView>(R.id.tv_login_tip)
        if (isLoggedIn) {
            tvLogin.text = getString(R.string.login_out)
            tvLoginTip.text = getString(R.string.logged_in_tip, userName ?: "")
        } else {
            tvLogin.text = getString(R.string.login_click)
            tvLoginTip.text = getString(R.string.login_tip)
    }
    }

    private fun displayIncomeExpenseDifference() {
        if (userEmail.isEmpty()) {
            buttonBalance.text = "--"
            return
        }
        val dataList = databasehelper.getAllData(userEmail).toMutableList()
        val adapter = BalanceAdapter(dataList)
        val totalIn = adapter.getTotalIncome()
        val totalOut = adapter.getTotalExpense()
        val net = totalIn - totalOut

        val drawable = ContextCompat.getDrawable(this, R.drawable.circle)
        val tintColor = if (net < 0) Color.parseColor("#ff91e9") else Color.parseColor("#fffd7c")

        drawable?.let {
            DrawableCompat.setTint(it, tintColor)
            buttonBalance.background = it
        }
        buttonBalance.text = String.format(Locale.getDefault(), "%.2f", net)
    }

    private fun showDatePickerDialog(textView: TextView) {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, y, m, d ->
                cal.set(y, m, d)
                updateDateText(textView)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_LOGIN && resultCode == RESULT_OK && data != null) {
            val avatarUrl = data.getStringExtra("user_avatar") ?: ""
            val userNameValue = data.getStringExtra("user_name") ?: ""
            val userEmailValue = data.getStringExtra("user_email") ?: ""

            val ivUser = findViewById<ImageView>(R.id.iv_user)
            Log.d("LOGIN", "avatarUrl: $avatarUrl, userName: $userNameValue, userEmail: $userEmailValue")
            val tvLogin = findViewById<TextView>(R.id.tv_login)
            val tvLoginTip = findViewById<TextView>(R.id.tv_login_tip)

            if (!avatarUrl.isNullOrEmpty()) {
                Glide.with(this)
                    .load(avatarUrl)
                    .placeholder(R.drawable.user)
                    .error(R.drawable.user)
                    .skipMemoryCache(true) // 不用快取防止載入錯圖
                    .into(ivUser)
            } else {
                ivUser.setImageResource(R.drawable.user)
            }
            tvLogin.text = getString(R.string.login_out)
            tvLoginTip.text = getString(R.string.logged_in_tip, userNameValue)
            isLoggedIn = true
            userName = userNameValue
            userAvatar = avatarUrl
            userEmail = userEmailValue

            val userPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
            userPrefs.edit().putString("user_email", userEmail).apply()

            displayIncomeExpenseDifference()
        }

        if (resultCode == RESULT_OK && tempExportData != null) {
            val uri = data?.data
            if (uri != null) {
                contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(tempExportData)
                    output.flush()
                }
                Toast.makeText(this, "匯出成功！", Toast.LENGTH_SHORT).show()
            }
            tempExportData = null
        }
    }

    fun startExportCSVWithSAF(csvBytes: ByteArray) {
        tempExportData = csvBytes
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/csv"
            putExtra(Intent.EXTRA_TITLE, "cash_data.csv")
        }
        startActivityForResult(intent, REQUEST_CODE_EXPORT_CSV)
    }

    fun createCSVBytes(): ByteArray {
        val baos = ByteArrayOutputStream()
        baos.write(0xEF)
        baos.write(0xBB)
        baos.write(0xBF)

        val writer = OutputStreamWriter(baos, Charsets.UTF_8)
            val csvWriter = CSVWriter(writer)

            csvWriter.writeNext(arrayOf("類別", "金額", "日期", "類型"))

            val data = databasehelper.getAllData(userEmail)
            var totalIncome = 0.0
            var totalExpense = 0.0

            for (item in data) {
                val typeText = if (item.type) "收入" else "支出"
                if (item.type) totalIncome += item.amount else totalExpense += item.amount
                csvWriter.writeNext(arrayOf(item.category, item.amount.toString(), item.date, typeText))
            }

            // ➕ 統計列
            csvWriter.writeNext(arrayOf("", "", "", ""))
            csvWriter.writeNext(arrayOf("總收入", totalIncome.toString(), "", ""))
            csvWriter.writeNext(arrayOf("總支出", totalExpense.toString(), "", ""))

        csvWriter.close()
        return baos.toByteArray()
    }

    fun startExportXLSXWithSAF(xlsxBytes: ByteArray) {
        tempExportData = xlsxBytes
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            putExtra(Intent.EXTRA_TITLE, "cash_data.xlsx")
        }
        startActivityForResult(intent, REQUEST_CODE_EXPORT_XLSX)
    }


    fun createXLSXBytes(): ByteArray {
        val baos = ByteArrayOutputStream()
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("記帳資料")

            val boldFont = workbook.createFont().apply {
                bold = true
                fontHeightInPoints = 12
            }

            val headerStyle = workbook.createCellStyle().apply {
                setFont(boldFont)
                alignment = HorizontalAlignment.CENTER
                fillForegroundColor = IndexedColors.LIGHT_CORNFLOWER_BLUE.index
                fillPattern = FillPatternType.SOLID_FOREGROUND
            }

            val incomeStyle = workbook.createCellStyle().apply {
                fillForegroundColor = IndexedColors.LIGHT_GREEN.index
                fillPattern = FillPatternType.SOLID_FOREGROUND
            }

            val expenseStyle = workbook.createCellStyle().apply {
                fillForegroundColor = IndexedColors.ROSE.index
                fillPattern = FillPatternType.SOLID_FOREGROUND
            }

            val summaryStyle = workbook.createCellStyle().apply {
                setFont(boldFont)
                alignment = HorizontalAlignment.CENTER
            }

            val header = sheet.createRow(0)
            val titles = listOf("類別", "金額", "日期", "類型")
            for (i in titles.indices) {
                val cell = header.createCell(i)
                cell.setCellValue(titles[i])
                cell.cellStyle = headerStyle
            }

            val data = databasehelper.getAllData(userEmail)
            var totalIncome = 0.0
            var totalExpense = 0.0

            for ((index, item) in data.withIndex()) {
                val row = sheet.createRow(index + 1)
                val typeText = if (item.type) "收入" else "支出"
                val style = if (item.type) incomeStyle else expenseStyle

                if (item.type) totalIncome += item.amount else totalExpense += item.amount

                row.createCell(0).apply {
                    setCellValue(item.category)
                    cellStyle = style
                }
                row.createCell(1).apply {
                    setCellValue(item.amount)
                    cellStyle = style
                }
                row.createCell(2).apply {
                    setCellValue(item.date)
                    cellStyle = style
                }
                row.createCell(3).apply {
                    setCellValue(typeText)
                    cellStyle = style
                }
            }

            // ➕ 統計列
            val summaryRow1 = sheet.createRow(data.size + 2)
            summaryRow1.createCell(0).apply {
                setCellValue("總收入")
                cellStyle = summaryStyle
            }
            summaryRow1.createCell(1).apply {
                setCellValue(totalIncome)
                cellStyle = summaryStyle
            }

            val summaryRow2 = sheet.createRow(data.size + 3)
            summaryRow2.createCell(0).apply {
                setCellValue("總支出")
                cellStyle = summaryStyle
            }
            summaryRow2.createCell(1).apply {
                setCellValue(totalExpense)
                cellStyle = summaryStyle
            }

        workbook.write(baos)
        workbook.close()
        return baos.toByteArray()
    }

    fun startExportPDFWithSAF(pdfBytes: ByteArray) {
        tempExportData = pdfBytes
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
            putExtra(Intent.EXTRA_TITLE, "cash_data.pdf")
        }
        startActivityForResult(intent, REQUEST_CODE_EXPORT_PDF)
    }

    fun createPDFBytes(): ByteArray {
        val baos = ByteArrayOutputStream()
        val document = Document()
        PdfWriter.getInstance(document, baos)
        document.open()

            // ✅ 載入中文字型
            val baseFont = BaseFont.createFont("assets/fonts/NotoSansTC.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED)
            val chineseFont = Font(baseFont, 12f, Font.NORMAL)
            val chineseBoldFont = Font(baseFont, 12f, Font.BOLD)
            val chineseTitleFont = Font(baseFont, 18f, Font.BOLD)

            val titleFont = Font(Font.FontFamily.HELVETICA, 18f, Font.BOLD)
            val title = Paragraph("記帳資料", chineseTitleFont)
            title.alignment = Element.ALIGN_CENTER
            title.spacingAfter = 16f
            document.add(title)

            val table = PdfPTable(4)
            table.widthPercentage = 100f
            table.setWidths(floatArrayOf(3f, 2f, 3f, 2f))

            val boldFont = Font(Font.FontFamily.HELVETICA, 12f, Font.BOLD)
            val headers = listOf("類別", "金額", "日期", "類型")
            for (text in headers) {
                val cell = PdfPCell(Phrase(text, chineseBoldFont))
                cell.horizontalAlignment = Element.ALIGN_CENTER
                cell.borderWidth = 1f
                cell.backgroundColor = BaseColor.LIGHT_GRAY
                table.addCell(cell)
            }

            val data = databasehelper.getAllData(userEmail)
            var totalIncome = 0.0
            var totalExpense = 0.0

            for (item in data) {
                val typeText = if (item.type) "收入" else "支出"
                val bgColor = if (item.type) BaseColor(232, 245, 233) else BaseColor(255, 235, 238)
                if (item.type) totalIncome += item.amount else totalExpense += item.amount

                listOf(item.category, item.amount.toString(), item.date, typeText).forEach { value ->
                    val cell = PdfPCell(Phrase(value, chineseFont))
                    cell.horizontalAlignment = Element.ALIGN_CENTER
                    cell.borderWidth = 1f
                    cell.backgroundColor = bgColor
                    table.addCell(cell)
                }
            }

            // ➕ 空白列
            repeat(4) {
                val blankCell = PdfPCell(Phrase(" "))
                blankCell.borderWidth = 0f
                table.addCell(blankCell)
            }

            // ➕ 統計列
            val summaryFont = Font(Font.FontFamily.HELVETICA, 12f, Font.BOLD)
            val incomeCell = PdfPCell(Phrase("總收入", chineseBoldFont))
            incomeCell.colspan = 2
            incomeCell.horizontalAlignment = Element.ALIGN_CENTER
            incomeCell.borderWidth = 1f
            table.addCell(incomeCell)

            val incomeValueCell = PdfPCell(Phrase(totalIncome.toString(), summaryFont))
            incomeValueCell.colspan = 2
            incomeValueCell.horizontalAlignment = Element.ALIGN_CENTER
            incomeValueCell.borderWidth = 1f
            table.addCell(incomeValueCell)

            val expenseCell = PdfPCell(Phrase("總支出", chineseBoldFont))
            expenseCell.colspan = 2
            expenseCell.horizontalAlignment = Element.ALIGN_CENTER
            expenseCell.borderWidth = 1f
            table.addCell(expenseCell)

            val expenseValueCell = PdfPCell(Phrase(totalExpense.toString(), summaryFont))
            expenseValueCell.colspan = 2
            expenseValueCell.horizontalAlignment = Element.ALIGN_CENTER
            expenseValueCell.borderWidth = 1f
            table.addCell(expenseValueCell)

            document.add(table)
        document.close()
        return baos.toByteArray()
    }

    private fun setupButtonNavigation() {
        findViewById<Button>(R.id.button_balance).setOnClickListener {
            if (!isLoggedIn) {
                Toast.makeText(this, getString(R.string.toast_login_first), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startActivity(Intent(this, balance::class.java))
        }
        findViewById<Button>(R.id.expand_button).setOnClickListener {
            if (!isLoggedIn) {
                Toast.makeText(this,  getString(R.string.toast_login_first), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startActivity(Intent(this, expense::class.java))
        }
        findViewById<Button>(R.id.income_button).setOnClickListener {
            if (!isLoggedIn) {
                Toast.makeText(this, getString(R.string.toast_login_first), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startActivity(Intent(this, income::class.java))
        }
    }
}
