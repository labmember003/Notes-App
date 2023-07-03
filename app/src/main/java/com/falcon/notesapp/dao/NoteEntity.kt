package com.falcon.notesapp.dao

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "NoteEntity")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    var __v: Int,
    var _id: String,
    var createdAt: String,
    var description: String,
    var title: String,
    var updatedAt: String,
    var userId: String,

    var isSynced: Boolean,
    var isDeleted: Boolean
)
