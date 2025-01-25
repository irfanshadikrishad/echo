package io.irfanshadikrishad.echo

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import io.irfanshadikrishad.echo.databinding.ActivityMyPostsBinding
import java.util.Date

class MyPosts : AppCompatActivity() {
    private lateinit var binding: ActivityMyPostsBinding
    private var userId: String? = null
    private lateinit var myPostsText: TextView
    private lateinit var myPostsView: RecyclerView
    private lateinit var firestore: FirebaseFirestore
    private val posts = mutableListOf<Post>()
    private lateinit var adapter: PostAdapter

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyPostsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize RecyclerView
        firestore = FirebaseFirestore.getInstance()
        myPostsView = binding.myPostsView
        myPostsView.layoutManager = LinearLayoutManager(this)
        adapter = PostAdapter(posts, firestore)
        myPostsView.adapter = adapter

        // Get the userId from intent
        userId = intent.getStringExtra("userId")
        if (!userId.isNullOrEmpty()) {
            try {
                // Fetch and display posts from Firestore
                firestore.collection("posts")
                    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .get().addOnSuccessListener { documents ->
                        posts.clear()
                        for (document in documents) {
                            val post = document.toObject(Post::class.java)
                            Log.d("dx156", (post.userId == userId).toString())
                            if (post.userId == userId) {
                                posts.add(post)
                            }
                        }
                        adapter.notifyDataSetChanged()
                        // If no posts, show the message
                        if (posts.isEmpty()) {
                            binding.noPosts.visibility = View.VISIBLE
                            binding.myPostsView.visibility = View.GONE
                        } else {
                            binding.noPosts.visibility = View.GONE
                            binding.myPostsView.visibility = View.VISIBLE
                        }
                    }
            } catch (error: Exception) {
                Log.e("my_posts", "$error")
                Log.e("my_posts", "$posts")
            }
        }
    }
}