package com.example.cash

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class BudgetActivity : AppCompatActivity() {

    private lateinit var etDay: EditText
    private lateinit var etWeek: EditText
    private lateinit var etMonth: EditText
    private lateinit var tvShowDay: TextView
    private lateinit var tvShowWeek: TextView
    private lateinit var tvShowMonth: TextView
    private lateinit var btnSave: Button
    private lateinit var btnReset: Button
    private lateinit var tvError: TextView

    private val PREFS_NAME = "budget_prefs"
    private var userEmail: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_budget)

        // 1. 取得 Google 登入 email
        userEmail = intent.getStringExtra("user_email") ?: ""
        if (userEmail.isEmpty()) {
            Toast.makeText(this, getString(R.string.toast_login_first), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        etDay = findViewById(R.id.et_budget_day)
        etWeek = findViewById(R.id.et_budget_week)
        etMonth = findViewById(R.id.et_budget_month)
        tvShowDay = findViewById(R.id.tv_budget_show_day)
        tvShowWeek = findViewById(R.id.tv_budget_show_week)
        tvShowMonth = findViewById(R.id.tv_budget_show_month)
        btnSave = findViewById(R.id.btn_budget_save)
        btnReset = findViewById(R.id.btn_budget_reset)
        tvError = findViewById(R.id.tv_budget_error)

        loadBudget()

        btnSave.setOnClickListener { saveBudget() }
        btnReset.setOnClickListener { resetBudget() }
    }

    private fun budgetKey(base: String): String {
        return "${base}_${userEmail}"
    }

    private fun saveBudget() {
        tvError.visibility = View.GONE

        val dayStr = etDay.text.toString()
        val weekStr = etWeek.text.toString()
        val monthStr = etMonth.text.toString()

        val day = dayStr.toDoubleOrNull()
        val week = weekStr.toDoubleOrNull()
        val month = monthStr.toDoubleOrNull()

        // 三個欄位都必須有填且都要正確（>0）
        if (dayStr.isBlank() || day == null || day <= 0 ||
            weekStr.isBlank() || week == null || week <= 0 ||
            monthStr.isBlank() || month == null || month <= 0
        ) {
            Toast.makeText(this, getString(R.string.budget_invalid), Toast.LENGTH_SHORT).show()
            return
        }

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putFloat(budgetKey("budget_day"), day!!.toFloat())
        editor.putFloat(budgetKey("budget_week"), week!!.toFloat())
        editor.putFloat(budgetKey("budget_month"), month!!.toFloat())
        editor.apply()

        Toast.makeText(this, getString(R.string.budget_saved), Toast.LENGTH_SHORT).show()
        loadBudget()
    }

    private fun resetBudget() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.remove(budgetKey("budget_day"))
        editor.remove(budgetKey("budget_week"))
        editor.remove(budgetKey("budget_month"))
        editor.apply()

        etDay.setText("")
        etWeek.setText("")
        etMonth.setText("")
        tvShowDay.text = ""
        tvShowWeek.text = ""
        tvShowMonth.text = ""
        tvError.visibility = View.GONE

        Toast.makeText(this, getString(R.string.budget_reset_success), Toast.LENGTH_SHORT).show()
    }

    private fun loadBudget() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val day = prefs.getFloat(budgetKey("budget_day"), 0f)
        val week = prefs.getFloat(budgetKey("budget_week"), 0f)
        val month = prefs.getFloat(budgetKey("budget_month"), 0f)

        if (day > 0f) {
            tvShowDay.text = getString(R.string.budget_show_text, day)
            etDay.setText(day.toString())
        } else {
            tvShowDay.text = ""
            etDay.setText("")
        }
        if (week > 0f) {
            tvShowWeek.text = getString(R.string.budget_show_text, week)
            etWeek.setText(week.toString())
        } else {
            tvShowWeek.text = ""
            etWeek.setText("")
        }
        if (month > 0f) {
            tvShowMonth.text = getString(R.string.budget_show_text, month)
            etMonth.setText(month.toString())
        } else {
            tvShowMonth.text = ""
            etMonth.setText("")
        }
    }
}
