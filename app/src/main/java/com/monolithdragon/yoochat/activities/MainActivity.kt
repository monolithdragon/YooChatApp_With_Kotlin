package com.monolithdragon.yoochat.activities

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import com.monolithdragon.yoochat.R
import com.monolithdragon.yoochat.databinding.ActivityMainBinding
import com.monolithdragon.yoochat.utilities.Constants
import com.monolithdragon.yoochat.utilities.PreferenceManager

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var database: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initFields()
        setListeners()
        loadUserDetails()
        getToken()
    }

    private fun setListeners() {
        binding.imageSignOut.setOnClickListener {
            signOut()
        }

        binding.fabUsers.setOnClickListener {
            switchToUsersActivity()
        }
    }

    private fun initFields() {
        auth = Firebase.auth
        preferenceManager = PreferenceManager(this@MainActivity)
        database = Firebase.firestore
    }

    private fun loadUserDetails() {
        binding.textName.text = preferenceManager.getString(Constants.KEY_USER_NAME)
        binding.imageProfile.setImageBitmap(
            getUserImage(
                preferenceManager.getString(Constants.KEY_USER_IMAGE)
            )
        )
    }

    private fun getToken() {
        Firebase.messaging.token.addOnSuccessListener(this::updateToken)
    }

    private fun updateToken(token: String) {
        preferenceManager.putString(Constants.KEY_USER_TOKEN, token)

        database.collection(Constants.KEY_COLLECTION_USERS)
            .document(preferenceManager.getString(Constants.KEY_USER_ID)!!)
            .update(Constants.KEY_USER_TOKEN, token)
            .addOnFailureListener {
                showMessage(getString(R.string.unable_update_token))
            }
    }

    private fun signOut() {
        auth.signOut()
        switchToSignInActivity()
    }

    private fun showMessage(message: String) {
        Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
    }

    private fun switchToSignInActivity() {
        val intent = Intent(this@MainActivity, SignInActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK.or(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
    }

    private fun switchToUsersActivity() {
        val intent = Intent(this@MainActivity, UsersActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK.or(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
    }

    private fun getUserImage(encodedImage: String?): Bitmap {
        val bytes = Base64.decode(encodedImage, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

}