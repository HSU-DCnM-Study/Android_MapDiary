package com.example.android_mapdiary.view.profile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.viewModels
import com.example.android_mapdiary.R
import com.example.android_mapdiary.common.ViewBindingActivity
import com.example.android_mapdiary.databinding.ActivityProfileBinding
import com.example.android_mapdiary.view.home.postlist.PostListFragment

class ProfileActivity : ViewBindingActivity<ActivityProfileBinding>() {

    private val viewModel: ProfileViewModel by viewModels()

    override val bindingInflater: (LayoutInflater) -> ActivityProfileBinding
        get() = ActivityProfileBinding::inflate

    companion object {
        fun getIntent(context: Context, userUuid: String): Intent {
            return Intent(context, ProfileActivity::class.java).apply {
                putExtra("userUuid", userUuid)
            }
        }
    }

    private fun getUserUuid(): String {
        return intent.getStringExtra("userUuid")!!
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.bindProfile(getUserUuid())

        replaceToProfileFragment()
    }

    override fun onSupportNavigateUp(): Boolean {
        when (supportFragmentManager.backStackEntryCount) {
            0 -> finish()
            1 -> supportFragmentManager.popBackStack()
        }
        return super.onSupportNavigateUp()
    }

    private fun replaceToProfileFragment() {
        val fragmentManager = supportFragmentManager
        val profileFragment = ProfileFragment(
            onClickPost = {
                addUserPostListFragment()
            }
        )
        fragmentManager.beginTransaction().apply {
            replace(R.id.fragment_container_view, profileFragment)
        }.commit()
    }

    private fun addUserPostListFragment() {
        val postPagingData = viewModel.profilePostUiState.value.pagingData
        val fragmentManager = supportFragmentManager
        // TODO: 클릭한 게시글의 인덱스로 스크롤 포지션 변경하기
        val postListFragment = PostListFragment(
            toolbarTitle = "",
            targetUserUuid = getUserUuid(),
            initPostPagingData = postPagingData,
            onBackButtonClick = {
                supportFragmentManager.popBackStack()
            }
        )
        fragmentManager.beginTransaction().apply {
            replace(R.id.fragment_container_view, postListFragment)
            addToBackStack(null)
        }.commit()
    }
}