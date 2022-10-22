package com.monolithdragon.yoochat.activities

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.monolithdragon.yoochat.R
import com.monolithdragon.yoochat.databinding.ActivitySignUpBinding
import java.io.ByteArrayOutputStream

class SignUpActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignUpBinding
    private lateinit var auth: FirebaseAuth

    private var encodedProfileImage: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initFields()
        setListeners()
    }

    private fun initFields() {
        auth = Firebase.auth
    }

    private fun setListeners() {
        binding.textAlreadyHaveAccount.setOnClickListener {
            switchToSignInActivity()
        }

        binding.buttonSignUp.setOnClickListener {
            if (isValidate()) {
                signUp()
            }
        }
    }

    private fun signUp() {
        val email = binding.inputEmail.text.toString()
        val password = binding.inputPassword.text.toString()

        loaded(true)

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    loaded(false)
                    switchToMainActivity()
                } else {
                    loaded(false)
                    showMessage("Authorization failed")
                }
            }
    }

    private fun isValidate(): Boolean {
       val name = binding.inputName.text.toString()
       val email = binding.inputEmail.text.toString()
       val password = binding.inputPassword.text.toString()
       val confirmedPassword = binding.inputConfirmPassword.text.toString()


        if (encodedProfileImage.isNullOrEmpty()) {
            encodedProfileImage =
                encodeImage(BitmapFactory.decodeResource(resources, R.drawable.profile_image))
        }

        if (name.trim().isEmpty()) {
            showMessage("Enter username...")
            return false
        }

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

        if (confirmedPassword.trim().isEmpty()) {
            showMessage("Enter confirmed password...")
            return false
        }

        if (confirmedPassword != password) {
            showMessage("Password not matched...")
            return false
        }

        return true
    }

    private fun encodeImage(bitmap: Bitmap): String {
        val previewWidth = 150
        val previewHeight = bitmap.height * previewWidth / bitmap.width
        val previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, true)
        val outputStream = ByteArrayOutputStream()
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream)
        val bytes = outputStream.toByteArray()

        return Base64.encodeToString(bytes, Base64.DEFAULT)
    }

    private fun showMessage(message: String) {
        Toast.makeText(this@SignUpActivity, message, Toast.LENGTH_SHORT).show()
    }

    private fun loaded(enabled: Boolean) {
        if (enabled) {
            binding.buttonSignUp.visibility = View.INVISIBLE
            binding.progressBarSignUp.visibility = View.VISIBLE
        } else {
            binding.progressBarSignUp.visibility = View.INVISIBLE
            binding.buttonSignUp.visibility = View.VISIBLE
        }
    }

    private fun switchToSignInActivity() {
        val intent = Intent(this@SignUpActivity, SignInActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK.or(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
    }

    private fun switchToMainActivity() {
        val intent = Intent(this@SignUpActivity, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK.or(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
    }
}