package com.falcon.notesapp.di

import com.falcon.notesapp.api.AuthInterceptor
import com.falcon.notesapp.api.NotesAPI
import com.falcon.notesapp.api.UserAPI
import com.falcon.notesapp.utils.Constants.BASE_URL
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class NetworkModule {
    @Provides
    @Singleton
    fun providesRetrofitBuilder() : Retrofit.Builder {
        return Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(BASE_URL)
    }

    @Singleton
    @Provides
    fun provideHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        return OkHttpClient.Builder().addInterceptor(authInterceptor).build()
    }

    @Provides
    @Singleton
    fun providesUserAPI(retrofitBuilder: Retrofit.Builder): UserAPI {
        return retrofitBuilder.build().create(UserAPI::class.java)
    }

    @Provides
    @Singleton
    fun providesNotesApi(retrofitBuilder: Retrofit.Builder, okHttpClient: OkHttpClient): NotesAPI {
       return retrofitBuilder
           .client(okHttpClient)
           .build()
           .create(NotesAPI::class.java)
    }
}