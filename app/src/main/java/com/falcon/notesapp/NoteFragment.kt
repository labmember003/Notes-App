package com.falcon.notesapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.falcon.notesapp.dao.NoteDatabase
import com.falcon.notesapp.databinding.FragmentNoteBinding
import com.falcon.notesapp.models.NoteRequest
import com.falcon.notesapp.models.NoteResponse
import com.falcon.notesapp.utils.NetworkResult
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class NoteFragment : Fragment() {

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
            } else {
//                Toast.makeText(requireContext(), title.isEmpty().toString() + description.isEmpty().toString(), Toast.LENGTH_SHORT).show()
                noteViewModel.createNode(noteRequest)
            }
        }
    }

    private fun deleteNote(note: NoteResponse?) {
        val dialogBuilder = AlertDialog.Builder(requireContext())
        dialogBuilder.setMessage("Are You Sure To Delete it ?")
        dialogBuilder.setTitle("Delete File")
        dialogBuilder.setPositiveButton("OK") { dialog, which ->
            if (note != null) {
                noteViewModel.deleteNote(note._id)
            }
        }
        dialogBuilder.setNegativeButton("Cancel") { dialog, which ->
            dialog.dismiss()
        }
        val dialog = dialogBuilder.create()
        dialog.show()
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
        noteViewModel.statusLiveData.observe(viewLifecycleOwner, Observer {
            when (it) {
                is NetworkResult.Success -> {
                    findNavController().popBackStack()
                }
                is NetworkResult.Error -> {

                }
                is NetworkResult.Loading -> {

                }
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}