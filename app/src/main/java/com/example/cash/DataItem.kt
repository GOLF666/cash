package com.example.cash

data class DataItem(
    val id: Int,
    val title: String,  // 標題/描述
    val amount: Double,    // 金額
    val type: Boolean,   // 類型（true: 收入, false: 支出）
    val category: String,
    val date: String,     // 日期（格式: YYYY-MM-DD）
    val userEmail: String
)
