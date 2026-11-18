package com.yusufcanmercan.weight_track_app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.yusufcanmercan.weight_track_app.R
import com.yusufcanmercan.weight_track_app.ui.edit.EditWeightScreen
import com.yusufcanmercan.weight_track_app.ui.graph.GraphScreen
import com.yusufcanmercan.weight_track_app.ui.home.HomeScreen
import com.yusufcanmercan.weight_track_app.ui.settings.SettingsScreen

private class TopLevelItem(val route: Any, val labelRes: Int, val icon: ImageVector)

/** Çekirdek ikon setinde çizgi grafik ikonu yok; küçük bir tane elle çizildi. */
private val GraphIcon: ImageVector = ImageVector.Builder(
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).path(
    stroke = SolidColor(androidx.compose.ui.graphics.Color.Black),
    strokeLineWidth = 2f,
    strokeLineCap = StrokeCap.Round,
) {
    moveTo(3f, 17f)
    lineTo(9f, 11f)
    lineTo(13f, 15f)
    lineTo(21f, 7f)
}.build()

/** Tek Activity'nin tüm ekranlarını ve alt çubuğu barındırır (ADR-0004). */
@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination

    val topLevelItems = listOf(
        TopLevelItem(Home, R.string.nav_home, Icons.Default.Home),
        TopLevelItem(Graph, R.string.nav_graph, GraphIcon),
        TopLevelItem(Settings, R.string.nav_settings, Icons.Default.Settings),
    )
    // Alt çubuk yalnız üç ana ekranda görünür; Ekle/Düzenle tam ekran.
    val showBottomBar = topLevelItems.any { destination?.hasRoute(it.route::class) == true }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    topLevelItems.forEach { item ->
                        NavigationBarItem(
                            selected = destination?.hierarchy?.any { it.hasRoute(item.route::class) } == true,
                            onClick = {
                                navController.navigate(item.route) {
                                    // Sekmeler arası geçişte yığın şişmesin, durum korunsun.
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = null) },
                            label = { Text(stringResource(item.labelRes)) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Home,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable<Home> {
                HomeScreen(
                    onAdd = { navController.navigate(EditWeight()) },
                    onEdit = { id -> navController.navigate(EditWeight(id)) },
                    onNewGoal = {
                        navController.navigate(Settings) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
            composable<Graph> { GraphScreen() }
            composable<Settings> { SettingsScreen() }
            composable<EditWeight> {
                EditWeightScreen(onSaved = { navController.popBackStack() })
            }
        }
    }
}
