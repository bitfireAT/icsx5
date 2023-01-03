package at.bitfire.icsdroid.ui.activity

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.ExperimentalAnimationApi
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
import at.bitfire.icsdroid.ui.data.animatedComposable
import at.bitfire.icsdroid.ui.model.CalendarModel
import at.bitfire.icsdroid.ui.model.EditSubscriptionModel
import at.bitfire.icsdroid.ui.reusable.LoadingBox
import at.bitfire.icsdroid.ui.screens.SubscriptionScreen
import at.bitfire.icsdroid.ui.screens.SubscriptionsScreen
import at.bitfire.icsdroid.ui.theme.setContentThemed
import at.bitfire.icsdroid.utils.toast
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.rememberAnimatedNavController

@OptIn(
    ExperimentalAnimationApi::class,
    ExperimentalMaterialApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalComposeUiApi::class,
)
class MainActivity : AppCompatActivity() {
    companion object {
        object Paths {
            val Subscriptions = NavigationPath("subscriptions")

            val Subscription = NavigationPath("subscription", mapOf("id" to NavType.LongType))
        }
    }

    private val model by viewModels<CalendarModel>()

    private val editModel by viewModels<EditSubscriptionModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentThemed {
            val navController = rememberAnimatedNavController()
            AnimatedNavHost(navController, startDestination = Paths.Subscriptions.route) {
                animatedComposable(Paths.Subscriptions) { SubscriptionsScreen(navController, model) }
                animatedComposable(Paths.Subscription) { entry ->
                    val id = entry.arguments?.getLong("id") ?: run {
                        toast(stringResource(R.string.could_not_load_calendar))
                        navController.navigate(Paths.Subscriptions.route)
                        return@animatedComposable
                    }
                    LaunchedEffect(Unit) {
                        editModel.load(id)
                    }
                    val subscription by editModel.subscription.observeAsState()
                    subscription?.let {
                        SubscriptionScreen(navController, it)
                    } ?: LoadingBox()
                }
            }
        }
    }
}
