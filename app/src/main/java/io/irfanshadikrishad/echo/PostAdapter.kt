package io.irfanshadikrishad.echo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore

class PostAdapter(
    private val posts: List<Post>,
    private val db: FirebaseFirestore
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    class PostViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val avatarImageView: ImageView = view.findViewById(R.id.avatarImageView)
        val nameTextView: TextView = view.findViewById(R.id.nameTextView)
        val contentTextView: TextView = view.findViewById(R.id.contentTextView)
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
        db.collection("users").document(post.userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val userName = document.getString("name") ?: "Unknown User"
                    val userAvatarUrl = document.getString("avatarUrl") ?: ""

                    // Set user name
                    holder.nameTextView.text = userName

                    // Load user avatar using Glide
                    Glide.with(holder.avatarImageView.context)
                        .load(userAvatarUrl)
                        .placeholder(R.drawable.default_avatar)
                        .into(holder.avatarImageView)
                } else {
                    holder.nameTextView.text = buildString {
                        append("Unknown User")
                    }
                    holder.avatarImageView.setImageResource(R.drawable.default_avatar)
                }
            }
            .addOnFailureListener {
                holder.nameTextView.text = buildString {
                    append("Unknown User")
                }
                holder.avatarImageView.setImageResource(R.drawable.default_avatar)
            }
    }

    override fun getItemCount(): Int = posts.size
}
