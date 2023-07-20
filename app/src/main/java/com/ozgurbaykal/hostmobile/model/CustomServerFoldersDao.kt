package com.ozgurbaykal.hostmobile.model

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface CustomServerFoldersDao {
    @Query("SELECT * FROM customserverfolders")
    fun getAll(): List<CustomServerFolders>

    @Insert
    fun insert(folder: CustomServerFolders)

    @Delete
    fun delete(folder: CustomServerFolders)
}