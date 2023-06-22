package com.falcon.notesapp.dao

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "NoteEntity")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val __v: Int,
    val _id: String,
    val createdAt: String,
    val description: String,
    val title: String,
    val updatedAt: String,
    val userId: String,

    val isSynced: Boolean,
    val isDeleted: Boolean
)
