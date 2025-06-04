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

class IncomeChartActivity : AppCompatActivity() {
    private lateinit var pieChart: PieChart
    private lateinit var tvDateRange: TextView
    private lateinit var tvTotalIncome: TextView
    private lateinit var rvIncomeDetail: RecyclerView
    private lateinit var dbHelper: databasehelper
    private lateinit var llCategoryRatio: FlexboxLayout

    private var currentRange: String = "week"
    private var customStart: Calendar? = null
    private var customEnd: Calendar? = null

    private lateinit var userEmail: String // 新增

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())

    // 收入分類對應顏色（青色範圍）
    private val categoryColorMap by lazy {
        mapOf(
            "薪水" to Color.parseColor("#A5D6A7"),  // 淺綠色
            "獎金" to Color.parseColor("#81C784"),  // 淺綠色
            "回饋" to Color.parseColor("#66BB6A"),  // 中等淺綠色
            "交易" to Color.parseColor("#4CAF50"),  // 亮綠色
            "股息" to Color.parseColor("#43A047"),  // 中等綠色
            "租金" to Color.parseColor("#8BC34A"),  // 淺綠色
            "投資" to Color.parseColor("#64B5F6"),  // 淺藍綠色
            "其他" to Color.parseColor("#C8E6C9"),  // 輕柔淺綠色
            "Salary" to Color.parseColor("#A5D6A7"),
            "Bonus" to Color.parseColor("#81C784"),
            "Rebate" to Color.parseColor("#66BB6A"),
            "Trade" to Color.parseColor("#4CAF50"),
            "Dvdend" to Color.parseColor("#43A047"),
            "Rents" to Color.parseColor("#8BC34A"),
            "Invst" to Color.parseColor("#64B5F6"),
            "others" to Color.parseColor("#C8E6C9")
        )
    }

    // 收入分類icon
    private val categoryIconMap by lazy {
        mapOf(
            "薪水" to R.drawable.salary,
            "獎金" to R.drawable.bonus,
            "回饋" to R.drawable.rebate,
            "交易" to R.drawable.trade,
            "股息" to R.drawable.dividend,
            "租金" to R.drawable.rents,
            "投資" to R.drawable.investment,
            "其他" to R.drawable.others,
            "Salary" to R.drawable.salary,
            "Bonus" to R.drawable.bonus,
            "Rebate" to R.drawable.rebate,
            "Trade" to R.drawable.trade,
            "Dvdend" to R.drawable.dividend,
            "Rents" to R.drawable.rents,
            "Invst" to R.drawable.investment,
            "others" to R.drawable.others
        )
    }

    // 顯示分類名稱
    private fun getCategoryDisplayName(category: String): String {
        return category.capitalize(Locale.getDefault()) // 根據分類名稱返回顯示名稱
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_income_chart)

        llCategoryRatio = findViewById(R.id.ll_category_ratio)

        dbHelper = databasehelper(this)
        pieChart = findViewById(R.id.pieChart)
        tvDateRange = findViewById(R.id.tv_date_range)
        rvIncomeDetail = findViewById(R.id.rv_income_detail)
        rvIncomeDetail.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)

        // 取得 userEmail
        val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        userEmail = prefs.getString("user_email", "") ?: ""

        findViewById<Button>(R.id.btn_week).setOnClickListener { updateRange("week") }
        findViewById<Button>(R.id.btn_month).setOnClickListener { updateRange("month") }
        findViewById<Button>(R.id.btn_year).setOnClickListener { updateRange("year") }
        findViewById<Button>(R.id.btn_custom).setOnClickListener { showCustomDateDialog() }

        findViewById<Button>(R.id.btn_expense).setOnClickListener {
            // 創建 Intent 來啟動支出頁面
            val intent = Intent(this, ExpenseChartActivity::class.java)
            startActivity(intent) // 啟動支出頁面
            finish() // 關閉當前頁面
        }

        beautifyPieChart()
        updateRange("week")
    }

    private fun beautifyPieChart() {
        pieChart.apply {
            description.isEnabled = false
            legend.isEnabled = false // 自訂 legend
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

        // 撈DB收入資料
        val allIncomeData = dbHelper.getAllData(userEmail)
            .filter { it.type } // 篩選出收入資料
            .mapNotNull { dataItem ->
                try {
                    val recDate = dateFormat.parse(dataItem.date)
                    IncomeRecord(
                        date = recDate ?: Date(),
                        category = dataItem.category,
                        amount = dataItem.amount.toInt()
                    )
                } catch (e: Exception) {
                    null
                }
            }

        val incomeInRange = allIncomeData.filter { income ->
            !income.date.before(start.time) && !income.date.after(end.time)
        }

        updatePieChart(incomeInRange)
        updateIncomeDetail(incomeInRange)
        updateCategoryRatio(incomeInRange)
    }

    // Dialog 兩個按鈕挑日期
    private fun showCustomDateDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialogs_custom_date, null)
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

    private fun updatePieChart(incomes: List<IncomeRecord>) {
        val grouped = incomes.groupBy { it.category }
            .mapValues { it.value.sumOf { rec -> rec.amount } }
        val totalIncomes = incomes.sumOf { it.amount }.toFloat()
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
                    return if (totalIncomes== 0f) "0%" else String.format("%.1f%%", (value / totalIncomes * 100f))
                }
            }
        }

        pieChart.data = PieData(dataSet)
        pieChart.centerText = getString(R.string.total_income) + "\n$${"%,d".format(totalIncomes.toInt())}"
        pieChart.setCenterTextSize(22f)
        pieChart.setCenterTextColor(Color.parseColor("#388E3C"))
        pieChart.setUsePercentValues(false) // 注意這裡要 false，自行算百分比
        pieChart.invalidate()
    }

    private fun updateCategoryRatio(incomes: List<IncomeRecord>) {
        llCategoryRatio.removeAllViews()
        val grouped = incomes.groupBy { it.category }
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
            tv.text = category.capitalize(Locale.getDefault()) // 顯示英文分類名稱
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

    private fun updateIncomeDetail(incomes: List<IncomeRecord>) {
        rvIncomeDetail.adapter = IncomeDetailAdapter(incomes, categoryIconMap, this, ::getCategoryDisplayName)
    }

    // ========= 收入資料&Adapter =========

    data class IncomeRecord(
        val date: Date,
        val category: String,
        val amount: Int
    )

    class IncomeDetailAdapter(
        private val list: List<IncomeRecord>,
        private val categoryIconMap: Map<String, Int>,
        private val context: Context,
        private val getCategoryDisplayName: (String) -> String
    ) : RecyclerView.Adapter<IncomeDetailAdapter.IncomeViewHolder>() {

        class IncomeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val iconView: ImageView = view.findViewById(R.id.iv_category_icon)
            val tvCategory: TextView = view.findViewById(R.id.tv_category_name)
            val tvAmount: TextView = view.findViewById(R.id.tv_amount)
            val tvDate: TextView = view.findViewById(R.id.tv_date)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IncomeViewHolder {
            val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_income_detail, parent, false)
            return IncomeViewHolder(itemView)
        }

        override fun getItemCount(): Int = list.size

        override fun onBindViewHolder(holder: IncomeViewHolder, position: Int) {
            val item = list[position]
            holder.tvCategory.text = getCategoryDisplayName(item.category)
            holder.tvAmount.text = "$${"%,d".format(item.amount)}"
            holder.tvDate.text = SimpleDateFormat("MM/dd", Locale.getDefault()).format(item.date)

            val iconRes = categoryIconMap[item.category] ?: R.drawable.ic_default
            holder.iconView.setImageResource(iconRes)
        }
    }
}
