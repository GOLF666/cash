package com.example.cash

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.android.flexbox.FlexboxLayout
import java.text.SimpleDateFormat
import java.util.*

class ExpenseChartActivity : AppCompatActivity() {
    private lateinit var pieChart: PieChart
    private lateinit var tvDateRange: TextView
    private lateinit var tvTotalExpense: TextView
    private lateinit var rvExpenseDetail: RecyclerView
    private lateinit var dbHelper: databasehelper
    private lateinit var llCategoryRatio: FlexboxLayout

    private var currentRange: String = "week"
    private var customStart: Calendar? = null
    private var customEnd: Calendar? = null

    private lateinit var userEmail: String // 新增

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())

    // 分類對應顏色
    private val categoryColorMap by lazy {
        mapOf(
            "早餐" to Color.parseColor("#FF5C93"),
            "午餐" to Color.parseColor("#FF9CB3"),
            "晚餐" to Color.parseColor("#F793B6"),
            "甜點" to Color.parseColor("#FF8CB0"),
            "飲料" to Color.parseColor("#E472A9"),
            "酒類" to Color.parseColor("#FF6F91"),
            "交通" to Color.parseColor("#FBB1BD"),
            "數位" to Color.parseColor("#FFB6C1"),
            "購物" to Color.parseColor("#FE8FB6"),
            "娛樂" to Color.parseColor("#FF7A90"),
            "日常" to Color.parseColor("#FFB0B7"),
            "房租" to Color.parseColor("#F57CBA"),
            "醫療" to Color.parseColor("#FF94C2"),
            "社交" to Color.parseColor("#F378A7"),
            "禮物" to Color.parseColor("#FA709A"),
            "其他" to Color.parseColor("#FFBBCC"),
            "Bfast" to Color.parseColor("#FF5C93"),
            "Lunch" to Color.parseColor("#FF9CB3"),
            "Dinner" to Color.parseColor("#F793B6"),
            "Dessert" to Color.parseColor("#FF8CB0"),
            "Drink" to Color.parseColor("#E472A9"),
            "Alcohol" to Color.parseColor("#FF6F91"),
            "Trnsprt" to Color.parseColor("#FBB1BD"),
            "Digital" to Color.parseColor("#FFB6C1"),
            "Shop" to Color.parseColor("#FE8FB6"),
            "Entmt" to Color.parseColor("#FF7A90"),
            "Daily" to Color.parseColor("#FFB0B7"),
            "Rent" to Color.parseColor("#F57CBA"),
            "Medical" to Color.parseColor("#FF94C2"),
            "Social" to Color.parseColor("#F378A7"),
            "Gift" to Color.parseColor("#FA709A"),
            "Other" to Color.parseColor("#FFBBCC")
        )
    }

    // 分類對應icon（使用統一的英文名稱作為key）
    private val categoryIconMap by lazy {
        mapOf(
            "早餐" to R.drawable.breakfast,
            "午餐" to R.drawable.lunch,
            "晚餐" to R.drawable.dinner,
            "甜點" to R.drawable.dessert,
            "飲料" to R.drawable.drink,
            "酒類" to R.drawable.alcohol,
            "交通" to R.drawable.traffic,
            "數位" to R.drawable.digital,
            "購物" to R.drawable.shopping,
            "娛樂" to R.drawable.entertainment,
            "日常" to R.drawable.daily,
            "房租" to R.drawable.rent,
            "醫療" to R.drawable.medical,
            "社交" to R.drawable.social,
            "禮物" to R.drawable.gift,
            "其他" to R.drawable.other,
            "Bfast" to R.drawable.breakfast,
            "Lunch" to R.drawable.lunch,
            "Dinner" to R.drawable.dinner,
            "Dessert" to R.drawable.dessert,
            "Drink" to R.drawable.drink,
            "Alcohol" to R.drawable.alcohol,
            "Trnsprt" to R.drawable.traffic,
            "Digital" to R.drawable.digital,
            "Shop" to R.drawable.shopping,
            "Entmt" to R.drawable.entertainment,
            "Daily" to R.drawable.daily,
            "Rent" to R.drawable.rent,
            "Medical" to R.drawable.medical,
            "Social" to R.drawable.social,
            "Gift" to R.drawable.gift,
            "Other" to R.drawable.other,
        )
    }

    // 顯示分類名稱
    private fun getCategoryDisplayName(category: String): String {
        // 這裡不需要使用 categoryNameResMap，直接返回英文的分類名稱
        return category.capitalize(Locale.getDefault()) // 簡單的方式，根據分類名稱自動生成顯示名稱
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expense_chart)

        llCategoryRatio = findViewById(R.id.ll_category_ratio)

        dbHelper = databasehelper(this)
        pieChart = findViewById(R.id.pieChart)
        tvDateRange = findViewById(R.id.tv_date_range)
        rvExpenseDetail = findViewById(R.id.rv_expense_detail)
        rvExpenseDetail.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)

        // 取得 userEmail（從 SharedPreferences，登入時存的 email）
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        userEmail = prefs.getString("user_email", "") ?: ""

        findViewById<Button>(R.id.btn_week).setOnClickListener { updateRange("week") }
        findViewById<Button>(R.id.btn_month).setOnClickListener { updateRange("month") }
        findViewById<Button>(R.id.btn_year).setOnClickListener { updateRange("year") }
        findViewById<Button>(R.id.btn_custom).setOnClickListener { showCustomDateDialog() }

        // 設置返回支出的按鈕點擊事件
        findViewById<Button>(R.id.btn_income).setOnClickListener {
            val intent = Intent(this, IncomeChartActivity::class.java)
            startActivity(intent)
            finish()
        }

        beautifyPieChart()
        updateRange("week")
    }

    private fun beautifyPieChart() {
        pieChart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setEntryLabelColor(Color.DKGRAY)
            setEntryLabelTextSize(16f)
            animateY(800)
            setHoleColor(Color.WHITE)
            holeRadius = 70f
            transparentCircleRadius = 74f
            setUsePercentValues(false)
        }
    }

    private fun updateRange(type: String) {
        currentRange = type
        val today = Calendar.getInstance()
        val start: Calendar
        val end: Calendar

        when (type) {
            "week" -> {
                start = today.clone() as Calendar
                start.set(Calendar.DAY_OF_WEEK, start.firstDayOfWeek)
                end = start.clone() as Calendar
                end.add(Calendar.DAY_OF_WEEK, 6)
            }
            "month" -> {
                start = today.clone() as Calendar
                start.set(Calendar.DAY_OF_MONTH, 1)
                end = start.clone() as Calendar
                end.set(Calendar.DAY_OF_MONTH, start.getActualMaximum(Calendar.DAY_OF_MONTH))
            }
            "year" -> {
                start = today.clone() as Calendar
                start.set(Calendar.MONTH, 0)
                start.set(Calendar.DAY_OF_MONTH, 1)
                end = today.clone() as Calendar
                end.set(Calendar.MONTH, 11)
                end.set(Calendar.DAY_OF_MONTH, 31)
            }
            "custom" -> {
                start = customStart ?: today
                end = customEnd ?: today
            }
            else -> {
                start = today
                end = today
            }
        }

        start.set(Calendar.HOUR_OF_DAY, 0)
        start.set(Calendar.MINUTE, 0)
        start.set(Calendar.SECOND, 0)
        start.set(Calendar.MILLISECOND, 0)

        end.set(Calendar.HOUR_OF_DAY, 23)
        end.set(Calendar.MINUTE, 59)
        end.set(Calendar.SECOND, 59)
        end.set(Calendar.MILLISECOND, 999)

        tvDateRange.text = getString(
            R.string.date_range_format,
            displayFormat.format(start.time),
            displayFormat.format(end.time)
        )

        // 撈DB支出
        val allExpenseData = dbHelper.getAllData(userEmail)
            .filter { !it.type }
            .mapNotNull { dataItem ->
                try {
                    val recDate = dateFormat.parse(dataItem.date)
                    ExpenseRecord(
                        date = recDate ?: Date(),
                        category = dataItem.category,
                        amount = dataItem.amount.toInt()
                    )
                } catch (e: Exception) {
                    null
                }
            }

        val expenseInRange = allExpenseData.filter { exp ->
            !exp.date.before(start.time) && !exp.date.after(end.time)
        }

        updatePieChart(expenseInRange)
        updateExpenseDetail(expenseInRange)
        updateCategoryRatio(expenseInRange)
    }

    private fun showCustomDateDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_custom_date, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        val btnStartDate = dialogView.findViewById<Button>(R.id.btn_start_date)
        val btnEndDate = dialogView.findViewById<Button>(R.id.btn_end_date)
        val btnConfirm = dialogView.findViewById<Button>(R.id.btn_confirm)
        var start: Calendar? = null
        var end: Calendar? = null

        fun updateBtnText() {
            btnStartDate.text = start?.let { displayFormat.format(it.time) } ?: getString(R.string.start_date)
            btnEndDate.text = end?.let { displayFormat.format(it.time) } ?: getString(R.string.end_date)
        }
        updateBtnText()

        btnStartDate.setOnClickListener {
            val now = start ?: Calendar.getInstance()
            DatePickerDialog(this, { _, y, m, d ->
                start = Calendar.getInstance().apply { set(y, m, d) }
                updateBtnText()
            }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show()
        }
        btnEndDate.setOnClickListener {
            val now = end ?: Calendar.getInstance()
            DatePickerDialog(this, { _, y, m, d ->
                end = Calendar.getInstance().apply { set(y, m, d) }
                updateBtnText()
            }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show()
        }

        btnConfirm.setOnClickListener {
            if (start != null && end != null) {
                if (start?.time == end?.time) {
                    customStart = start
                    customEnd = start
                    updateRange("custom")
                } else {
                    customStart = start
                    customEnd = end
                    updateRange("custom")
                }
                dialog.dismiss()
            } else {
                Toast.makeText(this, getString(R.string.toast_choose_date), Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun updatePieChart(expenses: List<ExpenseRecord>) {
        val grouped = expenses.groupBy { it.category }
            .mapValues { it.value.sumOf { rec -> rec.amount } }
        val totalExpense = expenses.sumOf { it.amount }.toFloat()
        val entries = grouped.map { PieEntry(it.value.toFloat(), getCategoryDisplayName(it.key)) }
        val pieColors = entries.map { entry ->
            categoryColorMap[entry.label] ?: Color.LTGRAY
        }

        val dataSet = PieDataSet(entries, "").apply {
            colors = pieColors
            valueTextSize = 16f
            valueTextColor = Color.BLACK
            sliceSpace = 6f
            // 使用百分比
            valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return if (totalExpense == 0f) "0%" else String.format("%.1f%%", (value / totalExpense * 100f))
                }
            }
        }

        pieChart.data = PieData(dataSet)
        pieChart.centerText = getString(R.string.total_expense) + "\n$${"%,d".format(totalExpense.toInt())}"
        pieChart.setCenterTextSize(22f)
        pieChart.setCenterTextColor(Color.parseColor("#C2185B"))
        pieChart.setUsePercentValues(false) // 注意這裡要 false，自行算百分比
        pieChart.invalidate()
    }

    private fun updateCategoryRatio(expenses: List<ExpenseRecord>) {
        llCategoryRatio.removeAllViews()
        val grouped = expenses.groupBy { it.category }
            .mapValues { it.value.sumOf { rec -> rec.amount } }
        grouped.forEach { (category, _) ->
            val color = categoryColorMap[category] ?: Color.LTGRAY
            val dot = ImageView(this)
            dot.setImageResource(R.drawable.bg_dot_circle)
            dot.setColorFilter(color)
            val params = LinearLayout.LayoutParams(24, 24)
            params.setMargins(12, 0, 8, 0)
            dot.layoutParams = params

            val tv = TextView(this)
            tv.text = getCategoryDisplayName(category) // 這裡依然保持使用英文分類名稱
            tv.textSize = 16f
            tv.setTextColor(Color.DKGRAY)
            tv.setPadding(0, 0, 12, 0)

            val wrapper = LinearLayout(this)
            wrapper.orientation = LinearLayout.HORIZONTAL
            wrapper.gravity = Gravity.CENTER_VERTICAL
            wrapper.addView(dot)
            wrapper.addView(tv)

            llCategoryRatio.addView(wrapper)
        }
    }

    private fun updateExpenseDetail(expenses: List<ExpenseRecord>) {
        rvExpenseDetail.adapter = ExpenseDetailAdapter(expenses, categoryIconMap, this, ::getCategoryDisplayName)
    }

    // ========= 16 類別資料&Adapter =========

    data class ExpenseRecord(
        val date: Date,
        val category: String,
        val amount: Int
    )

    class ExpenseDetailAdapter(
        private val list: List<ExpenseRecord>,
        private val categoryIconMap: Map<String, Int>,
        private val context: Context,
        private val getCategoryDisplayName: (String) -> String
    ) : RecyclerView.Adapter<ExpenseDetailAdapter.ExpenseViewHolder>() {

        class ExpenseViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val iconView: ImageView = view.findViewById(R.id.iv_category_icon)
            val tvCategory: TextView = view.findViewById(R.id.tv_category_name)
            val tvAmount: TextView = view.findViewById(R.id.tv_amount)
            val tvDate: TextView = view.findViewById(R.id.tv_date)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
            val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_expense_detail, parent, false)
            return ExpenseViewHolder(itemView)
        }

        override fun getItemCount(): Int = list.size

        override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
            val item = list[position]
            holder.tvCategory.text = getCategoryDisplayName(item.category)
            holder.tvAmount.text = "$${"%,d".format(item.amount)}"
            holder.tvDate.text = SimpleDateFormat("MM/dd", Locale.getDefault()).format(item.date)

            val iconRes = categoryIconMap[item.category] ?: R.drawable.ic_default
            holder.iconView.setImageResource(iconRes)
        }
    }
}
