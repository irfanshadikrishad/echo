package io.irfanshadikrishad.echo

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import io.irfanshadikrishad.echo.databinding.ActivityPostDetailsBinding
import java.util.Date

class PostDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPostDetailsBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var commentsAdapter: CommentsAdapter
    private val comments = mutableListOf<Comment>()
    private var postId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPostDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()
        firebaseAuth = FirebaseAuth.getInstance()

        // Get post ID from the intent
        postId = intent.getStringExtra("postId")
        if (postId == null) {
            Toast.makeText(this, "Invalid post ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Initialize RecyclerView
        commentsAdapter = CommentsAdapter(comments)
        binding.commentsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@PostDetailActivity)
            adapter = commentsAdapter
        }

        setupRecyclerView()
        loadPostDetails()
        loadComments()

        // Handle comment submission
        binding.commentButton.setOnClickListener {
            val commentText = binding.commentEditText.text.toString().trim()
            if (commentText.isNotEmpty()) {
                submitComment(commentText)
            } else {
                Toast.makeText(this, "Comment cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }
        // Handle delete post
        binding.pdDelete.setOnClickListener {
            val firestore = FirebaseFirestore.getInstance()
            firestore.collection("posts").document(postId!!).delete().addOnSuccessListener { _ ->
                Toast.makeText(this, "Post deleted successfully!", Toast.LENGTH_SHORT).show()
                finish()
            }.addOnFailureListener {
                Log.w("del196", "couldn't delete")
                Toast.makeText(this, "Post could not be deleted!", Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun setupRecyclerView() {
        binding.commentsRecyclerView.layoutManager = LinearLayoutManager(this)
        commentsAdapter = CommentsAdapter(comments)
        binding.commentsRecyclerView.adapter = commentsAdapter
    }

    private fun loadPostDetails() {
        db.collection("posts").document(postId!!).get().addOnSuccessListener { document ->
            if (document.exists()) {
                val post = document.toObject(Post::class.java)
                if (post != null) {
                    binding.contentTextView.text = post.content
                    binding.likeCountTextView.text = buildString {
                        append(post.likes.size)
                        append(" Likes")
                    }
                    binding.pdTimestamp.text = Date(post.timestamp).toLocaleString()
                    db.collection("users").document(post.userId).get()
                        .addOnSuccessListener { user ->
                            // Set User Name
                            binding.postUsername.text = user.getString("name")
                            if (post.userId == firebaseAuth.currentUser?.uid) {
                                binding.pdDelete.visibility = View.VISIBLE
                                binding.pdEdit.visibility = View.VISIBLE
                                // Open Dialog
                                binding.pdEdit.setOnClickListener {
                                    showEditDialog(post)
                                }
                            }
                            // Set Users Avatar
                            Glide.with(binding.root.context).load(user.getString("avatarUrl"))
                                .placeholder(R.drawable.default_avatar)
                                .into(binding.avatarImageView)
                        }.addOnFailureListener {
                            Log.e("PostDetailActivity", "Failed:")
                        }

                    binding.likeButton.setImageResource(
                        if (post.likes.contains(firebaseAuth.currentUser?.uid)) R.drawable.love_selected
                        else R.drawable.love_unselected
                    )

                    binding.likeButton.setOnClickListener {
                        toggleLike(post)
                    }
                }
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to load post details", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun loadComments() {
        if (postId.isNullOrEmpty()) {
            Toast.makeText(this, "Invalid Post ID", Toast.LENGTH_SHORT).show()
            return
        }
        db.collection("posts").document(postId!!).collection("comments")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(
                        this, "Failed to load comments: ${error.message}", Toast.LENGTH_SHORT
                    ).show()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    comments.clear() // Clear the existing comments list
                    for (document in snapshot.documents) {
                        val comment = document.toObject(Comment::class.java)
                        if (comment != null) {
                            comments.add(comment)
                        }
                    }
                    commentsAdapter.notifyDataSetChanged() // Notify adapter of the data change
                }
            }
    }

    private fun submitComment(commentText: String) {
        val userId = firebaseAuth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        val comment = Comment(
            id = db.collection("posts").document(postId!!).collection("comments").document().id,
            userId = userId,
            content = commentText,
            timestamp = System.currentTimeMillis()
        )

        db.collection("posts").document(postId!!).collection("comments").document(comment.id)
            .set(comment).addOnSuccessListener {
                Toast.makeText(this, "Comment added", Toast.LENGTH_SHORT).show()
                binding.commentEditText.text?.clear()
            }.addOnFailureListener {
                Toast.makeText(this, "Failed to add comment", Toast.LENGTH_SHORT).show()
            }
    }

    private fun toggleLike(post: Post) {
        val currentUserId = firebaseAuth.currentUser?.uid ?: return
        val postRef = db.collection("posts").document(post.id)

        if (post.likes.contains(currentUserId)) {
            postRef.update("likes", FieldValue.arrayRemove(currentUserId))
            binding.likeButton.setImageResource(R.drawable.love_unselected)
            binding.likeCountTextView.text = buildString {
                append(post.likes.size - 1)
                append(" Likes")
            }
        } else {
            postRef.update("likes", FieldValue.arrayUnion(currentUserId))
            binding.likeButton.setImageResource(R.drawable.love_selected)
            binding.likeCountTextView.text = buildString {
                append(post.likes.size + 1)
                append(" Likes")
            }
        }
    }

    // Edit Dialogue
    private fun showEditDialog(post: Post) {
        val builder = android.app.AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_edit_post, null)
        builder.setView(dialogView)
        builder.setCancelable(false)

        val editText = dialogView.findViewById<EditText>(R.id.editPostContent)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancelEditButton)
        val saveButton = dialogView.findViewById<Button>(R.id.saveEditButton)

        // Set current post content in the edit text
        editText.setText(post.content)

        val dialog = builder.create()

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        try {
            saveButton.setOnClickListener {
                val updatedContent = editText.text.toString().trim()
                if (updatedContent.isNotEmpty()) {
                    updatePost(post, updatedContent)
                    dialog.dismiss()
                } else {
                    Toast.makeText(this, "Content cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (error: Exception) {
            Toast.makeText(this, "Error updating: $error", Toast.LENGTH_SHORT).show()
        }
        dialog.show()
    }

    // Update Post functionality
    private fun updatePost(post: Post, updatedContent: String) {
        val postRef = db.collection("posts").document(post.id)
        postRef.update("content", updatedContent).addOnSuccessListener {
            Toast.makeText(this, "Post updated successfully!", Toast.LENGTH_SHORT).show()
            binding.contentTextView.text = updatedContent
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to update post", Toast.LENGTH_SHORT).show()
        }
    }

}
