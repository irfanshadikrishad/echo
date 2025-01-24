package io.irfanshadikrishad.echo

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class PostAdapter(
    private val posts: List<Post>, private val db: FirebaseFirestore
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    class PostViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val avatarImageView: ImageView = view.findViewById(R.id.avatarImageView)
        val nameTextView: TextView = view.findViewById(R.id.nameTextView)
        val contentTextView: TextView = view.findViewById(R.id.contentTextView)
        val likeCountTextView: TextView = view.findViewById(R.id.likeCountTextView)
        val commentCountTextView: TextView = view.findViewById(R.id.commentCountTextView)
        val likeButton: ImageView = view.findViewById(R.id.likeButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]

        // Set post content
        holder.contentTextView.text = post.content

        // Fetch user details from Firestore
        db.collection("users").document(post.userId).get().addOnSuccessListener { document ->
            if (document.exists()) {
                val userName = document.getString("name") ?: "Unknown User"
                val userAvatarUrl = document.getString("avatarUrl") ?: ""

                // Set user name and avatar
                holder.nameTextView.text = userName
                Glide.with(holder.avatarImageView.context).load(userAvatarUrl)
                    .placeholder(R.drawable.default_avatar).into(holder.avatarImageView)
            }
        }
        // Comment count
        db.collection("posts").document(post.id).collection("comments").get()
            .addOnSuccessListener { querySnapshot ->
                val commentCount = querySnapshot.size()
                holder.commentCountTextView.text = buildString {
                    append(commentCount)
                    append(" Comments")
                }
            }.addOnFailureListener { e ->
                Log.e("PostDetailActivity", "Failed to fetch comment count: ${e.message}")
            }

        // Update likes count
        holder.likeCountTextView.text = buildString {
            append(post.likes.size)
            append(" Likes")
        }

        // Check if the current user has liked the post
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        val isLiked = post.likes.contains(currentUserId)
        holder.likeButton.setImageResource(if (isLiked) R.drawable.love_selected else R.drawable.love_unselected)

        // Handle like button clicks
        holder.likeButton.setOnClickListener {
            currentUserId?.let { userId ->
                val postRef = db.collection("posts").document(post.id)

                if (isLiked) {
                    // Remove like
                    postRef.update("likes", FieldValue.arrayRemove(userId))
                    post.likes -= userId
                } else {
                    // Add like
                    postRef.update("likes", FieldValue.arrayUnion(userId))
                    post.likes += userId
                }

                // Update UI
                notifyItemChanged(position)
            }
        }

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, PostDetailActivity::class.java)
            intent.putExtra("postId", post.id)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = posts.size
}
