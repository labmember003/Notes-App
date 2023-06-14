package com.falcon.notesapp

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
import com.falcon.notesapp.models.NoteResponse
import com.falcon.notesapp.utils.Constants.TAG
import com.falcon.notesapp.utils.NetworkResult
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject


//  1.  Jo internet se data ayega, voh data base mei save kr diya old waala delete krke
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

    //    TODO - HANDLE APP CRASHING WHEN NO INTERNET CONNECTION

    @Inject
    lateinit var notesAPI: NotesAPI
    private lateinit var adapter: NoteAdapter
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        CoroutineScope(Dispatchers.IO).launch {
            val response = notesAPI.getNotes()
            Log.i(TAG, response.body().toString())
        }
        adapter = NoteAdapter(::onNoteClicked)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindObservers()
        noteViewModel.getNotes()
        binding.notesList.adapter = adapter
        binding.notesList.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        binding.addNote.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_noteFragment)
        }
        displayData()
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
                        updateDatabaseFromWeb(it.data)
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

    private suspend fun updateDatabaseFromWeb(data: List<NoteResponse>?) {
        noteDatabase.clearAllTables()
        data?.forEach {
            val note = NoteEntity(it.__v, it._id, it.createdAt, it.description, it.title, it.updatedAt, it.userId,
                isSynced = false,
                isDeleted = false
            )
            noteDatabase.noteDao().insertNote(note)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}