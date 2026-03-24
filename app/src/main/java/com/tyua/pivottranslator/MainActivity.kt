package com.tyua.pivottranslator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.tyua.pivottranslator.ui.navigation.NavGraph
import com.tyua.pivottranslator.ui.theme.PivotTranslatorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PivotTranslatorTheme {
                NavGraph()
            }
        }
    }
}
