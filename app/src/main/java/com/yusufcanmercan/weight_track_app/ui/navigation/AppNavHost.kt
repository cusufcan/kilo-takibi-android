package com.yusufcanmercan.weight_track_app.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.yusufcanmercan.weight_track_app.R
import com.yusufcanmercan.weight_track_app.platform.SeyirWidget
import com.yusufcanmercan.weight_track_app.ui.edit.EditWeightSheet
import com.yusufcanmercan.weight_track_app.ui.edit.EditWeightViewModel
import com.yusufcanmercan.weight_track_app.ui.home.*
import com.yusufcanmercan.weight_track_app.ui.settings.SettingsScreen
import com.yusufcanmercan.weight_track_app.ui.theme.Spacing
import com.yusufcanmercan.weight_track_app.ui.theme.SeyirTab
import com.yusufcanmercan.weight_track_app.ui.theme.SeyirTabIndicatorHeight
import com.yusufcanmercan.weight_track_app.ui.theme.SeyirTabIndicatorWidth
import com.yusufcanmercan.weight_track_app.ui.theme.SeyirTabVerticalPadding
import androidx.compose.foundation.selection.selectableGroup

sealed interface AppLaunch {
    data object QuickEntry : AppLaunch
    data class OpenWeekly(val throughDay: Long?) : AppLaunch
}

private class TopLevelItem(val route: Any, val labelRes: Int, val icon: Int)

private const val TabTransitionMillis = 150
private const val DetailFadeOutMillis = 60
private const val DetailFadeInMillis = 120
private val TabBarMinHeight = 60.dp

