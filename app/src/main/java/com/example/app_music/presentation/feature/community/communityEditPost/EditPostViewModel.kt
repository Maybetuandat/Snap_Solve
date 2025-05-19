package com.example.app_music.presentation.feature.community.communityEditPost

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_music.data.repository.PostRepositoryImpl
import com.example.app_music.data.repository.TopicRepositoryImpl
import com.example.app_music.domain.model.Post
import com.example.app_music.domain.model.Topic
import com.example.app_music.domain.utils.UrlUtils
import kotlinx.coroutines.launch

class EditPostViewModel : ViewModel() {
    private val postRepository = PostRepositoryImpl()
    private val topicRepository = TopicRepositoryImpl()

    private val _postToEdit = MutableLiveData<Post?>()
    val postToEdit: LiveData<Post?> = _postToEdit

    private val _topics = MutableLiveData<List<Topic>>()
    val topics: LiveData<List<Topic>> = _topics

    private val _selectedImages = MutableLiveData<List<Uri>>()
    val selectedImages: LiveData<List<Uri>> = _selectedImages

    private val _isUpdating = MutableLiveData<Boolean>()
    val isUpdating: LiveData<Boolean> = _isUpdating

    private val _updateResult = MutableLiveData<Boolean>()
    val updateResult: LiveData<Boolean> = _updateResult

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    // Lưu tất cả ảnh gốc của bài viết
    private val originalImageUrls = mutableListOf<String>()
    // Lưu ảnh hiện tại còn lại sau khi xóa
    private val currentImageUrls = mutableListOf<String>()
    // Danh sách ảnh mới được thêm vào
    private val newImageUris = mutableListOf<Uri>()

    // Tải bài viết cần chỉnh sửa
    fun loadPostForEdit(postId: Long) {
        _isUpdating.value = true
        viewModelScope.launch {
            try {
                val response = postRepository.getPostById(postId)
                if (response.isSuccessful) {
                    val post = response.body()
                    if (post != null) {
                        _postToEdit.value = post
                        // Lưu lại danh sách ảnh gốc
                        val images = post.getAllImages()
                        originalImageUrls.clear()
                        originalImageUrls.addAll(images)
                        currentImageUrls.clear()
                        currentImageUrls.addAll(images)
                        updateSelectedImagesDisplay()
                    } else {
                        _error.value = "Không thể tải dữ liệu bài viết"
                    }
                } else {
                    _error.value = "Lỗi tải bài viết: ${response.code()}"
                }
            } catch (e: Exception) {
                Log.e("EditPostViewModel", "Error loading post", e)
                _error.value = "Lỗi kết nối: ${e.message}"
            } finally {
                _isUpdating.value = false
            }
        }
    }

    // Tải danh sách topic
    fun loadTopics() {
        viewModelScope.launch {
            try {
                val response = topicRepository.getAllTopics()
                if (response.isSuccessful) {
                    _topics.value = response.body() ?: emptyList()
                } else {
                    _error.value = "Không thể tải danh sách chủ đề: ${response.code()}"
                }
            } catch (e: Exception) {
                Log.e("EditPostViewModel", "Error loading topics", e)
                _error.value = "Lỗi kết nối: ${e.message}"
            }
        }
    }

    // Thiết lập ảnh hiện có từ bài viết
    fun setExistingImages(imageUrls: List<String>) {
        originalImageUrls.clear()
        originalImageUrls.addAll(imageUrls)
        currentImageUrls.clear()
        currentImageUrls.addAll(imageUrls)
        updateSelectedImagesDisplay()
    }

    // Thêm ảnh mới
    fun addImages(uris: List<Uri>) {
        newImageUris.addAll(uris)
        updateSelectedImagesDisplay()
    }

    // Xóa ảnh theo vị trí
    fun removeImage(position: Int) {
        val totalImages = currentImageUrls.size + newImageUris.size

        if (position < 0 || position >= totalImages) return

        if (position < currentImageUrls.size) {
            // Xóa ảnh hiện có
            currentImageUrls.removeAt(position)
        } else {
            // Xóa ảnh mới thêm vào
            val newImageIndex = position - currentImageUrls.size
            newImageUris.removeAt(newImageIndex)
        }

        updateSelectedImagesDisplay()
    }

    // Cập nhật hiển thị danh sách ảnh đã chọn
    private fun updateSelectedImagesDisplay() {
        val allUris = mutableListOf<Uri>()

        // Chuyển đổi ảnh hiện có thành URIs để hiển thị
        currentImageUrls.forEach { url ->
            val absoluteUrl = UrlUtils.getAbsoluteUrl(url)
            allUris.add(Uri.parse(absoluteUrl))
        }

        // Thêm URIs ảnh mới
        allUris.addAll(newImageUris)

        _selectedImages.value = allUris
    }

    // Cập nhật bài viết
    fun updatePost(postId: Long, title: String, content: String, userId: Long, topicIds: List<Long>, newImagePaths: List<String>) {
        _isUpdating.value = true

        viewModelScope.launch {
            try {
                // Kết hợp ảnh cũ và ảnh mới thành một danh sách duy nhất
                val allImagePaths = mutableListOf<String>()
                allImagePaths.addAll(currentImageUrls) // Ảnh cũ (URLs)
                allImagePaths.addAll(newImagePaths) // Ảnh mới (file paths)

                Log.d("EditPostViewModel", "Sending to repository:")
                Log.d("EditPostViewModel", "- Current URLs: ${currentImageUrls.size}")
                Log.d("EditPostViewModel", "- New paths: ${newImagePaths.size}")
                Log.d("EditPostViewModel", "- Total: ${allImagePaths.size}")

                // Gọi repository với signature đơn giản giống createPost
                val response = postRepository.updatePost(
                    postId = postId,
                    title = title,
                    content = content,
                    userId = userId,
                    topicIds = topicIds,
                    imagePaths = allImagePaths // Truyền tất cả ảnh trong một list
                )

                if (response.isSuccessful) {
                    _updateResult.value = true
                } else {
                    _error.value = "Không thể cập nhật bài: ${response.code()} - ${response.message()}"
                    _updateResult.value = false
                }
            } catch (e: Exception) {
                Log.e("EditPostViewModel", "Error updating post", e)
                _error.value = "Lỗi kết nối: ${e.message}"
                _updateResult.value = false
            } finally {
                _isUpdating.value = false
            }
        }
    }

    // Lấy danh sách ảnh mới (chưa có trên server)
    fun getNewImages(): List<Uri> {
        return newImageUris.toList()
    }

    // Lấy danh sách ảnh hiện có (đã có trên server, chưa bị xóa)
    fun getCurrentImages(): List<String> {
        return currentImageUrls.toList()
    }

    // Debug method để kiểm tra trạng thái
    fun debugImageState() {
        Log.d("EditPostViewModel", "Original images: ${originalImageUrls.size}")
        originalImageUrls.forEachIndexed { index, url ->
            Log.d("EditPostViewModel", "Original [$index]: $url")
        }

        Log.d("EditPostViewModel", "Current images: ${currentImageUrls.size}")
        currentImageUrls.forEachIndexed { index, url ->
            Log.d("EditPostViewModel", "Current [$index]: $url")
        }

        Log.d("EditPostViewModel", "New images: ${newImageUris.size}")
        newImageUris.forEachIndexed { index, uri ->
            Log.d("EditPostViewModel", "New [$index]: $uri")
        }
    }
}