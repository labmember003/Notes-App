package com.falcon.notesapp.login

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.falcon.notesapp.R
import com.falcon.notesapp.databinding.FragmentLoginBinding
import com.falcon.notesapp.databinding.FragmentSignUpBinding


class SignUpFragment : Fragment() {

    private var _binding: FragmentSignUpBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignUpBinding.inflate(inflater, container, false)
        binding.loginPanel.setOnClickListener {
            binding.loginButton.visibility = View.GONE
            binding.animationView.visibility = View.VISIBLE
            binding.animationView.setAnimation("loading-dots.json")
            binding.animationView.playAnimation()
        }
        binding.accountExistTxt.setOnClickListener {
            findNavController().navigate(R.id.action_SignUpFragment_to_LoginFragment)
        }
        binding.accountExistTxt2.setOnClickListener {
            findNavController().navigate(R.id.action_SignUpFragment_to_LoginFragment)
        }
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}