package com.falcon.notesapp

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.falcon.notesapp.adapters.NoteAdapter
import com.falcon.notesapp.api.NotesAPI
import com.falcon.notesapp.dao.NoteDatabase
import com.falcon.notesapp.dao.NoteEntity
import com.falcon.notesapp.databinding.FragmentMainBinding
import com.falcon.notesapp.models.NoteRequest
import com.falcon.notesapp.models.NoteResponse
import com.falcon.notesapp.utils.NetworkResult
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

//  1.  Jo internet se data ayega, voh data base mei save kr diya old waala delete krke
//  1a delete nhi krenge existing notes, bs add krdenge network waale notes db mei

// Jo internet se data ayega, usko db mei with existing add krdenge, (CONDITION JO UNIQUE ID WAALE HONGE UNHE HE DB MEI ADD KIYA JAYEGA)
// IN SBKA AB EK HE ILAJ HAI, AT A TIME EK DEVICE SE HE ACCOUNT ACCESS KR SKTE HO..
/*
    SERVER END PE EK INT VALUE RKHENGE JO DEVICE SEND KREGA AT TIME OF LOGIN/REGISTRATION
    AND AGER DEVICE JEB LOGIN KR RRHA HO USS POINT OF TIME PE VALUE NON ZERO THO LOGIN ALLOW NHI KREGA
    AUR HAR LAUNCH PE SERVER VALUE AUR LOCAL VALUE CHECK KREGA...AGER SAME HAI THO PROCCEDD....VERNA UNDISMISSABLE DIALOGUE SAYING THAT
    YOU LOGGED IN FROM ANOTHER FUCKING DEVICE
 */


//  2. display the notes that are in database in rcv
//  3. jo add, update, del se data aaya (app mei add/ delete hua) usse database mei save kr liya, with isSynced false
//  4. on each operation either delete update or edit call the sync function on whose isSyned is false


@AndroidEntryPoint
class MainFragment : Fragment() {

    @Inject
    lateinit var noteDatabase: NoteDatabase

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!
    private val noteViewModel by viewModels<NoteViewModel>()

