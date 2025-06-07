package com.example.cash

import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import okhttp3.OkHttpClient
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*

class ChatActivity : AppCompatActivity() {
    private lateinit var etMessage: EditText
    private lateinit var tvResponse: TextView
    private lateinit var btnMic: ImageButton
    private lateinit var db: databasehelper
    private lateinit var userEmail: String
    private val REQUEST_CODE_SPEECH_INPUT = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        etMessage = findViewById(R.id.etMessage)
        tvResponse = findViewById(R.id.tvResponse)
        val btnSend = findViewById<Button>(R.id.btnSend)
        btnMic = findViewById(R.id.btnMic)
        db = databasehelper(this)

        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        userEmail = prefs.getString("user_email", "") ?: ""

        btnSend.setOnClickListener {
            val input = etMessage.text.toString().trim()
            if (input.isNotEmpty()) {
                if (input.contains("查詢") || input.contains("查看") || input.contains("支出") || input.contains("收入")) {
                    handleQuery(input)
                } else {
                    sendToCaiBaoForInsert(input)
                }
            }
        }

        btnMic.setOnClickListener {
            val languageCode = if (Locale.getDefault().language.startsWith("zh")) "zh-TW" else "en-US"
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
            intent.putExtra(
                RecognizerIntent.EXTRA_PROMPT,
                if (languageCode == "zh-TW") "請說出你要記錄的花費內容" else "Please describe your expense or income."
            )
            try {
                startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT)
            } catch (e: Exception) {
                Toast.makeText(this, "Speech recognition not supported", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SPEECH_INPUT && resultCode == RESULT_OK && data != null) {
            val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            result?.firstOrNull()?.let {
                etMessage.setText(it)
                if (it.contains("查詢") || it.contains("查看") || it.contains("支出") || it.contains("收入")) {
                    handleQuery(it)
                } else {
                    sendToCaiBaoForInsert(it)
                }
            }
        }
    }

    private fun handleQuery(input: String) {
        val pattern = Regex("""(\d{1,2})月(\d{1,2})日""")
        val match = pattern.find(input)
        val today = Calendar.getInstance(TimeZone.getTimeZone("Asia/Taipei"))
        val year = today.get(Calendar.YEAR)

        val (month, day) = if (match != null) {
            val m = match.groupValues[1].padStart(2, '0')
            val d = match.groupValues[2].padStart(2, '0')
            Pair(m, d)
        } else {
            val fmt = SimpleDateFormat("MM-dd", Locale.getDefault())
            val parts = fmt.format(today.time).split("-")
            Pair(parts[0], parts[1])
        }

        val date = "$year-$month-$day"
        val isExpense = input.contains("支出")
        val isIncome = input.contains("收入")

        val data = when {
            isExpense -> db.getExpenseByDate(userEmail, date)
            isIncome -> db.getIncomeByDate(userEmail, date)
            else -> db.getAllRecordsByDate(userEmail, date)
        }

        val output = if (data.isEmpty()) {
            "📭 找不到 $date 的${if (isExpense) "支出" else if (isIncome) "收入" else "記錄"}喔～"
        } else {
            val sb = StringBuilder("📅 $date 記錄如下：\n\n")
            data.forEach {
                sb.append("🔹${it.title} - ${it.amount}元 (${it.category})\n")
            }
            sb.toString()
        }

        runOnUiThread { tvResponse.text = output }
    }

