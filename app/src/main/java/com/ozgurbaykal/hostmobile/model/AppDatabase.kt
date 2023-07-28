package com.ozgurbaykal.hostmobile.model

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [CustomServerFolders::class], version = 3)
abstract class AppDatabase : RoomDatabase() {
    abstract fun folderDao(): CustomServerFoldersDao


    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "host_mobile_app_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .addMigrations(MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                return instance
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add 'is_selected' column to 'CustomServerFolders' table
                database.execSQL(
                    "ALTER TABLE CustomServerFolders ADD COLUMN is_selected INTEGER NOT NULL DEFAULT 0"
                )

                // Add 'selected_file' column to 'CustomServerFolders' table
                database.execSQL(
                    "ALTER TABLE CustomServerFolders ADD COLUMN selected_file TEXT"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {

                // Add 'selected_file' column to 'CustomServerFolders' table
                database.execSQL(
                    "ALTER TABLE CustomServerFolders ADD COLUMN selected_file_path TEXT"
                )
            }
        }
    }


}