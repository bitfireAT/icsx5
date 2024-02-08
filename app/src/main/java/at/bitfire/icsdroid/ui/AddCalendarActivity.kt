/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import at.bitfire.icsdroid.Constants
import at.bitfire.icsdroid.HttpClient
import at.bitfire.icsdroid.HttpUtils
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.calendar.LocalCalendar
import at.bitfire.icsdroid.model.CredentialsModel
import at.bitfire.icsdroid.model.CreateSubscriptionModel
import at.bitfire.icsdroid.model.SubscriptionSettingsModel
import at.bitfire.icsdroid.model.ValidationModel
import com.google.accompanist.themeadapter.material.MdcTheme
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.net.URI
import java.net.URISyntaxException

@OptIn(ExperimentalFoundationApi::class)
class AddCalendarActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TITLE = "title"
        const val EXTRA_COLOR = "color"
    }

    private val subscriptionSettingsModel by viewModels<SubscriptionSettingsModel>()
    private val credentialsModel by viewModels<CredentialsModel>()
    private val validationModel by viewModels<ValidationModel>()
    private val subscriptionModel by viewModels<CreateSubscriptionModel>()

    private val pickFile =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) {
                // keep the picked file accessible after the first sync and reboots
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

                subscriptionSettingsModel.url.value = uri.toString()
            }
        }

    override fun onCreate(inState: Bundle?) {
        super.onCreate(inState)

        if (inState == null) {
            intent?.apply {
                data?.let { uri ->
                    subscriptionSettingsModel.url.value = uri.toString()
                }
                getStringExtra(EXTRA_TITLE)?.let {
                    subscriptionSettingsModel.title.value = it
                }
                if (hasExtra(EXTRA_COLOR))
                    subscriptionSettingsModel.color.value =
                        getIntExtra(EXTRA_COLOR, LocalCalendar.DEFAULT_COLOR)
            }
        }

        subscriptionModel.success.observe(this) { success ->
            if (success) {
                // success, show notification and close activity
                Toast.makeText(this, getString(R.string.add_calendar_created), Toast.LENGTH_LONG).show()

                finish()
            }
        }
        subscriptionModel.errorMessage.observe(this) { message ->
            message?.let { Toast.makeText(this, it, Toast.LENGTH_LONG).show() }
        }

        setContent {
            MdcTheme {
                val pagerState = rememberPagerState { 2 }

                val url: String? by subscriptionSettingsModel.url.observeAsState(null)
                val urlError: String? by subscriptionSettingsModel.urlError.observeAsState(null)
                val supportsAuthentication: Boolean by subscriptionSettingsModel.supportsAuthentication.observeAsState(false)
                val title by subscriptionSettingsModel.title.observeAsState(null)
                val color by subscriptionSettingsModel.color.observeAsState(null)
                val ignoreAlerts by subscriptionSettingsModel.ignoreAlerts.observeAsState(false)
                val defaultAlarmMinutes by subscriptionSettingsModel.defaultAlarmMinutes.observeAsState(null)
                val defaultAllDayAlarmMinutes by subscriptionSettingsModel.defaultAllDayAlarmMinutes.observeAsState(null)

                val requiresAuth: Boolean by credentialsModel.requiresAuth.observeAsState(false)
                val username: String? by credentialsModel.username.observeAsState(null)
                val password: String? by credentialsModel.password.observeAsState(null)
                val isInsecure: Boolean by credentialsModel.isInsecure.observeAsState(false)

                val isVerifyingUrl: Boolean by validationModel.isVerifyingUrl.observeAsState(false)
                val validationResult: ResourceInfo? by validationModel.result.observeAsState(null)

                val isCreating: Boolean by subscriptionModel.isCreating.observeAsState(false)

                var showNextButton by remember { mutableStateOf(false) }

                // Receive updates for the URL introduction page
                LaunchedEffect(url, requiresAuth, username, password, isVerifyingUrl) {
                    if (isVerifyingUrl) {
                        showNextButton = true
                        return@LaunchedEffect
                    }

                    val uri = validateUri()
                    val authOK =
                        if (requiresAuth)
                            !username.isNullOrEmpty() && !password.isNullOrEmpty()
                        else
                            true
                    showNextButton = uri != null && authOK
                }

                // Receive updates for the Details page
                LaunchedEffect(title, color, ignoreAlerts, defaultAlarmMinutes, defaultAllDayAlarmMinutes) {
                    showNextButton = !subscriptionSettingsModel.title.value.isNullOrBlank()
                }

                LaunchedEffect(validationResult) {
                    Log.i("AddCalendarActivity", "Validation result updated: $validationResult")
                    if (validationResult == null || validationResult?.exception != null) return@LaunchedEffect
                    val info = validationResult!!

                    // When a result has been obtained, and it's neither null nor has an exception,
                    // clean the subscriptionSettingsModel, and move the pager to the next page
                    subscriptionSettingsModel.url.value = info.uri.toString()

                    if (subscriptionSettingsModel.color.value == null)
                        subscriptionSettingsModel.color.value =
                            info.calendarColor ?: ContextCompat.getColor(
                                this@AddCalendarActivity,
                                R.color.lightblue
                            )

                    if (subscriptionSettingsModel.title.value.isNullOrBlank())
                        subscriptionSettingsModel.title.value =
                            info.calendarName ?: info.uri.toString()

                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                }

                Scaffold(
                    topBar = { TopAppBar(pagerState, showNextButton, isVerifyingUrl, isCreating) }
                ) { paddingValues ->
                    HorizontalPager(
                        state = pagerState,
                        userScrollEnabled = false,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) { page ->
                        when (page) {
                            0 -> EnterUrlComposable(
                                requiresAuth = requiresAuth,
                                onRequiresAuthChange = credentialsModel.requiresAuth::setValue,
                                username = username,
                                onUsernameChange = credentialsModel.username::setValue,
                                password = password,
                                onPasswordChange = credentialsModel.password::setValue,
                                isInsecure = isInsecure,
                                url = url,
                                onUrlChange = subscriptionSettingsModel.url::setValue,
                                urlError = urlError,
                                supportsAuthentication = supportsAuthentication,
                                isVerifyingUrl = isVerifyingUrl,
                                validationResult = validationResult,
                                onValidationResultDismiss = { validationModel.result.value = null },
                                onPickFileRequested = { pickFile.launch(arrayOf("text/calendar")) },
                                onSubmit = { onNextRequested(1) }
                            )

                            1 -> SubscriptionSettingsComposable(
                                url = url,
                                title = title,
                                titleChanged = subscriptionSettingsModel.title::setValue,
                                color = color,
                                colorChanged = subscriptionSettingsModel.color::setValue,
                                ignoreAlerts = ignoreAlerts,
                                ignoreAlertsChanged = subscriptionSettingsModel.ignoreAlerts::setValue,
                                defaultAlarmMinutes = defaultAlarmMinutes,
                                defaultAlarmMinutesChanged = {
                                    subscriptionSettingsModel.defaultAlarmMinutes.postValue(
                                        it.toLongOrNull()
                                    )
                                },
                                defaultAllDayAlarmMinutes = defaultAllDayAlarmMinutes,
                                defaultAllDayAlarmMinutesChanged = {
                                    subscriptionSettingsModel.defaultAllDayAlarmMinutes.postValue(
                                        it.toLongOrNull()
                                    )
                                },
                                isCreating = isCreating,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        HttpClient.setForeground(false)
    }

    override fun onResume() {
        super.onResume()
        HttpClient.setForeground(true)
    }


    @Composable
    private fun TopAppBar(
        pagerState: PagerState,
        showNextButton: Boolean,
        isVerifyingUrl: Boolean,
        isCreating: Boolean
    ) {
        val scope = rememberCoroutineScope()
        androidx.compose.material.TopAppBar(
            navigationIcon = {
                IconButton(
                    onClick = {
                        // If first page, close activity
                        if (pagerState.currentPage <= 0) finish()
                        // otherwise, go back a page
                        else scope.launch {
                            // Needed for non-first-time validations to trigger following validation result updates
                            validationModel.result.postValue(null)
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    }
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                }
            },
            title = { Text(text = stringResource(R.string.activity_add_calendar)) },
            actions = {
                AnimatedVisibility(visible = showNextButton) {
                    IconButton(
                        onClick = { onNextRequested(pagerState.currentPage) },
                        enabled = !isVerifyingUrl && !isCreating
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                    }
                }
            }
        )
    }


    private fun onNextRequested(page: Int) {
        when (page) {
            // First page (Enter Url)
            0 -> {
                // flush the credentials if auth toggle is disabled
                if (credentialsModel.requiresAuth.value != true) {
                    credentialsModel.username.value = null
                    credentialsModel.password.value = null
                }

                val uri: Uri? = subscriptionSettingsModel.url.value?.let(Uri::parse)
                val authenticate = credentialsModel.requiresAuth.value ?: false

                if (uri != null) {
                    validationModel.validate(
                        uri,
                        if (authenticate) credentialsModel.username.value else null,
                        if (authenticate) credentialsModel.password.value else null
                    )
                }
            }
            // Second page (details and confirm)
            1 -> {
                subscriptionModel.create(subscriptionSettingsModel, credentialsModel)
            }
        }
    }


    /* dynamic changes */

    private fun validateUri(): Uri? {
        var errorMsg: String? = null

        var uri: Uri
        try {
            try {
                uri = Uri.parse(subscriptionSettingsModel.url.value ?: return null)
            } catch (e: URISyntaxException) {
                Log.d(Constants.TAG, "Invalid URL", e)
                errorMsg = e.localizedMessage
                return null
            }

            Log.i(Constants.TAG, uri.toString())

            if (uri.scheme.equals("webcal", true)) {
                uri = uri.buildUpon().scheme("http").build()
                subscriptionSettingsModel.url.value = uri.toString()
                return null
            } else if (uri.scheme.equals("webcals", true)) {
                uri = uri.buildUpon().scheme("https").build()
                subscriptionSettingsModel.url.value = uri.toString()
                return null
            }

            val supportsAuthenticate = HttpUtils.supportsAuthentication(uri)
            subscriptionSettingsModel.supportsAuthentication.value = supportsAuthenticate
            when (uri.scheme?.lowercase()) {
                "content" -> {
                    // SAF file, no need for auth
                }

                "http", "https" -> {
                    // check whether the URL is valid
                    try {
                        uri.toString().toHttpUrl()
                    } catch (e: IllegalArgumentException) {
                        Log.w(Constants.TAG, "Invalid URI", e)
                        errorMsg = e.localizedMessage
                        return null
                    }

                    // extract user name and password from URL
                    uri.userInfo?.let { userInfo ->
                        val credentials = userInfo.split(':')
                        credentialsModel.requiresAuth.value = true
                        credentialsModel.username.value = credentials.elementAtOrNull(0)
                        credentialsModel.password.value = credentials.elementAtOrNull(1)

                        val urlWithoutPassword =
                            URI(uri.scheme, null, uri.host, uri.port, uri.path, uri.query, null)
                        subscriptionSettingsModel.url.value = urlWithoutPassword.toString()
                        return null
                    }
                }

                else -> {
                    errorMsg = getString(R.string.add_calendar_need_valid_uri)
                    return null
                }
            }

            // warn if auth. required and not using HTTPS
            credentialsModel.isInsecure.value =
                credentialsModel.requiresAuth.value == true && !uri.scheme.equals("https", true)
        } finally {
            subscriptionSettingsModel.urlError.value = errorMsg
        }
        return uri
    }

}
