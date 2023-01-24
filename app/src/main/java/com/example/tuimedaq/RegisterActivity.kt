package com.example.tuimedaq

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.renderscript.Sampler
import android.util.Patterns
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.tuimedaq.databinding.ActivityLoginBinding
import com.example.tuimedaq.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.jakewharton.rxbinding2.widget.RxTextView
import io.reactivex.Flowable.combineLatest
import io.reactivex.Flowable.merge
import io.reactivex.Observable.combineLatest
import java.util.*
import io.reactivex.Observable

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth:FirebaseAuth
    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        val nameStream = RxTextView.textChanges (binding.etFullname)
            .skipInitialValue()
            .map { name ->
                name.isEmpty()
            }
        nameStream.subscribe {
            showNameExistAlert(it)
        }

        val emailStream = RxTextView.textChanges(binding.etEmail)
            .skipInitialValue()
            .map { email ->
                !Patterns.EMAIL_ADDRESS.matcher(email).matches()
            }
        emailStream.subscribe {
            showEmailValidAlert(it)
        }

        val usernameStream = RxTextView.textChanges(binding.etUsername)
            .skipInitialValue()
            .map { username ->
            username.length < 6
        }

        usernameStream.subscribe {
            showTextMinimalAlert(it, "Username")
        }

        val passwordStream = RxTextView.textChanges(binding.etPassword)
            .skipInitialValue()
            .map { password ->
                password.length < 6
            }
        passwordStream.subscribe {
            showTextMinimalAlert(it, "Password")
        }

        val passwordConfirmStream = Observable.merge(
            RxTextView.textChanges(binding.etPassword)
                .skipInitialValue()
                .map { password ->
            password.toString() != binding.etConfirmPassword.text.toString()
        },
        RxTextView.textChanges(binding.etConfirmPassword)
            .skipInitialValue ()
            .map { confirmPassword ->
                confirmPassword.toString() != binding.etPassword.text.toString()
            })
        passwordConfirmStream.subscribe {
            showPasswordConfirmAlert(it)
        }

        val invalidFieldsStream = Observable.combineLatest (
            nameStream,
            emailStream,
            usernameStream,
            passwordStream,
            passwordConfirmStream
        ) { nameInvalid: Boolean, emailInvalid: Boolean, usernameInvalid: Boolean, passwordInvalid: Boolean, passwordConfirmInvalid: Boolean ->
            !nameInvalid && !emailInvalid && !usernameInvalid && !passwordInvalid && !passwordConfirmInvalid
        }
        invalidFieldsStream.subscribe { isValid ->
            if (isValid) {
                binding.btnRegister.isEnabled = true
                binding.btnRegister.backgroundTintList =
                    ContextCompat.getColorStateList(this, R.color.blue_violet)
            } else {
                binding.btnRegister.isEnabled = false
                binding.btnRegister.backgroundTintList =
                    ContextCompat.getColorStateList(this, android.R.color.darker_gray)
            }
        }


        binding.btnRegister.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            registerUser(email,password)
        }
        binding.tvHaveAccount.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }

    private fun showNameExistAlert(isNotValid: Boolean) {
        binding.etFullname.error = if (isNotValid) "The name cannot be empty!" else null
    }

    private fun showTextMinimalAlert(isNotValid: Boolean, text: String) {
        if (text == "Username")
            binding.etUsername.error = if (isNotValid) "$text contain at least 6 digits!" else null
        else if (text == "Password")
            binding.etPassword.error = if (isNotValid) "$text contain at least 8 digits!" else null
    }

    private fun showEmailValidAlert(isNotValid: Boolean) {
        binding.etEmail.error = if (isNotValid) "Invalid email!" else null
    }

    private fun showPasswordConfirmAlert(isNotValid: Boolean){
    binding.etConfirmPassword.error = if (isNotValid) "Invalid password!" else null
}
    private fun registerUser(email:String, password:String){
        auth.createUserWithEmailAndPassword(email,password)
            .addOnCompleteListener(this){
                if(it.isSuccessful) {
                    startActivity(Intent(this, LoginActivity::class.java))
                    Toast.makeText(this,"Register Successful",Toast.LENGTH_SHORT).show()
                } else{
                    Toast.makeText(this,it.exception?.message,Toast.LENGTH_SHORT).show()
                }
            }
    }
}