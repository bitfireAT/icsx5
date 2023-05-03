/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.AlertDialog
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import at.bitfire.icsdroid.BuildConfig
import at.bitfire.icsdroid.Constants
import at.bitfire.icsdroid.R
import com.google.accompanist.themeadapter.material.MdcTheme
import com.mikepenz.aboutlibraries.ui.compose.LibrariesContainer

class InfoActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, LibsFragment())
                .commit()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.app_info_activity, menu)
        return true
    }

    fun showWebSite(item: MenuItem) {
        launchUri(Uri.parse("https://icsx5.bitfire.at/?pk_campaign=icsx5-app&pk_kwd=info-activity"))
    }

    fun showTwitter(item: MenuItem) {
        launchUri(Uri.parse("https://twitter.com/icsx5app"))
    }

    private fun launchUri(uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW, uri)
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.w(Constants.TAG, "No browser installed")
        }
    }

    class LibsFragment: Fragment() {
        @Composable
        fun TextDialog(@StringRes descriptionRes: Int, onDismissRequest: () -> Unit) {
            AlertDialog(
                onDismissRequest = onDismissRequest,
                confirmButton = {
                    TextButton(onClick = onDismissRequest) {
                        Text(stringResource(android.R.string.ok))
                    }
                },
                text = {
                    Text(
                        text = stringResource(descriptionRes),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            )
        }

        @Composable
        fun InfoHeader() {
            val context = LocalContext.current

            var showingLicenseDialog by remember { mutableStateOf(false) }
            if (showingLicenseDialog)
                TextDialog(R.string.app_info_gplv3_note) { showingLicenseDialog = false }

            var showingDonateDialog by remember { mutableStateOf(false) }
            if (showingDonateDialog)
                TextDialog(R.string.donate_message) { showingDonateDialog = false }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    bitmap = context.applicationInfo
                        .loadIcon(context.packageManager)
                        .toBitmap()
                        .asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(72.dp)
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
                    color = MaterialTheme.colors.onBackground.copy(
                        alpha = ContentAlpha.medium
                    )
                )

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { showingLicenseDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                    ) { Text(stringResource(R.string.app_info_gplv3)) }

                    if (BuildConfig.FLAVOR != "gplay")
                        OutlinedButton(
                            onClick = { showingDonateDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp)
                        ) { Text(stringResource(R.string.app_info_donate)) }
                }

                Text(
                    text = stringResource(R.string.app_info_description),
                    style = MaterialTheme.typography.subtitle2,
                    color = MaterialTheme.colors.onBackground.copy(
                        alpha = ContentAlpha.medium
                    )
                )
            }
        }

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View = ComposeView(requireContext()).apply {
            setContent {
                MdcTheme {
                    LibrariesContainer(
                        modifier = Modifier.fillMaxSize(),
                        header = {
                            item { InfoHeader() }
                        }
                    )
                }
            }
        }
    }
}