    private fun sendToCaiBaoForInsert(input: String) {
        val isChinese = Locale.getDefault().language.startsWith("zh")
        val taiwanTime = Calendar.getInstance(TimeZone.getTimeZone("Asia/Taipei")).time
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(taiwanTime)

        val expenseZh = listOf("早餐", "午餐", "晚餐", "甜點", "飲料", "酒類", "交通", "數位", "購物", "娛樂", "日常", "房租", "醫療", "社交", "禮物", "其他")
        val incomeZh = listOf("薪水", "獎金", "回饋", "交易", "股息", "租金", "投資", "其他")
        val expenseEn = listOf("Bfast", "Lunch", "Dinner", "Dessert", "Drink", "Alcohol", "Trnsprt", "Digital", "Shop", "Entmt", "Daily", "Rent", "Medical", "Social", "Gift", "Other")
        val incomeEn = listOf("Salary", "Bonus", "Rebate", "Trade", "Dvdend", "Rents", "Invst", "others")

        val categoryList = if (isChinese) (expenseZh + incomeZh).joinToString("\u3001") else (expenseEn + incomeEn).joinToString(", ")

        val prompt = if (isChinese) {
            """
           你是中文記帳助理財寶。請先判斷使用者的輸入是否與「記帳、金錢、花費、收入、支出」相關。
            - 如果是，請依下方格式輸出 JSON，欄位如下：
            - amount（金額）
            - type（0=支出, 1=收入）
            - category（只能為以下其中一個：$categoryList）
            - date（格式 yyyy-MM-dd，除非句中明確提到昨天或特定日期，否則一律填今天 $today）
            - 如果不是，請回覆：「我只能幫你記帳唷～請告訴我有多少支出或收入要記下來 💬」

            僅回傳純 JSON，不要加說明文字。
            「$input」
            """
        } else {
            """
             You are Caibao, a smart assistant. First, determine if the input is related to money, spending, income or expense.
            - If yes, return a JSON with:
            - amount (number)
            - type (0 = expense, 1 = income)
            - category (must be one of: $categoryList)
            - date (format yyyy-MM-dd, use today $today unless a specific date or 'yesterday' is mentioned)
            - If not, reply: "I can only help you with budgeting. Please tell me your income or expense 💬"

            Only return valid JSON. No extra explanation.
            Input: "$input"
            """
        }.trimIndent()

        val messages = listOf(
            Message("system", if (isChinese) "你是記帳助理財寶，擅長分類與格式化輸出。" else "You are Caibao, a smart assistant specialized in financial record parsing."),
            Message("user", prompt)
        )

        val retrofit = Retrofit.Builder()
            .baseUrl("https://41163-mb7swfeh-swedencentral.openai.azure.com")
            .addConverterFactory(GsonConverterFactory.create())
            .client(OkHttpClient())
            .build()

        val api = retrofit.create(GptApiService::class.java)
        val request = GptRequest(messages)

        api.sendMessage(request).enqueue(object : Callback<GptResponse> {
            override fun onResponse(call: Call<GptResponse>, response: Response<GptResponse>) {
                val reply = response.body()?.choices?.get(0)?.message?.content ?: "No response"
                try {
                    val jsonString = extractPureJson(reply)
                    val json = Gson().fromJson(jsonString, Map::class.java)
                    val amount = json["amount"].toString()
                    val type = (json["type"] as Double).toInt() == 1
                    val rawCategory = json["category"].toString().trim()
                    val date = json["date"].toString()

                    val fixedCategory = when {
                        type && isChinese && rawCategory !in incomeZh -> "其他"
                        type && !isChinese && rawCategory !in incomeEn -> "others"
                        !type && isChinese && rawCategory !in expenseZh -> "其他"
                        !type && !isChinese && rawCategory !in expenseEn -> "others"
                        else -> rawCategory
                    }

                    val typeText = if (type) "\uD83D\uDCB0 收入" else "\uD83D\uDCB8 支出"
                    val responseText = """
                        ${getString(R.string.conversion_success)}

                        ${getString(R.string.label_amount)} $amount 元
                        ${getString(R.string.label_category)} $fixedCategory
                        ${getString(R.string.label_date)} $date
                    
                        ${getString(R.string.redirect_hint)}
                    """.trimIndent()

                    runOnUiThread { tvResponse.text = responseText }

                    val intent = if (!type) Intent(this@ChatActivity, expense::class.java)
                    else Intent(this@ChatActivity, income::class.java)

                    intent.putExtra("amount", amount)
                    intent.putExtra("category", fixedCategory)
                    intent.putExtra("date", date)
                    intent.putExtra("from_gpt", true)
                    startActivity(intent)

                } catch (e: Exception) {
                    Log.e("GPT Parse Error", e.toString())
                    runOnUiThread { tvResponse.text = "⚠️ 財寶無法理解這筆資料，請再說一次！" }
                }
            }

            override fun onFailure(call: Call<GptResponse>, t: Throwable) {
                tvResponse.text = "錯誤：${t.message}"
            }
        })
    }

    private fun extractPureJson(text: String): String {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        return if (start >= 0 && end > start) {
            text.substring(start, end + 1)
        } else {
            "{}"
        }
    }
}
