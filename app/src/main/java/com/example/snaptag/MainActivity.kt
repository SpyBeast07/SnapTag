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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.snaptag.ui.components.BottomNavBar
import com.example.snaptag.ui.screens.AboutScreen
import com.example.snaptag.ui.screens.BillingScreen
import com.example.snaptag.ui.screens.SalesScreen
import com.example.snaptag.ui.screens.SettingsScreen
import com.example.snaptag.ui.screens.StatsScreen
import com.example.snaptag.ui.screens.StocksScreen
import com.example.snaptag.ui.theme.SnapTagTheme
import com.example.snaptag.viewmodel.BillingViewModel
import com.example.snaptag.viewmodel.ProductViewModel
import com.example.snaptag.viewmodel.ProductViewModelFactory
import com.example.snaptag.viewmodel.StatsViewModel
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = (application as SnapTagApp).repository
        val viewModelFactory = ProductViewModelFactory(repository, application)
        val sharedPrefs = getSharedPreferences("SnapTagPrefs", MODE_PRIVATE)
        val initialTheme = sharedPrefs.getString("theme_mode", "system") ?: "system"

        setContent {
            val productViewModel: ProductViewModel = viewModel(factory = viewModelFactory)
            val themeMode by productViewModel.themeMode.collectAsState(initialTheme)

            val isDark = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            val context = LocalContext.current
            LaunchedEffect(isDark) {
                val window = (context as? android.app.Activity)?.window
                if (window != null) {
                    val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                    insetsController.isAppearanceLightStatusBars = !isDark
                    insetsController.isAppearanceLightNavigationBars = !isDark
                    window.statusBarColor = Color.Transparent.toArgb()
                    window.navigationBarColor = Color.Transparent.toArgb()
                }
            }

            SnapTagTheme(themeMode = themeMode) {
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
            val activity = LocalContext.current as ComponentActivity
            MainScaffold(navController) {
                StocksScreen(
                    viewModelStoreOwner = activity,
                    viewModelFactory = viewModelFactory
                )
            }
        }
        composable("billing") {
            val activity = LocalContext.current as ComponentActivity
            MainScaffold(navController) {
                val billingViewModel: BillingViewModel = viewModel(
                    viewModelStoreOwner = activity,
                    factory = viewModelFactory
                )
                val productViewModel: ProductViewModel = viewModel(
                    viewModelStoreOwner = activity,
                    factory = viewModelFactory
                )
                BillingScreen(
                    viewModel = billingViewModel,
                    productViewModel = productViewModel
                )
            }
        }
        composable("stats") {
            val activity = LocalContext.current as ComponentActivity
            MainScaffold(navController) {
                val statsViewModel: StatsViewModel = viewModel(
                    viewModelStoreOwner = activity,
                    factory = viewModelFactory
                )
                StatsScreen(viewModel = statsViewModel)
            }
        }
        composable("sales") {
            val activity = LocalContext.current as ComponentActivity
            MainScaffold(navController) {
                val statsViewModel: StatsViewModel = viewModel(
                    viewModelStoreOwner = activity,
                    factory = viewModelFactory
                )
                SalesScreen(viewModel = statsViewModel)
            }
        }
        composable("settings") {
            val activity = LocalContext.current as ComponentActivity
            MainScaffold(navController) {
                SettingsScreen(
                    viewModelStoreOwner = activity,
                    viewModelFactory = viewModelFactory,
                    onNavigateToAbout = { navController.navigate("about") }
                )
            }
        }
        composable("about") {
            MainScaffold(navController) {
                AboutScreen(onNavigateBack = { navController.popBackStack() })
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
        bottomBar = { BottomNavBar(navController) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        // The padding here includes bottom navigation height and system insets.
        // We apply only the bottom padding and consume it to prevent double-padding in inner Scaffolds.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
                .consumeWindowInsets(PaddingValues(bottom = padding.calculateBottomPadding()))
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

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
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
}
