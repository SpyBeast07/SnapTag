package com.example.snaptag

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.snaptag.ui.components.BottomNavBar
import com.example.snaptag.ui.screens.AboutScreen
import com.example.snaptag.ui.screens.SettingsScreen
import com.example.snaptag.ui.screens.StocksScreen
import com.example.snaptag.ui.theme.SnapTagTheme
import com.example.snaptag.viewmodel.ProductViewModelFactory
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = (application as SnapTagApp).repository
        val viewModelFactory = ProductViewModelFactory(repository)
        setContent {
            SnapTagTheme {
                val navController = rememberNavController()
                AppNavigation(navController, viewModelFactory)
            }
        }
    }
}

@Composable
fun AppNavigation(navController: NavHostController, viewModelFactory: ProductViewModelFactory) {
    NavHost(navController = navController, startDestination = "loading") {
        composable("loading") {
            LoadingScreen(onLoaded = {
                navController.navigate("stocks") {
                    popUpTo("loading") { inclusive = true }
                }
            })
        }
        composable("stocks") {
            MainScaffold(navController) {
                StocksScreen(viewModelFactory = viewModelFactory)
            }
        }
        composable("settings") {
            MainScaffold(navController) {
                SettingsScreen(viewModelFactory = viewModelFactory)
            }
        }
        composable("about") {
            MainScaffold(navController) {
                AboutScreen()
            }
        }
    }
}

@Composable
fun MainScaffold(
    navController: NavHostController,
    content: @Composable () -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = { BottomNavBar(navController) }
    ) { padding ->
        // The padding here includes bottom navigation height.
        // We do NOT apply top padding here because each Screen has its own Scaffold
        // and TopBar which will handle the Status Bar (safe area) automatically.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
        ) {
            content()
        }
    }
}

@Composable
fun LoadingScreen(onLoaded: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(1000)
        onLoaded()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "SnapTag",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = "Fast stock entry for shops",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Built with ❤ by Kushagra",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.outline
            )
            Text(
                text = "Version 1.0.0",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}
