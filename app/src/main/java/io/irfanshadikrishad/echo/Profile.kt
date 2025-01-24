package io.irfanshadikrishad.echo

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.math.max

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
    private var currentDialogEditAvatar: ImageView? = null

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
                    val scaledBitmap = getScaledBitmap(uri)
                    selectedAvatarUri = uri
                    // Handle updating the ImageView in the dialog dynamically
                    updateDialogAvatar(scaledBitmap)
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

        currentDialogEditAvatar = editAvatar // Track the current ImageView for updates

        val dialog =
            AlertDialog.Builder(requireContext()).setView(dialogView).setTitle("Edit profile")
                .setCancelable(true).create()

        // Pre-fill user data
        val currentUser = firebaseAuth.currentUser
        currentUser?.let {
            firestore.collection("users").document(it.uid).get().addOnSuccessListener { document ->
                if (document.exists()) {
                    editName.setText(document.getString("name") ?: "")
                    editEmail.setText(document.getString("email") ?: "")
                    val avatarUrl = document.getString("avatarUrl")
                    if (!avatarUrl.isNullOrEmpty()) {
                        Picasso.get().load(avatarUrl).into(editAvatar)
                    }
                }
            }
        }

        // Change avatar logic
        changeAvatarButton.setOnClickListener {
            try {
                pickImageLauncher.launch("image/*")
            } catch (err: Exception) {
                Toast.makeText(
                    requireContext(), "Error: ${err.localizedMessage}", Toast.LENGTH_SHORT
                ).show()
            }
        }

        // Save button logic
        saveButton.setOnClickListener {
            val name = editName.text.toString().trim()
            val email = editEmail.text.toString().trim()

            if (name.isEmpty() && email.isEmpty() && !::selectedAvatarUri.isInitialized) {
                Toast.makeText(
                    requireContext(), "Please modify at least one field", Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            currentUser?.let { user ->
                firestore.collection("users").document(user.uid).get()
                    .addOnSuccessListener { document ->
                        if (::selectedAvatarUri.isInitialized) {
                            uploadAvatarToCloudinary(selectedAvatarUri,
                                name.ifEmpty { document.getString("name") },
                                email.ifEmpty { document.getString("email") })
                        } else {
                            updateUserProfile(
                                name.ifEmpty { document.getString("name") },
                                email.ifEmpty { document.getString("email") },
                                null
                            )
                        }
                    }
            }
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun uploadAvatarToCloudinary(uri: Uri, name: String?, email: String?) {
        MediaManager.get().upload(uri).options(
            ObjectUtils.asMap(
                "folder", "user_avatars", "public_id", firebaseAuth.currentUser?.uid
            )
        ).callback(object : UploadCallback {
            override fun onStart(requestId: String?) {}
            override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}
            override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                val avatarUrl = resultData?.get("url") as? String
                updateUserProfile(
                    name, email, avatarUrl
                )
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

    private fun updateUserProfile(name: String?, email: String?, avatarUrl: String?) {
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            // Fetch the current user details from Firestore
            firestore.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val currentName = document.getString("name")
                        val currentEmail = document.getString("email")
                        val currentAvatarUrl = document.getString("avatarUrl")

                        // Prepare the updated fields
                        val updatedUserMap = mutableMapOf<String, Any>()
                        if (!name.isNullOrEmpty() && name != currentName) {
                            updatedUserMap["name"] = name
                        }
                        if (!email.isNullOrEmpty() && email != currentEmail) {
                            updatedUserMap["email"] = email
                        }
                        if (!avatarUrl.isNullOrEmpty() && avatarUrl != currentAvatarUrl) {
                            updatedUserMap["avatarUrl"] = avatarUrl
                        }

                        // Update Firestore only if there are changes
                        if (updatedUserMap.isNotEmpty()) {
                            firestore.collection("users").document(currentUser.uid)
                                .update(updatedUserMap).addOnSuccessListener {
                                    Toast.makeText(
                                        requireContext(),
                                        "Profile updated successfully",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    fetchUserDetails()
                                }.addOnFailureListener { exception ->
                                    Toast.makeText(
                                        requireContext(),
                                        "Failed to update profile: ${exception.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        } else {
                            Toast.makeText(
                                requireContext(),
                                "No changes detected in the profile",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "User details not found in Firestore",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }.addOnFailureListener { exception ->
                    Toast.makeText(
                        requireContext(),
                        "Failed to fetch current profile: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    private fun updateDialogAvatar(uri: Bitmap?) {
        currentDialogEditAvatar?.setImageBitmap(uri)
    }

    private fun getScaledBitmap(uri: Uri): Bitmap? {
        val contentResolver = requireContext().contentResolver
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        // Decode bounds only to get image dimensions
        contentResolver.openInputStream(uri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream, null, options)
        }

        // Calculate the scaling factor
        val maxWidth = 1024  // Max width you want to scale the image to
        val maxHeight = 1024 // Max height you want to scale the image to
        val scaleFactor = max(options.outWidth / maxWidth, options.outHeight / maxHeight)

        // Decode the image with scaling applied
        options.inJustDecodeBounds = false
        options.inSampleSize = if (scaleFactor > 0) scaleFactor else 1

        contentResolver.openInputStream(uri)?.use { inputStream ->
            return BitmapFactory.decodeStream(inputStream, null, options)
        } ?: throw IOException("Failed to open image URI")
    }
}
