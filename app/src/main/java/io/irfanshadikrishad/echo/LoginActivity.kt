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
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize FirebaseAuth and Firestore
        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Input fields
        val emailInput = findViewById<TextInputEditText>(R.id.email)
        val passwordInput = findViewById<TextInputEditText>(R.id.password)

        // Login button
        val loginButton = findViewById<MaterialButton>(R.id.login_button)

        // Login button click listener
        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (validateInput(email, password)) {
                loginUser(email, password)
            }
        }

        //Redirect to register
        val register = findViewById<TextView>(R.id.redirect_register)
        register.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }

    // Login user with Firebase
    private fun loginUser(email: String, password: String) {
        firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Login successful
                val user = firebaseAuth.currentUser
                if (user != null) {
                    if (user.isEmailVerified) {
                        // Fetch user details from Firestore
                        fetchUserDetails(user.uid)
                    } else {
                        // User is not verified
                        Toast.makeText(
                            this, "Please verify your email before logging in.", Toast.LENGTH_LONG
                        ).show()
                        firebaseAuth.signOut() // Log out the user
                    }
                }
            } else {
                // Login failed
                Toast.makeText(
                    this, "Login Failed: ${task.exception?.message}", Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // Fetch user details from Firestore
    private fun fetchUserDetails(uid: String) {
        firestore.collection("users").document(uid).get().addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                // Extract user details
                val name = document.getString("name")
                val email = document.getString("email")
                val phone = document.getString("phone")

                // Show a success message
                Toast.makeText(this, "Welcome, $name!", Toast.LENGTH_SHORT).show()

                // Navigate to the dashboard activity
                val intent = Intent(this, DashboardActivity::class.java).apply {
                    putExtra("name", name)
                    putExtra("email", email)
                    putExtra("phone", phone)
                }
                startActivity(intent)
            } else {
                Toast.makeText(this, "User details not found!", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { e ->
            Toast.makeText(
                this, "Failed to fetch user details: ${e.message}", Toast.LENGTH_LONG
            ).show()
        }
    }

    // Validation function
    private fun validateInput(email: String, password: String): Boolean {
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError(R.id.textInputLayout_email, "Enter a valid email")
            return false
        }

        if (password.isEmpty() || password.length < 6) {
            showError(R.id.textInputLayout_password, "Password must be at least 6 characters")
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
