package dev.livin.instaloader.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.livin.instaloader.model.InstaPost
import dev.livin.instaloader.repository.InstaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


sealed class InstaUiState {
    object Idle : InstaUiState()
    object Loading : InstaUiState()
    data class Success(val post: InstaPost) : InstaUiState()
    data class Error(val message: String) : InstaUiState()
}

class InstaViewModel : ViewModel() {

    private val repository = InstaRepository()

    private val _uiState = MutableStateFlow<InstaUiState>(InstaUiState.Idle)
    val uiState: StateFlow<InstaUiState> = _uiState.asStateFlow()

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
}
