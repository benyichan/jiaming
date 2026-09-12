package com.xiaojiaoyin.jiaming.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

object Routes {
    const val HOME = "home"
    const val PROFILE = "profile"
    const val CANDIDATES = "candidates"
    const val EVIDENCE = "evidence/{given}"
    const val FAVORITES = "favorites"
    const val TOOLBOX = "toolbox"

    fun evidence(given: String) = "evidence/$given"
}

@Composable
fun JiamingNav(vm: MainViewModel) {
    val nav = rememberNavController()
    val profile by vm.profileFlow.collectAsState(initial = null)
    val showBottom = currentRoute(nav) in setOf(Routes.HOME, Routes.FAVORITES, Routes.TOOLBOX)

    Scaffold(
        bottomBar = {
            if (showBottom) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute(nav) == Routes.HOME,
                        onClick = { nav.navigate(Routes.HOME) { launchSingleTop = true; popUpTo(Routes.HOME) } },
                        icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                        label = { Text("首页") },
                    )
                    NavigationBarItem(
                        selected = currentRoute(nav) == Routes.TOOLBOX,
                        onClick = { nav.navigate(Routes.TOOLBOX) { launchSingleTop = true } },
                        icon = { Icon(Icons.Filled.Build, contentDescription = null) },
                        label = { Text("工具箱") },
                    )
                    NavigationBarItem(
                        selected = currentRoute(nav) == Routes.FAVORITES,
                        onClick = { nav.navigate(Routes.FAVORITES) { launchSingleTop = true } },
                        icon = { Icon(Icons.Filled.Favorite, contentDescription = null) },
                        label = { Text("收藏") },
                    )
                }
            }
        },
    ) { pad ->
        NavHost(
            navController = nav,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(pad),
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    profile = profile?.toDomain(),
                    vm = vm,
                    onEditProfile = { nav.navigate(Routes.PROFILE) },
                    onOpenCandidates = { nav.navigate(Routes.CANDIDATES) },
                )
            }
            composable(Routes.PROFILE) {
                ProfileEditScreen(
                    initial = profile?.toDomain(),
                    onSave = {
                        vm.saveProfile(it)
                        nav.popBackStack()
                    },
                )
            }
            composable(Routes.CANDIDATES) {
                CandidatesScreen(
                    vm = vm,
                    onOpen = { given -> nav.navigate(Routes.evidence(given)) },
                )
            }
            composable(Routes.EVIDENCE) { entry ->
                val given = entry.arguments?.getString("given") ?: ""
                EvidencePanelScreen(
                    vm = vm,
                    given = given,
                    profile = profile?.toDomain(),
                    onBack = { nav.popBackStack() },
                )
            }
            composable(Routes.FAVORITES) {
                FavoritesScreen(vm = vm)
            }
            composable(Routes.TOOLBOX) {
                ToolboxScreen(vm = vm)
            }
        }
    }
}

@Composable
private fun currentRoute(nav: NavHostController): String? =
    nav.currentBackStackEntryAsState().value?.destination?.route
