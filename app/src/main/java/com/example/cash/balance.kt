package com.example.cash

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ItemTouchHelper
import android.widget.Toast
import java.util.Calendar

class balance : AppCompatActivity() {
    private lateinit var databasehelper: databasehelper
    private lateinit var recyclerViewIncome: RecyclerView
    private lateinit var recyclerViewExpense: RecyclerView
    private lateinit var incomeAdapter: BalanceAdapter
    private lateinit var expenseAdapter: BalanceAdapter
    private var selectedDate: String? = null

    // 加這一行：全域存 userEmail（記得命名風格就照你專案）
    private lateinit var userEmail: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_balance)

        recyclerViewExpense = findViewById(R.id.recyclerView_expense)
        recyclerViewIncome = findViewById(R.id.recyclerView_income)
        databasehelper = databasehelper(this)

        // 加這一段：取得目前登入的 user_email
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        userEmail = prefs.getString("user_email", "") ?: ""

        recyclerViewIncome.layoutManager = LinearLayoutManager(this)
        recyclerViewExpense.layoutManager = LinearLayoutManager(this)

        displayData()

        // 改：點右上角 icon 彈 Dialog
        val imageButton_menu = findViewById<ImageButton>(R.id.imageButton_menu)
        imageButton_menu.setOnClickListener {
            showFilterDialog()
        }
    }

    // 新增：Dialog（有日曆、恢復、圖表）
    private fun showFilterDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_balance_filter, null)
        val dialog = android.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        val btnCalendar = dialogView.findViewById<android.widget.Button>(R.id.btn_date_picker)
        val btnRestore = dialogView.findViewById<android.widget.Button>(R.id.btn_reset)
        val btnChart = dialogView.findViewById<android.widget.Button>(R.id.btn_chart)

        btnCalendar.setOnClickListener {
            val today = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    selectedDate = String.format("%04d-%02d-%02d", year, month + 1, day)
                    displayDataForDate(selectedDate!!)
                    dialog.dismiss()
                },
                today.get(Calendar.YEAR),
                today.get(Calendar.MONTH),
                today.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        btnRestore.setOnClickListener {
            selectedDate = null
            displayData()
            dialog.dismiss()
        }

        btnChart.setOnClickListener {
            val intent = Intent(this, ExpenseChartActivity::class.java)
            intent.putExtra("TYPE", "expense") // 預設跳支出圖
            startActivity(intent)
            dialog.dismiss()
        }

        dialog.show()
    }

    // 原本功能：只顯示選擇日期資料
    private fun displayDataForDate(date: String) {
        val allData = databasehelper.getAllData(userEmail)
        val filteredData = allData.filter { it.date == date }

        val incomeData = filteredData.filter { it.type }
        val expenseData = filteredData.filter { !it.type }

        val incomeAdapter = BalanceAdapter(incomeData.toMutableList())
        val expenseAdapter = BalanceAdapter(expenseData.toMutableList())
        recyclerViewIncome.adapter = incomeAdapter
        recyclerViewExpense.adapter = expenseAdapter

        enableSwipeToDelete(recyclerViewIncome, incomeAdapter)
        enableSwipeToDelete(recyclerViewExpense, expenseAdapter)

        if (filteredData.isEmpty()) {
            Toast.makeText(this, "這天沒有任何紀錄", Toast.LENGTH_SHORT).show()
        }
    }

    // 原本功能：顯示全部資料
    private fun displayData() {
        val data = databasehelper.getAllData(userEmail)
        val incomeData = data.filter { it.type }
        val expenseData = data.filter { !it.type }

        val incomeAdapter = BalanceAdapter(incomeData.toMutableList())
        val expenseAdapter = BalanceAdapter(expenseData.toMutableList())
        recyclerViewIncome.adapter = incomeAdapter
        recyclerViewExpense.adapter = expenseAdapter

        enableSwipeToDelete(recyclerViewIncome, incomeAdapter)
        enableSwipeToDelete(recyclerViewExpense, expenseAdapter)

        if (data.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_data), Toast.LENGTH_SHORT).show()
        }
    }

    // 原本功能：滑動刪除
    private fun enableSwipeToDelete(recyclerView: RecyclerView, adapter: BalanceAdapter) {
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val itemToDelete = adapter.getItem(position)
                    databasehelper.deleteRecord(itemToDelete.id,userEmail)
                    adapter.removeItem(position)
                    Toast.makeText(this@balance, getString(R.string.record_deleted), Toast.LENGTH_SHORT).show()
                }
            }
        }

        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }
}
