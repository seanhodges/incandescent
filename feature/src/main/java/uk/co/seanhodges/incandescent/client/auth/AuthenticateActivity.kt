package uk.co.seanhodges.incandescent.client.auth

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.os.AsyncTask
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView

import kotlinx.android.synthetic.main.activity_authenticate.*
import uk.co.seanhodges.incandescent.client.Inject
import uk.co.seanhodges.incandescent.client.R
import uk.co.seanhodges.incandescent.lightwave.server.LWAuthenticatedResult
import uk.co.seanhodges.incandescent.lightwave.server.LightwaveServer
import java.lang.ref.WeakReference

class AuthenticateActivity : Activity() {

    private val server = Inject.server
    private var authTask: AuthenticateTask? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_authenticate)

        password.setOnEditorActionListener(TextView.OnEditorActionListener { _, id, _ ->
            if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                attemptLogin()
                return@OnEditorActionListener true
            }
            false
        })

        email_sign_in_button.setOnClickListener { attemptLogin() }
    }

    private fun attemptLogin() {
        if (authTask != null) {
            return
        }

        email.error = null
        password.error = null
        val emailStr = email.text.toString()
        val passwordStr = password.text.toString()

        var cancel = false
        var focusView: View? = null

        if (!TextUtils.isEmpty(passwordStr) && !isPasswordValid(passwordStr)) {
            password.error = getString(R.string.error_invalid_password)
            focusView = password
            cancel = true
        }
        if (TextUtils.isEmpty(emailStr)) {
            email.error = getString(R.string.error_field_required)
            focusView = email
            cancel = true
        } else if (!isEmailValid(emailStr)) {
            email.error = getString(R.string.error_invalid_email)
            focusView = email
            cancel = true
        }

        if (cancel) {
            focusView?.requestFocus()
        } else {
            showProgress(true)
            val authRepository = AuthRepository(WeakReference(applicationContext))
            authTask = AuthenticateTask(authRepository, emailStr, passwordStr)
            authTask!!.execute(server)
        }
    }

    private fun isEmailValid(email: String): Boolean {
        return email.length > 5 && email.contains("@")
    }

    private fun isPasswordValid(password: String): Boolean {
        return password.length > 4
    }

    private fun showProgress(show: Boolean) {
        val shortAnimTime = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()

        login_form.visibility = if (show) View.GONE else View.VISIBLE
        login_form.animate()
                .setDuration(shortAnimTime)
                .alpha((if (show) 0 else 1).toFloat())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        login_form.visibility = if (show) View.GONE else View.VISIBLE
                    }
                })

        login_progress.visibility = if (show) View.VISIBLE else View.GONE
        login_progress.animate()
                .setDuration(shortAnimTime)
                .alpha((if (show) 1 else 0).toFloat())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        login_progress.visibility = if (show) View.VISIBLE else View.GONE
                    }
                })
    }

    inner class AuthenticateTask internal constructor(
            private val authRepository: AuthRepository,
            private val emailStr: String,
            private val passwordStr: String
    ) : AsyncTask<LightwaveServer, Void, Boolean>() {

        override fun doInBackground(vararg server: LightwaveServer): Boolean? {
            try {
                Log.d(javaClass.name, "Authenticating...")
                val authResult : LWAuthenticatedResult = server[0].authenticate(emailStr, passwordStr)
                Log.d(javaClass.name, "Access token is: ${authResult.tokens.accessToken}")
                authRepository.save(authResult, emailStr, passwordStr)
            } catch (e: Exception) {
                Log.e(javaClass.name, "Authentication failed", e)
                return false
            }
            return true
        }

        override fun onPostExecute(success: Boolean?) {
            authTask = null
            showProgress(false)

            if (success!!) {
                finish()
            } else {
                password.error = getString(R.string.error_incorrect_password)
                password.requestFocus()
            }
        }

        override fun onCancelled() {
            authTask = null
            showProgress(false)
        }
    }
}
