package com.example.urduphotodesigner.ui.auth

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.urduphotodesigner.R
import com.example.urduphotodesigner.common.sealed.SignInUiState
import com.example.urduphotodesigner.databinding.FragmentSignupBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class SignupFragment : Fragment() {
    private var _binding: FragmentSignupBinding?= null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by activityViewModels()
    private var progressDialog: AlertDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignupBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupProgressDialog()
        setupClickListeners()
        observeRegistrationState()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupClickListeners() {
        binding.register.setOnClickListener {
            validateAndRegister()
        }

        binding.login.setOnClickListener {
            navigateToLogin()
        }

        binding.password.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableRight = binding.password.compoundDrawables[2]
                if (drawableRight != null) {
                    // Calculate clickable area (right side minus padding)
                    val clickableStart = binding.password.width -
                            binding.password.paddingRight -
                            drawableRight.bounds.width()

                    if (event.x >= clickableStart) {
                        togglePasswordVisibility(binding.password)
                        return@setOnTouchListener true
                    }
                }
            }
            false
        }

        binding.confirmPassword.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableRight = binding.confirmPassword.compoundDrawables[2]
                if (drawableRight != null) {
                    // Calculate clickable area (right side minus padding)
                    val clickableStart = binding.confirmPassword.width -
                            binding.confirmPassword.paddingRight -
                            drawableRight.bounds.width()

                    if (event.x >= clickableStart) {
                        togglePasswordVisibility(binding.confirmPassword)
                        return@setOnTouchListener true
                    }
                }
            }
            false
        }
    }

    private fun setupProgressDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_progress, null)
        progressDialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        progressDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        progressDialog?.window?.setDimAmount(0.5f) // Dim background
        progressDialog?.window?.setLayout(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun togglePasswordVisibility(editText: EditText) {
        val isCurrentlyHidden = editText.inputType ==
                (InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)

        if (isCurrentlyHidden) {
            // Show password
            editText.inputType =
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            editText.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_hide_pass, 0)
        } else {
            // Hide password
            editText.inputType =
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            editText.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_show_pass, 0)
        }

        // Move cursor to the end
        editText.setSelection(editText.text.length)
    }

    private fun validateAndRegister() {
        val name = binding.name.text.toString().trim()
        val email = binding.email.text.toString().trim()
        val password = binding.password.text.toString().trim()
        val confirmPassword = binding.confirmPassword.text.toString().trim()

        when {
            name.isEmpty() -> {
                binding.email.error = "Please enter your name"
                binding.name.requestFocus()
            }
            email.isEmpty() -> {
                binding.email.error = "Please enter your email"
                binding.email.requestFocus()
            }
            password.isEmpty() -> {
                binding.password.error = "Please enter a password"
                binding.password.requestFocus()
            }
            password != confirmPassword -> {
                binding.confirmPassword.error = "Passwords don't match"
                binding.confirmPassword.requestFocus()
            }
            password.length < 6 -> {
                binding.confirmPassword.error = "Password must be at least 6 characters"
                binding.confirmPassword.requestFocus()
            }
            else -> {
                authViewModel.performRegistration(name, email, password)
            }
        }
    }

    private fun observeRegistrationState() {
        lifecycleScope.launchWhenStarted {
            authViewModel.signInState.collectLatest { state ->
                when (state) {
                    is SignInUiState.Idle -> {
                    }
                    is SignInUiState.Loading -> {
                        progressDialog?.show()
                    }
                    is SignInUiState.RegistrationSuccess -> {
                        showSuccessMessage("Registration successful!")
                        progressDialog?.dismiss()
                        navigateToLogin()
                    }
                    is SignInUiState.Error -> {
                        showErrorMessage(state.message ?: "Registration failed")
                        progressDialog?.dismiss()
                    }
                    else -> {
                        progressDialog?.show()
                        // Handle other states if needed
                    }
                }
            }
        }
    }

    private fun showSuccessMessage(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun showErrorMessage(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun navigateToLogin() {
        authViewModel.clearUserData()
        findNavController().navigateUp()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}