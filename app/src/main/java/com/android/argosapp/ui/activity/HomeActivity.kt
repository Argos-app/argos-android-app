package com.android.argosapp.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.android.argosapp.auth.manager.ArgosApiAuthManager
import com.android.argosapp.auth.manager.FirebaseAuthManager
import com.android.argosapp.auth.session.UserSessionStorage
import com.android.argosapp.data.api.FirebaseRepository
import com.android.argosapp.databinding.ActivityHomeBinding

class HomeActivity : ComponentActivity() {
    private val firebaseAuthManager by lazy { FirebaseAuthManager(FirebaseRepository()) }
    private val argosApiAuthManager by lazy {
        ArgosApiAuthManager(
            ArgosApiRepository(homologation = true),
            UserSessionStorage(applicationContext),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.homeGreeting.text = "Olá, ${argosApiAuthManager.getUser()?.userName.orEmpty()}"
        binding.signOutButton.setOnClickListener {
            argosApiAuthManager.clearSession()
            firebaseAuthManager.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}
