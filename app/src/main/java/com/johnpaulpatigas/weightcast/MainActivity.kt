package com.johnpaulpatigas.weightcast

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.johnpaulpatigas.weightcast.feature.weight_logging.WeightLoggingScreen
import com.johnpaulpatigas.weightcast.feature.weight_logging.WeightLoggingViewModel
import com.johnpaulpatigas.weightcast.ui.theme.WeightCastTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WeightCastTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: WeightLoggingViewModel = hiltViewModel()
                    WeightLoggingScreen(viewModel = viewModel)
                }
            }
        }
    }
}
