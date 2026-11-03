package dev.livin.instaloader.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import dev.livin.instaloader.viewmodel.FileType
import kmpinstaloader.composeapp.generated.resources.Res
import kmpinstaloader.composeapp.generated.resources.download
import kmpinstaloader.composeapp.generated.resources.ic_video
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PostDetails(
    post: dev.livin.instaloader.model.InstaPost,
    isDownloading: Boolean = false,
    isDownloadAllLoading: Boolean = false,
    downloadAll: (List<dev.livin.instaloader.model.InstaMediaItem>) -> Unit,
    downloadFile: (String, FileType) -> Unit
) {

    val mediaList = remember(post) {
        if (post.mediaItems.isNotEmpty()) {
            post.mediaItems
        } else if (post.video?.isNotEmpty() == true) {
            listOf(
                dev.livin.instaloader.model.InstaMediaItem(
                    url = post.video!!,
                    isVideo = true,
                    thumbnailUrl = post.images.firstOrNull()
                )
            )
        } else {
            post.images.map { url ->
                dev.livin.instaloader.model.InstaMediaItem(
                    url = url,
                    isVideo = false,
                    thumbnailUrl = url
                )
            }
        }
    }

    val pagerState = rememberPagerState(pageCount = { mediaList.size })
    val currentMedia = mediaList.getOrNull(pagerState.currentPage)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Box {

            // 🔹 Horizontal swipe images
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth()
            ) { page ->

                val media = mediaList.getOrNull(page)
                AsyncImage(
                    model = media?.thumbnailUrl ?: media?.url,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(450.dp)
                        .background(Color.LightGray),
                    contentScale = ContentScale.Fit
                )
            }

            // 🔹 Caption overlay (bottom gradient)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            )
                        )
                    )


            ) {
                Text(
                    text = "(${pagerState.currentPage + 1}/${pagerState.pageCount})",
                    color = Color.White,
                    fontSize = 14.sp,
                    maxLines = 3,
                    modifier = Modifier.padding(6.dp)
                )

                if (isDownloading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp),
                        color = Color.White
                    )
                } else {

                    IconButton(
                        onClick = {
                            currentMedia?.let { media ->
                                val type = if (media.isVideo) FileType.Video else FileType.Image
                                downloadFile(media.url, type)
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)

                            .background(
                                Color.Black.copy(alpha = 0.5f),
                                shape = CircleShape
                            )

                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.download),
                            contentDescription = "Download",
                            tint = Color.White
                        )
                    }
                }


            }

            if (currentMedia?.isVideo == true) {
                IconButton(
                    onClick = {
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)

                        .background(
                            Color.Black.copy(alpha = 0.5f),
                            shape = CircleShape
                        )

                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_video),
                        contentDescription = "Download",
                        tint = Color.LightGray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (mediaList.size > 1) {
            // 🔹 Dot indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(mediaList.size) { index ->
                    val isSelected = pagerState.currentPage == index

                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .size(if (isSelected) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) Color.Blue else Color.Gray
                            )
                    )
                }
            }

        }
        Spacer(modifier = Modifier.height(8.dp))

        if (isDownloadAllLoading) {
            CircularProgressIndicator()
        } else {
            if (mediaList.isNotEmpty()) {
                Button(onClick = {
                    downloadAll(mediaList)
                }) {
                    Text("Download All")
                }
            }

        }
    }
}


@Preview("Post Image")
@Composable
fun PostDetailsPreviewImage() {

    MaterialTheme {

        PostDetails(
            post = dev.livin.instaloader.model.InstaPost(
                shortcode = "",
                caption = "This is a test caption",
                images = listOf(
                    "https://"
                ),
                video = null
            ),
            isDownloading = false,
            isDownloadAllLoading = false,
            downloadAll = {},
            downloadFile = { _, _ ->

            }
        )
    }
}

@Preview("Post Video")
@Composable
fun PostDetailsPreviewView() {

    MaterialTheme {

        PostDetails(
            post = dev.livin.instaloader.model.InstaPost(
                shortcode = "",
                caption = "This is a test caption",
                images = listOf(
                    "https://"
                ),
                video = "https://"
            ),
            isDownloading = false,
            isDownloadAllLoading = false,
            downloadAll = {},
            downloadFile = { _, _ ->

            }
        )
    }
}