package com.android.argosapp.view.activity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.android.argosapp.ArgosApplication
import com.android.argosapp.R
import com.android.argosapp.util.auth.AuthManager
import com.android.argosapp.util.session.UserSessionStorage
import com.android.argosapp.data.repository.FirebaseRepository
import com.android.argosapp.databinding.ActivityPasswordResetBinding
import com.android.argosapp.view.fragment.PasswordChangedFragment
import com.android.argosapp.view.fragment.ResetChangePasswordFragment
import com.android.argosapp.view.fragment.ResetEmailSentFragment
import com.android.argosapp.view.fragment.ResetRequestFragment
import com.android.argosapp.viewmodel.RedefinePasswordViewModel

class PasswordResetActivity : FragmentActivity() {
    private lateinit var binding: ActivityPasswordResetBinding
    private val authManager by lazy {
        AuthManager(
            (application as ArgosApplication).argosApiRepository,
            FirebaseRepository(),
            UserSessionStorage(applicationContext),
        )
    }
    val viewModel by lazy {
        ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                RedefinePasswordViewModel(authManager, applicationContext) as T
        })[RedefinePasswordViewModel::class.java]
    }
    private var resetCode = ""
    private var resetEmail = ""
    private var displayedStep: Step? = null
    private var resendAvailableAtMillis = 0L
    private val resendTimerHandler = Handler(Looper.getMainLooper())
    private val resendTimerTick = object : Runnable {
        override fun run() = refreshResendCountdown()
    }

    enum class Step { REQUEST, EMAIL_SENT, CHANGE_PASSWORD, SUCCESS }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        resendAvailableAtMillis = savedInstanceState?.getLong(STATE_RESEND_AVAILABLE_AT) ?: 0L
        resetEmail = savedInstanceState?.getString(STATE_RESET_EMAIL).orEmpty()
        binding = ActivityPasswordResetBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.loadingMessage.text = "Carregando..."
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = goBack()
        })
        if (savedInstanceState == null) {
            showStep(Step.REQUEST)
        } else {
            displayedStep = when (supportFragmentManager.findFragmentById(R.id.passwordResetContainer)) {
                is ResetRequestFragment -> Step.REQUEST
                is ResetEmailSentFragment -> Step.EMAIL_SENT
                is ResetChangePasswordFragment -> Step.CHANGE_PASSWORD
                is PasswordChangedFragment -> Step.SUCCESS
                else -> null
            }
        }
        refreshResendCountdown()
        authManager.extractPasswordResetCode(intent)?.let(::verifyCode)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putLong(STATE_RESEND_AVAILABLE_AT, resendAvailableAtMillis)
        outState.putString(STATE_RESET_EMAIL, resetEmail)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        resendTimerHandler.removeCallbacks(resendTimerTick)
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        authManager.extractPasswordResetCode(intent)?.let(::verifyCode)
    }

    fun goBack() {
        when (displayedStep) {
            Step.REQUEST, null -> finish()
            Step.SUCCESS -> navigateToLogin()
            else -> {
                supportFragmentManager.popBackStack()
                displayedStep = when (displayedStep) { Step.CHANGE_PASSWORD -> Step.EMAIL_SENT; else -> Step.REQUEST }
                refreshResendCountdown()
            }
        }
    }

    fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
        finish()
    }

    fun requestEmail(email: String) {
        resetEmail = email
        viewModel.sendResetEmail(email, ::showLoading, {
            startResendCooldown()
            showStep(Step.EMAIL_SENT)
        }, ::showError)
    }

    fun resendEmail() {
        if (System.currentTimeMillis() < resendAvailableAtMillis) {
            refreshResendCountdown()
            return
        }

        viewModel.sendResetEmail(resetEmail, ::showLoading, {
            startResendCooldown()
            showStep(Step.EMAIL_SENT)
        }, ::showError)
    }

    fun changePassword(password: String, confirmation: String) = viewModel.changePassword(resetCode, password, confirmation, ::showLoading, { showStep(Step.SUCCESS) }, ::showError)
    fun openEmailApp() = viewModel.openEmailApp(::showError)

    private fun verifyCode(code: String) = viewModel.verifyResetCode(code, ::showLoading, {
        resetCode = it
        showStep(Step.CHANGE_PASSWORD)
    }, ::showError)

    private fun showStep(step: Step) {
        val fragment = supportFragmentManager.findFragmentById(R.id.passwordResetContainer)
        if (displayedStep != step || fragment == null) {
            val transaction = supportFragmentManager.beginTransaction().replace(R.id.passwordResetContainer, fragmentFor(step))
            if (displayedStep != null && step != Step.REQUEST) transaction.addToBackStack(step.name)
            transaction.commit()
            displayedStep = step
        }
    }

    private fun showLoading(loading: Boolean) {
        binding.loadingOverlay.visibility = if (loading) View.VISIBLE else View.GONE
    }

    fun refreshResendCountdown() {
        resendTimerHandler.removeCallbacks(resendTimerTick)

        val remainingMillis = (resendAvailableAtMillis - System.currentTimeMillis()).coerceAtLeast(0L)
        val remainingSeconds = ((remainingMillis + 999L) / 1_000L).toInt()
        val emailSentFragment = supportFragmentManager.findFragmentById(R.id.passwordResetContainer) as? ResetEmailSentFragment
        if (emailSentFragment != null && emailSentFragment.view != null) {
            emailSentFragment.updateResendCountdown(remainingSeconds)
        }

        if (remainingMillis > 0L) {
            resendTimerHandler.postDelayed(resendTimerTick, minOf(remainingMillis, 1_000L))
        }
    }

    private fun startResendCooldown() {
        resendAvailableAtMillis = System.currentTimeMillis() + RESEND_COOLDOWN_MILLIS
        refreshResendCountdown()
    }

    private fun showError(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()

    private fun fragmentFor(step: Step): Fragment = when (step) {
        Step.REQUEST -> ResetRequestFragment()
        Step.EMAIL_SENT -> ResetEmailSentFragment()
        Step.CHANGE_PASSWORD -> ResetChangePasswordFragment()
        Step.SUCCESS -> PasswordChangedFragment()
    }

    private companion object {
        const val RESEND_COOLDOWN_MILLIS = 60_000L
        const val STATE_RESEND_AVAILABLE_AT = "resend_available_at_millis"
        const val STATE_RESET_EMAIL = "reset_email"
    }

}