    @Inject
    lateinit var notesAPI: NotesAPI
    private lateinit var adapter: NoteAdapter
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        adapter = NoteAdapter(::onNoteClicked)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (isNetworkAvailable(requireContext())) {
            bindObservers()
            noteViewModel.getNotes()
        } else {
            CoroutineScope(Dispatchers.Main).launch {
                delay(600)
                showSnackBar("Failed To Fetch Notes", activity)
            }

        }
        binding.notesList.adapter = adapter
        binding.notesList.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        binding.addNote.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_noteFragment)
        }
        CoroutineScope(Dispatchers.IO).launch {
            syncNotes()  // Update / Create / Delete
        }
        displayData()
    }

    private suspend fun syncNotes() {
        if (isNetworkAvailable(requireContext())) {
            syncDeletedNotes() // Handle Deleted Notes
            val unsyncedNotes: List<NoteEntity> = noteDatabase.noteDao().getUnsyncedNotes()
            val mappedUnsyncedNotesList = mapNoteEntityListToNoteResponseList(unsyncedNotes)
            unsyncedNotes.forEach {
//                noteEntityToNoteResponse()
                // check if it is case of create or update
                if (it._id == "toBeUpdated") { // Create note waala case
                    syncNewNotes(it)
                } else { // update note waala case
                    syncUpdatedNotes(it)
                }
            }
        }
        else {
            CoroutineScope(Dispatchers.Main).launch {
                delay(600)
                showSnackBar("Syncing Failed. Check Your Internet Connection", activity)
            }
        }
    }

    private suspend fun syncDeletedNotes() {
        val deletedNotesList: List<NoteEntity> = noteDatabase.noteDao().getDeletedNotes()
        val mappedDeletedNotesList = mapNoteEntityListToNoteResponseList(deletedNotesList)
        mappedDeletedNotesList.forEach {
            noteViewModel.deleteNote(it._id)
        }
        deletedNotesList.forEach {
            noteDatabase.noteDao().deleteNote(it)
        }
    }


    private suspend fun syncNewNotes(it: NoteEntity) {
        val response = noteViewModel.createNode(NoteRequest(it.description, it.title))
        val body = response.body()
//        it.__v = body?.__v ?: 0
        it.userId = body?.userId.toString()
        it._id = body?._id.toString()
        it.updatedAt = body?.updatedAt.toString()
        it.createdAt = body?.createdAt.toString()

        it.isSynced = true

        noteDatabase.noteDao().updateNote(it)

    }

    private suspend fun syncUpdatedNotes(it: NoteEntity) {
        noteViewModel.updateNote(it._id, NoteRequest(it.description, it.title))

        it.isSynced = true

        noteDatabase.noteDao().updateNote(it)
    }

    private fun displayData() {
        noteDatabase.noteDao().getNotes().observe(viewLifecycleOwner) {
            if (it.isEmpty()) {
                binding.listEmptyAnimation.isVisible = true
                binding.nothingToDisplayText.isVisible = true
            } else {
                binding.listEmptyAnimation.isVisible = false
                binding.nothingToDisplayText.isVisible = false
            }
            val convertedList = mapNoteEntityListToNoteResponseList(it)
            adapter.submitList(convertedList)
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

    private fun noteEntityToNoteResponse(noteEntity: NoteEntity): NoteResponse {
        return NoteResponse(noteEntity.__v, noteEntity._id, noteEntity.createdAt, noteEntity.description, noteEntity.title, noteEntity.updatedAt, noteEntity.userId)
    }

    private fun onNoteClicked(noteResponse: NoteResponse) {
        val bundle = Bundle()
        bundle.putString("note", Gson().toJson(noteResponse))
        findNavController().navigate(R.id.action_mainFragment_to_noteFragment, bundle)
    }

    private fun bindObservers() {
        noteDatabase.noteDao()
        noteViewModel.notesLiveData.observe(viewLifecycleOwner, Observer {
            binding.progressBar.isVisible = false
            when (it) {
                is NetworkResult.Success -> {
//                    if (it.data?.size == 0) {
//                        binding.listEmptyAnimation.isVisible = true
//                        binding.nothingToDisplayText.isVisible = true
//                    } else {
//                        binding.listEmptyAnimation.isVisible = false
//                        binding.nothingToDisplayText.isVisible = false
//                    }
//                    INSERTING DATA WHICH CAME FROM INTERNET INTO DATABASE
                    CoroutineScope(Dispatchers.IO).launch {
                        synchronizeListData(it.data)
                    }
//                    adapter.submitList(it.data)
                }
                is NetworkResult.Error -> {
                    Toast.makeText(requireContext(), it.message.toString(), Toast.LENGTH_SHORT).show()
                }
                is NetworkResult.Loading -> {
                    binding.progressBar.isVisible = true
                }
            }
        })
    }

    private suspend fun synchronizeListData(data: List<NoteResponse>?) {
        data?.forEach {
            Log.i("nimbumirchdhnaiya", noteDatabase.noteDao().checkEntryExists(it._id).toString())
            Log.i("asdfghjkl", checkNoteExistenceInDB(it).toString())
            if (!checkNoteExistenceInDB(it)) {
                val note = NoteEntity(it.__v, it._id, it.createdAt, it.description, it.title, it.updatedAt, it.userId,
                    isSynced = true,
                    isDeleted = false
                )
                noteDatabase.noteDao().insertNote(note)
            }
        }
    }

    private fun checkNoteExistenceInDB(note: NoteResponse): Boolean {
        if (noteDatabase.noteDao().checkEntryExists(note._id) == 0) {
            return false
        }
        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities =
            connectivityManager.getNetworkCapabilities(network) ?: return false
        return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
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