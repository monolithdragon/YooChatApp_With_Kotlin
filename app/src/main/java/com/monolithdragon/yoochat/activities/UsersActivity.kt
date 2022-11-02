package com.monolithdragon.yoochat.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.monolithdragon.yoochat.R
import com.monolithdragon.yoochat.adapters.UserAdapter
import com.monolithdragon.yoochat.databinding.ActivityUsersBinding
import com.monolithdragon.yoochat.listeners.UserListener
import com.monolithdragon.yoochat.models.User
import com.monolithdragon.yoochat.utilities.Constants
import com.monolithdragon.yoochat.utilities.PreferenceManager

class UsersActivity : BaseActivity(), UserListener {
    private lateinit var binding: ActivityUsersBinding
    private lateinit var database: FirebaseFirestore
    private lateinit var preferenceManager: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityUsersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initFields()
        setListeners()
        setUserDataFromDatabase()
    }

    private fun initFields() {
        database = Firebase.firestore
        preferenceManager = PreferenceManager(this@UsersActivity)
    }

    private fun setListeners() {
        binding.imageBack.setOnClickListener {
            switchBackToMainActivity()
        }
    }

    private fun setUserDataFromDatabase() {
        loaded(true)

        database.collection(Constants.KEY_COLLECTION_USERS)
            .get()
            .addOnSuccessListener { documents ->
                val currentUserId = preferenceManager.getString(Constants.KEY_USER_ID)
                val users: MutableList<User> = mutableListOf()

                for (document in documents) {
                    if (currentUserId == document.id) {
                        continue
                    }

                    val user = User()
                    user.id = document.id
                    user.name = document.getString(Constants.KEY_USER_NAME)
                    user.email = document.getString(Constants.KEY_USER_EMAIL)
                    user.profileImage = document.getString((Constants.KEY_USER_IMAGE))
                    user.token = document.getString(Constants.KEY_USER_TOKEN)

                    users.add(user)
                }

                if (users.isNotEmpty()) {
                    users.sortWith { x, y -> x.name!!.compareTo(y.name!!) }
                    val isOnline = preferenceManager.getBoolean(Constants.KEY_USER_ONLINE)!!

                    val adapter = UserAdapter(users, this@UsersActivity, isOnline)
                    binding.usersRecycleView.adapter = adapter
                    binding.usersRecycleView.visibility = View.VISIBLE
                } else {
                    showErrorMessage()
                }

                loaded(false)
            }
            .addOnFailureListener {
                loaded(false)
                showErrorMessage()
            }
    }

    private fun loaded(enabled: Boolean) {
        if (enabled) {
            binding.progressBar.visibility = View.VISIBLE
        } else {
            binding.progressBar.visibility = View.INVISIBLE
        }
    }

    private fun showErrorMessage() {
        binding.textErrorMessage.text = String.format("%s", getString(R.string.no_user_available))
        binding.textErrorMessage.visibility = View.VISIBLE
    }

    private fun switchBackToMainActivity() {
        val intent = Intent(this@UsersActivity, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK.or(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
    }

    private fun switchToChatActivity(user: User) {
        val intent = Intent(this@UsersActivity, ChatActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK.or(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        intent.putExtra(Constants.KEY_RECEIVER_USER, user)
        startActivity(intent)
    }

    override fun onClickListener(user: User) {
        switchToChatActivity(user)
        finish()
    }
}