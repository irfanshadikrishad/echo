package io.irfanshadikrishad.echo

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.cloudinary.utils.ObjectUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.jakewharton.picasso.OkHttp3Downloader
import com.squareup.picasso.Picasso
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

class Profile : Fragment() {
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var profileAvatar: ImageView
    private lateinit var profileName: TextView
    private lateinit var profileEmail: TextView
    private lateinit var editButton: Button
    private lateinit var logoutButton: Button

    private lateinit var selectedAvatarUri: Uri
    private lateinit var pickImageLauncher: ActivityResultLauncher<String>
    private lateinit var picassoInstance: Picasso

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
        editButton = view.findViewById(R.id.profile_edit)
        logoutButton = view.findViewById(R.id.profile_logout)

        // Initialize OkHttpClient and configure Picasso
        initializePicasso()

        // Initialize MediaManager if not already initialized
        if (!isMediaManagerInitialized()) {
            initializeCloudinary()
        }

        // Register the ActivityResultLauncher for picking an image
        pickImageLauncher =
            registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                if (uri != null) {
                    selectedAvatarUri = uri
                }
            }

        // Fetch and display user details
        fetchUserDetails()

        // Handle logout button
        logoutButton.setOnClickListener {
            firebaseAuth.signOut()
            Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
            // Redirect to LoginActivity
            val intent = Intent(requireContext(), LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
        }

        // Handle update button
        editButton.setOnClickListener {
            showEditProfileDialog()
        }

        return view
    }

    private fun initializePicasso() {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient =
            OkHttpClient.Builder().addInterceptor(logging).connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS).writeTimeout(30, TimeUnit.SECONDS).build()

        picassoInstance =
            Picasso.Builder(requireContext()).downloader(OkHttp3Downloader(okHttpClient)).build()
    }

    private fun fetchUserDetails() {
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            firestore.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { document ->
                    if (!isAdded) return@addOnSuccessListener // Ensure fragment is attached
                    if (document != null && document.exists()) {
                        val name = document.getString("name")
                        val email = document.getString("email")
                        val avatarUrl = document.getString("avatarUrl")

                        Log.d("Profile", "Fetched user details: $name $email $avatarUrl")

                        profileName.text = name ?: "N/A"
                        profileEmail.text = email ?: "N/A"

                        if (!avatarUrl.isNullOrEmpty()) {
                            Picasso.get().load(avatarUrl).into(profileAvatar)
                        } else {
                            profileAvatar.setImageResource(R.drawable.default_avatar)
                        }
                    } else {
                        if (isAdded) {
                            Toast.makeText(
                                requireContext(), "User details not found", Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }.addOnFailureListener { exception ->
                    if (isAdded) {
                        Toast.makeText(
                            requireContext(),
                            "Failed to fetch user details: ${exception.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        } else {
            if (isAdded) {
                Toast.makeText(requireContext(), "No authenticated user", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initializeCloudinary() {
        val config = mapOf(
            "cloud_name" to "dgczy8tct",
            "api_key" to "149311275935645",
            "api_secret" to "A2SBNOvZd68nTh3082AV5PRXX7g"
        )
        MediaManager.init(requireContext(), config)
    }

    private fun isMediaManagerInitialized(): Boolean {
        return try {
            MediaManager.get()
            true
        } catch (e: IllegalStateException) {
            false
        }
    }

    private fun showEditProfileDialog() {
        val dialogView = layoutInflater.inflate(R.layout.edit_profile_layout, null)

        val editName = dialogView.findViewById<EditText>(R.id.editName)
        val editEmail = dialogView.findViewById<EditText>(R.id.editEmail)
        val editAvatar = dialogView.findViewById<ImageView>(R.id.editAvatar)
        val changeAvatarButton = dialogView.findViewById<Button>(R.id.changeAvatarButton)
        val saveButton = dialogView.findViewById<Button>(R.id.saveButton)

        val dialog =
            AlertDialog.Builder(requireContext()).setView(dialogView).setTitle("Edit profile:")
                .setCancelable(true).create()

        changeAvatarButton.setOnClickListener {
            pickImageLauncher.launch("image/*")
            if (::selectedAvatarUri.isInitialized) {
                picassoInstance.load(selectedAvatarUri).into(editAvatar)
            }
        }

        saveButton.setOnClickListener {
            val name = editName.text.toString()
            val email = editEmail.text.toString()

            if (name.isEmpty() || email.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            if (::selectedAvatarUri.isInitialized) {
                uploadAvatarToCloudinary(selectedAvatarUri, name, email)
            } else {
                updateUserProfile(name, email, null)
            }

            dialog.dismiss()
        }

        dialog.show()
    }

    private fun uploadAvatarToCloudinary(uri: Uri, name: String, email: String) {
        MediaManager.get().upload(uri).options(
            ObjectUtils.asMap(
                "folder", "user_avatars", "public_id", firebaseAuth.currentUser?.uid
            )
        ).callback(object : UploadCallback {
            override fun onStart(requestId: String?) {}
            override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}
            override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                val avatarUrl = resultData?.get("url") as? String
                updateUserProfile(name, email, avatarUrl)
            }

            override fun onError(requestId: String?, error: ErrorInfo?) {
                Toast.makeText(
                    requireContext(),
                    "Avatar upload failed: ${error?.description}",
                    Toast.LENGTH_SHORT
                ).show()
            }

            override fun onReschedule(requestId: String?, error: ErrorInfo?) {}
        }).dispatch()
    }

    private fun updateUserProfile(name: String, email: String, avatarUrl: String?) {
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            val userMap = hashMapOf(
                "name" to name, "email" to email
            )
            avatarUrl?.let {
                userMap["avatarUrl"] = it
            }

            firestore.collection("users").document(currentUser.uid).set(userMap)
                .addOnSuccessListener {
                    Toast.makeText(
                        requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT
                    ).show()
                    fetchUserDetails()
                }.addOnFailureListener { exception ->
                    Toast.makeText(
                        requireContext(),
                        "Failed to update profile: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }
}
