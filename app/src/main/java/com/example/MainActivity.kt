package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.ui.LocalBeeApp
import com.example.ui.LocalBeeViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Setup edge to edge rendering
        enableEdgeToEdge()
        
        // Instantiate our local-bee viewmodel
        val viewModel = ViewModelProvider(this)[LocalBeeViewModel::class.java]
        
        setContent {
            LocalBeeApp(viewModel = viewModel)
        }
    }
}
