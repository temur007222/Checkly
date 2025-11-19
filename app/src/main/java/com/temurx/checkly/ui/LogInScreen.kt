package com.temurx.checkly.ui

import androidx.fragment.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.temurx.checkly.R
import com.google.firebase.auth.FirebaseAuth
import com.temurx.checkly.databinding.FragmentLogInScreenBinding
class LogInScreen : Fragment() {
    private var _binding: FragmentLogInScreenBinding? = null
    private val binding get() = _binding!!

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLogInScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Auto-login if already authenticated
        if (auth.currentUser != null) {
            navigateToTasks()
        }

        binding.loginButton.setOnClickListener {
            val email = binding.loginEmail.text.toString().trim()
            val password = binding.loginPw.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.loginButton.isEnabled = false

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    binding.loginButton.isEnabled = true
                    if (task.isSuccessful) {
                        navigateToTasks()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Login failed: ${task.exception?.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }
    }

    private fun navigateToTasks() {
        findNavController().navigate(R.id.action_logInScreen_to_tasksScreen)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
