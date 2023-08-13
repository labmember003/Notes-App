package com.falcon.notesapp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class NoteFragment : Fragment() {
    /*
        NOTE NOTE NOTE NOTE NOTE NOTE
        NOTE NOTE NOTE NOTE NOTE NOTE
        NOTE NOTE NOTE NOTE NOTE NOTE
        NOTE NOTE NOTE NOTE NOTE NOTE
        NOTE NOTE NOTE NOTE NOTE NOTE

        NOTES FRAGMENT MEI KOI NETWORK CALL NHI HOGI, ONLY DB CALLS
     */

    private val noteViewModel by viewModels<NoteViewModel>()
    @Inject
    lateinit var tokenManager: TokenManager

    @Inject
    lateinit var noteDatabase: NoteDatabase

    private var _binding: FragmentNoteBinding? = null
    private val binding get() = _binding!!
//    private val noteViewModel by viewModels<NoteViewModel>()
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
//        bindObservers()
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
                CoroutineScope(Dispatchers.IO).launch {
                    updateNoteInDatabase(note, noteRequest)
                }
            } else {
                CoroutineScope(Dispatchers.IO).launch {
                    storeNoteInDatabase(noteRequest)
                }
            }
            val b = Bundle()
            b.putBoolean("isReturningFromDeleteNote", false)
            findNavController().navigate(R.id.action_noteFragment_to_mainFragment, b)
        }
        binding.shareButton.setOnClickListener {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, "${binding.titleEditText.text} : \n${binding.descriptionEditText.text}")
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, null)
            startActivity(shareIntent)
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
                    if (isNetworkAvailable(requireContext())) {
                        noteViewModel.deleteNote(note._id)
                    }
                    withContext(Dispatchers.Main) {
                        val b = Bundle()
                        b.putBoolean("isReturningFromDeleteNote", true)
                        findNavController().navigate(R.id.action_noteFragment_to_mainFragment, b)
                    }
                }
            }
        }
        dialogBuilder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }
        val dialog = dialogBuilder.create()
        dialog.show()
    }

    private suspend fun deleteNoteFromDB(note: NoteResponse) {
        val noteEntity = NoteEntity(note!!.__v, note._id, note.createdAt,
            description = note.description,
            title = note.title,
            note.updatedAt, note.userId, isSynced = false, isDeleted = true
        )
        noteDatabase.noteDao().updateNote(noteEntity)
//        hum yaaha actual mei delete nhi krenge just because suppose:
//        mai offline tha, maine kuch notes bnaye....fir mai online gya...notes sync ho gye
//        then mai offline gya ...maine ek note delete kr diya
//        ab mai online gya....tho online aur offline waali list add ho jayegi
//        tho voh note jo delte ho chuka hai voh dubra aa jayega
//        tho jeb server pe delete hoga uss point of time per he hum database se delete krenge
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

//    private fun bindObservers() {
//        if (isNetworkAvailable(requireContext())) {
//            noteViewModel.statusLiveData.observe(viewLifecycleOwner) {
//                when (it) {
//                    is NetworkResult.Success -> {
//                        findNavController().popBackStack()
//                    }
//                    is NetworkResult.Error -> {
//
//                    }
//                    is NetworkResult.Loading -> {
//
//                    }
//                }
//            }
//        }
//    }

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

    private fun showSnackBar(message: String?, activity: Activity?) {
        if (null != activity && null != message) {
            Snackbar.make(
                activity.findViewById(android.R.id.content),
                message, Snackbar.LENGTH_SHORT
            ).show()
        }
    }
}