@Composable
fun AppNavHost(
    launch: AppLaunch?,
    displayName: String?,
    onReady: () -> Unit,
    onLaunchHandled: () -> Unit,
) {
    val nav = rememberNavController()
    val backStackEntry by nav.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val context = LocalContext.current
    val homeViewModel: HomeViewModel = hiltViewModel()
    val editViewModel: EditWeightViewModel = hiltViewModel()
    val state by homeViewModel.uiState.collectAsStateWithLifecycle()
    if (!state.isLoading) SideEffect(onReady)
    var editorOpen by rememberSaveable { mutableStateOf(false) }
    var keepEmptyHome by rememberSaveable { mutableStateOf(false) }
    var entryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var savedEntryId by rememberSaveable { mutableStateOf<Long?>(null) }

    fun openEditor(id: Long? = null) {
        keepEmptyHome = !state.isLoading && state.items.isEmpty()
        entryId = id
        savedEntryId = null
        editViewModel.start(id)
        editorOpen = true
    }

    fun openTab(route: Any) {
        if (route == Home) {
            nav.popBackStack<Home>(inclusive = false, saveState = true)
            return
        }
        nav.navigate(route) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = route != Profile
        }
    }

    LaunchedEffect(editorOpen) {
        if (editorOpen && savedEntryId == null && !editViewModel.isStarted) editViewModel.start(entryId)
    }
    LaunchedEffect(launch, destination, editorOpen) {
        if (launch == null || destination == null || editorOpen) return@LaunchedEffect
        onLaunchHandled()
        when (launch) {
            AppLaunch.QuickEntry -> {
                openTab(Home)
                openEditor()
            }
            is AppLaunch.OpenWeekly -> nav.navigate(Weekly(launch.throughDay ?: 0L)) { launchSingleTop = true }
        }
    }

    val tabs = listOf(
        TopLevelItem(Home, R.string.nav_home, R.drawable.ic_seyir_home),
        TopLevelItem(Progress, R.string.nav_progress, R.drawable.ic_seyir_progress),
        TopLevelItem(History, R.string.nav_history, R.drawable.ic_seyir_history),
        TopLevelItem(Profile, R.string.nav_profile, R.drawable.ic_seyir_user),
    )
    fun NavDestination?.isTab() = tabs.any { tab -> this?.hasRoute(tab.route::class) == true }

    val profileDetail = destination?.hasRoute<Settings>() == true || destination?.hasRoute<Milestones>() == true
    val showBottomBar = destination.isTab() || profileDetail
    var lastTab by remember { mutableIntStateOf(0) }
    val activeTab = if (profileDetail) tabs.lastIndex else tabs.indexOfFirst {
        destination?.hasRoute(it.route::class) == true
    }.takeIf { it >= 0 } ?: lastTab
    SideEffect { lastTab = activeTab }
    val indicatorPosition by animateFloatAsState(activeTab.toFloat(), tween(TabTransitionMillis), label = "Active tab")
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val navigationBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    var tabBarHeight by remember(density, navigationBottom) { mutableStateOf(TabBarMinHeight + 1.dp + navigationBottom) }

    @Composable
    fun ScreenWithTabs(content: @Composable () -> Unit) {
        Box(Modifier.fillMaxSize().padding(bottom = tabBarHeight)) { content() }
    }

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = fadeIn(tween(DetailFadeInMillis, delayMillis = DetailFadeOutMillis)),
                exit = fadeOut(tween(DetailFadeOutMillis)),
            ) {
                val colors = MaterialTheme.colorScheme
                Column(
                    Modifier.background(colors.surfaceContainerHigh)
                        .onSizeChanged { size ->
                            tabBarHeight = with(density) { size.height.toDp() }
                        }.navigationBarsPadding(),
                ) {
                    HorizontalDivider(color = colors.outlineVariant)
                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                        val tabWidth = maxWidth / tabs.size
                        Box(
                            Modifier.offset {
                                IntOffset((tabWidth * indicatorPosition + (tabWidth - SeyirTabIndicatorWidth) / 2).roundToPx(),
                                    SeyirTabVerticalPadding.roundToPx())
                            }.width(SeyirTabIndicatorWidth).height(SeyirTabIndicatorHeight)
                                .background(colors.primaryContainer, CircleShape),
                        )
                        Row(Modifier.fillMaxWidth().heightIn(min = TabBarMinHeight).selectableGroup()) {
                            tabs.forEachIndexed { index, tab ->
                                SeyirTab(
                                    label = stringResource(tab.labelRes),
                                    glyph = tab.icon,
                                    selected = index == activeTab,
                                    onClick = { openTab(tab.route) },
                                    modifier = Modifier.weight(1f).heightIn(min = TabBarMinHeight),
                                )
                            }
                        }
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            nav,
            startDestination = Home,
            modifier = Modifier.fillMaxSize().padding(
                start = padding.calculateStartPadding(layoutDirection),
                top = padding.calculateTopPadding(),
                end = padding.calculateEndPadding(layoutDirection),
            ),
            enterTransition = {
                if (initialState.destination.isTab() && targetState.destination.isTab()) fadeIn(tween(TabTransitionMillis))
                else fadeIn(tween(DetailFadeInMillis, delayMillis = DetailFadeOutMillis))
            },
            exitTransition = {
                if (initialState.destination.isTab() && targetState.destination.isTab()) fadeOut(tween(TabTransitionMillis))
                else fadeOut(tween(DetailFadeOutMillis))
            },
        ) {
            composable<Home> {
                ScreenWithTabs {
                    HomeScreen(
                        state,
                        displayName = displayName,
                        holdTrend = editorOpen,
                        keepEmptyHome = editorOpen && keepEmptyHome,
                        onQuickEntry = { openEditor() },
                        onOpenSettings = { nav.navigate(Settings) { launchSingleTop = true } },
                        onOpenProgress = { openTab(Progress) },
                        onOpenHistory = { openTab(History) },
                        onOpenMilestones = { nav.navigate(Milestones) },
                        onOpenWeekly = { nav.navigate(Weekly()) },
                    )
                }
            }
            composable<Progress> {
                ScreenWithTabs {
                    ProgressScreen(state, onAdd = { openEditor() })
                }
            }
            composable<History> {
                ScreenWithTabs {
                    HistoryScreen(state, onEdit = { openEditor(it) }, onAdd = { openEditor() })
                }
            }
            composable<Profile> {
                ScreenWithTabs {
                    ProfileScreen(
                        onSettings = { nav.navigate(Settings) },
                        onMilestones = { nav.navigate(Milestones) },
                    )
                }
            }
            composable<Settings> {
                ScreenWithTabs {
                    SettingsScreen(onBack = { nav.popBackStack() }, hasMeasurements = state.items.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            Card {
                                LinkText(stringResource(R.string.weekly_summary)) { nav.navigate(Weekly()) }
                                if (SeyirWidget.canPin(context)) {
                                    LinkText(stringResource(R.string.widget_pin)) { SeyirWidget.pin(context) }
                                }
                            }
                            if (state.bmi != null) BmiCard(state) {}
                        }
                    }
                }
            }
            composable<Milestones> {
                ScreenWithTabs {
                    MilestonesScreen(
                        state,
                        onBack = { nav.popBackStack() },
                        onSettings = { nav.navigate(Settings) { launchSingleTop = true } },
                    )
                }
            }
            composable<Weekly> { entry ->
                Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))) {
                    WeeklyScreen(state, entry.toRoute<Weekly>().throughDay.takeUnless { it == 0L }) { nav.popBackStack() }
                }
            }
        }
    }

    if (editorOpen) {
        EditWeightSheet(
            viewModel = editViewModel,
            resultId = savedEntryId,
            homeState = state,
            onDismiss = { editorOpen = false },
            onResultDismiss = {
                editorOpen = false
                savedEntryId = null
                nav.popBackStack<Home>(inclusive = false)
            },
            onSaved = { id ->
                SeyirWidget.refresh(context)
                if (id == null) editorOpen = false else savedEntryId = id
            },
        )
    }
}
