package com.falcon.notesapp

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.falcon.notesapp.dao.NoteDatabase
import com.falcon.notesapp.dao.NoteEntity
import com.falcon.notesapp.databinding.FragmentNoteBinding
import com.falcon.notesapp.models.NoteRequest
import com.falcon.notesapp.models.NoteResponse
import com.falcon.notesapp.utils.NetworkResult
import com.falcon.notesapp.utils.TokenManager
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NoteFragment : Fragment() {

    @Inject
    lateinit var tokenManager: TokenManager

    @Inject
    lateinit var noteDatabase: NoteDatabase

    private var _binding: FragmentNoteBinding? = null
    private val binding get() = _binding!!
    private val noteViewModel by viewModels<NoteViewModel>()
    private var note: NoteResponse? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNoteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val jsonNote = arguments?.getString("note")
        if (jsonNote != null) {
            note = Gson().fromJson(jsonNote, NoteResponse::class.java)
            note?.let {
                binding.titleEditText.setText(it.title)
                binding.descriptionEditText.setText(it.description)
            }
        }
        bindHandlers()
        bindObservers()
        CoroutineScope(Dispatchers.IO).launch {
            syncNotes()  // Update / Create / Delete
        }
    }

    private fun syncDeletedNotes() {
        val deletedNotesList: List<NoteEntity> = noteDatabase.noteDao().getDeletedNotes()
        val mappedDeletedNotesList = mapNoteEntityListToNoteResponseList(deletedNotesList)
        mappedDeletedNotesList.forEach {
            noteViewModel.deleteNote(it._id)
        }
    }

    private suspend fun syncNotes() {
        if (isNetworkAvailable(requireContext())) {
            syncDeletedNotes() // Handle Deleted Notes

            val unsyncedNotes: List<NoteEntity> = noteDatabase.noteDao().getUnsyncedNotes()
            val mappedUnsyncedNotesList = mapNoteEntityListToNoteResponseList(unsyncedNotes)
            mappedUnsyncedNotesList.forEach {
                // check if it is case of create or update
                if (it._id == "toBeUpdated") { // Create note waala case
//                it
                    syncNewNotes(it)
//                syncWithWeb(requireContext(), )
                } else { // update note waala case
                    syncUpdatedNotes(it)
                }
            }
        }
        else {
            Toast.makeText(requireContext(), "Syncing Failed. Check Your Internet Connection", Toast.LENGTH_SHORT).show()
        }


    }

    private suspend fun syncNewNotes(it: NoteResponse) {
        val response = noteViewModel.createNode(NoteRequest(it.description, it.title))
        val body = response.body()
        it.__v = body?.__v ?: 0
        it.userId = body?.userId.toString()
        it._id = body?._id.toString()
        it.updatedAt = body?.updatedAt.toString()
        it.createdAt = body?.createdAt.toString()
    }

    private fun syncUpdatedNotes(it: NoteResponse) {
        noteViewModel.updateNote(it._id, NoteRequest(it.description, it.title))
    }

    private fun bindHandlers() {
        binding.deleteButton.setOnClickListener {
            deleteNote(note)
        }
        binding.backButton.setOnClickListener {
            findNavController().popBackStack()
        }
        binding.submitButton.setOnClickListener {
            val title = binding.titleEditText.text.toString()
            val description = binding.descriptionEditText.text.toString()
            isDetailsValid(title, description)
            val noteRequest = NoteRequest(description = description, title = title)
            if (note != null) {
                noteViewModel.updateNote(note!!._id, noteRequest)
                CoroutineScope(Dispatchers.IO).launch {
                    updateNoteInDatabase(note, noteRequest)
                }
                syncUpdatedNotes(note!!)
//                syncWithWeb(requireContext(), noteRequest, note, OPERATION.UPDATE)
            } else {
                CoroutineScope(Dispatchers.IO).launch {
                    storeNoteInDatabase(noteRequest)
                }
//                syncWithWeb(requireContext(), noteRequest, null, OPERATION.CREATE)
//                syncNewNotes()
//                if (internet permissions are enabled then)
//                noteViewModel.createNode(noteRequest)
//                and make isSynced true
//                noteViewModel.createNode(noteRequest)
            }
        }
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities =
            connectivityManager.getNetworkCapabilities(network) ?: return false
        return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

//    private fun syncWithWeb(context: Context, noteRequest: NoteRequest?, note: NoteResponse?, operation: OPERATION) {
//        if (isNetworkAvailable(context)) {
//            when (operation) {
//                OPERATION.DELETE -> {
//                    noteViewModel.deleteNote(note!!._id)
//                }
//                OPERATION.CREATE -> {
//                    noteViewModel.createNode(noteRequest!!)
//                }
//                else -> {
//
//                }
//            }
//        } else {
//            Toast.makeText(requireContext(), "Net nhi hai bsdk", Toast.LENGTH_SHORT).show()
//        }
//    }

    private suspend fun updateNoteInDatabase(existingNote: NoteResponse?, noteRequest: NoteRequest) {
        val noteEntity = NoteEntity(existingNote!!.__v, existingNote._id, existingNote.createdAt,
                description = noteRequest.description,
                title = noteRequest.title,
                existingNote.updatedAt, existingNote.userId, isSynced = false, isDeleted = false
            )

        noteDatabase.noteDao().updateNote(noteEntity)
    }

    private suspend fun storeNoteInDatabase(noteRequest: NoteRequest) {
//        val random = Random(System.currentTimeMillis())
//        val randomId = random.nextInt(900000) + 100000
        val noteEntity = NoteEntity(0, "toBeUpdated", "toBeUpdated", noteRequest.description, noteRequest.title, "toBeUpdated", tokenManager.getToken().toString(),
            isSynced = false,
            isDeleted = false
        )
        noteDatabase.noteDao().insertNote(noteEntity)
//        yaaha database mei store krne ke liye _id bhi chahiye
//        but phele waale cases mei voh backend se generate ho rrha tha
//        but fir mai ager app se ek unique id generate krdu then usko mai server per bhejunga kaise
//        server per bhejne ke liye tho sirf NoteRequest ka object bhej skta hu na mai
//        aur noteRequest mei sirf title and description hai
//        tho server alag id bna dega aur app alag id bna dega
    }

    private fun deleteNote(note: NoteResponse?) {
        val dialogBuilder = AlertDialog.Builder(requireContext())
        dialogBuilder.setMessage("Are You Sure To Delete it ?")
        dialogBuilder.setTitle("Delete File")
        dialogBuilder.setPositiveButton("OK") { _, _ ->
            if (note != null) {
//                noteViewModel.deleteNote(note._id)
                CoroutineScope(Dispatchers.IO).launch {
                    deleteNoteFromDB(note)
                }
//                syncWithWeb(requireContext(), null, note, OPERATION.DELETE)
            }
        }
        dialogBuilder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }
        val dialog = dialogBuilder.create()
        dialog.show()
    }

    private suspend fun deleteNoteFromDB(note: NoteResponse) {
        val noteEntity = NoteEntity(note.__v, note._id, note.createdAt, note.description, note.title
            , note.updatedAt, note.userId, isSynced = false, isDeleted = true
        )
        noteDatabase.noteDao().deleteNote(noteEntity)
    }

    private fun isDetailsValid(title: String, description: String): Boolean {
        if (title.isEmpty()) {
            binding.outlinedTextField.error = "Title is required"
            return false
        }
        if (description.isEmpty()) {
            binding.outlinedTextField2.error = "Description is required"
            return false
        }
        return true
    }

    private fun bindObservers() {
        noteViewModel.statusLiveData.observe(viewLifecycleOwner) {
            when (it) {
                is NetworkResult.Success -> {
                    findNavController().popBackStack()
                }
                is NetworkResult.Error -> {

                }
                is NetworkResult.Loading -> {

                }
            }
        }
    }

    private fun mapNoteEntityListToNoteResponseList(noteEntityList: List<NoteEntity>?): MutableList<NoteResponse> {
        val list: MutableList<NoteResponse> = emptyList<NoteResponse>().toMutableList()
        noteEntityList?.forEach {
            val noteResponse = NoteResponse(it.__v, it._id, it.createdAt, it.description, it.title, it.updatedAt, it.userId)
            list.add(noteResponse)
        }
        return list
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}