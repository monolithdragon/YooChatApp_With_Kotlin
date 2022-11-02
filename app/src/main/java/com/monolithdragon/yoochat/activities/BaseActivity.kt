package com.monolithdragon.yoochat.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.monolithdragon.yoochat.utilities.Constants
import com.monolithdragon.yoochat.utilities.PreferenceManager

open class BaseActivity : AppCompatActivity() {
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var database: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initFields()
    }

    private fun initFields() {
        preferenceManager = PreferenceManager(this@BaseActivity)
        database = Firebase.firestore
    }

    override fun onPause() {
        super.onPause()
        preferenceManager.putBoolean(Constants.KEY_USER_ONLINE, false)
        database.collection(Constants.KEY_COLLECTION_USERS)
            .document(preferenceManager.getString(Constants.KEY_USER_ID)!!)
            .update(Constants.KEY_USER_ONLINE, false)
    }

    override fun onResume() {
        super.onResume()
        preferenceManager.putBoolean(Constants.KEY_USER_ONLINE, true)
        database.collection(Constants.KEY_COLLECTION_USERS)
            .document(preferenceManager.getString(Constants.KEY_USER_ID)!!)
            .update(Constants.KEY_USER_ONLINE, true)
    }
}