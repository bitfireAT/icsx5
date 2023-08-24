/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.with
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Link
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import at.bitfire.icsdroid.IntentUtils.share
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.UriUtils
import at.bitfire.icsdroid.UriUtils.getFileName
import at.bitfire.icsdroid.ui.logic.BackHandlerCompat
import at.bitfire.icsdroid.ui.logic.ExceptionUtils.annotatedString
import at.bitfire.icsdroid.ui.subscription.SubscriptionCredentials
import at.bitfire.icsdroid.ui.subscription.SubscriptionCredentialsModel
import at.bitfire.icsdroid.ui.subscription.SubscriptionSettingsModel
import at.bitfire.icsdroid.ui.subscription.SubscriptionValidationModel
import com.google.accompanist.themeadapter.material.MdcTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

@OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalAnimationApi::class,
    ExperimentalComposeUiApi::class
)
class AddCalendarActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TITLE = "title"
        const val EXTRA_COLOR = "color"

        const val RESULT_SUBSCRIPTION_ID = "subscriptionId"
    }

    private val subscriptionSettingsModel by viewModels<SubscriptionSettingsModel>()
    private val subscriptionCredentialsModel by viewModels<SubscriptionCredentialsModel>()

    private val validationModel by viewModels<SubscriptionValidationModel>()

    /**
     * Provides a contract for launching requests of new subscription creation. If the creation was
     * successful, the result matches the new subscription's ID. Otherwise it returns null.
     */
    object Contract : ActivityResultContract<Contract.Data, Long?>() {
        /**
         * Contains all the input data that can be passed to the Activity when launching.
         *
         * @param title if any, the default title of the new subscription.
         * @param color if any, the default color of the new subscription.
         */
        data class Data(
            val title: String?,
            @ColorInt val color: Int?
        )

        override fun createIntent(context: Context, input: Data): Intent =
            Intent(context, AddCalendarActivity::class.java).apply {
                putExtra(EXTRA_TITLE, input.title)
                putExtra(EXTRA_COLOR, input.color)
            }

        override fun parseResult(resultCode: Int, intent: Intent?): Long? {
            if (resultCode == Activity.RESULT_OK) {
                return intent?.getLongExtra(RESULT_SUBSCRIPTION_ID, -1)?.takeIf { it >= 0 }
            }
            return null
        }
    }

    private val pickFile =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) {
                // keep the picked file accessible after the first sync and reboots
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

                subscriptionSettingsModel.url.postValue(uri.toString())
            }
        }

    override fun onCreate(inState: Bundle?) {
        super.onCreate(inState)

        if (inState == null) {
            intent?.let(subscriptionSettingsModel::loadFromIntent)
        }

        setContent {
            MdcTheme {
                val softwareKeyboard = LocalSoftwareKeyboardController.current

                val coroutineScope = rememberCoroutineScope()
                val pagerState = rememberPagerState()

                // FIXME - When #176 is merged, this will be mutableIntStateOf
                var currentPage by remember { mutableStateOf(0) }

                LaunchedEffect(pagerState) {
                    snapshotFlow { pagerState.currentPage }.collect { currentPage = it }
                }

                val url by subscriptionSettingsModel.url.observeAsState()

                val validationResult by validationModel.result.observeAsState()

                LaunchedEffect(validationResult) {
                    snapshotFlow { validationResult }
                        .distinctUntilChanged()
                        .filterNotNull()
                        .collect { result ->
                            val exception = result.exception
                            if (exception != null) {
                                // There has been an error, go back
                                coroutineScope.launch { pagerState.animateScrollToPage(0) }
                            } else {
                                // Link is fine, go to next page
                                coroutineScope.launch { pagerState.animateScrollToPage(2) }
                            }
                        }
                }

                fun onBack() {
                    when (pagerState.currentPage) {
                        // If in the first page, exit Activity
                        0 -> {
                            setResult(Activity.RESULT_CANCELED)
                            finish()
                        }
                        // The second page only shows progress, so ignore, and wait until completes
                        1 -> {}
                        // The third page is the last one, if back is pressed, don't go to the
                        // second one, skip to the first
                        2 -> {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(0)
                            }
                        }
                    }
                }

                fun onNext() {
                    when (currentPage) {
                        0 -> {
                            url?.let { uri ->
                                // Hide the software keyboard if any
                                softwareKeyboard?.hide()

                                if (uri.startsWith("content", true)) {
                                    // Scroll to the third page
                                    coroutineScope.launch { pagerState.animateScrollToPage(3) }
                                } else if (UriUtils.isValidUri(uri)) {
                                    // Scroll to the second page
                                    coroutineScope.launch { pagerState.animateScrollToPage(1) }
                                    // Validate the introduced url
                                    validationModel.validate(
                                        Uri.parse(uri),
                                        subscriptionCredentialsModel.username.value,
                                        subscriptionCredentialsModel.password.value
                                    )
                                } else {

                                }
                            }
                        }

                        1 -> {
                            // not allowed
                        }

                        2 -> {
                            // TODO - create the subscription
                        }
                    }
                }

                BackHandlerCompat(onBack = ::onBack)

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(stringResource(R.string.activity_add_calendar))
                            },
                            navigationIcon = {
                                AnimatedVisibility(
                                    visible = currentPage != 1,
                                    enter = slideInHorizontally { -it },
                                    exit = slideOutHorizontally { -it }
                                ) {
                                    IconButton(onClick = ::onBack) {
                                        Icon(
                                            Icons.Rounded.ArrowBack,
                                            stringResource(R.string.action_back)
                                        )
                                    }
                                }
                            },
                            actions = {
                                AnimatedVisibility(
                                    visible = when (currentPage) {
                                        0 -> url?.let { UriUtils.isValidUri(it) } ?: false
                                        else -> false
                                    },
                                    enter = slideInHorizontally { it },
                                    exit = slideOutHorizontally { it }
                                ) {
                                    IconButton(onClick = ::onNext) {
                                        Icon(
                                            Icons.Rounded.ArrowForward,
                                            stringResource(R.string.action_next)
                                        )
                                    }
                                }
                            }
                        )
                    }
                ) { paddingValues ->
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        AnimatedContent(
                            targetState = validationResult?.exception,
                            label = "animate-error-card",
                            transitionSpec = {
                                slideInVertically { -it } with slideOutVertically { -it }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                                .align(Alignment.BottomCenter)
                                .zIndex(9999f)
                        ) { exception ->
                            if (exception != null) {
                                ExceptionCard(exception)
                            }
                        }

                        HorizontalPager(
                            key = { it },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues),
                            pageCount = 3,
                            state = pagerState,
                            userScrollEnabled = false
                        ) { page ->
                            Column(modifier = Modifier.fillMaxSize()) {
                                when (page) {
                                    0 -> SourceSelectionPage(onNextRequested = ::onNext)
                                    1 -> CheckingPage()
                                    2 -> DetailsPage()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * The first page of the scroll view. Shows the options to select the url to subscribe to, or
     * the ICS file.
     */
    @Composable
    @Suppress("UnusedReceiverParameter")
    fun ColumnScope.SourceSelectionPage(onNextRequested: () -> Unit) {
        val coroutineScope = rememberCoroutineScope()
        val pagerState = rememberPagerState()

        // FIXME - When #176 is merged, this will be mutableIntStateOf
        var currentTab by remember { mutableStateOf(0) }

        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.currentPage }
                .distinctUntilChanged()
                .collect { currentTab = it }
        }

        TabRow(selectedTabIndex = currentTab) {
            Tab(
                selected = currentTab == 0,
                text = { Text(stringResource(R.string.add_calendar_type_url)) },
                icon = { Icon(Icons.Rounded.Link, stringResource(R.string.add_calendar_type_url)) },
                onClick = { coroutineScope.launch { pagerState.animateScrollToPage(0) } }
            )
            Tab(
                selected = currentTab == 1,
                text = { Text(stringResource(R.string.add_calendar_type_file)) },
                icon = {
                    Icon(
                        Icons.Rounded.FolderOpen,
                        stringResource(R.string.add_calendar_type_file)
                    )
                },
                onClick = { coroutineScope.launch { pagerState.animateScrollToPage(1) } }
            )
        }
        HorizontalPager(
            pageCount = 2,
            state = pagerState,
            key = { "source-$it" }
        ) { page ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                when (page) {
                    0 -> UrlDetailsPage(onNextRequested)
                    1 -> FileDetailsPage()
                }
            }
        }
    }

    @Composable
    fun ColumnScope.UrlDetailsPage(onNextRequested: () -> Unit) {
        val url by subscriptionSettingsModel.url.observeAsState(initial = "")

        TextField(
            value = url,
            onValueChange = {
                subscriptionSettingsModel.url.value = it

                // When url modified, invalidate validation result
                validationModel.result.postValue(null)
            },
            label = { Text(stringResource(R.string.add_calendar_url_text)) },
            placeholder = { Text(stringResource(R.string.add_calendar_url_sample)) },
            modifier = Modifier.fillMaxWidth(),
            isError = !UriUtils.isValidUri(url),
            singleLine = true,
            maxLines = 1,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Go
            ),
            keyboardActions = KeyboardActions {
                onNextRequested()
            }
        )

        AnimatedVisibility(
            visible = !UriUtils.isValidUri(url)
        ) {
            Text(
                text = stringResource(R.string.add_calendar_need_valid_uri),
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.error,
                modifier = Modifier.fillMaxWidth()
            )
        }

        SubscriptionCredentials(
            subscriptionCredentialsModel
        )
    }

    @Composable
    @Suppress("UnusedReceiverParameter")
    fun ColumnScope.FileDetailsPage() {
        val context = LocalContext.current

        val url by subscriptionSettingsModel.url.observeAsState(initial = "")

        TextField(
            value = if (UriUtils.isValidUri(url)) {
                Uri.parse(url).getFileName(context) ?: ""
            } else {
                ""
            },
            onValueChange = { },
            label = { Text(stringResource(R.string.add_calendar_pick_file)) },
            modifier = Modifier
                .fillMaxWidth()
                .focusProperties { canFocus = false },
            singleLine = true,
            readOnly = true,
            maxLines = 1,
            interactionSource = remember { MutableInteractionSource() }
                .also { interactionSource ->
                    LaunchedEffect(interactionSource) {
                        interactionSource.interactions.collect {
                            if (it is PressInteraction.Release) {
                                pickFile.launch(arrayOf("text/calendar"))
                            }
                        }
                    }
                }
        )
    }

    /**
     * User can't interact in this page, background check is being performed.
     */
    @Composable
    @Suppress("UnusedReceiverParameter")
    fun ColumnScope.CheckingPage() {
        Card(
            modifier = Modifier.padding(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
            ) {
                val url by subscriptionSettingsModel.url.observeAsState()

                CircularProgressIndicator()

                Column(
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.add_calendar_validating),
                        style = MaterialTheme.typography.subtitle1
                    )
                    url?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.caption
                        )
                    }
                }
            }
        }
    }

    /**
     * The user can introduce all the details of the subscription. This includes name, color...
     */
    @Composable
    @Suppress("UnusedReceiverParameter")
    fun ColumnScope.DetailsPage() {

    }

    @Composable
    fun ExceptionCard(exception: Exception) {
        val clipboardManager = LocalClipboardManager.current

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = 5.dp
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.add_calendar_error_title),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.subtitle1
                    )
                    IconButton(
                        onClick = { validationModel.result.postValue(null) }
                    ) {
                        Icon(Icons.Rounded.Close, stringResource(R.string.action_close))
                    }
                }

                val errorColor = MaterialTheme.colors.error
                val exceptionText = remember { exception.annotatedString(errorColor) }
                Text(
                    text = exceptionText,
                    style = MaterialTheme.typography.caption,
                    color = errorColor,
                    modifier = Modifier
                        .heightIn(max = 150.dp)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState())
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = { clipboardManager.setText(exceptionText) }
                    ) {
                        Text(stringResource(R.string.action_copy))
                    }
                    TextButton(onClick = { share(exceptionText.text) }) {
                        Text(stringResource(R.string.action_share))
                    }
                }
            }
        }
    }

}
