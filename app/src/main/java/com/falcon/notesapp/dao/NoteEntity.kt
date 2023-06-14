package com.falcon.notesapp.dao

import androidx.room.Entity

@Entity(tableName = "NoteEntity")
data class NoteEntity(
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
