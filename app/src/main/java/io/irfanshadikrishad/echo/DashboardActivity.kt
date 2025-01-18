package io.irfanshadikrishad.echo

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class DashboardActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // Initialize FirebaseAuth and Firestore
        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // TextViews for user details
        val nameTextView = findViewById<TextView>(R.id.user_name)
        val emailTextView = findViewById<TextView>(R.id.user_email)
        val phoneTextView = findViewById<TextView>(R.id.user_phone)

        // Logout Button
        val logoutButton = findViewById<Button>(R.id.logout_button)

        // Get user details passed from LoginActivity
        val name = intent.getStringExtra("name")
        val email = intent.getStringExtra("email")
        val phone = intent.getStringExtra("phone")

        // Display user details
        if (name != null && email != null && phone != null) {
            nameTextView.text = buildString {
                append("Name: ")
                append(name)
            }
            emailTextView.text = buildString {
                append("Email: ")
                append(email)
            }
            phoneTextView.text = buildString {
                append("Phone: ")
                append(phone)
            }
        } else {
            // Fetch from Firestore if data not passed
            fetchUserDetails(nameTextView, emailTextView, phoneTextView)
        }

        // Logout functionality
        logoutButton.setOnClickListener {
            firebaseAuth.signOut() // Sign out from Firebase
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
            // Navigate back to LoginActivity
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Clear back stack
            startActivity(intent)
            finish()
        }
    }

    // Fetch user details from Firestore
    private fun fetchUserDetails(
        nameTextView: TextView,
        emailTextView: TextView,
        phoneTextView: TextView
    ) {
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            firestore.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        // Extract user details
                        val name = document.getString("name")
                        val email = document.getString("email")
                        val phone = document.getString("phone")

                        // Update TextViews
                        nameTextView.text = buildString {
                            append("Name: ")
                            append(name)
                        }
                        emailTextView.text = buildString {
                            append("Email: ")
                            append(email)
                        }
                        phoneTextView.text = buildString {
                            append("Phone: ")
                            append(phone)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    nameTextView.text = buildString {
                        append("Failed to fetch user details: ")
                        append(e.message)
                    }
                }
        }
    }
}
