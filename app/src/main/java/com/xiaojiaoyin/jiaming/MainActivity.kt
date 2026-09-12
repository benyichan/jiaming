package com.xiaojiaoyin.jiaming

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.xiaojiaoyin.jiaming.ui.JiamingNav
import com.xiaojiaoyin.jiaming.ui.theme.JiamingTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val graph = (application as JiamingApp).graph
        setContent {
            JiamingTheme {
                JiamingNav(graph.mainViewModel)
            }
        }
    }
}
