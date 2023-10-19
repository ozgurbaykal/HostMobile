package com.ozgurbaykal.hostmobile.control

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context, dbName: String, version: Int = 1) : SQLiteOpenHelper(context, dbName, null, version) {
    val mContext: Context = context;

    override fun onCreate(db: SQLiteDatabase?) {
        // İlk başlangıçta tablo oluşturmak isterseniz burada oluşturabilirsiniz
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        // Database sürümü yükseltildiğinde burada işlemler yapabilirsiniz
    }

    fun createTable(query: String) {
        writableDatabase.execSQL(query)
    }

    fun dropTable(tableName: String) {
        writableDatabase.execSQL("DROP TABLE IF EXISTS $tableName")
    }

    fun insert(tableName: String, values: ContentValues): Long {
        return writableDatabase.insert(tableName, null, values)
    }

    fun update(tableName: String, values: ContentValues, whereClause: String, whereArgs: Array<String>): Int {
        return writableDatabase.update(tableName, values, whereClause, whereArgs)
    }

    fun delete(tableName: String, whereClause: String, whereArgs: Array<String>): Int {
        return writableDatabase.delete(tableName, whereClause, whereArgs)
    }

    fun query(query: String, selectionArgs: Array<String>? = null): Cursor {
        return readableDatabase.rawQuery(query, selectionArgs)
    }

    fun clearDatabase() {
        onUpgrade(writableDatabase, 1, 2)
    }

    fun deleteDatabase(dbName: String): Boolean {
        return mContext.deleteDatabase(dbName)
    }

    fun tableExists(db: SQLiteDatabase, tableName: String): Boolean {
        val query = "SELECT name FROM sqlite_master WHERE type='table' AND name=?"
        val cursor = db.rawQuery(query, arrayOf(tableName))
        val exists = cursor.moveToFirst()
        cursor.close()
        return exists
    }
}