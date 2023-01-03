package at.bitfire.icsdroid.ui.data

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.*
import at.bitfire.icsdroid.Constants
import com.google.accompanist.navigation.animation.composable

class NavigationPath(
    path: String,
    args: Map<String, NavType<*>> = emptyMap(),
) {
    val route: String = listOf(path, *args.map { "{${it.key}}" }.toTypedArray()).joinToString("/")

    val arguments = args.map { navArgument(it.key) { type = it.value } }

    fun navigate(navHostController: NavHostController, vararg args: Pair<String, Any>) {
        var path = route
        args.forEach { (key, value) ->
            path = path.replace("{$key}", value.toString())
        }
        Log.v(Constants.TAG, "Navigating to $path")
        navHostController.navigate(path)
    }
}

@ExperimentalAnimationApi
fun NavGraphBuilder.composable(
    path: NavigationPath,
    deepLinks: List<NavDeepLink> = emptyList(),
    enterTransition: (AnimatedContentScope<NavBackStackEntry>.() -> EnterTransition?)? = null,
    exitTransition: (AnimatedContentScope<NavBackStackEntry>.() -> ExitTransition?)? = null,
    popEnterTransition: (AnimatedContentScope<NavBackStackEntry>.() -> EnterTransition?)? = null,
    popExitTransition: (AnimatedContentScope<NavBackStackEntry>.() -> ExitTransition?)? = null,
    content: @Composable (AnimatedVisibilityScope.(NavBackStackEntry) -> Unit)
) {
    composable(
        path.route,
        path.arguments,
        deepLinks,
        enterTransition,
        exitTransition,
        popEnterTransition,
        popExitTransition,
        content,
    )
}

@ExperimentalAnimationApi
fun NavGraphBuilder.animatedComposable(
    path: NavigationPath,
    deepLinks: List<NavDeepLink> = emptyList(),
    content: @Composable (AnimatedVisibilityScope.(NavBackStackEntry) -> Unit),
) {
    composable(
        path,
        deepLinks = deepLinks,
        enterTransition = {
            slideIntoContainer(
                AnimatedContentScope.SlideDirection.Left,
                animationSpec = tween(400)
            )
        },
        exitTransition = {
            slideOutOfContainer(
                AnimatedContentScope.SlideDirection.Right,
                animationSpec = tween(400)
            )
        },
        popEnterTransition = {
            slideIntoContainer(
                AnimatedContentScope.SlideDirection.Right,
                animationSpec = tween(700)
            )
        },
        popExitTransition = {
            slideOutOfContainer(
                AnimatedContentScope.SlideDirection.Right,
                animationSpec = tween(700)
            )
        },
        content = content,
    )
}
