package com.falcon.notesapp.api

import android.app.DownloadManager.Request
import com.falcon.notesapp.models.UserRequest
import com.falcon.notesapp.models.UserResponse
import retrofit2.http.POST
import retrofit2.Response

interface UserAPI {
    @POST("/users/signup")
    suspend fun signup(userRequest: UserRequest): Response<UserResponse>

    @POST("/users/signin")
    suspend fun signin(userRequest: UserRequest): Response<UserResponse>
}