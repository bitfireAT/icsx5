package at.bitfire.icsdroid.ui.activity

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavType
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.ui.data.NavigationPath
import at.bitfire.icsdroid.ui.data.composable
import at.bitfire.icsdroid.ui.model.CalendarModel
import at.bitfire.icsdroid.ui.model.CreateSubscriptionModel
import at.bitfire.icsdroid.ui.model.EditSubscriptionModel
import at.bitfire.icsdroid.ui.reusable.LoadingBox
import at.bitfire.icsdroid.ui.screens.CreateSubscription
import at.bitfire.icsdroid.ui.screens.SubscriptionScreen
import at.bitfire.icsdroid.ui.screens.SubscriptionsScreen
import at.bitfire.icsdroid.ui.theme.setContentThemed
import at.bitfire.icsdroid.utils.toast
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.google.accompanist.pager.ExperimentalPagerApi

@OptIn(
    ExperimentalPagerApi::class,
    ExperimentalMaterialApi::class,
    ExperimentalAnimationApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalComposeUiApi::class,
)
class MainActivity : AppCompatActivity() {
    companion object {
        object Paths {
            val Subscriptions = NavigationPath("subscriptions")

            val Subscription = NavigationPath("subscription", mapOf("id" to NavType.LongType))

            val Create = NavigationPath("create")
        }
    }

    private val model by viewModels<CalendarModel>()

    private val editModel by viewModels<EditSubscriptionModel>()

    private val createModel by viewModels<CreateSubscriptionModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentThemed {
            val navController = rememberAnimatedNavController()
            AnimatedNavHost(navController, startDestination = Paths.Subscriptions.route) {
                composable(
                    Paths.Subscriptions,
                    enterTransition = {
                        when (initialState.destination.route) {
                            Paths.Create.route -> slideIntoContainer(
                                AnimatedContentScope.SlideDirection.Right,
                                animationSpec = tween(400)
                            )
                            else -> slideIntoContainer(
                                AnimatedContentScope.SlideDirection.Left,
                                animationSpec = tween(400)
                            )
                        }
                    },
                    exitTransition = {
                        when (targetState.destination.route) {
                            Paths.Create.route -> slideOutOfContainer(
                                AnimatedContentScope.SlideDirection.Left,
                                animationSpec = tween(400)
                            )
                            else -> slideOutOfContainer(
                                AnimatedContentScope.SlideDirection.Right,
                                animationSpec = tween(400)
                            )
                        }
                    }
                ) { SubscriptionsScreen(navController, model) }
                composable(Paths.Subscription) { entry ->
                    val id = entry.arguments?.getLong("id") ?: run {
                        toast(stringResource(R.string.could_not_load_calendar))
                        navController.navigate(Paths.Subscriptions.route)
                        return@composable
                    }
                    LaunchedEffect(Unit) {
                        editModel.load(id)
                    }
                    val subscription by editModel.subscription.observeAsState()
                    subscription?.let {
                        SubscriptionScreen(navController, it)
                    } ?: LoadingBox()
                }
                composable(Paths.Create) { CreateSubscription(navController, createModel) }
            }
        }
    }
}
