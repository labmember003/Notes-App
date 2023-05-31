package com.falcon.notesapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.falcon.notesapp.models.NoteRequest
import com.falcon.notesapp.repository.NoteRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

class NoteViewModel @Inject constructor(private val noteRepository: NoteRepository): ViewModel() {

    val notesLiveData get() = noteRepository.notesLiveData
    val statusLiveData get() = noteRepository.statusLiveData

    fun getNotes() {
        viewModelScope.launch {
            noteRepository.getNotes()
        }
    }

    fun createNode(noteRequest: NoteRequest) {
        viewModelScope.launch {
            noteRepository.createNote(noteRequest)
        }
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