package io.irfanshadikrishad.echo

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Input fields
        val nameInput = findViewById<TextInputEditText>(R.id.name)
        val emailInput = findViewById<TextInputEditText>(R.id.email)
        val phoneInput = findViewById<TextInputEditText>(R.id.phone)
        val passwordInput = findViewById<TextInputEditText>(R.id.password)
        val confirmPasswordInput = findViewById<TextInputEditText>(R.id.confirm_password)

        // Register button
        val registerButton = findViewById<MaterialButton>(R.id.register_button)

        // Login navigation
        val loginNavigation = findViewById<TextView>(R.id.login_navigation)

        // Navigate to login activity
        loginNavigation.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        // Register button click listener
        registerButton.setOnClickListener {
            val name = nameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val phone = phoneInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            val confirmPassword = confirmPasswordInput.text.toString().trim()

            if (validateInput(name, email, phone, password, confirmPassword)) {
                registerUserWithFirebase(name, email, phone, password)
            }
        }
    }

    // Firebase registration function
    private fun registerUserWithFirebase(
        name: String,
        email: String,
        phone: String,
        password: String
    ) {
        val firebaseAuth = FirebaseAuth.getInstance()
        val firestore = FirebaseFirestore.getInstance()

        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Get the current user
                    val user = firebaseAuth.currentUser

                    // Update profile with display name
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build()

                    user?.updateProfile(profileUpdates)?.addOnCompleteListener {
                        if (it.isSuccessful) {
                            // Save additional details to Firestore
                            val userDetails = hashMapOf(
                                "uid" to user.uid,
                                "name" to name,
                                "email" to email,
                                "phone" to phone
                            )

                            firestore.collection("users").document(user.uid)
                                .set(userDetails)
                                .addOnSuccessListener {
                                    Toast.makeText(
                                        this,
                                        "Registration Successful!",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    // Navigate to login page
                                    val intent = Intent(this, LoginActivity::class.java)
                                    startActivity(intent)
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(
                                        this,
                                        "Failed to save details: ${e.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                        }
                    }
                } else {
                    // Registration failed
                    Toast.makeText(
                        this,
                        "Registration Failed: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    // Validation function
    private fun validateInput(
        name: String,
        email: String,
        phone: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        if (name.isEmpty()) {
            showError(R.id.textInputLayout_name, "Name is required")
            return false
        }

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError(R.id.textInputLayout_email, "Enter a valid email")
            return false
        }

        if (phone.isEmpty() || phone.length < 10) {
            showError(R.id.textInputLayout_phone, "Enter a valid phone number")
            return false
        }

        if (password.isEmpty() || password.length < 6) {
            showError(R.id.textInputLayout_password, "Password must be at least 6 characters")
            return false
        }

        if (password != confirmPassword) {
            showError(R.id.textInputLayout_confirm_password, "Passwords do not match")
            return false
        }

        return true
    }

    // Show error in TextInputLayout
    private fun showError(layoutId: Int, errorMessage: String) {
        val textInputLayout = findViewById<TextInputLayout>(layoutId)
        textInputLayout.error = errorMessage
        textInputLayout.requestFocus()
    }
}
