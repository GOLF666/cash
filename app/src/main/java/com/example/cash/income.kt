package com.example.cash

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.app.DatePickerDialog
import android.text.Editable
import android.text.TextWatcher
import android.widget.ImageButton
import android.widget.LinearLayout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


class income : AppCompatActivity() {
    private var currentInput: String = ""
    private var operator: String? = null
    private var firstOperand: Double =0.0
    private var result: Double = 0.0
    private var typeofinsert = 1
    private var currentCategory: String = ""
    private lateinit var databasehelper: databasehelper
    private lateinit var resultTextView: TextView
    private lateinit var titleTextView: TextView

    private lateinit var userEmail: String // 新增


    private var selectedDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)


    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_income)

        // 取得 userEmail（從 SharedPreferences，登入時存的 email）
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        userEmail = prefs.getString("user_email", "") ?: ""


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val navBarHeight = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            findViewById<LinearLayout>(R.id.bottom).setPadding(0, 0, 0, navBarHeight)
            insets
        }

        //label variable
        resultTextView= findViewById(R.id.resultTextView)
        titleTextView= findViewById(R.id.input_title)
        databasehelper = databasehelper(this)
        val dateTextView: TextView = findViewById(R.id.dateTextView)

        val dateButton:ImageButton= findViewById(R.id.imageButton)
        dateButton.setOnClickListener {
            showDatePicker()
        }

        resultTextView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val inputText = s.toString().trim()
                if(inputText.isNotEmpty()){
                    result = inputText.toDoubleOrNull() ?: 0.0
                }
                else{
                    Toast.makeText(resultTextView.context, "Please input amount", Toast.LENGTH_SHORT).show()
                }
            }
            override fun afterTextChanged(s: Editable?) {
            }
        })

        findViewById<ImageButton>(R.id.button_salary).setOnClickListener({selectCategories(getString(R.string.salary))})
        findViewById<ImageButton>(R.id.button_dividend).setOnClickListener({selectCategories(getString(R.string.dividend))})
        findViewById<ImageButton>(R.id.button_bonus).setOnClickListener({selectCategories(getString(R.string.bonus))})
        findViewById<ImageButton>(R.id.button_investment).setOnClickListener({selectCategories(getString(R.string.investment))})
        findViewById<ImageButton>(R.id.button_trade).setOnClickListener({selectCategories(getString(R.string.trade))})
        findViewById<ImageButton>(R.id.button_rents).setOnClickListener({selectCategories(getString(R.string.rents))})
        findViewById<ImageButton>(R.id.button_rebate).setOnClickListener({selectCategories(getString(R.string.rebate))})
        findViewById<ImageButton>(R.id.button_others).setOnClickListener({selectCategories(getString(R.string.others))})

        findViewById<Button>(R.id.button1).setOnClickListener { appendNumber("1", resultTextView) }
        findViewById<Button>(R.id.button2).setOnClickListener { appendNumber("2", resultTextView) }
        findViewById<Button>(R.id.button3).setOnClickListener { appendNumber("3", resultTextView) }
        findViewById<Button>(R.id.button4).setOnClickListener { appendNumber("4", resultTextView) }
        findViewById<Button>(R.id.button5).setOnClickListener { appendNumber("5", resultTextView) }
        findViewById<Button>(R.id.button6).setOnClickListener { appendNumber("6", resultTextView) }
        findViewById<Button>(R.id.button7).setOnClickListener { appendNumber("7", resultTextView) }
        findViewById<Button>(R.id.button8).setOnClickListener { appendNumber("8", resultTextView) }
        findViewById<Button>(R.id.button9).setOnClickListener { appendNumber("9", resultTextView) }
        findViewById<Button>(R.id.button0).setOnClickListener { appendNumber("0", resultTextView) }

        //運算事件
        findViewById<Button>(R.id.button_add).setOnClickListener { setOperator("+", resultTextView) }
        findViewById<Button>(R.id.button_minus).setOnClickListener { setOperator("-", resultTextView) }
        findViewById<Button>(R.id.button_multiply).setOnClickListener { setOperator("×", resultTextView) }
        findViewById<Button>(R.id.button_divide).setOnClickListener { setOperator("÷", resultTextView) }

        findViewById<Button>(R.id.button_cancel).setOnClickListener{
            currentInput = ""
            firstOperand = 0.0
            operator = null
            resultTextView.text = "0"
        }

        findViewById<Button>(R.id.button_equal).setOnClickListener {
            // 确保 operator 不为空，并且 currentInput 中有有效的数字
            if (operator != null && currentInput.isNotEmpty()) {
                val secondOperand = currentInput.toDoubleOrNull() ?: 0.0
                // 计算结果
                when (operator) {
                    "+" -> result = firstOperand + secondOperand
                    "-" -> result = firstOperand - secondOperand
                    "×" -> result = firstOperand * secondOperand
                    "÷" -> {
                        // 防止除以0
                        result = if (secondOperand != 0.0) firstOperand / secondOperand else 0.0
                    }
                }
                // 显示结果
                resultTextView.text = result.toString()

                // 清空运算符和输入
                operator = null
                currentInput = ""
            } else {
                // 如果没有输入或 operator 为 null，显示错误消息
                resultTextView.text = "Error"
            }
        }


        findViewById<Button>(R.id.button_save).setOnClickListener{
            insertData()
        }

    }
    private fun appendNumber(number: String, resultTextView: TextView) {
        currentInput += number
        resultTextView.text = currentInput
    }
    private fun setOperator(op: String, resultTextView: TextView) {
        operator = op
        firstOperand = currentInput.toDoubleOrNull() ?: 0.0
        currentInput = ""
    }

    private fun insertData() {
        val title = titleTextView.text.toString()
        val amount = result.toString() // 確保這裡是 String
        val type = typeofinsert == 1 // 判斷是收入還是支出
        var category = currentCategory
        val date = selectedDate

        if( category.isEmpty())
        {
            category = "Unknown Category"
        }

        // 插入數據
        val insertResult = databasehelper.insert(title, amount, type, category, selectedDate, userEmail)

        // 檢查插入結果
        if (insertResult != -1L) {
            Toast.makeText(this, getString(R.string.insert_success), Toast.LENGTH_SHORT).show()
            // ✅ 插入成功 → 回到主頁或上一頁
            finish() // ⬅️ 這行就是關鍵！
        } else {
            Log.e("DatabaseError", "Insert failed for title: $title, amount: $amount, type: $type, date: $date")
            Toast.makeText(this, getString(R.string.insert_failed), Toast.LENGTH_SHORT).show()
        }
        currentInput = ""
        Log.d("DatabaseCheck", databasehelper.getAllData(userEmail).toString())
    }

    override fun onResume() {
        super.onResume()
        // 根據當前語言重新設定日期格式
        val dateTextView: TextView = findViewById(R.id.dateTextView)
        val calendar = Calendar.getInstance()
        updateDateTextView(dateTextView, calendar)
    }

    private fun updateDateTextView(textView: TextView, calendar: Calendar) {
        val locale = if (Locale.getDefault().language == "en") Locale.ENGLISH else Locale.TAIWAN
        val dateFormat = if (locale == Locale.ENGLISH) {
            SimpleDateFormat("yyyy-MM-dd", locale)
        } else {
            SimpleDateFormat("yyyy年MM月dd日", locale)
        }
        textView.text = dateFormat.format(calendar.time)
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        // 創建日期選擇對話框
        val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            // 更新選擇的日期
            selectedDate = String.format(Locale.getDefault(),"%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
            Toast.makeText(this, "Selected date: $selectedDate", Toast.LENGTH_SHORT).show()
        }, year, month, day)

        datePickerDialog.show()
    }
    private fun selectCategories(category: String) {
        // 处理选择的类别
        currentCategory = category // Update the current category
        Toast.makeText(this, getString(R.string.category_selected, category), Toast.LENGTH_SHORT).show()
    }
}