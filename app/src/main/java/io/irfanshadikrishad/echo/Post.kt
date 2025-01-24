package io.irfanshadikrishad.echo

data class Post(
    val id: String = "",
    val userId: String = "",
    val content: String = "",
    val timestamp: Long = 0L,
    var likes: List<String> = emptyList()
)
