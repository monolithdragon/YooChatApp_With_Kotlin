package com.monolithdragon.yoochat.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.monolithdragon.yoochat.R
import com.monolithdragon.yoochat.databinding.ActivitySignInBinding
import com.monolithdragon.yoochat.utilities.Constants
import com.monolithdragon.yoochat.utilities.PreferenceManager

class SignInActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignInBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseFirestore
    private lateinit var preferenceManager: PreferenceManager

    private var email:String? = null
    private var password: String? = null

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
        database = Firebase.firestore
        preferenceManager = PreferenceManager(this@SignInActivity)
    }

    private fun setListener() {
        binding.textHaveNewAccount.setOnClickListener {
            switchToSignUpActivity()
        }

        binding.forgotPassword.setOnClickListener {
            resetPassword()
        }
        
        binding.buttonSignIn.setOnClickListener {
            email = binding.inputEmail.text.toString()
            password = binding.inputPassword.text.toString()

            if (isValidate()) {
                signIn()
            }
        }
    }

    private fun isValidate(): Boolean {

        if (email?.trim()!!.isEmpty()) {
            showMessage("Enter email address...")
            return false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email!!).matches()) {
            showMessage("Enter valid email address...")
            return false
        }

        if (password?.trim()!!.isEmpty()) {
            showMessage("Enter password...")
            return false
        }

        return true
    }

    private fun signIn() {

        loaded(true)

        auth.signInWithEmailAndPassword(email!!, password!!)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    setUserPreferenceDataFromDatabase()
                } else {
                    loaded(false)
                    showMessage("Authentication failed.")
                }
            }
    }

    private fun setUserPreferenceDataFromDatabase() {
        database.collection(Constants.KEY_COLLECTION_USERS)
            .whereEqualTo(Constants.KEY_USER_EMAIL, email)
            .get()
            .addOnSuccessListener { snapshots ->
                val document = snapshots.documents[0]

                preferenceManager.putString(Constants.KEY_USER_ID, document.id)
                preferenceManager.putString(Constants.KEY_USER_NAME, document.getString(Constants.KEY_USER_NAME)!!)
                preferenceManager.putString(Constants.KEY_USER_IMAGE, document.getString(Constants.KEY_USER_IMAGE)!!)

                loaded(false)
                switchToMainActivity()
            }
            .addOnFailureListener { exception ->
                loaded(false)
                showMessage(exception.message!!)
            }
    }

    private fun resetPassword() {
        val resetEmail = EditText(this@SignInActivity)
        val passwordResetDialog = AlertDialog.Builder(this@SignInActivity)
        passwordResetDialog.setTitle(getString(R.string.reset_password))
        passwordResetDialog.setMessage(getString(R.string.enter_your_email))
        passwordResetDialog.setView(resetEmail)

        passwordResetDialog.setPositiveButton(getString(R.string.reset_dialog_yes)) { dialog, _ ->
            val email = resetEmail.text.toString()

            if (email.trim().isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                auth.sendPasswordResetEmail(email)
                    .addOnSuccessListener {
                        showMessage(getString(R.string.reset_link_sent))
                        dialog.dismiss()
                    }
                    .addOnFailureListener {
                        showMessage(getString(R.string.reset_link_not_sent) + it.message)
                        dialog.dismiss()
                    }
            }
        }

        passwordResetDialog.setNegativeButton(getString(R.string.reset_dialog_cancel)) { dialog, _ ->
            // close the dialog
            dialog.cancel()
        }

        passwordResetDialog.create().show()
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