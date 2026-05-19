package dev.livin.instaloader.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kmpinstaloader.composeapp.generated.resources.Res
import kmpinstaloader.composeapp.generated.resources.close_small
import kmpinstaloader.composeapp.generated.resources.download
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen() {

    var text by remember { mutableStateOf("") }


    Scaffold(modifier = Modifier.fillMaxSize(), topBar = {
        LargeTopAppBar(title = {
            Text("InstaLoader", style = MaterialTheme.typography.headlineLarge)
        })
    }, floatingActionButton = {
        FloatingActionButton(
            modifier = Modifier.windowInsetsPadding(WindowInsets.ime),
            onClick = {}
        ) {
            Icon(
                painter = painterResource(Res.drawable.download),
                contentDescription = null
            )
        }
    }) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).fillMaxSize()
                .background(MaterialTheme.colorScheme.background).padding(16.dp).imePadding()
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = {
                    text = it
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Post link")

                },
                trailingIcon = {
                    IconButton(onClick = {

                    }, content = {
                        Icon(
                            painter = painterResource(Res.drawable.close_small),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    })
                }

            )

        }

    }
}