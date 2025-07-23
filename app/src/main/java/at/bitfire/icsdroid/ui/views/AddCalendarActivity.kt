/*
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 */

package at.bitfire.icsdroid.ui.views

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.imePadding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.IntentCompat
import androidx.core.view.WindowCompat
import at.bitfire.icsdroid.HttpClient
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.calendar.LocalCalendar
import at.bitfire.icsdroid.model.AddSubscriptionModel
import at.bitfire.icsdroid.ui.screen.AddSubscriptionScreen
import at.bitfire.icsdroid.ui.theme.setContentThemed
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddCalendarActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TITLE = "title"
        const val EXTRA_COLOR = "color"
    }

    private val addSubscriptionModel by viewModels<AddSubscriptionModel>()

    private val pickFile =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) {
                // keep the picked file accessible after the first sync and reboots
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                addSubscriptionModel.subscriptionSettingsRepository.setUrl(uri.toString())

                // Get file name
                val displayName = contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (!cursor.moveToFirst()) return@use null
                    val name = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    cursor.getString(name)
                }
                addSubscriptionModel.subscriptionSettingsRepository.setFileName(displayName)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        configureEdgeToEdge()
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContentThemed {
            val context = LocalContext.current

            // on success, show notification and close activity
            if (addSubscriptionModel.uiState.success) {
                Toast.makeText(context, getString(R.string.add_calendar_created), Toast.LENGTH_LONG).show()
                finish()
            }

            // on error, show error message
            addSubscriptionModel.uiState
                .errorMessage?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }

            // If launched by intent
            LaunchedEffect(intent) {
                with(addSubscriptionModel.subscriptionSettingsRepository) {
                    if (savedInstanceState == null) {
                        intent?.apply {
                            try {
                                (data ?: getStringExtra(Intent.EXTRA_TEXT))
                                    ?.toString()
                                    ?.trim()
                                    ?.let(::setUrl)
                                    ?.also {
                                        addSubscriptionModel.checkUrlIntroductionPage()
                                    }
                            } catch (_: IllegalArgumentException) {
                                // Data does not have a valid url
                            }

                            IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                                ?.toString()
                                ?.let(::setUrl)
                                ?.also {
                                    addSubscriptionModel.checkUrlIntroductionPage()
                                }

                            getStringExtra(EXTRA_TITLE)
                                ?.let(::setTitle)
                            takeIf { hasExtra(EXTRA_COLOR) }
                                ?.getIntExtra(EXTRA_COLOR, LocalCalendar.DEFAULT_COLOR)
                                ?.let(::setColor)
                        }
                    }
                }
            }

            Box(modifier = Modifier.imePadding()) {
                AddSubscriptionScreen(
                    onPickFileRequested = { pickFile.launch(arrayOf("text/calendar")) },
                    finish = ::finish,
                    checkUrlIntroductionPage = addSubscriptionModel::checkUrlIntroductionPage
                )
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

}
