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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import at.bitfire.icsdroid.Constants
import at.bitfire.icsdroid.HttpClient
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.calendar.LocalCalendar
import at.bitfire.icsdroid.model.CreateSubscriptionModel
import at.bitfire.icsdroid.model.CredentialsModel
import at.bitfire.icsdroid.model.SubscriptionSettingsModel
import at.bitfire.icsdroid.model.ValidationModel
import at.bitfire.icsdroid.ui.screen.AddCalendarScreen
import at.bitfire.icsdroid.ui.theme.lightblue
import at.bitfire.icsdroid.ui.theme.setContentThemed
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.net.URI
import java.net.URISyntaxException

class AddCalendarActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TITLE = "title"
        const val EXTRA_COLOR = "color"
    }

    private val subscriptionSettingsModel by viewModels<SubscriptionSettingsModel>()
    private val credentialsModel by viewModels<CredentialsModel>()
    private val validationModel by viewModels<ValidationModel>()
    private val createSubscriptionModel by viewModels<CreateSubscriptionModel>()

    private val pickFile =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) {
                // keep the picked file accessible after the first sync and reboots
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                subscriptionSettingsModel.setUrl(uri.toString())

                // Get file name
                val displayName = contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (!cursor.moveToFirst()) return@use null
                    val name = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    cursor.getString(name)
                }
                subscriptionSettingsModel.setFileName(displayName)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentThemed {
            val context = LocalContext.current

            // on success, show notification and close activity
            if (createSubscriptionModel.uiState.success) {
                Toast.makeText(context, getString(R.string.add_calendar_created), Toast.LENGTH_LONG).show()
                finish()
            }

            // on error, show error message
            createSubscriptionModel.uiState
                .errorMessage?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }

            // If launched by intent
            LaunchedEffect(intent) {
                if (savedInstanceState == null) {
                    intent?.apply {
                        try {
                            (data ?: getStringExtra(Intent.EXTRA_TEXT))
                                ?.toString()
                                ?.stripUrl()
                                ?.let(subscriptionSettingsModel::setUrl)
                                ?.also {
                                    createSubscriptionModel.checkUrlIntroductionPage(
                                        subscriptionSettingsModel,
                                        validationModel,
                                        credentialsModel
                                    )
                                }
                        } catch (_: IllegalArgumentException) {
                            // Data does not have a valid url
                        }

                        (intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri)
                            ?.toString()
                            ?.let(subscriptionSettingsModel::setUrl)
                            ?.also {
                                createSubscriptionModel.checkUrlIntroductionPage(
                                    subscriptionSettingsModel,
                                    validationModel,
                                    credentialsModel
                                )
                            }

                        getStringExtra(EXTRA_TITLE)
                            ?.let(subscriptionSettingsModel::setTitle)
                        takeIf { hasExtra(EXTRA_COLOR) }
                            ?.getIntExtra(EXTRA_COLOR, LocalCalendar.DEFAULT_COLOR)
                            ?.let(subscriptionSettingsModel::setColor)
                    }
                }
            }

            AddCalendarScreen(
                createSubscriptionModel = createSubscriptionModel,
                subscriptionSettingsModel = subscriptionSettingsModel,
                credentialsModel = credentialsModel,
                validationModel = validationModel,
                onPickFileRequested = { pickFile.launch(arrayOf("text/calendar")) },
                finish = ::finish,
                checkUrlIntroductionPage = {
                    createSubscriptionModel.checkUrlIntroductionPage(
                        subscriptionSettingsModel,
                        validationModel,
                        credentialsModel
                    )
                }
            )
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
