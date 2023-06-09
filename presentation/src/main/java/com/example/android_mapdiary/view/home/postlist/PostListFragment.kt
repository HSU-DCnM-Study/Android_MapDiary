package com.example.android_mapdiary.view.home.postlist

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.PagingData
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.android_mapdiary.R
import com.example.android_mapdiary.common.PagingLoadStateAdapter
import com.example.android_mapdiary.common.ViewBindingFragment
import com.example.android_mapdiary.common.registerObserverForScrollToTop
import com.example.android_mapdiary.common.setListeners
import com.example.android_mapdiary.databinding.FragmentPostListBinding
import com.example.android_mapdiary.view.home.map.MapActivity
import com.example.android_mapdiary.view.posting.PostingActivity
import com.example.android_mapdiary.view.profile.ProfileActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PostListFragment(
    private val toolbarTitle: String? = null,
    private val targetUserUuid: String? = null,
    private val initPostPagingData: PagingData<PostItemUiState>? = null,
    private val onBackButtonClick: (() -> Unit)? = null
) : ViewBindingFragment<FragmentPostListBinding>() {

    private val viewModel: PostListViewModel by viewModels()

    private lateinit var bottomSheetDialog: BottomSheetDialog

    private lateinit var launcher: ActivityResultLauncher<Intent>

    override val bindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> FragmentPostListBinding
        get() = FragmentPostListBinding::inflate

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.bind(targetUserUuid, initPostPagingData)

        val adapter = PostAdapter(
            onClickMoreButton = ::onClickMoreInfoButton,
            onClickUser = ::onClickUser
        )
        initToolbar()
        initEvent()
        initRecyclerView(adapter)
        initBottomSheetDialog(adapter)

        launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                adapter.refresh()
            }
        }
        setFragmentResultListener("refreshPosts") { _, _ ->
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
                delay(300)
                withContext(Dispatchers.Main) {
                    adapter.refresh()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect {
                    updateUi(it, adapter)
                }
            }
        }
    }

    private fun initToolbar() {
        if (toolbarTitle != null) {
            val activity = requireActivity() as AppCompatActivity
            activity.setSupportActionBar(binding.toolBar)
            activity.supportActionBar?.apply {
                setDisplayHomeAsUpEnabled(true)
                setDisplayShowHomeEnabled(true)
                title = this@PostListFragment.toolbarTitle
            }
            binding.toolbarTitle.isVisible = false
        }

        if (onBackButtonClick != null) {
            binding.toolBar.setNavigationOnClickListener {
                onBackButtonClick.invoke()
            }
        }

        binding.toolBar.inflateMenu(R.menu.menu_home_option)
        binding.toolBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_add_post -> {
                    startPostingActivity()
                    true
                }
                R.id.action_my_profile -> {
                    startProfileActivity(viewModel.uiState.value.currentUserUuid)
                    true
                }
                else -> throw IllegalArgumentException()
            }
        }
    }

    private fun initBottomSheetDialog(adapter: PostAdapter) {
        bottomSheetDialog = BottomSheetDialog(requireContext()).apply {
            setContentView(R.layout.bottom_sheet_post_item)
        }
        bottomSheetDialog.behavior.isDraggable = false

        val editButton = bottomSheetDialog.findViewById<LinearLayoutCompat>(R.id.editBar)
        editButton?.setOnClickListener {
            bottomSheetDialog.hide()

            val post = requireNotNull(viewModel.uiState.value.selectedPostItem)
            val postContent = post.content
            val postImage = post.imageUrl
            val postUuid = post.uuid

            val intent = PostingActivity.getIntent(
                requireContext(),
                postContent = postContent,
                postImage = postImage,
                postUuid = postUuid
            )

            launcher.launch(intent)
        }

        val removeButton = bottomSheetDialog.findViewById<LinearLayoutCompat>(R.id.removeBar)
        removeButton?.setOnClickListener {
            bottomSheetDialog.hide()
            MaterialAlertDialogBuilder(requireContext()).apply {
                setTitle(getString(R.string.delete_post))
                setMessage(R.string.are_you_sure_you_want_to_delete)
                setNegativeButton(R.string.cancel) { _, _ -> }
                setPositiveButton(R.string.delete) { _, _ ->
                    viewModel.deleteSelectedPost(onDeleted = { adapter.refresh() })
                }
            }.show()
        }
    }

    private fun initEvent() {
        binding.fab.setOnClickListener {
            navigateToMapActivity()
        }
    }

    private fun initRecyclerView(adapter: PostAdapter) {
        binding.apply {
            recyclerView.adapter = adapter.withLoadStateFooter(
                PagingLoadStateAdapter { adapter.retry() }
            )
            recyclerView.layoutManager = LinearLayoutManager(context)

            loadState.setListeners(adapter, swipeRefreshLayout)
            loadState.emptyText.text = getString(R.string.follow_some_people)
            loadState.emptyText.textSize = 20.0f

            adapter.registerObserverForScrollToTop(recyclerView)
        }
    }

    private fun updateUi(uiState: PostListUiState, adapter: PostAdapter) {
        adapter.submitData(viewLifecycleOwner.lifecycle, uiState.pagingData)
        if (uiState.userMessage != null) {
            showSnackBar(getString(uiState.userMessage))
            viewModel.userMessageShown()
        }
    }

    private fun onClickMoreInfoButton(uiState: PostItemUiState) {
        viewModel.showPostOptionBottomSheet(uiState)
        bottomSheetDialog.show()
    }

    private fun onClickUser(uiState: PostItemUiState) {
        startProfileActivity(uiState.writerUuid)
    }

    private fun showSnackBar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun startProfileActivity(userUuid: String) {
        val intent = ProfileActivity.getIntent(requireContext(), userUuid)
        launcher.launch(intent)
    }

    private fun startPostingActivity() {
        val intent = PostingActivity.getIntent(requireContext())
        launcher.launch(intent)
    }

    private fun navigateToMapActivity() {
        val intent = MapActivity.getIntent(requireContext())
        launcher.launch(intent)
    }
}