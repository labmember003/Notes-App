package com.falcon.notesapp.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Update
import androidx.room.Delete
import androidx.room.Query

@Dao
interface NoteDaoInterface {

    @Insert
    suspend fun insertNote(noteEntity: NoteEntity) // insert krke isSynced false krdega

    @Update
    suspend fun updateNote(noteEntity: NoteEntity) // Id bhi NoteEntity se he extract krlega tu
    // update krke isSynced false krdega

    @Delete
    suspend fun deleteNote(noteEntity: NoteEntity) // Id bhi NoteEntity se he extract krlega tu
    // update krke isSynced false krdega
    // aur isDeleted true

    @Query("SELECT * FROM NoteEntity")
    fun getNotes(): LiveData<List<NoteEntity>>

    @Query("SELECT * FROM NoteEntity WHERE isDeleted = 1")
    fun getDeletedNotes(): List<NoteEntity>

    @Query("SELECT * FROM NoteEntity WHERE isSynced = 0")
    fun getUnsyncedNotes(): List<NoteEntity>
}
