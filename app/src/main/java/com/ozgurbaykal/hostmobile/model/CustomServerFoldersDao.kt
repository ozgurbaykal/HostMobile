package com.ozgurbaykal.hostmobile.model

import androidx.lifecycle.LiveData
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
    @Query("SELECT * FROM customserverfolders WHERE is_selected = 1 LIMIT 1")
    fun getSelectedFolderLiveData(): LiveData<CustomServerFolders?>

    @Query("SELECT selected_file FROM CustomServerFolders WHERE folder_name = :folderName")
    fun getSelectedFile(folderName: String?): String?
}