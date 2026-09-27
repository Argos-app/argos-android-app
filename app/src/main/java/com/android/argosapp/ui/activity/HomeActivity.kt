package com.android.argosapp.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.android.argosapp.auth.manager.AuthManager
import com.android.argosapp.auth.session.UserSessionStorage
import com.android.argosapp.data.api.ArgosApiRepository
import com.android.argosapp.data.api.FirebaseRepository
import com.android.argosapp.databinding.ActivityHomeBinding

class HomeActivity : ComponentActivity() {
    private val authManager by lazy {
        AuthManager(
            ArgosApiRepository(homologation = true),
            FirebaseRepository(),
            UserSessionStorage(applicationContext),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.homeGreeting.text = "Olá, ${authManager.getUser()?.userName.orEmpty()}"
        binding.signOutButton.setOnClickListener {
            authManager.clearSession()
            authManager.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}
