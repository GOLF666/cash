package com.example.cash

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.DecimalFormat
import android.graphics.Color
import android.widget.ImageView
import androidx.core.content.ContextCompat

class BalanceAdapter(private val dataList: MutableList<DataItem>) : RecyclerView.Adapter<BalanceAdapter.ViewHolder>() {

    val incomeList: MutableList<DataItem> = dataList.filter { it.type }.toMutableList()
    val expenseList: MutableList<DataItem> = dataList.filter { !it.type }.toMutableList()

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.titleTextView)
        val amountTextView: TextView = itemView.findViewById(R.id.amountTextView)
        val categoryTextView: TextView = itemView.findViewById(R.id.categoriesTextView)
        val dateTextView: TextView = itemView.findViewById(R.id.dateTextView)
        val iconImageView: ImageView = itemView.findViewById(R.id.image_view_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_data, parent, false)
        return ViewHolder(view)
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)

        holder.titleTextView.text =
            holder.itemView.context.getString(R.string.title_label, item.title)
        val decimalFormat = DecimalFormat("#,###.00")
        val formattedAmount = decimalFormat.format(item.amount)

        holder.amountTextView.text =
            holder.itemView.context.getString(R.string.amount_label, formattedAmount)

        // 設置類別（category）
        holder.categoryTextView.text =
            holder.itemView.context.getString(R.string.category_label, item.category)

        // 設置日期（date）
        holder.dateTextView.text = holder.itemView.context.getString(R.string.date_label, item.date)

        // 🔥 這段是重點：依分類給 icon
        val iconRes = when (item.category) {
            "早餐" -> R.drawable.breakfast
            "午餐" -> R.drawable.lunch
            "晚餐" -> R.drawable.dinner
            "甜點" -> R.drawable.dessert
            "飲料" -> R.drawable.drink
            "酒類" -> R.drawable.alcohol
            "交通" -> R.drawable.traffic
            "數位" -> R.drawable.digital
            "購物" -> R.drawable.shopping
            "娛樂" -> R.drawable.entertainment
            "日常" -> R.drawable.daily
            "房租" -> R.drawable.rent
            "醫療" -> R.drawable.medical
            "社交" -> R.drawable.social
            "禮物" -> R.drawable.gift
            "其他" -> R.drawable.other
            "薪水" -> R.drawable.salary
            "獎金" -> R.drawable.bonus
            "回饋" -> R.drawable.rebate
            "交易" -> R.drawable.trade
            "股息" -> R.drawable.dividend
            "租金" -> R.drawable.rents
            "投資" -> R.drawable.investment
            "其他" -> R.drawable.others
            "Bfast" -> R.drawable.breakfast
            "Lunch" -> R.drawable.lunch
            "Dinner" -> R.drawable.dinner
            "Dessert" -> R.drawable.dessert
            "Drink" -> R.drawable.drink
            "Alcohol" -> R.drawable.alcohol
            "Trnsprt" -> R.drawable.traffic
            "Digital" -> R.drawable.digital
            "Shop" -> R.drawable.shopping
            "Entmt" -> R.drawable.entertainment
            "Daily" -> R.drawable.daily
            "Rent" -> R.drawable.rent
            "Medical" -> R.drawable.medical
            "Social" -> R.drawable.social
            "Gift" -> R.drawable.gift
            "Other" -> R.drawable.other
            "Salary" -> R.drawable.salary
            "Bonus" -> R.drawable.bonus
            "Rebate" -> R.drawable.rebate
            "Trade" -> R.drawable.trade
            "Dvdend" -> R.drawable.dividend
            "Rents" -> R.drawable.rents
            "Invst" -> R.drawable.investment
            "others" -> R.drawable.others
            else -> R.drawable.ic_default // 預設 icon
        }
        holder.iconImageView.setImageResource(iconRes)

        // 💡（選用）可根據收入支出動態調整金額顏色
        holder.amountTextView.setTextColor(
            if (item.type) Color.parseColor("#388E3C") // 收入綠色
            else Color.parseColor("#E53935") // 支出紅色
        )
    }

    override fun getItemCount(): Int = incomeList.size + expenseList.size

    fun getItem(position: Int): DataItem {
        return if (position < incomeList.size) {
            incomeList[position] // 返回收入項目
        } else {
            expenseList[position - incomeList.size] // 返回支出項目
        }
    }
    fun getTotalIncome(): Double {
        return dataList.filter { it.type }.sumOf { it.amount }
    }
    fun getTotalExpense(): Double {
        return dataList.filter { !it.type }.sumOf { it.amount }
    }

    fun removeItem(position: Int) {
        if (position < incomeList.size) {
            incomeList.removeAt(position) // 刪除收入項目
        } else {
            expenseList.removeAt(position - incomeList.size) // 刪除支出項目
        }
        notifyItemRemoved(position) // 通知 RecyclerView 更新
    }
}