package at.bitfire.icsdroid.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.ui.activity.MainActivity.Companion.Paths
import at.bitfire.icsdroid.ui.pages.CreateSubscriptionFilePage
import at.bitfire.icsdroid.ui.pages.CreateSubscriptionLinkPage
import at.bitfire.icsdroid.ui.reusable.TabWithTextAndIcon
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState

@Composable
@ExperimentalPagerApi
@ExperimentalMaterial3Api
fun CreateSubscription(navHostController: NavHostController) {
    var selectedTab by remember { mutableStateOf(0) }

    val pagerState = rememberPagerState()
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { selectedTab = it }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.activity_add_calendar)) },
                navigationIcon = {
                    IconButton(onClick = { Paths.Subscriptions.navigate(navHostController) }) {
                        Icon(
                            Icons.Rounded.KeyboardArrowLeft,
                            stringResource(R.string.edit_calendar_cancel),
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 8.dp),
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth(),
            ) {
                TabWithTextAndIcon(
                    selected = selectedTab == 0,
                    onClick = { pagerState.animateScrollToPage(0) },
                    icon = Icons.Rounded.Link,
                    text = R.string.add_calendar_subscribe_url,
                )
                TabWithTextAndIcon(
                    selected = selectedTab == 1,
                    onClick = { pagerState.animateScrollToPage(1) },
                    icon = Icons.Rounded.Folder,
                    text = R.string.add_calendar_subscribe_file,
                )
            }
            HorizontalPager(
                count = 2,
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                when (page) {
                    0 -> CreateSubscriptionLinkPage()
                    1 -> CreateSubscriptionFilePage()
                }
            }
        }
    }
}
