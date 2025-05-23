package com.example.urduphotodesigner.ui.auth

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.urduphotodesigner.R
import com.example.urduphotodesigner.databinding.FragmentLoginBinding
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.auth.api.identity.SignInCredential
import com.google.android.gms.common.api.ApiException
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    // Google Identity Services components
    private lateinit var oneTapClient: SignInClient
    private lateinit var signInRequest: BeginSignInRequest

    private val oneTapResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        handleSignInResult(result)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupGoogleSignIn()
        setClickListeners()
    }

    private fun setupGoogleSignIn() {
        oneTapClient = Identity.getSignInClient(requireActivity())

        signInRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId(getString(R.string.server_client_id)) // From google-services.json
                    .setFilterByAuthorizedAccounts(false)
                    .build()
            )
            .setAutoSelectEnabled(false)
            .build()
    }

    private fun setClickListeners() {
        binding.googleSignIn.setOnClickListener {
            signInWithGoogle()
        }
    }

    private fun signInWithGoogle() {
        try {
            oneTapClient.beginSignIn(signInRequest)
                .addOnSuccessListener { result ->
                    Log.d(TAG, "One Tap sign-in intent received")
                    val intentSenderRequest = IntentSenderRequest.Builder(result.pendingIntent.intentSender).build()
                    oneTapResultLauncher.launch(intentSenderRequest)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "One Tap sign-in failed: ${e.localizedMessage}")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in signInWithGoogle: ${e.message}")
        }
    }

    private fun handleSignInResult(result: ActivityResult) {
        Log.d(TAG, "handleSignInResult triggered")

        if (result.resultCode == Activity.RESULT_OK) {
            try {
                val credential = oneTapClient.getSignInCredentialFromIntent(result.data)
                val idToken = credential.googleIdToken
                when {
                    idToken != null -> {
                        Log.d(TAG, "ID Token received: $idToken")
                        onGoogleSignInSuccess(credential.id, credential.displayName)
                    }
                    else -> {
                        Log.w(TAG, "No ID token!")
                    }
                }
            } catch (e: ApiException) {
                onGoogleSignInFailure(e)
            }
        } else {
            Log.w(TAG, "Sign-in canceled or failed. resultCode=${result.resultCode}")
        }
    }

    private fun onGoogleSignInSuccess(email: String?, displayName: String?) {
        Log.d(TAG, "Sign-in successful: $email, $displayName")
        Toast.makeText(requireContext(), "$email", Toast.LENGTH_SHORT).show()
        findNavController().navigate(R.id.homeFragment)
    }

    private fun onGoogleSignInFailure(e: Exception) {
        Log.w(TAG, "Sign-in failed: ${e.localizedMessage}", e)
        Toast.makeText(requireContext(), "Sign-in failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    companion object {
        private const val TAG = "LoginFragment"
    }
}