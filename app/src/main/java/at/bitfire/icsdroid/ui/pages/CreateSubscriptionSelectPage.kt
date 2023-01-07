package at.bitfire.icsdroid.ui.pages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TabRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.ui.model.CreateSubscriptionModel
import at.bitfire.icsdroid.ui.reusable.TabWithTextAndIcon
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.PagerState
import kotlinx.coroutines.CoroutineScope

/**
 * The page that allows the selection of the resource to be selected for the creation of the
 * subscription. Shows a tabbed view with the [CreateSubscriptionFilePage] and
 * [CreateSubscriptionLinkPage] options.
 */
@ExperimentalMaterial3Api
@ExperimentalComposeUiApi
@ExperimentalPagerApi
@Composable
fun ColumnScope.CreateSubscriptionSelectPage(
    pagerState: PagerState,
    model: CreateSubscriptionModel,
    selectedTab: Int,
    onTabSelected: suspend CoroutineScope.(index: Int) -> Unit,
) {
    TabRow(
        selectedTabIndex = selectedTab,
        modifier = Modifier.fillMaxWidth(),
    ) {
        TabWithTextAndIcon(
            selected = selectedTab == 0,
            onClick = { onTabSelected(0) },
            icon = Icons.Rounded.Link,
            text = R.string.add_calendar_subscribe_url,
        )
        TabWithTextAndIcon(
            selected = selectedTab == 1,
            onClick = { onTabSelected(1) },
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
        Column(Modifier.fillMaxSize()) {
            when (page) {
                0 -> CreateSubscriptionLinkPage(model)
                1 -> CreateSubscriptionFilePage(model)
            }
        }
    }
}
