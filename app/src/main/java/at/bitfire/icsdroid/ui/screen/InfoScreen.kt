package at.bitfire.icsdroid.ui.screen

import androidx.annotation.StringRes
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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.text.HtmlCompat
import androidx.datastore.preferences.core.edit
import at.bitfire.icsdroid.BuildConfig
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.Settings.Companion.nextReminder
import at.bitfire.icsdroid.dataStore
import at.bitfire.icsdroid.service.ComposableStartupService
import at.bitfire.icsdroid.service.ComposableStartupService.Companion.FLAG_DONATION_DIALOG
import at.bitfire.icsdroid.ui.partials.ExtendedTopAppBar
import at.bitfire.icsdroid.ui.partials.GenericAlertDialog
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.rememberLibraries
import kotlinx.coroutines.runBlocking
import java.util.ServiceLoader

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun InfoScreen(
    compStartupServices: ServiceLoader<ComposableStartupService>,
    onNavigateUp: () -> Unit
) {
    val resources = LocalResources.current
    val uriHandler = LocalUriHandler.current

    val hasDonateDialogService = compStartupServices.any { it.hasFlag(FLAG_DONATION_DIALOG) }

    Scaffold(
        topBar = {
            ExtendedTopAppBar(
                navigationIcon = {
                    IconButton(onNavigateUp) {
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
                    IconButton(
                        onClick = {
                            uriHandler.openUri("https://icsx5.bitfire.at/?pk_campaign=icsx5-app&pk_kwd=info-activity")
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_public),
                            contentDescription = stringResource(R.string.app_info_web_site)
                        )
                    }
                    IconButton(
                        onClick = {
                            uriHandler.openUri("https://fosstodon.org/@davx5app")
                        }
                    ) {
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
            License(hasDonateDialogService)
            LibrariesContainer(
                libraries = libraries
            )
        }
    }
}

@Composable
private fun Header() {
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
private fun License(
    hasDonateDialogService: Boolean
) {
    val context = LocalContext.current

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
                    context.dataStore.edit { it[nextReminder] = 0 }
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
private fun TextDialog(@StringRes text: Int, state: MutableState<Boolean>) {
    GenericAlertDialog(
        content = { Text(HtmlCompat.fromHtml(
            stringResource(text).replace("\n", "<br/>"),
            HtmlCompat.FROM_HTML_MODE_COMPACT).toString()) },
        confirmButton = stringResource(R.string.edit_calendar_dismiss) to {
            state.value = false
        },
    ) { state.value = false }
}
