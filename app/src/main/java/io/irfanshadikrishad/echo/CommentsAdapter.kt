package io.irfanshadikrishad.echo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso

class CommentsAdapter(private val comments: List<Comment>) :
    RecyclerView.Adapter<CommentsAdapter.CommentViewHolder>() {

    class CommentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val contentTextView: TextView = view.findViewById(R.id.commentContentTextView)
        val timestampTextView: TextView = view.findViewById(R.id.commentTimestampTextView)
        val avatar: ImageView = view.findViewById(R.id.comment_avatar)
        val username: TextView = view.findViewById(R.id.comment_username)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val db = FirebaseFirestore.getInstance()
        val comment = comments[position]
        db.collection("users").document(comment.userId).get().addOnSuccessListener { user ->
            val avatarUrl = user.getString("avatarUrl")
            val username = user.getString("name")
            holder.username.text = username
            if (!avatarUrl.isNullOrEmpty()) {
                Picasso.get().load(user.getString("avatarUrl")).into(holder.avatar)
            }
        }
        holder.contentTextView.text = comment.content
        holder.timestampTextView.text =
            java.text.DateFormat.getDateTimeInstance().format(comment.timestamp)

    }

    override fun getItemCount(): Int = comments.size
}
