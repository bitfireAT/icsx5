package at.bitfire.icsdroid.ui.data

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.*
import at.bitfire.icsdroid.Constants
import com.google.accompanist.navigation.animation.composable

/**
 * Defines a destination for a NavGraph.
 * @param path The path of the destination. Must not include arguments, they are added automatically.
 * @param args Arguments to be added to the path.
 */
class NavigationPath(
    path: String,
    args: Map<String, NavType<*>> = emptyMap(),
) {
    /**
     * The resulting route built from the given path and arguments.
     */
    val route: String = listOf(path, *args.map { "{${it.key}}" }.toTypedArray()).joinToString("/")

    /**
     * A list of all the arguments to be used.
     */
    val arguments = args.map { navArgument(it.key) { type = it.value } }

    /**
     * Navigates to the current path using the given [navHostController]. Uses the given [args].
     * @param navHostController The controller to use for the navigation.
     * @param args The arguments for replacing in the navigator.
     * @throws IllegalArgumentException If there are missing arguments in [args].
     */
    fun navigate(navHostController: NavHostController, vararg args: Pair<String, Any>) {
        var path = route
        arguments.forEach { arg ->
            val (key, value) = args.find { it.first == arg.name } ?: throw IllegalArgumentException("Missing argument: ${arg.name}")
            path = path.replace("{$key}", value.toString())
        }
        Log.v(Constants.TAG, "Navigating to $path")
        navHostController.navigate(path)
    }
}

/**
 * Provides an utility function for composing [NavigationPath]s easily and in a more readable form.
 * @param path The navigation path to be composed.
 * @param deepLinks List of deep links to associate with the destinations.
 * @param enterTransition Callback to determine the destination's enter transition.
 * @param exitTransition Callback to determine the destination's exit transition.
 * @param popEnterTransition Callback to determine the destination's pop enter transition.
 * @param popExitTransition Callback to determine the destination's pop exit transition.
 * @param content Composable for the destination
 */
@ExperimentalAnimationApi
fun NavGraphBuilder.composable(
    path: NavigationPath,
    deepLinks: List<NavDeepLink> = emptyList(),
    enterTransition: (AnimatedContentScope<NavBackStackEntry>.() -> EnterTransition?)? = {
        slideIntoContainer(
            AnimatedContentScope.SlideDirection.Left,
            animationSpec = tween(400)
        )
    },
    exitTransition: (AnimatedContentScope<NavBackStackEntry>.() -> ExitTransition?)? = {
        slideOutOfContainer(
            AnimatedContentScope.SlideDirection.Right,
            animationSpec = tween(400)
        )
    },
    popEnterTransition: (AnimatedContentScope<NavBackStackEntry>.() -> EnterTransition?)? = {
        slideIntoContainer(
            AnimatedContentScope.SlideDirection.Right,
            animationSpec = tween(700)
        )
    },
    popExitTransition: (AnimatedContentScope<NavBackStackEntry>.() -> ExitTransition?)? = {
        slideOutOfContainer(
            AnimatedContentScope.SlideDirection.Right,
            animationSpec = tween(700)
        )
    },
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
