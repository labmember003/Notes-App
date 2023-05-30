package com.falcon.notesapp.api

import com.falcon.notesapp.models.NoteRequest
import com.falcon.notesapp.models.NoteResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface NotesAPI {
    @GET("/notes")
    suspend fun getNotes(): Response<List<NoteResponse>>

    @POST("/notes")
    suspend fun createNote(@Body noteRequest: NoteRequest): Response<NoteResponse>

    @PUT("/notes/{noteID}")
    suspend fun updateNote(@Path("noteID") noteID: String, @Body noteRequest: NoteRequest): Response<NoteResponse>

    @DELETE("/notes/{noteID}")
    suspend fun deleteNote(@Path("noteID") noteID: String): Response<NoteResponse>
}