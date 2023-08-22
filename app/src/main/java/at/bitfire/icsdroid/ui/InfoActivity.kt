/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toBitmap
import androidx.core.text.HtmlCompat
import at.bitfire.icsdroid.BuildConfig
import at.bitfire.icsdroid.Constants
import at.bitfire.icsdroid.R
import com.google.accompanist.themeadapter.material.MdcTheme
import com.mikepenz.aboutlibraries.ui.compose.LibrariesContainer

class InfoActivity: ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MainLayout()
        }
    }

    fun showWebSite() {
        launchUri(Uri.parse("https://icsx5.bitfire.at/?pk_campaign=icsx5-app&pk_kwd=info-activity"))
    }

    fun showMastodon() {
        launchUri(Uri.parse("https://fosstodon.org/@davx5app"))
    }

    private fun launchUri(uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW, uri)
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, getString(R.string.no_browser), Toast.LENGTH_LONG).show()
            Log.w(Constants.TAG, "No browser to view $uri")
        }
    }


    @Composable
    @Preview
    fun MainLayout() {
        MdcTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        navigationIcon = {
                            IconButton({ onNavigateUp() }) {
                                Icon(
                                    imageVector = Icons.Filled.ArrowBack,
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
                Column(Modifier.padding(contentPadding)) {
                    Header()
                    License()
                    LibrariesContainer()
                }
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
                style = MaterialTheme.typography.h5,
                color = MaterialTheme.colors.onBackground
            )
            Text(
                text = stringResource(
                    R.string.app_info_version,
                    BuildConfig.VERSION_NAME,
                    BuildConfig.FLAVOR
                ),
                style = MaterialTheme.typography.subtitle1,
                color = MaterialTheme.colors.onBackground,
                modifier = Modifier.alpha(ContentAlpha.medium)
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
                onClick = { showDonateDialog.value = true },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
            ) {
                Text(stringResource(R.string.app_info_donate))
            }
        }
    }

    @Composable
    fun TextDialog(@StringRes text: Int, state: MutableState<Boolean>, buttons: @Composable () -> Unit = {}) {
        AlertDialog(
            text = {
                AndroidView({ context ->
                   TextView(context).also {
                       it.text = HtmlCompat.fromHtml(
                           getString(text).replace("\n", "<br/>"),
                           HtmlCompat.FROM_HTML_MODE_COMPACT)
                   }
                }, modifier = Modifier.verticalScroll(rememberScrollState()))
            },
            buttons = buttons,
            onDismissRequest = { state.value = false }
        )
    }

}