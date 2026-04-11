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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kmpinstaloader.composeapp.generated.resources.Res
import kmpinstaloader.composeapp.generated.resources.download
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PostDetails(
    post: dev.livin.instaloader.model.InstaPost,
    isDownloading: Boolean = false,
    isDownloadAllLoading: Boolean = false,
    downloadAll: (List<String>) -> Unit,
    downloadImage: (String) -> Unit
) {

    val pagerState = rememberPagerState(pageCount = { post.images.size })

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

                AsyncImage(
                    model = post.images[page],
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp),
                    contentScale = ContentScale.Crop
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
                    .padding(12.dp)
            ) {
                Text(
                    text = "(${pagerState.currentPage}/${pagerState.pageCount}) ${post.caption}",
                    color = Color.White,
                    fontSize = 14.sp,
                    maxLines = 3
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
                            downloadImage(post.images[pagerState.currentPage])
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
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 🔹 Dot indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(post.images.size) { index ->
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

        Spacer(modifier = Modifier.height(8.dp))

        if (isDownloadAllLoading) {
            CircularProgressIndicator()
        } else {
            Button(onClick = {
                downloadAll(post.images)
            }) {
                Text("Download All")
            }
        }
    }
}
