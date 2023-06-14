package com.falcon.notesapp.dao

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun providesNoteDatabase(@ApplicationContext context: Context): NoteDatabase {
        return Room.databaseBuilder(context, NoteDatabase::class.java, "NoteDB").build()
    }
}