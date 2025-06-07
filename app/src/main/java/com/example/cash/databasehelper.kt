package com.example.cash

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class databasehelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "moneyfy_database.db"
        private const val DATABASE_VERSION = 3
        const val COLUMN_ID = "id"
        const val COLUMN_TITLE = "title"
        const val COLUMN_AMOUNT = "amount"
        const val COLUMN_TYPE = "type"
        const val COLUMN_CATEGORY = "category" // e.g., 'food', 'travel'
        const val COLUMN_DATE = "date"
        const val COLUMN_USER_EMAIL = "user_email"
    }

    override fun onCreate(db: SQLiteDatabase) {
        Log.d("Database", "Creating accountingTABLE")
        val sql = """
        CREATE TABLE IF NOT EXISTS accountingTABLE (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            title TEXT NOT NULL,
            amount TEXT NOT NULL,
            type INTEGER NOT NULL,
            category TEXT NOT NULL,
            date TEXT NOT NULL,
            user_email TEXT NOT NULL
        )
    """.trimIndent()
        db.execSQL(sql)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS accountingTABLE")
        onCreate(db) // 這會直接用最新 onCreate 建好正確欄位
    }

    // 新增資料（建議 amount 可以用 Double 型別比較好，如果你都用 String 也 ok）
    fun insert(title: String, amount: String, type: Boolean, category: String, date: String, userEmail: String): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TITLE, title)
            put(COLUMN_AMOUNT, amount)
            put(COLUMN_TYPE, if (type) 1 else 0)
            put(COLUMN_CATEGORY, category)
            put(COLUMN_DATE, date)
            put(COLUMN_USER_EMAIL, userEmail)
        }
        val result = db.insert("accountingTABLE", null, values)
        db.close()
        return result
    }

    // **重點：加 userEmail 參數**
    fun getAllData(userEmail: String): List<DataItem> {
        val dataList = mutableListOf<DataItem>()
        val db = this.readableDatabase

        try {
            val cursor = db.rawQuery(
                "SELECT * FROM accountingTABLE WHERE user_email = ?",
                arrayOf(userEmail)
            )
            if (cursor.moveToFirst()) {
                do {
                    val idIndex = cursor.getColumnIndex(COLUMN_ID)
                    val titleIndex = cursor.getColumnIndex(COLUMN_TITLE)
                    val amountIndex = cursor.getColumnIndex(COLUMN_AMOUNT)
                    val typeIndex = cursor.getColumnIndex(COLUMN_TYPE)
                    val catoIndex = cursor.getColumnIndex(COLUMN_CATEGORY)
                    val dateIndex = cursor.getColumnIndex(COLUMN_DATE)

                    val id = cursor.getInt(idIndex)
                    val title = cursor.getString(titleIndex)
                    val amountString = cursor.getString(amountIndex)
                    val amount = amountString.toDoubleOrNull() ?: 0.0
                    val type = cursor.getInt(typeIndex) == 1
                    val category = cursor.getString(catoIndex)
                    val date = cursor.getString(dateIndex)

                    dataList.add(DataItem(id, title, amount, type, category, date, userEmail))
                } while (cursor.moveToNext())
            }
            cursor.close()
        } catch (e: Exception) {
            Log.e("Database Error", "Error fetching data", e)
        } finally {
            db.close()
        }
        return dataList
    }

    // 查詢某天的支出紀錄（type = false，即 0）
    fun getExpenseByDate(userEmail: String, date: String): List<DataItem> {
        return getFilteredByDate(userEmail, date, isExpense = true)
    }

    // 查詢某天的收入紀錄（type = true，即 1）
    fun getIncomeByDate(userEmail: String, date: String): List<DataItem> {
        return getFilteredByDate(userEmail, date, isExpense = false)
    }

    // 查詢某天的全部紀錄（收入 + 支出）
    fun getAllRecordsByDate(userEmail: String, date: String): List<DataItem> {
        val dataList = mutableListOf<DataItem>()
        val db = this.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM accountingTABLE WHERE date = ? AND user_email = ?",
            arrayOf(date, userEmail)
        )

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID))
                val title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE))
                val amountString = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_AMOUNT))
                val type = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TYPE)) == 1
                val category = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY))
                val date = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE))

                val amount = amountString.toDoubleOrNull() ?: 0.0
                dataList.add(DataItem(id, title, amount, type, category, date, userEmail))
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return dataList
    }

    // 🔒內部共用邏輯（根據收入/支出過濾）
    private fun getFilteredByDate(userEmail: String, date: String, isExpense: Boolean): List<DataItem> {
        val typeValue = if (isExpense) 0 else 1
        val dataList = mutableListOf<DataItem>()
        val db = this.readableDatabase

        val cursor = db.rawQuery(
            "SELECT * FROM accountingTABLE WHERE date = ? AND user_email = ? AND type = ?",
            arrayOf(date, userEmail, typeValue.toString())
        )

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID))
                val title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE))
                val amountString = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_AMOUNT))
                val type = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TYPE)) == 1
                val category = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY))
                val date = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE))

                val amount = amountString.toDoubleOrNull() ?: 0.0
                dataList.add(DataItem(id, title, amount, type, category, date, userEmail))
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return dataList
    }

    fun titleExists(title: String, userEmail: String): Boolean {
        val db = this.readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM accountingTABLE WHERE title = ? AND user_email = ?",
            arrayOf(title, userEmail)
        )
        val exists = cursor.moveToFirst() && cursor.getInt(0) > 0
        cursor.close()
        db.close()
        return exists
    }

    fun deleteRecord(id: Int, userEmail: String) {
        val db = writableDatabase
        db.delete(
            "accountingTABLE",
            "id = ? AND user_email = ?",
            arrayOf(id.toString(), userEmail)
        )
        db.close()
    }
}
