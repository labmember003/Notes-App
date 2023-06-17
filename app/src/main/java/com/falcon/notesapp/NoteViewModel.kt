package com.falcon.notesapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.falcon.notesapp.models.NoteRequest
import com.falcon.notesapp.models.NoteResponse
import com.falcon.notesapp.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import retrofit2.Response
import javax.inject.Inject

@HiltViewModel
class NoteViewModel @Inject constructor(
    private val noteRepository: NoteRepository
): ViewModel() {

    val notesLiveData get() = noteRepository.notesLiveData
    val statusLiveData get() = noteRepository.statusLiveData

    fun getNotes() {
        viewModelScope.launch {
            noteRepository.getNotes()
        }
    }

    suspend fun createNode(noteRequest: NoteRequest): Response<NoteResponse> {
        val response = CoroutineScope(Dispatchers.IO).async {
            val response = noteRepository.createNote(noteRequest)
            response
        }
        return response.await()
    }

    fun updateNote(nodeId: String, noteRequest: NoteRequest) {
        viewModelScope.launch {
            noteRepository.updateNote(nodeId, noteRequest)
        }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            noteRepository.deleteNote(noteId)
        }
    }
}