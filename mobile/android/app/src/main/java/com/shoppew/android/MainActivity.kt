package com.shoppew.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.shoppew.android.core.push.AppRouteDispatcher
import com.shoppew.android.core.push.PushRouteResolver
import com.shoppew.android.ui.ShoppewApp
import com.shoppew.android.ui.theme.ShoppewTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var routeDispatcher: AppRouteDispatcher
    @Inject lateinit var routeResolver: PushRouteResolver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        dispatch(intent)
        setContent {
            ShoppewTheme {
                ShoppewApp(routeDispatcher.routes)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        dispatch(intent)
    }

    private fun dispatch(intent: Intent?) {
        routeResolver.fromUri(intent?.data)?.let(routeDispatcher::dispatch)
    }
}
