package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.components.HmxToast
import com.example.ui.gallery.GalleryScreen
import com.example.ui.setup.SetupScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SophisticatedBg
import com.example.ui.theme.SophisticatedPrimary
import com.example.viewmodel.AppScreen
import com.example.viewmodel.GalleryViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: GalleryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainContent(viewModel: GalleryViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SophisticatedBg)
    ) {
        when (uiState.screen) {
            is AppScreen.InitialLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = SophisticatedPrimary,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            is AppScreen.Setup -> {
                SetupScreen(
                    isLoading = uiState.isLoading,
                    onContinue = { name ->
                        viewModel.handleSetup(name)
                    }
                )
                // Also display toast if active on setup screen
                HmxToast(toast = uiState.activeToast)
            }

            is AppScreen.Gallery -> {
                GalleryScreen(
                    viewModel = viewModel,
                    uiState = uiState
                )
            }
        }
    }
}
