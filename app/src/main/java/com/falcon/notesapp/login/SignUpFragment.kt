package com.falcon.notesapp.login

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.falcon.notesapp.AuthViewModel
import com.falcon.notesapp.R
import com.falcon.notesapp.databinding.FragmentSignUpBinding
import com.falcon.notesapp.models.UserRequest
import com.falcon.notesapp.utils.NetworkResult
import com.falcon.notesapp.utils.TokenManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SignUpFragment : Fragment() {

    private var _binding: FragmentSignUpBinding? = null
    private val binding get() = _binding!!
    private val authViewModel by viewModels<AuthViewModel>()

    @Inject
    lateinit var tokenManager: TokenManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignUpBinding.inflate(inflater, container, false)
        if (tokenManager.getToken() != null) {
            findNavController().navigate(R.id.action_SignUpFragment_to_mainFragment)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.loginPanel.setOnClickListener {
            if (isNetworkAvailable(requireContext())) {
                binding.errorTextview.visibility = View.INVISIBLE
                val validateResult = validateUserInput()
                if (validateResult.first) {
                    binding.loginButton.visibility = View.GONE
                    binding.animationView.visibility = View.VISIBLE
                    binding.animationView.setAnimation("loading-dots.json")
                    binding.animationView.playAnimation()
                    authViewModel.registerUser(getUserRequest())
                } else {
                    binding.errorTextview.text = validateResult.second
                    binding.errorTextview.visibility = View.VISIBLE
                }
            }
            else {
                binding.errorTextview.text = "Please ensure a network connection is available for the registration process."
                binding.errorTextview.visibility = View.VISIBLE
            }

        }
        binding.accountExistLL.setOnClickListener {
            findNavController().navigate(R.id.action_SignUpFragment_to_LoginFragment)
        }
        binding.authGoogle.setOnClickListener {
            Toast.makeText(requireContext(), "Coming Soon", Toast.LENGTH_SHORT).show()
        }
        bindObservers()
    }

    private fun getUserRequest(): UserRequest {
        return UserRequest(binding.EmailText.text.toString(), binding.password.text.toString(), binding.username.text.toString() )
    }

    private fun validateUserInput(): Pair<Boolean, String> {
        val userRequest = getUserRequest()
        return authViewModel.validateCredentials(userRequest.username, userRequest.email, userRequest.password, false)
    }

    private fun bindObservers() {
        authViewModel.userResponseLiveData.observe(viewLifecycleOwner) {
            when (it) {
                is NetworkResult.Success -> {
                    tokenManager.saveToken(it.data!!.token)
                    findNavController().navigate(R.id.action_SignUpFragment_to_mainFragment)
                }
                is NetworkResult.Error -> {
                    binding.errorTextview.text = it.message
                    binding.errorTextview.visibility = View.VISIBLE
                    binding.animationView.visibility = View.GONE
                    binding.loginButton.visibility = View.VISIBLE
                }
                is NetworkResult.Loading -> {
                    binding.loginButton.visibility = View.GONE
                    binding.animationView.visibility = View.VISIBLE
                    binding.animationView.setAnimation("loading-dots.json")
                    binding.animationView.playAnimation()
                }
            }
        }
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
}