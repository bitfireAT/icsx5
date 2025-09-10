/*
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 */

package at.bitfire.icsdroid

import android.content.Context
import at.bitfire.cert4android.CustomCertManager
import at.bitfire.icsdroid.ui.ForegroundTracker
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent
import okhttp3.brotli.BrotliInterceptor
import okhttp3.internal.tls.OkHostnameVerifier
import javax.net.ssl.SSLContext

class AppHttpClient @AssistedInject constructor(
    @Assisted customUserAgent: String?,
    @Assisted createEngine: (CustomCertManager, SSLContext) -> HttpClientEngine,
    @ApplicationContext context: Context
) {

    @AssistedFactory
    interface Factory {
        fun create(
            customUserAgent: String? = null,
            createEngine: (CustomCertManager, SSLContext) -> HttpClientEngine = { certManager, sslContext ->
                OkHttp.create {
                    addNetworkInterceptor(BrotliInterceptor)
                    config {
                        sslSocketFactory(sslContext.socketFactory, certManager)
                        hostnameVerifier(certManager.HostnameVerifier(OkHostnameVerifier))
                    }
                }
            }
        ): AppHttpClient
    }

    // CustomCertManager is Closeable, but HttpClient will live as long as the application is in memory,
    // so we don't need to close it
    private val certManager = CustomCertManager(
        context = context,
        appInForeground = ForegroundTracker.inForeground
    )

    private val sslContext = SSLContext.getInstance("TLS")
    init {
        sslContext.init(null, arrayOf(certManager), null)
    }

    val httpClient = HttpClient(createEngine(certManager, sslContext)) {
        // Add user given user agent to all engines
        install(UserAgent) {
            agent = customUserAgent ?: Constants.USER_AGENT
        }

        // Increase default timeouts
        install(HttpTimeout) {
            connectTimeoutMillis = 20_000
            requestTimeoutMillis = 60_000
            socketTimeoutMillis = 60_000
        }

        // Disable redirect following, it's handled by CalendarFetcher
        followRedirects = false
    }

}