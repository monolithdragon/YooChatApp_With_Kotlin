package com.monolithdragon.yoochat.activities

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.monolithdragon.yoochat.R
import com.monolithdragon.yoochat.databinding.ActivitySignUpBinding
import com.monolithdragon.yoochat.utilities.Constants
import com.monolithdragon.yoochat.utilities.PreferenceManager
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException

class SignUpActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignUpBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseFirestore
    private lateinit var preferenceManager: PreferenceManager

    private var name: String? = null
    private var email:String? = null
    private var password: String? = null
    private var confirmedPassword: String? = null
    private var encodedProfileImage: String? = null

    private val selectProfileImage: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                val imageUri = result.data?.data

                try {
                    val inputStream = contentResolver.openInputStream(imageUri!!)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    binding.imageProfile.setImageBitmap(bitmap)
                    binding.imgAddImage.visibility = View.GONE
                    encodedProfileImage = encodeImage(bitmap)

                } catch (e: FileNotFoundException) {
                    e.printStackTrace()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initFields()
        setListeners()
    }

    private fun initFields() {
        auth = Firebase.auth
        database = Firebase.firestore
        preferenceManager = PreferenceManager(this@SignUpActivity)
    }

    private fun setListeners() {
        binding.textAlreadyHaveAccount.setOnClickListener {
            switchToSignInActivity()
        }

        binding.buttonSignUp.setOnClickListener {
            name = binding.inputName.text.toString()
            email = binding.inputEmail.text.toString()
            password = binding.inputPassword.text.toString()
            confirmedPassword = binding.inputConfirmPassword.text.toString()

            if (isValidate()) {
                signUp()
            }
        }

        binding.imageProfile.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            selectProfileImage.launch(intent)
        }
    }

    private fun signUp() {
        loaded(true)

        auth.createUserWithEmailAndPassword(email!!, password!!)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    addUserToDatabase()
                } else {
                    loaded(false)
                    showMessage("Authorization failed")
                }
            }
    }

    private fun addUserToDatabase() {
        val user = hashMapOf(
            Constants.KEY_USER_NAME to name,
            Constants.KEY_USER_EMAIL to email,
            Constants.KEY_USER_IMAGE to encodedProfileImage
        )

        database.collection(Constants.KEY_COLLECTION_USERS)
            .add(user)
            .addOnSuccessListener { documentReference ->
                loaded(false)

                preferenceManager.putString(Constants.KEY_USER_ID, documentReference.id)
                preferenceManager.putString(Constants.KEY_USER_NAME, name!!)
                preferenceManager.putString(Constants.KEY_USER_IMAGE, encodedProfileImage!!)

                switchToMainActivity()
            }
            .addOnFailureListener { exception ->
                loaded(false)
                showMessage(exception.message!!)
            }
    }

    private fun isValidate(): Boolean {

        if (encodedProfileImage.isNullOrEmpty()) {
            encodedProfileImage =
                encodeImage(BitmapFactory.decodeResource(resources, R.drawable.profile_image))
        }

        if (name?.trim()!!.isEmpty()) {
            showMessage("Enter username...")
            return false
        }

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

        if (confirmedPassword?.trim()!!.isEmpty()) {
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