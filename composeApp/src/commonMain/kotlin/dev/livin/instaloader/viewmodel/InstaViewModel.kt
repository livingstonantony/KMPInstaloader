package dev.livin.instaloader.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.livin.instaloader.model.InstaPost
import dev.livin.instaloader.repository.InstaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


sealed class InstaUiState<out T> {

    object Idle : InstaUiState<Nothing>()

    object Loading : InstaUiState<Nothing>()

    data class Success<T>(val post: T) : InstaUiState<T>()

    data class Error(val message: String) : InstaUiState<Nothing>()
}

class InstaViewModel : ViewModel() {

    private val repository = InstaRepository()

    private val _uiState = MutableStateFlow<InstaUiState<InstaPost>>(InstaUiState.Idle)
    val uiState: StateFlow<InstaUiState<InstaPost>> = _uiState.asStateFlow()

    private val _getFileByUrl = MutableStateFlow<InstaUiState<ByteArray?>>(InstaUiState.Idle)
    val getFileByUrl: StateFlow<InstaUiState<ByteArray?>> = _getFileByUrl.asStateFlow()

    private val _getFilesByUrl = MutableStateFlow<InstaUiState<List<ByteArray>?>>(InstaUiState.Idle)
    val getFilesByUrl: StateFlow<InstaUiState<List<ByteArray>?>> = _getFilesByUrl.asStateFlow()


    fun fetchPost(shortcode: String) {
        if (shortcode.isBlank()) {
            _uiState.value = InstaUiState.Error("Shortcode cannot be empty")
            return
        }
        viewModelScope.launch {
            _uiState.value = InstaUiState.Loading

            try {
                val post = repository.getPost(shortcode.trim())
                println("Video URL: ${post.video}")
                _uiState.value = InstaUiState.Success(post)
            } catch (e: Exception) {
                println("Error fetching post: ${e.message}")
                _uiState.value = InstaUiState.Error("Error fetching post: ${e.message}")
            }
        }
    }

    fun getFileByUrl(url: String) {
        viewModelScope.launch {
            _getFileByUrl.value = InstaUiState.Loading
            try {
                val file = repository.downloadFile(url)
                _getFileByUrl.value = InstaUiState.Success(file)
            } catch (e: Exception) {
                println("Error fetching file: ${e.message}")
            }

        }
    }


}


fun ByteArray.formatSize(): String {
    val bytes = this.size

    return when {
        bytes >= 1024 * 1024 -> {
            val mb = bytes / (1024.0 * 1024.0)
            "%.2f MB".format(mb)
        }
        bytes >= 1024 -> {
            val kb = bytes / 1024.0
            "%.2f KB".format(kb)
        }
        else -> "$bytes Bytes"
    }
}
