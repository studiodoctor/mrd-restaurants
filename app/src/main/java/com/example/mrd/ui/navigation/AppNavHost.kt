package com.example.mrd.ui.navigation

import androidx.activity.compose.BackHandler
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.util.Consumer
import android.content.Intent
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import com.example.mrd.ui.browse.BrowseRoute
import com.example.mrd.ui.detail.DetailRoute
import com.example.mrd.ui.favorites.FavoritesRoute
import com.example.mrd.ui.settings.SettingsRoute
import kotlinx.serialization.Serializable

/** Type-safe destinations. The route class name doubles as the nav-graph contract between squads. */
@Serializable
data object BrowseDestination

@Serializable
data object FavoritesDestination

@Serializable
data object SettingsDestination

@Serializable
data class DetailDestination(val id: String)

/** `mrd://restaurant/{id}` — also declared in the manifest. */
private const val DEEP_LINK_BASE = "mrd://restaurant"

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    // MainActivity is singleTop, so a deep link fired while the app is open arrives in
    // onNewIntent, which Navigation Compose does not watch on its own. Forward it.
    val activity = LocalContext.current.findComponentActivity()
    DisposableEffect(activity, navController) {
        val listener = Consumer<Intent> { intent -> navController.handleDeepLink(intent) }
        activity?.addOnNewIntentListener(listener)
        onDispose { activity?.removeOnNewIntentListener(listener) }
    }

    NavHost(navController = navController, startDestination = BrowseDestination) {
        composable<BrowseDestination> {
            BrowseRoute(
                onRestaurantClick = { id -> navController.navigate(DetailDestination(id)) },
                onSettingsClick = { navController.navigate(SettingsDestination) },
            )
        }
        composable<SettingsDestination> {
            SettingsRoute(
                onBack = { navController.navigateUp() },
                onFavoritesClick = { navController.navigate(FavoritesDestination) },
            )
        }
        composable<FavoritesDestination> {
            FavoritesRoute(
                onBack = { navController.navigateUp() },
                onRestaurantClick = { id -> navController.navigate(DetailDestination(id)) },
            )
        }
        composable<DetailDestination>(
            deepLinks = listOf(navDeepLink<DetailDestination>(basePath = DEEP_LINK_BASE)),
        ) {
            // A deep link can be the *only* entry on the back stack. Up/back must then land on
            // the list rather than closing the app.
            val hasPrevious = navController.previousBackStackEntry != null
            val goBack: () -> Unit = {
                if (hasPrevious) {
                    navController.navigateUp()
                } else {
                    navController.navigate(BrowseDestination) {
                        popUpTo<DetailDestination> { inclusive = true }
                    }
                }
            }
            BackHandler(enabled = !hasPrevious, onBack = goBack)
            DetailRoute(onBack = goBack)
        }
    }
}

private tailrec fun Context.findComponentActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findComponentActivity()
    else -> null
}
