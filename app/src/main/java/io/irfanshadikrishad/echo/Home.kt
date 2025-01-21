package io.irfanshadikrishad.echo

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import io.irfanshadikrishad.echo.databinding.FragmentHomeBinding

class Home : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private val posts = mutableListOf<Post>()
    private lateinit var postAdapter: PostAdapter
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        // Initialize Firebase
        firebaseAuth = FirebaseAuth.getInstance()
        // Initialize RecyclerView
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            postAdapter = PostAdapter(posts)
            adapter = postAdapter
        }

        // Load posts
        loadPosts()

        // Handle post creation
        binding.postButton.setOnClickListener {
            val content = binding.textInputEditText.text.toString().trim()
            if (content.isNotEmpty()) {
                createPost(content)
            } else {
                Toast.makeText(context, "Post content cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }

        return binding.root
    }

    private fun createPost(content: String) {
        // Check if the user is authenticated
        val userId = firebaseAuth.currentUser?.uid
        Log.w("POST", "createPost called. $userId $content")
        if (userId.isNullOrEmpty()) {
            Toast.makeText(context, "User not authenticated!", Toast.LENGTH_SHORT).show()
            return
        }
        // Create a new post object
        val post = Post(
            id = db.collection("posts").document().id,
            userId = userId,
            content = content,
            timestamp = System.currentTimeMillis()
        )
        // Save the post to Firestore
        db.collection("posts").document(post.id).set(post).addOnSuccessListener {
            // Notify user of success
            Toast.makeText(context, "Post created!", Toast.LENGTH_SHORT).show()

            // Clear the text input field
            binding.textInputEditText.text?.clear()

            // Add the new post to the list and update the RecyclerView
            posts.add(0, post) // Add the post to the top of the list
            postAdapter.notifyItemInserted(0) // Notify adapter about the new item
            binding.recyclerView.scrollToPosition(0) // Scroll to the top
        }.addOnFailureListener { exception ->
            // Notify user of failure
            Toast.makeText(
                context, "Failed to create post: ${exception.message}", Toast.LENGTH_SHORT
            ).show()
        }.addOnCanceledListener {
            // Handle case where the operation was canceled
            Toast.makeText(context, "Post creation was canceled!", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun loadPosts() {
        db.collection("posts")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING).get()
            .addOnSuccessListener { documents ->
                val oldSize = posts.size // Track current size
                posts.clear() // Clear the current list

                for (document in documents) {
                    val post = document.toObject(Post::class.java)
                    posts.add(post)
                }

                val newSize = posts.size // Track the new size
                if (newSize > oldSize) {
                    // Notify only the newly added items
                    postAdapter.notifyItemRangeInserted(oldSize, newSize - oldSize)
                } else {
                    postAdapter.notifyDataSetChanged() // Fallback for full refresh
                }
            }.addOnFailureListener {
                Toast.makeText(context, "Failed to load posts", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
