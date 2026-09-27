package com.android.argosapp.view.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.android.argosapp.ArgosApplication
import com.android.argosapp.util.auth.AuthManager
import com.android.argosapp.util.session.UserSessionStorage
import com.android.argosapp.data.repository.FirebaseRepository
import com.android.argosapp.databinding.ActivityHomeBinding

class HomeActivity : ComponentActivity() {
    private val authManager by lazy {
        AuthManager(
            (application as ArgosApplication).argosApiRepository,
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
