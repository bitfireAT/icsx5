/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.ui.views

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import at.bitfire.icsdroid.Constants
import at.bitfire.icsdroid.HttpClient
import at.bitfire.icsdroid.HttpUtils
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.calendar.LocalCalendar
import at.bitfire.icsdroid.model.CreateSubscriptionModel
import at.bitfire.icsdroid.model.CredentialsModel
import at.bitfire.icsdroid.model.SubscriptionSettingsModel
import at.bitfire.icsdroid.model.ValidationModel
import at.bitfire.icsdroid.ui.ResourceInfo
import at.bitfire.icsdroid.ui.partials.ExtendedTopAppBar
import at.bitfire.icsdroid.ui.theme.lightblue
import at.bitfire.icsdroid.ui.theme.setContentThemed
import java.net.URI
import java.net.URISyntaxException
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrl

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
                subscriptionSettingsModel.url.postValue(uri.toString())

                // Get file name
                val displayName = contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (!cursor.moveToFirst()) return@use null
                    val name = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    cursor.getString(name)
                }
                subscriptionSettingsModel.fileName.postValue(displayName)
            }
        }

    override fun onCreate(inState: Bundle?) {
        super.onCreate(inState)

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

        setContentThemed {
            val pagerState = rememberPagerState { 2 }

            val url: String? by subscriptionSettingsModel.url.observeAsState(null)
            val fileName: String? by subscriptionSettingsModel.fileName.observeAsState(null)
            val urlError: String? by subscriptionSettingsModel.urlError.observeAsState(null)
            val supportsAuthentication: Boolean by subscriptionSettingsModel.supportsAuthentication.observeAsState(false)
            val title by subscriptionSettingsModel.title.observeAsState(null)
            val color by subscriptionSettingsModel.color.observeAsState(null)
            val ignoreAlerts by subscriptionSettingsModel.ignoreAlerts.observeAsState(false)
            val defaultAlarmMinutes by subscriptionSettingsModel.defaultAlarmMinutes.observeAsState(null)
            val defaultAllDayAlarmMinutes by subscriptionSettingsModel.defaultAllDayAlarmMinutes.observeAsState(null)
            val ignoreDescription by subscriptionSettingsModel.ignoreDescription.observeAsState(false)

            val requiresAuth: Boolean by credentialsModel.requiresAuth.observeAsState(false)
            val username: String? by credentialsModel.username.observeAsState(null)
            val password: String? by credentialsModel.password.observeAsState(null)
            val isInsecure: Boolean by credentialsModel.isInsecure.observeAsState(false)

            val isVerifyingUrl: Boolean by validationModel.isVerifyingUrl.observeAsState(false)
            val validationResult: ResourceInfo? by validationModel.result.observeAsState(null)

            val isCreating: Boolean by subscriptionModel.isCreating.observeAsState(false)
            val showNextButton by subscriptionModel.showNextButton.observeAsState(false)

            LaunchedEffect(intent) {
                if (inState == null) {
                    intent?.apply {
                        try {
                            (data ?: getStringExtra(Intent.EXTRA_TEXT))
                                ?.toString()
                                ?.stripUrl()
                                ?.let(subscriptionSettingsModel.url::postValue)
                                ?.also { checkUrlIntroductionPage() }
                        } catch (_: IllegalArgumentException) {
                            // Data does not have a valid url
                        }

                        (intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri)
                            ?.toString()
                            ?.let(subscriptionSettingsModel.url::postValue)
                            ?.also { checkUrlIntroductionPage() }

                        getStringExtra(EXTRA_TITLE)
                            ?.let(subscriptionSettingsModel.title::postValue)
                        takeIf { hasExtra(EXTRA_COLOR) }
                            ?.getIntExtra(EXTRA_COLOR, LocalCalendar.DEFAULT_COLOR)
                            ?.let(subscriptionSettingsModel.color::postValue)
                    }
                }
            }

            // Receive updates for the URL introduction page
            LaunchedEffect(url, requiresAuth, username, password, isVerifyingUrl) {
                checkUrlIntroductionPage()
            }

            // Receive updates for the Details page
            LaunchedEffect(title, color, ignoreAlerts, defaultAlarmMinutes, defaultAllDayAlarmMinutes) {
                subscriptionModel.showNextButton.postValue(!title.isNullOrBlank())
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
                        info.calendarColor ?: lightblue.toArgb()

                if (subscriptionSettingsModel.title.value.isNullOrBlank())
                    subscriptionSettingsModel.title.value =
                        info.calendarName ?: info.uri.toString()

                pagerState.animateScrollToPage(pagerState.currentPage + 1)
            }

            Scaffold(
                topBar = { AddCalendarTopAppBar(pagerState, showNextButton, isVerifyingUrl, isCreating) },
                bottomBar = { AddCalendarBottomAppBar(pagerState, showNextButton, isVerifyingUrl, isCreating) }
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
                            fileName = fileName,
                            onUrlChange = {
                                subscriptionSettingsModel.fileName.value = null // file name is computed from URL
                                subscriptionSettingsModel.url.value = it
                            },
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
                            ignoreDescription = ignoreDescription,
                            onIgnoreDescriptionChanged = subscriptionSettingsModel.ignoreDescription::setValue,
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

    override fun onPause() {
        super.onPause()
        HttpClient.setForeground(false)
    }

    override fun onResume() {
        super.onResume()
        HttpClient.setForeground(true)
    }


    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun AddCalendarTopAppBar(
        pagerState: PagerState,
        showNextButton: Boolean,
        isVerifyingUrl: Boolean,
        isCreating: Boolean
    ) {
        val scope = rememberCoroutineScope()
        ExtendedTopAppBar(
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

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun AddCalendarBottomAppBar(
        pagerState: PagerState,
        showNextButton: Boolean,
        isVerifyingUrl: Boolean,
        isCreating: Boolean
    ) {
        AnimatedVisibility(
            pagerState.currentPage == 0 && showNextButton,
            enter = expandVertically()
        ) {
            BottomAppBar(
                content = {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { onNextRequested(pagerState.currentPage) },
                            enabled = !isVerifyingUrl && !isCreating
                        ) {
                            Text(stringResource(R.string.activity_add_calendar_subscribe).uppercase())
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                stringResource(R.string.activity_add_calendar_subscribe)
                            )
                        }
                    }
                }
            )
        }
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

    private fun checkUrlIntroductionPage() {
        if (validationModel.isVerifyingUrl.value == true) {
            subscriptionModel.showNextButton.postValue(true)
        } else {
            val uri = validateUri()
            val authOK =
                if (credentialsModel.requiresAuth.value == true)
                    !credentialsModel.username.value.isNullOrEmpty() &&
                        !credentialsModel.password.value.isNullOrEmpty()
                else
                    true
            subscriptionModel.showNextButton.postValue(uri != null && authOK)
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

    /**
     * Strips the URL from a string. For example, the following string:
     * ```
     * "This is a URL: https://example.com"
     * ```
     * will return:
     * ```
     * "https://example.com"
     * ```
     * _Quotes are not included_
     * @return The URL found in the string
     * @throws IllegalArgumentException if no URL is found in the string
     */
    private fun String.stripUrl(): String? {
        return "([a-zA-Z]+)://(\\w+)(.\\w+)*[/\\w*]*".toRegex()
            .find(this)
            ?.value
    }

}
