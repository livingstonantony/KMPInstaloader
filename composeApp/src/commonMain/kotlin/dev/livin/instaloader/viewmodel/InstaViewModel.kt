package dev.livin.instaloader.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.livin.instaloader.model.InstaPost
import dev.livin.instaloader.repository.InstaRepository
import dev.livin.instaloader.utils.isValidInstagramUrl
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

sealed class FileType {
    object Image : FileType()
    object Video : FileType()
}



/**
 * Represents a downloaded file.
 *
 * ByteArray uses reference equality by default, which means two arrays with
 * identical content are considered different if they are different instances.
 *
 * We override equals() and hashCode() to perform content-based comparison
 * using ByteArray.contentEquals() and ByteArray.contentHashCode(), ensuring
 * DownloadedFile instances are compared based on their actual file data and
 * type rather than array references.
 */
data class DownloadedFile(
    val data: ByteArray?,
    val type: FileType
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as DownloadedFile

        if (!data.contentEquals(other.data)) return false
        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        var result = data?.contentHashCode() ?: 0
        result = 31 * result + type.hashCode()
        return result
    }
}

class InstaViewModel : ViewModel() {

    private val repository = InstaRepository()

    private val _uiState = MutableStateFlow<InstaUiState<InstaPost>>(InstaUiState.Idle)
    val uiState: StateFlow<InstaUiState<InstaPost>> = _uiState.asStateFlow()



    private val _getFileByUrl =
        MutableStateFlow<InstaUiState<DownloadedFile>>(InstaUiState.Idle)

    val getFileByUrl: StateFlow<InstaUiState<DownloadedFile>> =
        _getFileByUrl.asStateFlow()

    private val _getFilesByUrl =
        MutableStateFlow<InstaUiState<List<DownloadedFile>>>(InstaUiState.Idle)

    val getFilesByUrl: StateFlow<InstaUiState<List<DownloadedFile>>> =
        _getFilesByUrl.asStateFlow()


    fun fetchPost(shortcode: String) {
        if (shortcode.isBlank()) {
            _uiState.value = InstaUiState.Error("Shortcode cannot be empty")
            return
        }

        //validating if its Instagram URL or not
        if (!isValidInstagramUrl(shortcode)){
            _uiState.value = InstaUiState.Error("Please use a valid Instagram URL")
            return
        }

        viewModelScope.launch {
            _uiState.value = InstaUiState.Loading
            _getFileByUrl.value = InstaUiState.Idle
            _getFilesByUrl.value = InstaUiState.Idle

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


    fun getFileByUrl(url: String, type: FileType) {
        viewModelScope.launch {
            _getFileByUrl.value = InstaUiState.Loading
            try {
                val file = repository.downloadFile(url)
                _getFileByUrl.value = InstaUiState.Success(
                    DownloadedFile(file, type)
                )
            } catch (e: Exception) {
                println("Error fetching file: ${e.message}")
            }

        }
    }



    fun getFilesByUrl(urls: List<String>) {
        viewModelScope.launch {
            _getFilesByUrl.value = InstaUiState.Loading
            try {
                val files = repository.downloadFiles(urls)

                val result = urls.mapIndexed { index, url ->
                    val file = files.getOrNull(index)

                    val type = when {
                        url.contains(".mp4", ignoreCase = true) -> FileType.Video
                        url.contains(".jpg", ignoreCase = true) ||
                                url.contains(".jpeg", ignoreCase = true) ||
                                url.contains(".png", ignoreCase = true) -> FileType.Image
                        else -> FileType.Image
                    }

                    DownloadedFile(file, type)
                }

                _getFilesByUrl.value = InstaUiState.Success(result)

            } catch (e: Exception) {
                _getFilesByUrl.value =
                    InstaUiState.Error(e.message ?: "Error fetching files")
            }
        }
    }


}
