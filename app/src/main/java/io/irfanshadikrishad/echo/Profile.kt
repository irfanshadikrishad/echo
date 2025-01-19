package io.irfanshadikrishad.echo

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso

class Profile : Fragment() {
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var profileAvatar: ImageView
    private lateinit var profileName: TextView
    private lateinit var profileEmail: TextView
    private lateinit var updateButton: Button
    private lateinit var logoutButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        // Initialize FirebaseAuth and Firestore
        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Initialize UI components
        profileAvatar = view.findViewById(R.id.profile_avatar)
        profileName = view.findViewById(R.id.profile_name)
        profileEmail = view.findViewById(R.id.profile_email)
        updateButton = view.findViewById(R.id.profile_update)
        logoutButton = view.findViewById(R.id.profile_logout)

        // Fetch and display user details
        fetchUserDetails()

        // Handle logout button
        logoutButton.setOnClickListener {
            firebaseAuth.signOut()
            Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
            // Redirect to LoginActivity
            val intent = Intent(requireContext(), LoginActivity::class.java).apply {
                flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Clear the back stack
            }
            startActivity(intent)
        }

        // Handle update button
        updateButton.setOnClickListener {
            Toast.makeText(
                requireContext(), "Update functionality not implemented", Toast.LENGTH_SHORT
            ).show()
        }

        return view
    }

    private fun fetchUserDetails() {
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            firestore.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        // Get user details
                        val name = document.getString("name")
                        val email = document.getString("email")
                        val avatarUrl =
                            document.getString("avatarUrl") // Assuming you store an avatar URL

                        // Update UI
                        profileName.text = name ?: "N/A"
                        profileEmail.text = email ?: "N/A"

                        // Load avatar using Picasso or Glide
                        if (!avatarUrl.isNullOrEmpty()) {
                            Picasso.get().load(avatarUrl).into(profileAvatar)
                        } else {
                            profileAvatar.setImageResource(R.drawable.default_avatar) // Default avatar
                        }
                    } else {
                        Toast.makeText(
                            requireContext(), "User details not found", Toast.LENGTH_SHORT
                        ).show()
                    }
                }.addOnFailureListener { exception ->
                    Toast.makeText(
                        requireContext(),
                        "Failed to fetch user details: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        } else {
            Toast.makeText(requireContext(), "No authenticated user", Toast.LENGTH_SHORT).show()
        }
    }
}
