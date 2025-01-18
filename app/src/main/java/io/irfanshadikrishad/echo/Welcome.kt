package io.irfanshadikrishad.echo

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class Welcome : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if the user is already logged in
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            // User is logged in, redirect to DashboardActivity
            val intent = Intent(this, DashboardActivity::class.java)
            startActivity(intent)
            finish() // Close the Welcome activity
            return
        }

        // If not logged in, show the Welcome activity
        setContentView(R.layout.activity_welcome)

        // Find the forward icon ImageView
        val forwardIcon = findViewById<ImageView>(R.id.forward_icon)

        // Set an onClick listener for the forward icon
        forwardIcon.setOnClickListener {
            // Navigate to LoginActivity
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }
}
