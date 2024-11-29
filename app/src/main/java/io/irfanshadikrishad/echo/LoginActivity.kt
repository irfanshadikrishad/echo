package io.irfanshadikrishad.echo

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

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
                // Validation passed
                Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show()

                // Navigate to another activity (e.g., DashboardActivity)
                val intent = Intent(this, DashboardActivity::class.java)
                startActivity(intent)
            }
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
