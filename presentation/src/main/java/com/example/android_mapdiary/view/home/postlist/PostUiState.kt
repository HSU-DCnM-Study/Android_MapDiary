package com.example.android_mapdiary.view.home.postlist

import androidx.annotation.StringRes
import androidx.paging.PagingData
import com.example.domain.model.Post

data class PostListUiState(
    val pagingData: PagingData<PostItemUiState> = PagingData.empty(),
    val selectedPostItem: PostItemUiState? = null,
    val currentUserUuid: String,
    @StringRes
    val userMessage: Int? = null
)

data class PostItemUiState(
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

fun Post.toUiState() = PostItemUiState(
    uuid = uuid,
    writerUuid = writerUuid,
    writerName = writerName,
    writerProfileImageUrl = writerProfileImageUrl,
    content = content,
    imageUrl = imageUrl,
    likeCount = likeCount,
    meLiked = meLiked,
    isMine = isMine,
    timeAgo = timeAgo,
    latitude = latitude,
    longitude = longitude
)
