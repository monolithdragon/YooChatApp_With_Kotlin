package com.monolithdragon.yoochat.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.monolithdragon.yoochat.databinding.ActivitySignInBinding

class SignInActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignInBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initFields()
        setListener()
    }

    override fun onStart() {
        super.onStart()
        
        if(auth.currentUser != null) {
            switchToMainActivity()
        }
    }

    private fun initFields() {
        auth = Firebase.auth
    }

    private fun setListener() {
        binding.textHaveNewAccount.setOnClickListener {
            switchToSignUpActivity()
        }
        
        binding.buttonSignIn.setOnClickListener { 
            if (isValidate()) {
                signIn()
            }
        }
    }

    private fun isValidate(): Boolean {
        val email = binding.inputEmail.text.toString()
        val password = binding.inputPassword.text.toString()

        if (email.trim().isEmpty()) {
            showMessage("Enter email address...")
            return false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showMessage("Enter valid email address...")
            return false
        }

        if (password.trim().isEmpty()) {
            showMessage("Enter password...")
            return false
        }

        return true
    }

    private fun signIn() {
        val email = binding.inputEmail.text.toString()
        val password = binding.inputPassword.text.toString()

        loaded(true)

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    loaded(false)
                    switchToMainActivity()
                } else {
                    loaded(false)
                    showMessage("Authentication failed.")
                }
            }
    }
    
    private fun showMessage(message: String) {
        Toast.makeText(this@SignInActivity, message, Toast.LENGTH_SHORT).show()
    }

    private fun loaded(enabled: Boolean) {
        if (enabled) {
            binding.buttonSignIn.visibility = View.INVISIBLE
            binding.progressBarSignIn.visibility = View.VISIBLE
        } else {
            binding.progressBarSignIn.visibility = View.INVISIBLE
            binding.buttonSignIn.visibility = View.VISIBLE
        }
    }

    private fun switchToSignUpActivity() {
        val intent = Intent(this@SignInActivity, SignUpActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK.or(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
    }

    private fun switchToMainActivity() {
        val intent = Intent(this@SignInActivity, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK.or(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
    }

}