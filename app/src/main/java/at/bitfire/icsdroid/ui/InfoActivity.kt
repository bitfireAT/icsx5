/*
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 */

package at.bitfire.icsdroid.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import androidx.core.text.HtmlCompat
import androidx.datastore.preferences.core.edit
import at.bitfire.icsdroid.BuildConfig
import at.bitfire.icsdroid.Constants
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.Settings.Companion.nextReminder
import at.bitfire.icsdroid.dataStore
import at.bitfire.icsdroid.service.ComposableStartupService
import at.bitfire.icsdroid.service.ComposableStartupService.Companion.FLAG_DONATION_DIALOG
import at.bitfire.icsdroid.ui.partials.ExtendedTopAppBar
import at.bitfire.icsdroid.ui.partials.GenericAlertDialog
import at.bitfire.icsdroid.ui.theme.setContentThemed
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.rememberLibraries
import kotlinx.coroutines.runBlocking
import java.util.ServiceLoader

class InfoActivity: AppCompatActivity() {

    /**
     * Will get updated when the activity is created.
     * Stores whether the donate dialog should be shown or not.
     */
    private var hasDonateDialogService: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Init and collect all ComposableStartupServices
        val compStartupServices = ServiceLoader.load(ComposableStartupService::class.java)
            .also { srv ->
                hasDonateDialogService = srv.any { it.hasFlag(FLAG_DONATION_DIALOG) }
            }

        setContentThemed {
            compStartupServices.forEach { service ->
                val show: Boolean by service.shouldShow()
                if (show) service.Content()
            }

            MainLayout()
        }
    }

    private fun showWebSite() {
        launchUri("https://icsx5.bitfire.at/?pk_campaign=icsx5-app&pk_kwd=info-activity".toUri())
    }

    private fun showMastodon() {
        launchUri("https://fosstodon.org/@davx5app".toUri())
    }

    private fun launchUri(uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW, uri)
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, getString(R.string.no_browser), Toast.LENGTH_LONG).show()
            Log.w(Constants.TAG, "No browser to view $uri")
        }
    }


    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    @Preview
    fun MainLayout() {
        Scaffold(
            topBar = {
                ExtendedTopAppBar(
                    navigationIcon = {
                        IconButton({ onNavigateUp() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null
                            )
                        }
                    },
                    title = {
                        Text(
                            stringResource(R.string.app_name)
                        )
                    },
                    actions = {
                        IconButton({ showWebSite() }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_public),
                                contentDescription = stringResource(R.string.app_info_web_site)
                            )
                        }
                        IconButton({ showMastodon() }) {
                            Icon(
                                painter = painterResource(R.drawable.mastodon_white),
                                contentDescription = stringResource(R.string.app_info_mastodon)
                            )
                        }
                    }
                )
            }
        ) { contentPadding ->
            val libraries by rememberLibraries {
                resources.openRawResource(R.raw.aboutlibraries).bufferedReader().use { input ->
                    input.readText()
                }
            }

            Column(Modifier.padding(contentPadding)) {
                Header()
                License()
                LibrariesContainer(
                    libraries = libraries
                )
            }
        }
    }

    @Composable
    fun Header() {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val context = LocalContext.current

            Image(
                bitmap = context.applicationInfo
                    .loadIcon(context.packageManager)
                    .toBitmap()
                    .asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(72.dp)
            )
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = stringResource(
                    R.string.app_info_version,
                    BuildConfig.VERSION_NAME,
                    BuildConfig.FLAVOR
                ),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Normal
            )
            Text(
                text = stringResource(R.string.app_info_copyright),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            )
        }
    }

    @Composable
    fun License() {
        val showLicenseDialog = rememberSaveable { mutableStateOf(false) }
        if (showLicenseDialog.value)
            TextDialog(R.string.app_info_gplv3_note, showLicenseDialog)

        val showDonateDialog = rememberSaveable { mutableStateOf(false) }
        if (showDonateDialog.value)
            TextDialog(R.string.donate_message, showDonateDialog)

        Row {
            OutlinedButton(
                onClick = { showLicenseDialog.value = true },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
            ) {
                Text(stringResource(R.string.app_info_gplv3))
            }
            OutlinedButton(
                onClick = {
                    if (hasDonateDialogService) runBlocking {
                        // If there's a donate dialog service, show the dialog
                        dataStore.edit { it[nextReminder] = 0 }
                    } else {
                        // If there's no service, show the donate dialog directly
                        showDonateDialog.value = true
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
            ) {
                Text(stringResource(R.string.app_info_donate))
            }
        }
    }

    @Composable
    fun TextDialog(@StringRes text: Int, state: MutableState<Boolean>) {
        GenericAlertDialog(
            content = { Text(HtmlCompat.fromHtml(
                    getString(text).replace("\n", "<br/>"),
                    HtmlCompat.FROM_HTML_MODE_COMPACT).toString()) },
            confirmButton = stringResource(R.string.edit_calendar_dismiss) to {
                state.value = false
            },
        ) { state.value = false }
    }

}