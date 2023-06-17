package com.falcon.notesapp.models

data class NoteResponse(
    var __v: Int,
    var _id: String,
    var createdAt: String,
    var description: String,
    var title: String,
    var updatedAt: String,
    var userId: String
)