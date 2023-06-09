package com.example.domain.model

data class Post(
    val uuid: String,
    val writerUuid: String,
    val writerName: String,
    val writerProfileImageUrl: String?,
    val content: String,
    val imageUrl: String,
    val likeCount: Int,
    val meLiked: Boolean,
    val isMine: Boolean,
    val timeAgo: String,
    val latitude: Double,
    val longitude: Double,
)
